package com.ruzbyte.speedrunner.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Run}. */
class RunTest {

  private static final Instant START = Instant.ofEpochMilli(0L);

  private static List<Split> oneSplit() {
    return new ArrayList<>(List.of(new Split("End", Instant.ofEpochMilli(5_000L))));
  }

  @Test
  @DisplayName("exposes the category it was built with")
  void exposesCategory() {
    final Run run = new Run("Any% NMG", oneSplit(), START);

    assertEquals("Any% NMG", run.category());
  }

  @Test
  @DisplayName("is unaffected by later mutation of the source split list")
  void copiesSplitsDefensively() {
    final List<Split> source = oneSplit();
    final Run run = new Run("Any%", source, START);

    source.add(new Split("Injected", Instant.ofEpochMilli(9_000L)));

    assertEquals(1, run.splits().size());
  }

  @Test
  @DisplayName("exposes an immutable split list")
  void exposesImmutableSplits() {
    final Run run = new Run("Any%", oneSplit(), START);

    assertThrows(
        UnsupportedOperationException.class,
        () -> run.splits().add(new Split("Injected", Instant.ofEpochMilli(9_000L))));
  }

  @Test
  @DisplayName("rejects a blank category")
  void rejectsBlankCategory() {
    assertThrows(IllegalArgumentException.class, () -> new Run(" ", oneSplit(), START));
  }

  @Test
  @DisplayName("rejects a null split list")
  void rejectsNullSplits() {
    assertThrows(NullPointerException.class, () -> new Run("Any%", null, START));
  }

  @Test
  @DisplayName("rejects a null start instant")
  void rejectsNullStart() {
    assertThrows(NullPointerException.class, () -> new Run("Any%", oneSplit(), null));
  }

  @Test
  @DisplayName("reports zero total time when there are no splits")
  void totalTimeIsZeroWithoutSplits() {
    final Run run = new Run("Any%", List.of(), START);

    assertEquals(Duration.ZERO, run.totalTime());
  }

  @Test
  @DisplayName("reports total time as the last split minus the start instant")
  void totalTimeIsLastSplitMinusStart() {
    final List<Split> splits =
        List.of(
            new Split("Mid", Instant.ofEpochMilli(2_000L)),
            new Split("End", Instant.ofEpochMilli(5_000L)));
    final Run run = new Run("Any%", splits, START);

    assertEquals(Duration.ofMillis(5_000L), run.totalTime());
  }
}
