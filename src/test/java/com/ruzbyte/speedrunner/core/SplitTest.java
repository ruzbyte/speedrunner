package com.ruzbyte.speedrunner.core;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for {@link Split}. */
class SplitTest {

  @Test
  @DisplayName("exposes the name and timestamp it was built with")
  void exposesComponents() {
    final Instant timestamp = Instant.ofEpochMilli(1_000L);

    final Split split = new Split("Bridge of Eldin", timestamp);

    assertAll(
        () -> assertEquals("Bridge of Eldin", split.name()),
        () -> assertEquals(timestamp, split.timestamp()));
  }

  @ParameterizedTest
  @DisplayName("rejects a null or blank name")
  @ValueSource(strings = {"", " ", "\t"})
  void rejectsBlankName(final String name) {
    final Instant timestamp = Instant.ofEpochMilli(1_000L);

    assertThrows(IllegalArgumentException.class, () -> new Split(name, timestamp));
  }

  @Test
  @DisplayName("rejects a null timestamp")
  void rejectsNullTimestamp() {
    assertThrows(NullPointerException.class, () -> new Split("Bridge", null));
  }
}
