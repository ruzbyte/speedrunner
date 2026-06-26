package com.ruzbyte.speedrunner.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link VsAverage}. */
class VsAverageTest {

  private static final Instant START = Instant.EPOCH;
  private final CompareStrategy strategy = new VsAverage();

  private static Run runWithSegments(final int segments, final long totalSeconds) {
    final java.util.List<Split> splits = new java.util.ArrayList<>();
    for (int i = 1; i <= segments; i++) {
      // Segment timestamps are irrelevant to the average reference except the last (the total);
      // place intermediate splits before the end and the final split at the total.
      final long at = (i < segments) ? i : totalSeconds;
      splits.add(new Split("S" + i, START.plusSeconds(at)));
    }
    return new Run("Any%", splits, START);
  }

  @Test
  @DisplayName("compares the run total against the mean golden segment times the segment count")
  void comparesAgainstMeanGoldenTimesSegments() {
    // Golden mean = (30 + 45) / 2 = 37.5s; over 2 segments the reference total is 75s.
    final PersonalBest pb =
        new PersonalBest(
            "Any%",
            List.of(Duration.ofSeconds(30L), Duration.ofSeconds(45L)), Duration.ofMinutes(9L));

    final Duration delta = strategy.compare(runWithSegments(2, 80L), pb);

    assertEquals(Duration.ofSeconds(5L), delta);
  }

  @Test
  @DisplayName("returns the run total when there is no golden data to average")
  void noGoldenReturnsRunTotal() {
    final PersonalBest pb = new PersonalBest("Any%", List.of(), Duration.ofMinutes(9L));

    final Duration delta = strategy.compare(runWithSegments(2, 80L), pb);

    assertEquals(Duration.ofSeconds(80L), delta);
  }

  @Test
  @DisplayName("returns zero when the run has no segments")
  void noSegmentsReturnsZero() {
    final PersonalBest pb =
        new PersonalBest("Any%", List.of(Duration.ofSeconds(30L)), Duration.ofMinutes(9L));
    final Run empty = new Run("Any%", List.of(), START);

    final Duration delta = strategy.compare(empty, pb);

    assertEquals(Duration.ZERO, delta);
  }
}
