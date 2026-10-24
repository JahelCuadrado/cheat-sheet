package com.einsteinmods.cheatsheet.core.ores;

/**
 * How many times per chunk a placement tries: {@code CountPlacement} (a fixed count or a uniform
 * range) and {@code RarityFilter} (once every N chunks on average).
 *
 * @param minCount fewest tries per chunk
 * @param maxCount most tries per chunk
 * @param oneInChunks 1, or N for "once every N chunks"
 */
public record PlacementCount(int minCount, int maxCount, int oneInChunks) {
  public PlacementCount {
    if (minCount < 0 || maxCount < minCount || oneInChunks < 1) {
      throw new IllegalArgumentException(
          "bad count " + minCount + ".." + maxCount + " / " + oneInChunks);
    }
  }

  public static PlacementCount exactly(int count) {
    return new PlacementCount(count, count, 1);
  }

  public static PlacementCount between(int minCount, int maxCount) {
    return new PlacementCount(minCount, maxCount, 1);
  }

  public static PlacementCount oneInChunks(int chunks) {
    return new PlacementCount(1, 1, chunks);
  }

  /** Average tries per chunk. */
  public double perChunk() {
    return (minCount + maxCount) / 2.0 / oneInChunks;
  }
}
