package com.ruzbyte.speedrunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Unit tests for {@link TimeFormat}. */
class TimeFormatTest {

  @Test
  @DisplayName("formats zero as 0:00:00.000")
  void formatsZero() {
    assertEquals("0:00:00.000", TimeFormat.format(0L));
  }

  @ParameterizedTest
  @DisplayName("formats representative durations")
  @CsvSource({
    "1, 0:00:00.001",
    "999, 0:00:00.999",
    "1000, 0:00:01.000",
    "61000, 0:01:01.000",
    "3661001, 1:01:01.001",
  })
  void formatsDurations(final long millis, final String expected) {
    assertEquals(expected, TimeFormat.format(millis));
  }

  @Test
  @DisplayName("rejects negative durations")
  void rejectsNegative() {
    assertThrows(IllegalArgumentException.class, () -> TimeFormat.format(-1L));
  }
}
