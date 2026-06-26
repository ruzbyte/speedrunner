package com.ruzbyte.speedrunner.adapters;

import com.ruzbyte.speedrunner.TimeFormat;
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
   * Prompts the user to choose one of the repository's seeded categories.
   *
   * @param repository the repository whose categories are offered
   * @param input the input source
   * @param logger the logger for the prompt
   * @return the chosen category, or {@code null} if none are seeded or input ended
   * @throws IOException if reading the input fails
   */
  public static String chooseCategory(
      final SplitRepository repository, final BufferedReader input, final Logger logger)
      throws IOException {
    final List<String> categories = new ArrayList<>(repository.categories());
    if (categories.isEmpty()) {
      return null;
    }
    emit(logger, "Select a category:");
    for (int i = 0; i < categories.size(); i++) {
      emit(logger, (i + 1) + ") " + categories.get(i));
    }
    String line = input.readLine();
    while (line != null) {
      final String trimmed = line.trim();
      if (isInRange(trimmed, categories.size())) {
        return categories.get(Integer.parseInt(trimmed) - 1);
      }
      emit(logger, "Enter a number between 1 and " + categories.size() + ".");
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
