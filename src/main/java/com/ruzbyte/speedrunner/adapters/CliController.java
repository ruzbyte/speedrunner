package com.ruzbyte.speedrunner.adapters;

import com.ruzbyte.speedrunner.TimeFormat;
import com.ruzbyte.speedrunner.core.PersonalBest;
import com.ruzbyte.speedrunner.core.Run;
import com.ruzbyte.speedrunner.core.SpeedrunTimer;
import com.ruzbyte.speedrunner.core.Split;
import com.ruzbyte.speedrunner.ports.SplitRepository;
import com.ruzbyte.speedrunner.ports.TimerListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.logging.log4j.Logger;

/**
 * Console adapter: reads commands from an input stream and drives the {@link SpeedrunTimer}, and
 * implements {@link TimerListener} to render timer events. All output goes through Log4J2 (via the
 * single {@link #emit} sink), never directly to {@code System.out}.
 */
public final class CliController implements TimerListener {

  private static final String PROMPT = "Commands: start, split, pause, resume, reset, status, quit";

  private final SpeedrunTimer timer;
  private final BufferedReader input;
  private final Logger logger;

  /**
   * Wires the controller to its timer, input and logger.
   *
   * @param timer the timer to drive
   * @param input the command source
   * @param logger the Log4J2 logger all output is routed through
   */
  public CliController(final SpeedrunTimer timer, final BufferedReader input, final Logger logger) {
    this.timer = timer;
    this.input = input;
    this.logger = logger;
  }

  /**
   * Lets the user pick an existing configured route or create a new one. Existing routes are listed
   * as "game — category"; an extra option creates a new run by prompting for the game, category and
   * split names, which is then persisted via the repository so it is available next launch.
   *
   * @param repository the repository whose routes are offered and to which a new one is saved
   * @param input the input source
   * @param logger the logger for the prompts
   * @return the chosen or newly configured route, or {@code null} if input ended first
   * @throws IOException if reading the input fails
   */
  public static PersonalBest chooseRoute(
      final SplitRepository repository, final BufferedReader input, final Logger logger)
      throws IOException {
    final List<PersonalBest> routes = repository.layouts();
    if (routes.isEmpty()) {
      return setupNewRoute(repository, input, logger);
    }
    emit(logger, "Select a run:");
    for (int i = 0; i < routes.size(); i++) {
      final PersonalBest route = routes.get(i);
      emit(logger, (i + 1) + ") " + route.game() + " — " + route.category());
    }
    final int createOption = routes.size() + 1;
    emit(logger, createOption + ") Configure a new run…");
    String line = input.readLine();
    while (line != null) {
      final String trimmed = line.trim();
      if (isInRange(trimmed, createOption)) {
        final int choice = Integer.parseInt(trimmed);
        if (choice == createOption) {
          return setupNewRoute(repository, input, logger);
        }
        return routes.get(choice - 1);
      }
      emit(logger, "Enter a number between 1 and " + createOption + ".");
      line = input.readLine();
    }
    return null;
  }

  /**
   * Interactively configures a new route: game, category and one split name per line (a blank line
   * ends the list). The route is persisted with no times yet and returned.
   *
   * @return the configured route, or {@code null} if input ended before it was complete
   */
  private static PersonalBest setupNewRoute(
      final SplitRepository repository, final BufferedReader input, final Logger logger)
      throws IOException {
    emit(logger, "Game name:");
    final String game = readNonBlank(input, logger, "Game name");
    if (game == null) {
      return null;
    }
    emit(logger, "Category name:");
    final String category = readNonBlank(input, logger, "Category name");
    if (category == null) {
      return null;
    }
    emit(logger, "Enter split names, one per line; blank line to finish:");
    final List<String> splitNames = readSplitNames(input, logger);
    if (splitNames.isEmpty()) {
      return null;
    }
    final PersonalBest route =
        new PersonalBest(game, category, splitNames, List.of(), Duration.ZERO);
    repository.saveLayout(route);
    emit(
        logger,
        "Configured " + game + " — " + category + " with " + splitNames.size() + " splits.");
    return route;
  }

