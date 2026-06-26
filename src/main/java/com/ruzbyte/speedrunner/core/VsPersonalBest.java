package com.ruzbyte.speedrunner.core;

import java.time.Duration;

/**
 * Comparison against the personal-best total time: how far the current run's total is ahead of or
 * behind the best completed run ({@link PersonalBest#bestTotal()}).
 */
public final class VsPersonalBest implements CompareStrategy {

  @Override
  public Duration compare(final Run current, final PersonalBest reference) {
    return current.totalTime().minus(reference.bestTotal());
  }
}
