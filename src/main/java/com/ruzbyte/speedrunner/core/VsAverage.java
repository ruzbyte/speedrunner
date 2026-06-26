package com.ruzbyte.speedrunner.core;

import java.time.Duration;
import java.util.List;

/**
 * Comparison against an even-paced average reference: the mean golden segment duration projected
 * over the number of segments in the current run.
 *
 * <p>The reference total is {@code (sum(goldenSplits) / goldenSplits.size()) ×
 * currentSegmentCount}. When there is no golden data or no current segment, the reference total is
 * zero and the delta is the current total.
 */
public final class VsAverage implements CompareStrategy {

  @Override
  public Duration compare(final Run current, final PersonalBest reference) {
    final List<Duration> golden = reference.goldenSplits();
    final int segments = current.splits().size();
    if (golden.isEmpty() || segments == 0) {
      return current.totalTime();
    }
    Duration sumOfBest = Duration.ZERO;
    for (final Duration goldenSplit : golden) {
      sumOfBest = sumOfBest.plus(goldenSplit);
    }
    final Duration meanGolden = sumOfBest.dividedBy(golden.size());
    final Duration referenceTotal = meanGolden.multipliedBy(segments);
    return current.totalTime().minus(referenceTotal);
  }
}
