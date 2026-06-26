package com.ruzbyte.speedrunner.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ruzbyte.speedrunner.core.PersonalBest;
import com.ruzbyte.speedrunner.core.Run;
import com.ruzbyte.speedrunner.core.SpeedrunTimer;
import com.ruzbyte.speedrunner.core.SplitCalculator;
import com.ruzbyte.speedrunner.core.VsPersonalBest;
import com.ruzbyte.speedrunner.ports.Clock;
import com.ruzbyte.speedrunner.ports.SplitRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link CliController}. */
@ExtendWith(MockitoExtension.class)
class CliControllerTest {

  private static final String CATEGORY = "Any%";
  private static final Clock FIXED_CLOCK = () -> Instant.EPOCH;

  @Mock private SplitRepository repository;
  @Mock private Logger logger;

  private static PersonalBest layout(final int segments) {
    final List<Duration> golden = new ArrayList<>();
    for (int i = 0; i < segments; i++) {
      golden.add(Duration.ofSeconds(10L));
    }
    return new PersonalBest(CATEGORY, golden, Duration.ofSeconds(100L));
  }

  private CliController controllerFor(final String commands) {
    final SpeedrunTimer timer =
        new SpeedrunTimer(
            CATEGORY, FIXED_CLOCK, repository, new SplitCalculator(new VsPersonalBest()));
    final CliController cli =
        new CliController(timer, new BufferedReader(new StringReader(commands)), logger);
    timer.addListener(cli);
    return cli;
  }

  @Test
  @DisplayName("dispatches start and reports the run started")
  void dispatchesStart() throws IOException {
    when(repository.load(CATEGORY)).thenReturn(layout(2));

    controllerFor("start\nquit\n").run();

    verify(repository).load(CATEGORY);
    verify(logger).info("Run started.");
  }

  @Test
  @DisplayName("reports an illegal command instead of throwing")
  void reportsIllegalCommand() throws IOException {
    controllerFor("split\nquit\n").run();

    verify(logger).info("Not allowed right now: split is not allowed in state IdleState");
  }

  @Test
  @DisplayName("reports an unknown command")
  void reportsUnknownCommand() throws IOException {
    controllerFor("frobnicate\nquit\n").run();

    verify(logger).info("Unknown command: frobnicate");
  }

  @Test
  @DisplayName("prints the prompt and exits on quit")
  void exitsOnQuit() throws IOException {
    controllerFor("quit\n").run();

    verify(logger).info(PROMPT_LINE);
  }

  @Test
  @DisplayName("a finishing split saves the run")
  void finishingSplitSavesRun() throws IOException {
    when(repository.load(CATEGORY)).thenReturn(layout(1));

    controllerFor("start\nsplit\nquit\n").run();

    verify(repository).save(any(Run.class));
    verify(logger).info("Finished! Total 0:00:00.000.");
  }

  @Test
  @DisplayName("status reports elapsed and split count")
  void statusReportsElapsedAndSplits() throws IOException {
    when(repository.load(CATEGORY)).thenReturn(layout(2));

    controllerFor("start\nstatus\nquit\n").run();

    verify(logger).info("Elapsed: 0:00:00.000");
    verify(logger).info("Splits: 0");
  }

  @Test
  @DisplayName("status while idle reports no active run for the comparison")
  void statusWhileIdleReportsNoActiveRun() throws IOException {
    controllerFor("status\nquit\n").run();

    verify(logger).info("vs PB: (no active run)");
  }

  @Test
  @DisplayName("renders pause, resume and reset events")
  void rendersPauseResumeReset() throws IOException {
    when(repository.load(CATEGORY)).thenReturn(layout(3));

    controllerFor("start\npause\nresume\nreset\nquit\n").run();

    verify(logger).info("Resumed.");
    verify(logger).info("Reset.");
  }

  @Test
  @DisplayName("chooseCategory returns the selected category")
  void chooseCategoryReturnsSelection() throws IOException {
    when(repository.categories()).thenReturn(new LinkedHashSet<>(List.of("Any%", "120 Star")));

    final String chosen =
        CliController.chooseCategory(
            repository, new BufferedReader(new StringReader("1\n")), logger);

    assertEquals("Any%", chosen);
  }

  @Test
  @DisplayName("chooseCategory re-prompts on invalid input")
  void chooseCategoryRetriesOnInvalid() throws IOException {
    when(repository.categories()).thenReturn(new LinkedHashSet<>(List.of("Any%")));

    final String chosen =
        CliController.chooseCategory(
            repository, new BufferedReader(new StringReader("nope\n1\n")), logger);

    assertEquals("Any%", chosen);
    verify(logger).info("Enter a number between 1 and 1.");
  }

  @Test
  @DisplayName("chooseCategory returns null when nothing is seeded")
  void chooseCategoryReturnsNullWhenEmpty() throws IOException {
    when(repository.categories()).thenReturn(Set.of());

    final String chosen =
        CliController.chooseCategory(repository, new BufferedReader(new StringReader("")), logger);

    assertNull(chosen);
  }

  @Test
  @DisplayName("formats a positive (behind) delta with a plus sign")
  void formatsPositiveDelta() {
    assertEquals("+0:01:30.000", CliController.formatSigned(Duration.ofSeconds(90L)));
  }

  @Test
  @DisplayName("formats a negative (ahead) delta with a minus sign")
  void formatsNegativeDelta() {
    assertEquals("-0:01:30.000", CliController.formatSigned(Duration.ofSeconds(-90L)));
  }

  private static final String PROMPT_LINE =
      "Commands: start, split, pause, resume, reset, status, quit";
}
