package com.ruzbyte.speedrunner.core;

import java.time.Instant;

/**
 * Running: the run is in progress. {@code split} records a segment (and auto-finishes once the PB's
 * segment count is reached), {@code pause} suspends it, {@code reset} discards it.
 */
public final class RunningState extends TimerState {

  /**
   * Binds the state to its orchestrator.
   *
   * @param timer the orchestrator; must not be {@code null}
   */
  RunningState(final SpeedrunTimer timer) {
    super(timer);
  }

  @Override
  public void split(final Instant now) {
    final Instant adjusted = now.minus(timer.accumulatedPause());
    final Split split = new Split(timer.nextSplitName(), adjusted);
    timer.addSplit(split);
    timer.fireSplit(split);
    if (timer.splitCount() == timer.expectedSegments()) {
      timer.setState(new FinishedState(timer), now);
    }
  }

  @Override
  public void pause(final Instant now) {
    timer.beginPause(now);
    timer.firePause(now);
    timer.setState(new PausedState(timer), now);
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
  public void resume(final Instant now) {
    reject("resume");
  }
}
