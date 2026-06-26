package com.ruzbyte.speedrunner.core;

import java.time.Instant;

/**
 * Paused: the run is suspended. {@code resume} continues it (accumulating the pause duration),
 * {@code reset} discards it.
 */
public final class PausedState extends TimerState {

  /**
   * Binds the state to its orchestrator.
   *
   * @param timer the orchestrator; must not be {@code null}
   */
  PausedState(final SpeedrunTimer timer) {
    super(timer);
  }

  @Override
  public void resume(final Instant now) {
    timer.endPause(now);
    timer.fireResume(now);
    timer.setState(new RunningState(timer), now);
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
}
