package com.ruzbyte.speedrunner.ports;

import com.ruzbyte.speedrunner.core.Run;
import com.ruzbyte.speedrunner.core.Split;
import java.time.Instant;

/**
 * Observer port: notified when the timer changes state. Events fire at their point of origin,
 * including from a state's {@code entry()}/{@code exit()}. The CLI implements this to render
 * output; a later GUI can attach the same way.
 */
public interface TimerListener {

  /**
   * Called when a run starts.
   *
   * @param now the instant the run started
   */
  void onStart(Instant now);

  /**
   * Called when a split is recorded.
   *
   * @param split the recorded split
   */
  void onSplit(Split split);

  /**
   * Called when the run is paused.
   *
   * @param now the instant the run was paused
   */
  void onPause(Instant now);

  /**
   * Called when the run is resumed.
   *
   * @param now the instant the run was resumed
   */
  void onResume(Instant now);

  /**
   * Called when the run finishes.
   *
   * @param run the completed run
   */
  void onFinish(Run run);

  /** Called when the timer is reset. */
  void onReset();
}
