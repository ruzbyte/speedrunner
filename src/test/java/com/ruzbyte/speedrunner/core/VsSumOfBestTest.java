package com.ruzbyte.speedrunner.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link VsSumOfBest}. */
class VsSumOfBestTest {

  private static final Instant START = Instant.EPOCH;
  private final CompareStrategy strategy = new VsSumOfBest();

  private static Run runOfSeconds(final long totalSeconds) {
    final Split end = new Split("End", START.plusSeconds(totalSeconds));
    return new Run("Sonic", "Any%", List.of(end), START);
  }

  private static PersonalBest pbWithGolden(final long... goldenSeconds) {
    final List<Duration> golden =
        java.util.Arrays.stream(goldenSeconds).mapToObj(Duration::ofSeconds).toList();
    return new PersonalBest("Sonic", "Any%", List.of(), golden, Duration.ofMinutes(99L));
  }

  @Test
  @DisplayName("compares the run total against the summed golden splits")
  void comparesAgainstSumOfGolden() {
    final Duration delta = strategy.compare(runOfSeconds(70L), pbWithGolden(30L, 45L));

    assertEquals(Duration.ofSeconds(-5L), delta);
  }

  @Test
  @DisplayName("treats an empty golden set as a zero reference")
  void emptyGoldenIsZeroReference() {
    final Duration delta = strategy.compare(runOfSeconds(70L), pbWithGolden());

    assertEquals(Duration.ofSeconds(70L), delta);
  }
}
