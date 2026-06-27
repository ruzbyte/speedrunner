package com.ruzbyte.speedrunner.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link VsPersonalBest}. */
class VsPersonalBestTest {

  private static final Instant START = Instant.EPOCH;
  private final CompareStrategy strategy = new VsPersonalBest();

  private static Run runOfSeconds(final long totalSeconds) {
    final Split end = new Split("End", START.plusSeconds(totalSeconds));
    return new Run("Sonic", "Any%", List.of(end), START);
  }

  private static PersonalBest pbOfMinutes(final long bestMinutes) {
    return new PersonalBest("Sonic", "Any%", List.of(), List.of(), Duration.ofMinutes(bestMinutes));
  }

  @Test
  @DisplayName("reports a negative delta when the run is ahead of the personal best")
  void aheadIsNegative() {
    final Duration delta = strategy.compare(runOfSeconds(600L), pbOfMinutes(12L));

    assertEquals(Duration.ofMinutes(-2L), delta);
  }

  @Test
  @DisplayName("reports a positive delta when the run is behind the personal best")
  void behindIsPositive() {
    final Duration delta = strategy.compare(runOfSeconds(900L), pbOfMinutes(12L));

    assertEquals(Duration.ofMinutes(3L), delta);
  }

  @Test
  @DisplayName("reports a zero delta when the run matches the personal best")
  void equalIsZero() {
    final Duration delta = strategy.compare(runOfSeconds(720L), pbOfMinutes(12L));

    assertEquals(Duration.ZERO, delta);
  }
}
