package com.ruzbyte.speedrunner.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link PersonalBest}. */
class PersonalBestTest {

  private static final Duration BEST_TOTAL = Duration.ofMinutes(12);

  private static List<Duration> goldenSplits() {
    return new ArrayList<>(List.of(Duration.ofSeconds(30), Duration.ofSeconds(45)));
  }

  @Test
  @DisplayName("exposes the best total it was built with")
  void exposesBestTotal() {
    final PersonalBest pb = new PersonalBest("Any%", goldenSplits(), BEST_TOTAL);

    assertEquals(BEST_TOTAL, pb.bestTotal());
  }

  @Test
  @DisplayName("is unaffected by later mutation of the source golden-split list")
  void copiesGoldenSplitsDefensively() {
    final List<Duration> source = goldenSplits();
    final PersonalBest pb = new PersonalBest("Any%", source, BEST_TOTAL);

    source.add(Duration.ofSeconds(99));

    assertEquals(2, pb.goldenSplits().size());
  }

  @Test
  @DisplayName("exposes an immutable golden-split list")
  void exposesImmutableGoldenSplits() {
    final PersonalBest pb = new PersonalBest("Any%", goldenSplits(), BEST_TOTAL);

    assertThrows(
        UnsupportedOperationException.class, () -> pb.goldenSplits().add(Duration.ofSeconds(1)));
  }

  @Test
  @DisplayName("rejects a blank category")
  void rejectsBlankCategory() {
    assertThrows(
        IllegalArgumentException.class, () -> new PersonalBest("", goldenSplits(), BEST_TOTAL));
  }

  @Test
  @DisplayName("rejects null golden splits")
  void rejectsNullGoldenSplits() {
    assertThrows(NullPointerException.class, () -> new PersonalBest("Any%", null, BEST_TOTAL));
  }

  @Test
  @DisplayName("rejects a null best total")
  void rejectsNullBestTotal() {
    assertThrows(NullPointerException.class, () -> new PersonalBest("Any%", goldenSplits(), null));
  }
}
