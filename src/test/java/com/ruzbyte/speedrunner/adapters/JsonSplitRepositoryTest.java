package com.ruzbyte.speedrunner.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ruzbyte.speedrunner.core.PersonalBest;
import com.ruzbyte.speedrunner.core.Run;
import com.ruzbyte.speedrunner.core.Split;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for {@link JsonSplitRepository}. */
class JsonSplitRepositoryTest {

  private static final String GAME = "Sonic";
  private static final String CATEGORY = "Any%";

  @TempDir private Path tempDir;

  private Path seed(final String json) throws IOException {
    final Path file = tempDir.resolve("speedruns.json");
    Files.writeString(file, json);
    return file;
  }

  private static Run runWith(final long... splitSecondsFromStart) {
    final List<Split> splits = new java.util.ArrayList<>();
    int index = 1;
    for (final long seconds : splitSecondsFromStart) {
      splits.add(new Split("S" + index++, Instant.EPOCH.plusSeconds(seconds)));
    }
    return new Run(GAME, CATEGORY, splits, Instant.EPOCH);
  }

  @Test
  @DisplayName("loads a seeded personal best from the file")
  void loadsSeededPersonalBest() throws IOException {
    final Path file =
        seed(
            "{\"personalBests\":[{\"game\":\"Sonic\",\"category\":\"Any%\","
                + "\"splitNames\":[\"S1\",\"S2\"],"
                + "\"goldenSplits\":[\"PT30S\",\"PT45S\"],"
                + "\"bestTotal\":\"PT1M40S\"}],\"runs\":[]}");
    final JsonSplitRepository repository = new JsonSplitRepository(file);

    final PersonalBest pb = repository.load(GAME, CATEGORY);

    assertEquals(List.of(Duration.ofSeconds(30L), Duration.ofSeconds(45L)), pb.goldenSplits());
    assertEquals(List.of("S1", "S2"), pb.splitNames());
  }

  @Test
  @DisplayName("loads an unknown route as a layout-less personal best")
  void loadsUnknownRouteAsLayoutless() throws IOException {
    final Path file = seed("{\"personalBests\":[],\"runs\":[]}");
    final JsonSplitRepository repository = new JsonSplitRepository(file);

    final PersonalBest pb = repository.load(GAME, "Unknown");

    assertTrue(pb.splitNames().isEmpty());
  }

  @Test
  @DisplayName("loads a layout-less personal best when the file does not exist")
  void loadsLayoutlessWhenFileMissing() {
    final JsonSplitRepository repository =
        new JsonSplitRepository(tempDir.resolve("does-not-exist.json"));

    assertTrue(repository.load(GAME, CATEGORY).splitNames().isEmpty());
  }

  @Test
  @DisplayName("lists the configured routes")
  void listsConfiguredRoutes() throws IOException {
    final Path file =
        seed(
            "{\"personalBests\":["
                + "{\"game\":\"Sonic\",\"category\":\"Any%\",\"splitNames\":[\"S1\"],"
                + "\"goldenSplits\":[\"PT0S\"],\"bestTotal\":\"PT0S\"},"
                + "{\"game\":\"Mario 64\",\"category\":\"120 Star\",\"splitNames\":[\"S1\"],"
                + "\"goldenSplits\":[\"PT0S\"],\"bestTotal\":\"PT0S\"}],\"runs\":[]}");
    final JsonSplitRepository repository = new JsonSplitRepository(file);

    final List<PersonalBest> routes = repository.layouts();

    assertEquals(2, routes.size());
    assertEquals("Any%", routes.get(0).category());
    assertEquals("120 Star", routes.get(1).category());
  }

  @Test
  @DisplayName("persists a configured layout so it survives a reload")
  void saveLayoutSurvivesReload() {
    final Path file = tempDir.resolve("new.json");
    final PersonalBest layout =
        new PersonalBest(GAME, CATEGORY, List.of("S1", "S2"), List.of(), Duration.ZERO);
    new JsonSplitRepository(file).saveLayout(layout);

    final PersonalBest reloaded = new JsonSplitRepository(file).load(GAME, CATEGORY);

    assertEquals(List.of("S1", "S2"), reloaded.splitNames());
  }

  @Test
  @DisplayName("saving a run improves the persisted personal best, surviving a reload")
  void saveImprovesPersonalBestAcrossReload() throws IOException {
    final Path file =
        seed(
            "{\"personalBests\":[{\"game\":\"Sonic\",\"category\":\"Any%\","
                + "\"splitNames\":[\"S1\",\"S2\"],"
                + "\"goldenSplits\":[\"PT0S\",\"PT0S\"],\"bestTotal\":\"PT0S\"}],\"runs\":[]}");
    new JsonSplitRepository(file).save(runWith(25L, 75L));

    final PersonalBest reloaded = new JsonSplitRepository(file).load(GAME, CATEGORY);

    assertEquals(
        List.of(Duration.ofSeconds(25L), Duration.ofSeconds(50L)), reloaded.goldenSplits());
  }

  @Test
  @DisplayName("saving a run persists its best total, surviving a reload")
  void savePersistsBestTotalAcrossReload() throws IOException {
    final Path file =
        seed(
            "{\"personalBests\":[{\"game\":\"Sonic\",\"category\":\"Any%\","
                + "\"splitNames\":[\"S1\",\"S2\"],"
                + "\"goldenSplits\":[\"PT0S\",\"PT0S\"],\"bestTotal\":\"PT0S\"}],\"runs\":[]}");
    new JsonSplitRepository(file).save(runWith(25L, 75L));

    final PersonalBest reloaded = new JsonSplitRepository(file).load(GAME, CATEGORY);

    assertEquals(Duration.ofSeconds(75L), reloaded.bestTotal());
  }
}
