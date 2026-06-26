package com.ruzbyte.speedrunner.core;

import java.time.Instant;

/**
 * Abstract base of the State pattern. Each concrete state holds a back-reference to its
 * orchestrator {@link SpeedrunTimer} (passed via the constructor) and is created fresh on each
 * transition ({@code timer.setState(new RunningState(timer), now)}).
 *
 * <p>States hold <strong>no run data</strong> — that lives in the orchestrator; their only field is
 * the orchestrator reference. They reach the repository and listeners only through the
 * orchestrator's package-private helpers ({@code timer.save(...)}, {@code timer.fireFinish(...)},
 * …).
 *
 * <p>The five command methods are abstract so each state declares explicitly what it does for every
 * command; illegal commands throw {@link IllegalStateException} via {@link #reject(String)}. The
 * timestamp {@code now} is passed through to every method and never re-read from the clock inside a
 * state. The {@link #entry(Instant)} / {@link #exit(Instant)} hooks default to no side effect and
 * are overridden where a transition has one.
 */
public abstract class TimerState {

  /** The orchestrator this state acts upon. */
  protected final SpeedrunTimer timer;

  /**
   * Binds the state to its orchestrator. Only the orchestrator (a {@code final} class that
   * validates its own inputs) constructs states, always passing a non-null reference.
   *
   * @param timer the orchestrator
   */
  protected TimerState(final SpeedrunTimer timer) {
    this.timer = timer;
  }

  /**
   * Handles the {@code start} command in this state.
   *
   * @param now the timestamp of the command, taken once by the orchestrator
   */
  public abstract void start(Instant now);

  /**
   * Handles the {@code split} command in this state.
   *
   * @param now the timestamp of the command, taken once by the orchestrator
   */
  public abstract void split(Instant now);

  /**
   * Handles the {@code pause} command in this state.
   *
   * @param now the timestamp of the command, taken once by the orchestrator
   */
  public abstract void pause(Instant now);

  /**
   * Handles the {@code resume} command in this state.
   *
   * @param now the timestamp of the command, taken once by the orchestrator
   */
  public abstract void resume(Instant now);

  /**
   * Handles the {@code reset} command in this state.
   *
   * @param now the timestamp of the command, taken once by the orchestrator
   */
  public abstract void reset(Instant now);

  /**
   * Side effect performed when this state is entered. Defaults to nothing.
   *
   * @param now the timestamp of the transition
   */
  public void entry(final Instant now) {
    // No side effect by default; a state overrides this when being entered has one
    // (e.g. FinishedState builds and saves the run).
  }

  /**
   * Side effect performed when this state is left. Defaults to nothing.
   *
   * @param now the timestamp of the transition
   */
  public void exit(final Instant now) {
    // No side effect by default; a state overrides this when leaving it has one.
  }

  /**
   * Transitions back to a fresh {@link IdleState}, discarding the in-progress run. Shared by the
   * states that allow {@code reset}.
   *
   * @param now the timestamp of the transition
   */
  protected final void resetToIdle(final Instant now) {
    timer.clearRun();
    timer.fireReset();
    timer.setState(new IdleState(timer), now);
  }

  /**
   * Rejects an illegal command for this state.
   *
   * @param command the rejected command name
   * @throws IllegalStateException always
   */
  protected final void reject(final String command) {
    throw new IllegalStateException(
        command + " is not allowed in state " + getClass().getSimpleName());
  }
}
