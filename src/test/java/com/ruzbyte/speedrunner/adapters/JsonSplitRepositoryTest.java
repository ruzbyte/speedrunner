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
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for {@link JsonSplitRepository}. */
class JsonSplitRepositoryTest {

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
    return new Run("Any%", splits, Instant.EPOCH);
  }

  @Test
  @DisplayName("loads a seeded personal best from the file")
  void loadsSeededPersonalBest() throws IOException {
    final Path file =
        seed(
            "{\"personalBests\":{\"Any%\":{\"category\":\"Any%\","
                + "\"goldenSplits\":[\"PT30S\",\"PT45S\"],\"bestTotal\":\"PT1M40S\"}},\"runs\":[]}");
    final JsonSplitRepository repository = new JsonSplitRepository(file);

    final PersonalBest pb = repository.load("Any%");

    assertEquals(List.of(Duration.ofSeconds(30L), Duration.ofSeconds(45L)), pb.goldenSplits());
  }

  @Test
  @DisplayName("loads a missing category as a layout-less personal best")
  void loadsMissingCategoryAsLayoutless() throws IOException {
    final Path file = seed("{\"personalBests\":{},\"runs\":[]}");
    final JsonSplitRepository repository = new JsonSplitRepository(file);

    final PersonalBest pb = repository.load("Unknown");

    assertTrue(pb.goldenSplits().isEmpty());
  }

  @Test
  @DisplayName("loads a layout-less personal best when the file does not exist")
  void loadsLayoutlessWhenFileMissing() {
    final JsonSplitRepository repository =
        new JsonSplitRepository(tempDir.resolve("does-not-exist.json"));

    assertTrue(repository.load("Any%").goldenSplits().isEmpty());
  }

  @Test
  @DisplayName("lists the seeded categories")
  void listsSeededCategories() throws IOException {
    final Path file =
        seed(
            "{\"personalBests\":{"
                + "\"Any%\":{\"category\":\"Any%\",\"goldenSplits\":[\"PT0S\"],\"bestTotal\":\"PT0S\"},"
                + "\"120 Star\":{\"category\":\"120 Star\",\"goldenSplits\":[\"PT0S\"],"
                + "\"bestTotal\":\"PT0S\"}},\"runs\":[]}");
    final JsonSplitRepository repository = new JsonSplitRepository(file);

    assertEquals(Set.of("Any%", "120 Star"), repository.categories());
  }

  @Test
  @DisplayName("saving a run improves the persisted personal best, surviving a reload")
  void saveImprovesPersonalBestAcrossReload() throws IOException {
    final Path file =
        seed(
            "{\"personalBests\":{\"Any%\":{\"category\":\"Any%\","
                + "\"goldenSplits\":[\"PT0S\",\"PT0S\"],\"bestTotal\":\"PT0S\"}},\"runs\":[]}");
    new JsonSplitRepository(file).save(runWith(25L, 75L));

    final PersonalBest reloaded = new JsonSplitRepository(file).load("Any%");

    assertEquals(
        List.of(Duration.ofSeconds(25L), Duration.ofSeconds(50L)), reloaded.goldenSplits());
  }

  @Test
  @DisplayName("saving a run persists its best total, surviving a reload")
  void savePersistsBestTotalAcrossReload() throws IOException {
    final Path file =
        seed(
            "{\"personalBests\":{\"Any%\":{\"category\":\"Any%\","
                + "\"goldenSplits\":[\"PT0S\",\"PT0S\"],\"bestTotal\":\"PT0S\"}},\"runs\":[]}");
    new JsonSplitRepository(file).save(runWith(25L, 75L));

    final PersonalBest reloaded = new JsonSplitRepository(file).load("Any%");

    assertEquals(Duration.ofSeconds(75L), reloaded.bestTotal());
  }
}
