package com.ruzbyte.speedrunner.core;

import java.time.Instant;

/**
 * Finished: the run is complete. Its {@link #entry} builds the immutable {@link Run} once, saves it
 * through the repository and notifies listeners; only {@code reset} is legal afterwards.
 */
public final class FinishedState extends TimerState {

  /**
   * Binds the state to its orchestrator.
   *
   * @param timer the orchestrator; must not be {@code null}
   */
  FinishedState(final SpeedrunTimer timer) {
    super(timer);
  }

  @Override
  public void entry(final Instant now) {
    final Run run = timer.buildRun();
    timer.save(run);
    timer.fireFinish(run);
  }

  @Override
  public void reset(final Instant now) {
    resetToIdle(now);
  }

  @Override
  public void start(final Instant now) {
    reject("start");
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
}
