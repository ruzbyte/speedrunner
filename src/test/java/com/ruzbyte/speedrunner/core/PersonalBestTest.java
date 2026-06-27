package com.ruzbyte.speedrunner.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link PersonalBest}. */
class PersonalBestTest {

  private static final String GAME = "Sonic";
  private static final String CATEGORY = "Any%";
  private static final List<String> NAMES = List.of("S1", "S2");
  private static final Duration BEST_TOTAL = Duration.ofMinutes(12);

  private static List<Duration> goldenSplits() {
    return new ArrayList<>(List.of(Duration.ofSeconds(30), Duration.ofSeconds(45)));
  }

  @Test
  @DisplayName("exposes the route and best total it was built with")
  void exposesComponents() {
    final PersonalBest pb = new PersonalBest(GAME, CATEGORY, NAMES, goldenSplits(), BEST_TOTAL);

    assertEquals(GAME, pb.game());
    assertEquals(CATEGORY, pb.category());
    assertEquals(NAMES, pb.splitNames());
    assertEquals(BEST_TOTAL, pb.bestTotal());
  }

  @Test
  @DisplayName("is unaffected by later mutation of the source golden-split list")
  void copiesGoldenSplitsDefensively() {
    final List<Duration> source = goldenSplits();
    final PersonalBest pb = new PersonalBest(GAME, CATEGORY, NAMES, source, BEST_TOTAL);

    source.add(Duration.ofSeconds(99));

    assertEquals(2, pb.goldenSplits().size());
  }

  @Test
  @DisplayName("is unaffected by later mutation of the source split-name list")
  void copiesSplitNamesDefensively() {
    final List<String> source = new ArrayList<>(List.of("S1", "S2"));
    final PersonalBest pb = new PersonalBest(GAME, CATEGORY, source, goldenSplits(), BEST_TOTAL);

    source.add("S3");

    assertEquals(2, pb.splitNames().size());
  }

  @Test
  @DisplayName("exposes an immutable golden-split list")
  void exposesImmutableGoldenSplits() {
    final PersonalBest pb = new PersonalBest(GAME, CATEGORY, NAMES, goldenSplits(), BEST_TOTAL);

    assertThrows(
        UnsupportedOperationException.class, () -> pb.goldenSplits().add(Duration.ofSeconds(1)));
  }

  @Test
  @DisplayName("rejects a blank game")
  void rejectsBlankGame() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PersonalBest(" ", CATEGORY, NAMES, goldenSplits(), BEST_TOTAL));
  }

  @Test
  @DisplayName("rejects a blank category")
  void rejectsBlankCategory() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PersonalBest(GAME, "", NAMES, goldenSplits(), BEST_TOTAL));
  }

  @Test
  @DisplayName("rejects null split names")
  void rejectsNullSplitNames() {
    assertThrows(
        NullPointerException.class,
        () -> new PersonalBest(GAME, CATEGORY, null, goldenSplits(), BEST_TOTAL));
  }

  @Test
  @DisplayName("rejects null golden splits")
  void rejectsNullGoldenSplits() {
    assertThrows(
        NullPointerException.class,
        () -> new PersonalBest(GAME, CATEGORY, NAMES, null, BEST_TOTAL));
  }

  @Test
  @DisplayName("rejects a null best total")
  void rejectsNullBestTotal() {
    assertThrows(
        NullPointerException.class,
        () -> new PersonalBest(GAME, CATEGORY, NAMES, goldenSplits(), null));
  }

  /** Builds a run whose splits sit at the given cumulative second-offsets from the epoch start. */
  private static Run runWith(final long... splitSecondsFromStart) {
    final List<Split> splits = new ArrayList<>();
    int index = 1;
    for (final long seconds : splitSecondsFromStart) {
      splits.add(new Split("S" + index++, Instant.EPOCH.plusSeconds(seconds)));
    }
    return new Run(GAME, CATEGORY, splits, Instant.EPOCH);
  }

  @Test
  @DisplayName("keeps the faster of each golden split when merging a run")
  void improvedWithKeepsFasterGoldenSplits() {
    final PersonalBest pb =
        new PersonalBest(
            GAME,
            CATEGORY,
            NAMES,
            List.of(Duration.ofSeconds(30L), Duration.ofSeconds(45L)),
            Duration.ofSeconds(100L));

    final PersonalBest improved = pb.improvedWith(runWith(25L, 75L));

    assertEquals(
        List.of(Duration.ofSeconds(25L), Duration.ofSeconds(45L)), improved.goldenSplits());
  }

  @Test
  @DisplayName("carries the route definition over when merging a run")
  void improvedWithKeepsRoute() {
    final PersonalBest pb =
        new PersonalBest(
            GAME,
            CATEGORY,
            NAMES,
            List.of(Duration.ofSeconds(30L), Duration.ofSeconds(45L)),
            Duration.ofSeconds(100L));

    final PersonalBest improved = pb.improvedWith(runWith(25L, 75L));

    assertEquals(GAME, improved.game());
    assertEquals(CATEGORY, improved.category());
    assertEquals(NAMES, improved.splitNames());
  }

  @Test
  @DisplayName("keeps the faster total when merging a run")
  void improvedWithKeepsFasterTotal() {
    final PersonalBest pb =
        new PersonalBest(
            GAME,
            CATEGORY,
            NAMES,
            List.of(Duration.ofSeconds(30L), Duration.ofSeconds(45L)),
            Duration.ofSeconds(100L));

    final PersonalBest improved = pb.improvedWith(runWith(25L, 75L));

    assertEquals(Duration.ofSeconds(75L), improved.bestTotal());
  }

  @Test
  @DisplayName("treats a zero golden split as unset and adopts the run's segment")
  void improvedWithTreatsZeroAsUnset() {
    final PersonalBest pb =
        new PersonalBest(
            GAME, CATEGORY, NAMES, List.of(Duration.ZERO, Duration.ZERO), Duration.ZERO);

    final PersonalBest improved = pb.improvedWith(runWith(25L, 75L));

    assertEquals(
        List.of(Duration.ofSeconds(25L), Duration.ofSeconds(50L)), improved.goldenSplits());
  }

  @Test
  @DisplayName("adopts the run's segments when no golden layout was recorded yet")
  void improvedWithAdoptsRunOnSizeMismatch() {
    final PersonalBest pb = new PersonalBest(GAME, CATEGORY, NAMES, List.of(), Duration.ZERO);

    final PersonalBest improved = pb.improvedWith(runWith(25L, 75L));

    assertEquals(
        List.of(Duration.ofSeconds(25L), Duration.ofSeconds(50L)), improved.goldenSplits());
  }
}