  /** Reads non-blank split names until a blank line; re-prompts if none are given before EOF. */
  private static List<String> readSplitNames(final BufferedReader input, final Logger logger)
      throws IOException {
    final List<String> splitNames = new ArrayList<>();
    String line = input.readLine();
    while (line != null) {
      final String trimmed = line.trim();
      if (trimmed.isEmpty()) {
        if (!splitNames.isEmpty()) {
          break;
        }
        emit(logger, "Enter at least one split name.");
      } else {
        splitNames.add(trimmed);
      }
      line = input.readLine();
    }
    return splitNames;
  }

  /** Reads lines until a non-blank one is entered, re-prompting on blanks; null on EOF. */
  private static String readNonBlank(
      final BufferedReader input, final Logger logger, final String label) throws IOException {
    String line = input.readLine();
    while (line != null) {
      final String trimmed = line.trim();
      if (!trimmed.isEmpty()) {
        return trimmed;
      }
      emit(logger, label + " must not be blank.");
      line = input.readLine();
    }
    return null;
  }

  /**
   * Runs the command loop until {@code quit}/{@code exit} or end of input.
   *
   * @throws IOException if reading the input fails
   */
  public void run() throws IOException {
    emit(logger, PROMPT);
    String line = input.readLine();
    while (line != null) {
      final String command = line.trim().toLowerCase(Locale.ROOT);
      if ("quit".equals(command) || "exit".equals(command)) {
        return;
      }
      if (!command.isEmpty()) {
        dispatch(command);
      }
      line = input.readLine();
    }
  }

  @Override
  public void onStart(final Instant now) {
    emit(logger, "Run started.");
  }

  @Override
  public void onSplit(final Split split) {
    emit(logger, split.name() + " at " + format(timer.elapsed()));
  }

  @Override
  public void onPause(final Instant now) {
    emit(logger, "Paused at " + format(timer.elapsed()) + ".");
  }

  @Override
  public void onResume(final Instant now) {
    emit(logger, "Resumed.");
  }

  @Override
  public void onFinish(final Run run) {
    emit(logger, "Finished! Total " + format(run.totalTime()) + ".");
    emit(logger, "vs PB: " + formatSigned(timer.compareToPersonalBest()));
  }

  @Override
  public void onReset() {
    emit(logger, "Reset.");
  }

  private void dispatch(final String command) {
    try {
      switch (command) {
        case "start" -> timer.start();
        case "split" -> timer.split();
        case "pause" -> timer.pause();
        case "resume" -> timer.resume();
        case "reset" -> timer.reset();
        case "status" -> showStatus();
        default -> emit(logger, "Unknown command: " + command);
      }
    } catch (final IllegalStateException e) {
      emit(logger, "Not allowed right now: " + e.getMessage());
    }
  }

  private void showStatus() {
    emit(logger, "Elapsed: " + format(timer.elapsed()));
    final List<Split> splits = timer.splits();
    emit(logger, "Splits: " + splits.size());
    for (final Split split : splits) {
      emit(logger, "  - " + split.name());
    }
    try {
      emit(logger, "vs PB: " + formatSigned(timer.compareToPersonalBest()));
    } catch (final IllegalStateException e) {
      emit(logger, "vs PB: (no active run)");
    }
  }

  private static String format(final Duration duration) {
    return TimeFormat.format(duration.toMillis());
  }

  /**
   * Formats a signed comparison delta as {@code ±H:MM:SS.mmm} (negative means ahead).
   *
   * @param delta the comparison delta
   * @return the formatted, signed string
   */
  static String formatSigned(final Duration delta) {
    final long millis = delta.toMillis();
    final String sign = millis < 0L ? "-" : "+";
    return sign + TimeFormat.format(Math.abs(millis));
  }

  private static boolean isInRange(final String text, final int max) {
    try {
      final int value = Integer.parseInt(text);
      return value >= 1 && value <= max;
    } catch (final NumberFormatException e) {
      return false;
    }
  }

  private static void emit(final Logger logger, final String message) {
    logger.info(message);
  }
}
