package com.ruzbyte.speedrunner.adapters;

import com.ruzbyte.speedrunner.ports.Clock;
import java.time.Instant;

/** Production {@link Clock} adapter backed by the real system time ({@link Instant#now()}). */
public final class SystemClock implements Clock {

  @Override
  public Instant now() {
    return Instant.now();
  }
}
