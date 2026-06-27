package com.ruzbyte.speedrunner.core;

import java.time.Instant;

/** Idle: nothing is running. Only {@code start} is legal. */
public final class IdleState extends TimerState {

  /**
   * Binds the state to its orchestrator.
   *
   * @param timer the orchestrator; must not be {@code null}
   */
  IdleState(final SpeedrunTimer timer) {
    super(timer);
  }

  @Override
  public void start(final Instant now) {
    final PersonalBest reference = timer.loadReference();
    if (reference.splitNames().isEmpty()) {
      throw new IllegalStateException(
          "no split layout configured for category: " + timer.category());
    }
    timer.startRun(now);
    timer.fireStart(now);
    timer.setState(new RunningState(timer), now);
  }

  @Override
  public void split(final Instant now) {
    reject("split");
  }

  @Override
  public void pause(final Instant now) {
    reject("pause");
  }

  @Override
  public void resume(final Instant now) {
    reject("resume");
  }

  @Override
  public void reset(final Instant now) {
    reject("reset");
  }
}
