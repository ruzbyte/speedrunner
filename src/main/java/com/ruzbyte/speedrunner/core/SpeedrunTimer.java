package com.ruzbyte.speedrunner.core;

import com.ruzbyte.speedrunner.ports.Clock;
import com.ruzbyte.speedrunner.ports.SplitRepository;
import com.ruzbyte.speedrunner.ports.TimerListener;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Context of the State pattern: holds the in-progress run data (splits, start instant, accumulated
 * pause) and delegates each command to the current {@link TimerState}, taking the timestamp once
 * and passing it through.
 *
 * <p>Each state holds a back-reference to this orchestrator and is created fresh on transition; the
 * states hold no run data of their own and reach the repository and listeners only through the
 * package-private helpers here ({@code save}, {@code fireXxx}, …). The immutable {@link Run} is
 * built once in {@code FinishedState.entry}. Clock, repository and calculator are injected through
 * the constructor (manual DI from {@code Main}; no singleton).
 *
 * <p>The current state is the single source of truth for whether a run exists: the run fields are
 * re-initialised on every {@code start} and are only read while a run is active, so no null
 * sentinels are used to mark "no run".
 */
public final class SpeedrunTimer {

  private final String gameName;
  private final String categoryName;
  private final Clock clock;
  private final SplitRepository repository;
  private final SplitCalculator calculator;
  private final List<TimerListener> listeners = new ArrayList<>();
  private final List<Split> recordedSplits = new ArrayList<>();

  private TimerState currentState;
  private PersonalBest reference;
  private Instant startInstant;
  private Duration pauseAccumulated = Duration.ZERO;
  private Instant pauseStart;

  /**
   * Creates a timer for a single route (game and category) with its collaborators injected.
   *
   * @param game the game; must not be {@code null} or blank
   * @param category the speedrun category; must not be {@code null} or blank
   * @param clock the time source; must not be {@code null}
   * @param repository the persistence port; must not be {@code null}
   * @param calculator the comparison calculator; must not be {@code null}
   */
  public SpeedrunTimer(
      final String game,
      final String category,
      final Clock clock,
      final SplitRepository repository,
      final SplitCalculator calculator) {
    if (game == null || game.isBlank()) {
      throw new IllegalArgumentException("game must not be blank");
    }
    if (category == null || category.isBlank()) {
      throw new IllegalArgumentException("category must not be blank");
    }
    this.gameName = game;
    this.categoryName = category;
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
    this.repository = Objects.requireNonNull(repository, "repository must not be null");
    this.calculator = Objects.requireNonNull(calculator, "calculator must not be null");
    this.currentState = new IdleState(this);
  }

  /** Starts a run. */
  public void start() {
    currentState.start(clock.now());
  }

  /** Records a split (and auto-finishes when the segment count is reached). */
  public void split() {
    currentState.split(clock.now());
  }

  /** Pauses the run. */
  public void pause() {
    currentState.pause(clock.now());
  }

  /** Resumes a paused run. */
  public void resume() {
    currentState.resume(clock.now());
  }

  /** Resets the timer to idle, discarding the in-progress run. */
  public void reset() {
    currentState.reset(clock.now());
  }

  /**
   * Registers a listener for timer events.
   *
   * @param listener the listener; must not be {@code null}
   */
  public void addListener(final TimerListener listener) {
    listeners.add(Objects.requireNonNull(listener, "listener must not be null"));
  }

  /**
   * Compares the current run against the personal best for the category, using the active strategy.
   *
   * @return the signed delta {@code current − reference}; negative means ahead
   * @throws IllegalStateException if no run has been started
   */
  public Duration compareToPersonalBest() {
    if (currentState instanceof IdleState) {
      throw new IllegalStateException("no run to compare against a personal best");
    }
    return calculator.compareAgainst(buildRun(), reference);
  }

  /**
   * Returns the elapsed run time, with paused time excluded: {@link Duration#ZERO} while idle, the
   * live time while running, the frozen time while paused, and the final total once finished.
   *
   * @return the pause-adjusted elapsed time for the current state
   */
  public Duration elapsed() {
    if (currentState instanceof IdleState) {
      return Duration.ZERO;
    }
    if (currentState instanceof FinishedState) {
      return buildRun().totalTime();
    }
    final Instant base = (currentState instanceof PausedState) ? pauseStart : clock.now();
    return Duration.between(startInstant, base.minus(pauseAccumulated));
  }

  /**
   * Returns the game this timer runs.
   *
   * @return the game
   */
  public String game() {
    return gameName;
  }

  /**
   * Returns the category this timer runs.
   *
   * @return the category
   */
  public String category() {
    return categoryName;
  }

  /**
   * Returns an immutable snapshot of the splits recorded so far.
   *
   * @return the recorded splits
   */
  public List<Split> splits() {
    return List.copyOf(recordedSplits);
  }

  // ---- package-private helpers used by the states ----

  TimerState state() {
    return currentState;
  }

  void setState(final TimerState newState, final Instant now) {
    currentState.exit(now);
    currentState = newState;
    newState.entry(now);
  }

  PersonalBest loadReference() {
    reference = repository.load(gameName, categoryName);
    return reference;
  }

  int expectedSegments() {
    return reference.splitNames().size();
  }

  Duration accumulatedPause() {
    return pauseAccumulated;
  }

  void startRun(final Instant now) {
    recordedSplits.clear();
    startInstant = now;
    pauseAccumulated = Duration.ZERO;
  }

  String nextSplitName() {
    return reference.splitNames().get(recordedSplits.size());
  }

  void addSplit(final Split split) {
    recordedSplits.add(split);
  }

  int splitCount() {
    return recordedSplits.size();
  }

  void beginPause(final Instant now) {
    pauseStart = now;
  }

  void endPause(final Instant now) {
    pauseAccumulated = pauseAccumulated.plus(Duration.between(pauseStart, now));
  }

  void clearRun() {
    recordedSplits.clear();
    pauseAccumulated = Duration.ZERO;
  }

  Run buildRun() {
    return new Run(gameName, categoryName, recordedSplits, startInstant);
  }

  void save(final Run run) {
    repository.save(run);
  }

  void fireStart(final Instant now) {
    for (final TimerListener listener : listeners) {
      listener.onStart(now);
    }
  }

  void fireSplit(final Split split) {
    for (final TimerListener listener : listeners) {
      listener.onSplit(split);
    }
  }

  void firePause(final Instant now) {
    for (final TimerListener listener : listeners) {
      listener.onPause(now);
    }
  }

  void fireResume(final Instant now) {
    for (final TimerListener listener : listeners) {
      listener.onResume(now);
    }
  }

  void fireFinish(final Run run) {
    for (final TimerListener listener : listeners) {
      listener.onFinish(run);
    }
  }

  void fireReset() {
    for (final TimerListener listener : listeners) {
      listener.onReset();
    }
  }
}
