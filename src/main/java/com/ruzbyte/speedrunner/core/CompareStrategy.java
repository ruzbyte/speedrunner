package com.ruzbyte.speedrunner.core;

import java.time.Duration;

/**
 * Strategy for comparing a run against a personal-best reference. Implementations are pure domain
 * policy (no external dependencies) and are swapped at runtime through {@link SplitCalculator}.
 *
 * <p>The returned delta is {@code current − reference}: a negative duration means the current run
 * is ahead (faster) of the reference, a positive duration means it is behind (slower).
 */
@FunctionalInterface
public interface CompareStrategy {

  /**
   * Compares the current run against the reference and returns the signed delta.
   *
   * @param current the current run
   * @param reference the personal-best reference for the category
   * @return the signed difference {@code current − reference}; negative means ahead
   */
  Duration compare(Run current, PersonalBest reference);
}
