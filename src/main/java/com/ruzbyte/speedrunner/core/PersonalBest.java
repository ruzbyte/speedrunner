package com.ruzbyte.speedrunner.core;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * An immutable personal-best reference for a category: the golden (best-ever) split durations and
 * the best total time. Loaded by the repository and used by the comparison strategies.
 *
 * @param category the speedrun category; must not be {@code null} or blank
 * @param goldenSplits the best-ever duration of each segment; must not be {@code null}, defensively
 *     copied to an immutable list
 * @param bestTotal the best completed total time; must not be {@code null}
 */
public record PersonalBest(String category, List<Duration> goldenSplits, Duration bestTotal) {

  /**
   * Validates the components and stores an immutable copy of the golden splits.
   *
   * @throws NullPointerException if {@code goldenSplits} or {@code bestTotal} is {@code null}
   * @throws IllegalArgumentException if {@code category} is {@code null} or blank
   */
  public PersonalBest {
    Objects.requireNonNull(bestTotal, "bestTotal must not be null");
    Objects.requireNonNull(goldenSplits, "goldenSplits must not be null");
    if (category == null || category.isBlank()) {
      throw new IllegalArgumentException("category must not be blank");
    }
    goldenSplits = List.copyOf(goldenSplits);
  }
}
