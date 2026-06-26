package com.ruzbyte.speedrunner.core;

import java.time.Duration;
import java.util.ArrayList;
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

  /**
   * Returns a new personal best merged with a completed run: each golden split becomes the faster
   * of the stored split and the run's matching segment, and the best total becomes the faster of
   * the two. A stored value of {@link Duration#ZERO} counts as "unset" and is always replaced. When
   * no golden layout is recorded yet (size mismatch), the run's segments are adopted as-is.
   *
   * @param run the completed run to merge in
   * @return a new, improved personal best (this instance is never mutated)
   */
  public PersonalBest improvedWith(final Run run) {
    final List<Duration> runSegments = run.segments();
    final List<Duration> mergedGolden;
    if (goldenSplits.size() == runSegments.size()) {
      mergedGolden = new ArrayList<>(goldenSplits.size());
      for (int i = 0; i < goldenSplits.size(); i++) {
        mergedGolden.add(faster(goldenSplits.get(i), runSegments.get(i)));
      }
    } else {
      mergedGolden = runSegments;
    }
    return new PersonalBest(category, mergedGolden, faster(bestTotal, run.totalTime()));
  }

  /**
   * Returns the faster of two durations, treating a zero {@code stored} value as "unset".
   *
   * @param stored the currently stored duration ({@link Duration#ZERO} means unset)
   * @param candidate the candidate duration from a new run
   * @return the duration to keep
   */
  private static Duration faster(final Duration stored, final Duration candidate) {
    if (stored.isZero()) {
      return candidate;
    }
    return stored.compareTo(candidate) <= 0 ? stored : candidate;
  }
}
