package com.ruzbyte.speedrunner;

/** Console entry point for the speedrunner timer. */
public final class Main {

  private Main() {
    // Utility entry-point class; not instantiable.
  }

  /**
   * Application entry point.
   *
   * @param args command-line arguments (unused)
   */
  public static void main(final String[] args) {
    System.out.println(TimeFormat.format(0L));
  }
}
