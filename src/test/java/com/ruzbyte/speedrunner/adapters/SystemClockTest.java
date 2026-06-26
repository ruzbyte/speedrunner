package com.ruzbyte.speedrunner.adapters;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link SystemClock}. */
class SystemClockTest {

  @Test
  @DisplayName("returns a non-null instant")
  void returnsNonNull() {
    assertNotNull(new SystemClock().now());
  }

  @Test
  @DisplayName("returns an instant close to the real current time")
  void returnsCurrentTime() {
    final Instant before = Instant.now();

    final Instant now = new SystemClock().now();

    final Instant after = Instant.now();
    assertTrue(
        !now.isBefore(before.minusSeconds(1L)) && !now.isAfter(after.plusSeconds(1L)),
        () -> "expected " + now + " between " + before + " and " + after);
    assertTrue(Duration.between(before, after).toMinutes() < 1L);
  }
}
