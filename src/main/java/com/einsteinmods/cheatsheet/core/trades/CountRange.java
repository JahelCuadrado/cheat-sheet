package com.einsteinmods.cheatsheet.core.trades;

/**
 * An amount that may be random: a price, or how many items a barter gives.
 *
 * @param min fewest (inclusive)
 * @param max most (inclusive)
 */
public record CountRange(int min, int max) {
  public CountRange {
    if (min < 0 || max < min) {
      throw new IllegalArgumentException("bad range " + min + ".." + max);
    }
  }

  public static CountRange exactly(int count) {
    return new CountRange(count, count);
  }

  public boolean isFixed() {
    return min == max;
  }
}
