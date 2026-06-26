package com.ruzbyte.speedrunner.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ruzbyte.speedrunner.ports.Clock;
import com.ruzbyte.speedrunner.ports.SplitRepository;
import com.ruzbyte.speedrunner.ports.TimerListener;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link SpeedrunTimer} and the state machine. */
@ExtendWith(MockitoExtension.class)
class SpeedrunTimerTest {

  private static final String CATEGORY = "Any%";

  @Mock private SplitRepository repository;
  @Mock private TimerListener listener;

  private MutableClock clock;
  private SpeedrunTimer timer;

  @BeforeEach
  void setUp() {
    clock = new MutableClock(Instant.EPOCH);
    timer =
        new SpeedrunTimer(CATEGORY, clock, repository, new SplitCalculator(new VsPersonalBest()));
    timer.addListener(listener);
  }

  private void seedLayout(final int segments, final Duration bestTotal) {
    final List<Duration> golden = new ArrayList<>();
    for (int i = 0; i < segments; i++) {
      golden.add(Duration.ofSeconds(10L));
    }
    when(repository.load(CATEGORY)).thenReturn(new PersonalBest(CATEGORY, golden, bestTotal));
  }

  private static Instant at(final long seconds) {
    return Instant.EPOCH.plusSeconds(seconds);
  }

  @Test
  @DisplayName("start from idle enters running and fires onStart")
  void startEntersRunning() {
    seedLayout(2, Duration.ofSeconds(100L));

    timer.start();

    assertInstanceOf(RunningState.class, timer.state());
    verify(listener).onStart(Instant.EPOCH);
  }

  @Test
  @DisplayName("start is rejected when the category has no seeded layout")
  void startRejectedWithoutLayout() {
    seedLayout(0, Duration.ofSeconds(100L));

    assertThrows(IllegalStateException.class, () -> timer.start());
    assertInstanceOf(IdleState.class, timer.state());
  }

  @Test
  @DisplayName("split while idle is rejected")
  void splitWhileIdleRejected() {
    assertThrows(IllegalStateException.class, () -> timer.split());
  }

  @Test
  @DisplayName("pause while idle is rejected")
  void pauseWhileIdleRejected() {
    assertThrows(IllegalStateException.class, () -> timer.pause());
  }

  @Test
  @DisplayName("resume while idle is rejected")
  void resumeWhileIdleRejected() {
    assertThrows(IllegalStateException.class, () -> timer.resume());
  }

  @Test
  @DisplayName("reset while idle is rejected")
  void resetWhileIdleRejected() {
    assertThrows(IllegalStateException.class, () -> timer.reset());
  }

  @Test
  @DisplayName("a split is recorded with a name and the current timestamp")
  void recordsSplit() {
    seedLayout(3, Duration.ofSeconds(100L));
    timer.start();
    clock.set(at(10L));

    timer.split();

    final ArgumentCaptor<Split> captor = ArgumentCaptor.forClass(Split.class);
    verify(listener).onSplit(captor.capture());
    assertEquals(at(10L), captor.getValue().timestamp());
  }

  @Test
  @DisplayName("pause time is subtracted from later split timestamps")
  void pauseSubtractedFromSplitTimestamp() {
    seedLayout(3, Duration.ofSeconds(100L));
    timer.start();
    clock.set(at(10L));
    timer.pause();
    clock.set(at(30L));
    timer.resume();
    clock.set(at(40L));

    timer.split();

    final ArgumentCaptor<Split> captor = ArgumentCaptor.forClass(Split.class);
    verify(listener).onSplit(captor.capture());
    assertEquals(at(20L), captor.getValue().timestamp());
  }

  @Test
  @DisplayName("reaching the segment count auto-finishes, builds and saves the run")
  void autoFinishSavesRun() {
    seedLayout(2, Duration.ofSeconds(100L));
    timer.start();
    clock.set(at(10L));
    timer.split();
    clock.set(at(25L));

    timer.split();

    final ArgumentCaptor<Run> captor = ArgumentCaptor.forClass(Run.class);
    verify(repository).save(captor.capture());
    final Run saved = captor.getValue();
    assertEquals(2, saved.splits().size());
    assertInstanceOf(FinishedState.class, timer.state());
  }

  @Test
  @DisplayName("a finished run total excludes paused time")
  void finishedTotalExcludesPause() {
    seedLayout(2, Duration.ofSeconds(100L));
    timer.start();
    clock.set(at(10L));
    timer.pause();
    clock.set(at(30L));
    timer.resume();
    clock.set(at(40L));
    timer.split();
    clock.set(at(60L));

    timer.split();

    final ArgumentCaptor<Run> captor = ArgumentCaptor.forClass(Run.class);
    verify(repository).save(captor.capture());
    assertEquals(Duration.ofSeconds(40L), captor.getValue().totalTime());
  }

  @Test
  @DisplayName("pause transitions to paused and fires onPause")
  void pauseTransitions() {
    seedLayout(3, Duration.ofSeconds(100L));
    timer.start();
    clock.set(at(10L));

    timer.pause();

    assertInstanceOf(PausedState.class, timer.state());
    verify(listener).onPause(at(10L));
  }

  @Test
  @DisplayName("resume transitions back to running and fires onResume")
  void resumeTransitions() {
    seedLayout(3, Duration.ofSeconds(100L));
    timer.start();
    clock.set(at(10L));
    timer.pause();
    clock.set(at(20L));

    timer.resume();

    assertInstanceOf(RunningState.class, timer.state());
    verify(listener).onResume(at(20L));
  }

