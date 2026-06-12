package com.ruzbyte.speedrunner;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Minimaler Smoke-Test für die Platzhalterklasse.
 *
 * <p>Existiert primär, damit (a) die Surefire/JUnit-5-Verdrahtung nachweislich läuft und (b) das
 * JaCoCo-70%-Gate erfüllt ist. Inhaltliche Tests folgen mit der echten Fachlogik.
 */
class ApplicationTest {

  @Test
  void greetingReturnsExpectedMessage() {
    assertEquals("Speedrunner project skeleton initialised", new Application().greeting());
  }

  @Test
  void mainRunsWithoutThrowing() {
    assertDoesNotThrow(() -> Application.main(new String[] {}));
  }
}
