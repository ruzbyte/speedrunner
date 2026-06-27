package com.ruzbyte.speedrunner.core;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An immutable per-route reference: the route definition (the game, the category and the ordered
 * names of its splits) together with the achieved bests (the golden split durations and the best
 * total time). Loaded by the repository and used by the comparison strategies.
 *
 * <p>The split names define the route's layout; {@code goldenSplits} align to them by index, so
 * {@code goldenSplits.get(i)} is the best-ever duration of the segment named {@code
 * splitNames.get(i)}. A freshly configured route carries its split names with no times yet (empty
 * golden splits and a zero best total) until the first run is completed.
 *
 * @param game the game the route belongs to; must not be {@code null} or blank
 * @param category the speedrun category; must not be {@code null} or blank
 * @param splitNames the ordered split names defining the route; must not be {@code null},
 *     defensively copied to an immutable list
 * @param goldenSplits the best-ever duration of each segment; must not be {@code null}, defensively
 *     copied to an immutable list
 * @param bestTotal the best completed total time; must not be {@code null}
 */
public record PersonalBest(
    String game,
    String category,
    List<String> splitNames,
    List<Duration> goldenSplits,
    Duration bestTotal) {

  /**
   * Validates the components and stores immutable copies of the split names and golden splits.
   *
   * @throws NullPointerException if {@code splitNames}, {@code goldenSplits} or {@code bestTotal}
   *     is {@code null}
   * @throws IllegalArgumentException if {@code game} or {@code category} is {@code null} or blank
   */
  public PersonalBest {
    Objects.requireNonNull(bestTotal, "bestTotal must not be null");
    Objects.requireNonNull(goldenSplits, "goldenSplits must not be null");
    Objects.requireNonNull(splitNames, "splitNames must not be null");
    if (game == null || game.isBlank()) {
      throw new IllegalArgumentException("game must not be blank");
    }
    if (category == null || category.isBlank()) {
      throw new IllegalArgumentException("category must not be blank");
    }
    splitNames = List.copyOf(splitNames);
    goldenSplits = List.copyOf(goldenSplits);
  }

  /**
   * Returns a new personal best merged with a completed run: each golden split becomes the faster
   * of the stored split and the run's matching segment, and the best total becomes the faster of
   * the two. A stored value of {@link Duration#ZERO} counts as "unset" and is always replaced. When
   * no golden layout is recorded yet (size mismatch), the run's segments are adopted as-is. The
   * route definition (game, category, split names) is carried over unchanged.
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
    return new PersonalBest(
        game, category, splitNames, mergedGolden, faster(bestTotal, run.totalTime()));
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