  @Test
  @DisplayName("start and resume are rejected while running")
  void illegalWhileRunning() {
    seedLayout(3, Duration.ofSeconds(100L));
    timer.start();

    assertThrows(IllegalStateException.class, () -> timer.start());
    assertThrows(IllegalStateException.class, () -> timer.resume());
  }

  @Test
  @DisplayName("start, split and pause are rejected while paused")
  void illegalWhilePaused() {
    seedLayout(3, Duration.ofSeconds(100L));
    timer.start();
    timer.pause();

    assertThrows(IllegalStateException.class, () -> timer.start());
    assertThrows(IllegalStateException.class, () -> timer.split());
    assertThrows(IllegalStateException.class, () -> timer.pause());
  }

  @Test
  @DisplayName("reset from running returns to idle and fires onReset")
  void resetFromRunning() {
    seedLayout(3, Duration.ofSeconds(100L));
    timer.start();

    timer.reset();

    assertInstanceOf(IdleState.class, timer.state());
    verify(listener).onReset();
  }

  @Test
  @DisplayName("reset from paused returns to idle")
  void resetFromPaused() {
    seedLayout(3, Duration.ofSeconds(100L));
    timer.start();
    timer.pause();

    timer.reset();

    assertInstanceOf(IdleState.class, timer.state());
  }

  @Test
  @DisplayName("reset from finished returns to idle; further commands are rejected until restart")
  void resetFromFinished() {
    seedLayout(1, Duration.ofSeconds(100L));
    timer.start();
    clock.set(at(10L));
    timer.split();
    assertInstanceOf(FinishedState.class, timer.state());

    timer.reset();

    assertInstanceOf(IdleState.class, timer.state());
  }

  @Test
  @DisplayName("split, pause and resume are rejected while finished")
  void illegalWhileFinished() {
    seedLayout(1, Duration.ofSeconds(100L));
    timer.start();
    clock.set(at(10L));
    timer.split();

    assertThrows(IllegalStateException.class, () -> timer.split());
    assertThrows(IllegalStateException.class, () -> timer.pause());
    assertThrows(IllegalStateException.class, () -> timer.resume());
  }

  @Test
  @DisplayName("compareToPersonalBest returns the active strategy's delta")
  void compareToPersonalBest() {
    seedLayout(3, Duration.ofSeconds(100L));
    timer.start();
    clock.set(at(30L));
    timer.split();

    final Duration delta = timer.compareToPersonalBest();

    assertEquals(Duration.ofSeconds(-70L), delta);
  }

  @Test
  @DisplayName("compareToPersonalBest is rejected when no run has started")
  void compareWhileIdleRejected() {
    assertThrows(IllegalStateException.class, () -> timer.compareToPersonalBest());
  }

  @Test
  @DisplayName("the splits snapshot is immutable")
  void splitsSnapshotIsImmutable() {
    seedLayout(3, Duration.ofSeconds(100L));
    timer.start();
    clock.set(at(10L));
    timer.split();

    final List<Split> snapshot = timer.splits();

    assertThrows(
        UnsupportedOperationException.class, () -> snapshot.add(new Split("x", Instant.EPOCH)));
  }

  @Test
  @DisplayName("rejects a blank category")
  void rejectsBlankCategory() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new SpeedrunTimer(" ", clock, repository, new SplitCalculator(new VsPersonalBest())));
  }

  @Test
  @DisplayName("rejects a null clock")
  void rejectsNullClock() {
    assertThrows(
        NullPointerException.class,
        () ->
            new SpeedrunTimer(
                CATEGORY, null, repository, new SplitCalculator(new VsPersonalBest())));
  }

  @Test
  @DisplayName("rejects a null listener")
  void rejectsNullListener() {
    assertThrows(NullPointerException.class, () -> timer.addListener(null));
  }

  @Test
  @DisplayName("elapsed is zero while idle")
  void elapsedIsZeroWhenIdle() {
    assertEquals(Duration.ZERO, timer.elapsed());
  }

  @Test
  @DisplayName("elapsed tracks the clock while running")
  void elapsedWhileRunning() {
    seedLayout(2, Duration.ofSeconds(100L));
    timer.start();
    clock.set(at(15L));

    assertEquals(Duration.ofSeconds(15L), timer.elapsed());
  }

  @Test
  @DisplayName("elapsed is frozen while paused")
  void elapsedFrozenWhilePaused() {
    seedLayout(2, Duration.ofSeconds(100L));
    timer.start();
    clock.set(at(10L));
    timer.pause();
    clock.set(at(50L));

    assertEquals(Duration.ofSeconds(10L), timer.elapsed());
  }

  @Test
  @DisplayName("elapsed excludes paused time after resuming")
  void elapsedExcludesPauseAfterResume() {
    seedLayout(2, Duration.ofSeconds(100L));
    timer.start();
    clock.set(at(10L));
    timer.pause();
    clock.set(at(30L));
    timer.resume();
    clock.set(at(50L));

    assertEquals(Duration.ofSeconds(30L), timer.elapsed());
  }

  @Test
  @DisplayName("elapsed is the final total once finished")
  void elapsedIsFinalTotalWhenFinished() {
    seedLayout(1, Duration.ofSeconds(100L));
    timer.start();
    clock.set(at(10L));
    timer.split();
    clock.set(at(100L));

    assertEquals(Duration.ofSeconds(10L), timer.elapsed());
  }

  /** A clock whose instant can be advanced explicitly, for deterministic time-based tests. */
  private static final class MutableClock implements Clock {

    private Instant now;

    MutableClock(final Instant start) {
      this.now = start;
    }

    void set(final Instant instant) {
      this.now = instant;
    }

    @Override
    public Instant now() {
      return now;
    }
  }
}
