package com.ruzbyte.speedrunner;

import com.ruzbyte.speedrunner.adapters.CliController;
import com.ruzbyte.speedrunner.adapters.JsonSplitRepository;
import com.ruzbyte.speedrunner.adapters.SystemClock;
import com.ruzbyte.speedrunner.core.SpeedrunTimer;
import com.ruzbyte.speedrunner.core.SplitCalculator;
import com.ruzbyte.speedrunner.core.VsPersonalBest;
import com.ruzbyte.speedrunner.ports.Clock;
import com.ruzbyte.speedrunner.ports.SplitRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Console entry point and composition root: creates the clock, repository, calculator, timer and
 * CLI controller and wires them together (manual dependency injection, no singletons).
 */
public final class Main {

  private static final String DEFAULT_DATA_FILE = "speedruns.json";

  private Main() {
    // Utility entry-point class; not instantiable.
  }

  /**
   * Application entry point.
   *
   * @param args optional path to the JSON data file (defaults to {@code speedruns.json})
   */
  public static void main(final String[] args) {
    final Path dataFile = Path.of(args.length > 0 ? args[0] : DEFAULT_DATA_FILE);
    final Logger logger = LogManager.getLogger(Main.class);
    final SplitRepository repository = new JsonSplitRepository(dataFile);
    final Clock clock = new SystemClock();
    final SplitCalculator calculator = new SplitCalculator(new VsPersonalBest());

    try (BufferedReader input =
        new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
      final String category = CliController.chooseCategory(repository, input, logger);
      if (category == null) {
        logger.info("No categories seeded in {} — add one before running.", dataFile);
        return;
      }
      final SpeedrunTimer timer = new SpeedrunTimer(category, clock, repository, calculator);
      final CliController cli = new CliController(timer, input, logger);
      timer.addListener(cli);
      cli.run();
    } catch (final IOException e) {
      final String message = "Input error: " + e.getMessage();
      logger.error(message);
    }
  }
}
