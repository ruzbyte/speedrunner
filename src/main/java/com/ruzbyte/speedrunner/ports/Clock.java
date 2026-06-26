package com.ruzbyte.speedrunner.ports;

import java.time.Instant;

/**
 * Port abstracting the source of the current time, so time-dependent logic is deterministically
 * testable: production wires a real system clock, tests wire a fixed test clock.
 */
@FunctionalInterface
public interface Clock {

  /**
   * Returns the current instant from this clock.
   *
   * @return the current instant
   */
  Instant now();
}
