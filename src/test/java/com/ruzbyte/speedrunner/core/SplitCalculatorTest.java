package com.ruzbyte.speedrunner.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link SplitCalculator}. */
class SplitCalculatorTest {

  private static final Run RUN =
      new Run("Any%", List.of(new Split("End", Instant.EPOCH.plusSeconds(1L))), Instant.EPOCH);
  private static final PersonalBest PB =
      new PersonalBest("Any%", List.of(), Duration.ofSeconds(1L));

  @Test
  @DisplayName("delegates the comparison to the active strategy")
  void delegatesToStrategy() {
    final CompareStrategy stub = (run, reference) -> Duration.ofSeconds(7L);
    final SplitCalculator calculator = new SplitCalculator(stub);

    assertEquals(Duration.ofSeconds(7L), calculator.compareAgainst(RUN, PB));
  }

  @Test
  @DisplayName("uses the new strategy after it is swapped at runtime")
  void usesSwappedStrategy() {
    final SplitCalculator calculator =
        new SplitCalculator((run, reference) -> Duration.ofSeconds(1L));

    calculator.setStrategy((run, reference) -> Duration.ofSeconds(2L));

    assertEquals(Duration.ofSeconds(2L), calculator.compareAgainst(RUN, PB));
  }

  @Test
  @DisplayName("rejects a null strategy at construction")
  void rejectsNullStrategyAtConstruction() {
    assertThrows(NullPointerException.class, () -> new SplitCalculator(null));
  }

  @Test
  @DisplayName("rejects a null strategy when swapping")
  void rejectsNullStrategyWhenSwapping() {
    final SplitCalculator calculator = new SplitCalculator((run, reference) -> Duration.ZERO);

    assertThrows(NullPointerException.class, () -> calculator.setStrategy(null));
  }

  @Test
  @DisplayName("rejects a null run")
  void rejectsNullRun() {
    final SplitCalculator calculator = new SplitCalculator((run, reference) -> Duration.ZERO);

    assertThrows(NullPointerException.class, () -> calculator.compareAgainst(null, PB));
  }

  @Test
  @DisplayName("rejects a null personal best")
  void rejectsNullPersonalBest() {
    final SplitCalculator calculator = new SplitCalculator((run, reference) -> Duration.ZERO);

    assertThrows(NullPointerException.class, () -> calculator.compareAgainst(RUN, null));
  }
}
