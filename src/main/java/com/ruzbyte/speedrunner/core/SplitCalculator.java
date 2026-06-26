package com.ruzbyte.speedrunner.core;

import java.time.Duration;
import java.util.Objects;

/**
 * Applies the active {@link CompareStrategy} to compare a run against a personal best. The strategy
 * is swappable at runtime ({@link #setStrategy}), which is how the comparison mode (PB / Sum of
 * Best / Average) is changed while the application runs.
 */
public final class SplitCalculator {

  private CompareStrategy strategy;

  /**
   * Creates a calculator with the initial comparison strategy.
   *
   * @param strategy the initial strategy; must not be {@code null}
   */
  public SplitCalculator(final CompareStrategy strategy) {
    this.strategy = Objects.requireNonNull(strategy, "strategy must not be null");
  }

  /**
   * Swaps the active comparison strategy.
   *
   * @param newStrategy the strategy to use from now on; must not be {@code null}
   */
  public void setStrategy(final CompareStrategy newStrategy) {
    this.strategy = Objects.requireNonNull(newStrategy, "strategy must not be null");
  }

  /**
   * Compares a run against a personal best using the active strategy.
   *
   * @param run the current run; must not be {@code null}
   * @param personalBest the personal-best reference; must not be {@code null}
   * @return the signed delta {@code run − reference}; negative means ahead
   */
  public Duration compareAgainst(final Run run, final PersonalBest personalBest) {
    Objects.requireNonNull(run, "run must not be null");
    Objects.requireNonNull(personalBest, "personalBest must not be null");
    return strategy.compare(run, personalBest);
  }
}
