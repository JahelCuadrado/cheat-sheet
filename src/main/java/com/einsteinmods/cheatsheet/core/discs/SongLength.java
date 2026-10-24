package com.einsteinmods.cheatsheet.core.discs;

import java.util.Locale;

/** Song lengths as players read them on a disc: {@code m:ss}. */
public final class SongLength {
  private static final int SECONDS_PER_MINUTE = 60;

  private SongLength() {}

  /** {@code 178.0} → {@code "2:58"}; fractions of a second are rounded to the nearest second. */
  public static String format(float seconds) {
    if (seconds < 0 || Float.isNaN(seconds)) {
      throw new IllegalArgumentException("bad length: " + seconds);
    }
    int total = Math.round(seconds);
    return String.format(
        Locale.ROOT, "%d:%02d", total / SECONDS_PER_MINUTE, total % SECONDS_PER_MINUTE);
  }
}
