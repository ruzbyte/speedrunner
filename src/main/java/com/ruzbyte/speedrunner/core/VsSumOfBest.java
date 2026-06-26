package com.ruzbyte.speedrunner.core;

import java.time.Duration;

/**
 * Comparison against the sum of best (golden) splits: the theoretical best run in which every
 * segment is run at its personal-best pace. The reference total is the sum of {@link
 * PersonalBest#goldenSplits()}.
 */
public final class VsSumOfBest implements CompareStrategy {

  @Override
  public Duration compare(final Run current, final PersonalBest reference) {
    Duration sumOfBest = Duration.ZERO;
    for (final Duration golden : reference.goldenSplits()) {
      sumOfBest = sumOfBest.plus(golden);
    }
    return current.totalTime().minus(sumOfBest);
  }
}
