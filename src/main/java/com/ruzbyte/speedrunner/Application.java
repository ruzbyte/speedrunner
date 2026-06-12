package com.ruzbyte.speedrunner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Platzhalter-Einstiegsklasse des Projekts.
 *
 * <p>Demonstriert von Anfang an das Logging-Muster: es wird ein Log4j2-Logger verwendet (kein
 * {@code System.out}).
 */
public final class Application {

  private static final Logger LOGGER = LogManager.getLogger(Application.class);

  /**
   * Liefert die Begrüßungsnachricht des Skeletts.
   *
   * @return eine konstante Statusmeldung
   */
  public String greeting() {
    return "Speedrunner project skeleton initialised";
  }

  /**
   * Programmeinstieg.
   *
   * <p>Loggt eine konstante Meldung. Bewusst ein String-Literal (kein Methodenaufruf im
   * Log-Argument), damit PMDs {@code GuardLogStatement} nicht anschlägt.
   *
   * @param args Kommandozeilenargumente (ungenutzt)
   */
  public static void main(final String[] args) {
    LOGGER.info("Speedrunner application started");
  }
}
