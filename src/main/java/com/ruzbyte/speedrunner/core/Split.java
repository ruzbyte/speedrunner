package com.ruzbyte.speedrunner.core;

import java.time.Instant;
import java.util.Objects;

/**
 * An immutable split: a named checkpoint captured at an absolute instant.
 *
 * <p>Absolute timestamps (not summed deltas) are stored so that elapsed times are derived by
 * subtraction, avoiding accumulation drift over long runs.
 *
 * @param name the split label; must not be {@code null} or blank
 * @param timestamp the absolute instant at which the split was taken; must not be {@code null}
 */
public record Split(String name, Instant timestamp) {

  /**
   * Validates the split components.
   *
   * @throws NullPointerException if {@code timestamp} is {@code null}
   * @throws IllegalArgumentException if {@code name} is {@code null} or blank
   */
  public Split {
    Objects.requireNonNull(timestamp, "timestamp must not be null");
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("split name must not be blank");
    }
  }
}
