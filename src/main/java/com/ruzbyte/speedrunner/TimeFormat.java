package com.ruzbyte.speedrunner;

/** Formats elapsed speedrun durations for display. */
public final class TimeFormat {

  private static final long MILLIS_PER_SECOND = 1_000L;
  private static final long MILLIS_PER_MINUTE = 60_000L;
  private static final long MILLIS_PER_HOUR = 3_600_000L;

  private TimeFormat() {
    // Utility class; not instantiable.
  }

  /**
   * Formats an elapsed duration as {@code H:MM:SS.mmm}.
   *
   * @param millis the elapsed time in milliseconds; must not be negative
   * @return the formatted timer string
   * @throws IllegalArgumentException if {@code millis} is negative
   */
  public static String format(final long millis) {
    if (millis < 0L) {
      throw new IllegalArgumentException("millis must not be negative: " + millis);
    }
    final long hours = millis / MILLIS_PER_HOUR;
    final long minutes = (millis / MILLIS_PER_MINUTE) % 60L;
    final long seconds = (millis / MILLIS_PER_SECOND) % 60L;
    final long fraction = millis % MILLIS_PER_SECOND;
    return String.format("%d:%02d:%02d.%03d", hours, minutes, seconds, fraction);
  }
}
