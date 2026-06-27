package com.ruzbyte.speedrunner.core;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An immutable, completed run: the ordered splits captured for one attempt of a category.
 *
 * <p>Constructed once when the run finishes (in {@code FinishedState.entry}) and then handed to the
 * repository; never mutated afterwards. The in-progress data lives in the {@code SpeedrunTimer}
 * context until that point.
 *
 * @param game the game the run belongs to; must not be {@code null} or blank
 * @param category the speedrun category; must not be {@code null} or blank
 * @param splits the ordered splits; must not be {@code null}, defensively copied to an immutable
 *     list
 * @param startInstant the absolute instant the run was started; must not be {@code null}
 */
public record Run(String game, String category, List<Split> splits, Instant startInstant) {

  /**
   * Validates the components and stores an immutable copy of the splits.
   *
   * @throws NullPointerException if {@code splits} or {@code startInstant} is {@code null}
   * @throws IllegalArgumentException if {@code game} or {@code category} is {@code null} or blank
   */
  public Run {
    Objects.requireNonNull(startInstant, "startInstant must not be null");
    Objects.requireNonNull(splits, "splits must not be null");
    if (game == null || game.isBlank()) {
      throw new IllegalArgumentException("game must not be blank");
    }
    if (category == null || category.isBlank()) {
      throw new IllegalArgumentException("category must not be blank");
    }
    splits = List.copyOf(splits);
  }

  /**
   * Returns the total elapsed time of the run: the last split's timestamp minus the start instant,
   * or {@link Duration#ZERO} if no splits were recorded.
   *
   * <p>This raw elapsed deliberately does not subtract pauses — pause is tracked on the {@code
   * SpeedrunTimer} context, not in the record (see the README "Notes"). The comparison strategies
   * build on this single, authoritative definition of a run's total.
   *
   * @return the total elapsed duration of the run
   */
  public Duration totalTime() {
    if (splits.isEmpty()) {
      return Duration.ZERO;
    }
    final Split last = splits.get(splits.size() - 1);
    return Duration.between(startInstant, last.timestamp());
  }

  /**
   * Returns the duration of each segment: the time between consecutive splits, with the first
   * segment measured from the start instant. Empty if no splits were recorded.
   *
   * @return the immutable list of segment durations, in order
   */
  public List<Duration> segments() {
    final List<Duration> result = new ArrayList<>(splits.size());
    Instant previous = startInstant;
    for (final Split split : splits) {
      result.add(Duration.between(previous, split.timestamp()));
      previous = split.timestamp();
    }
    return List.copyOf(result);
  }
}
