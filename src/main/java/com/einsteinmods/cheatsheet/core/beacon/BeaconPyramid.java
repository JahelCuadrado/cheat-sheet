package com.einsteinmods.cheatsheet.core.beacon;

/**
 * A beacon's pyramid ({@code BeaconBlockEntity}): level {@code n} adds a square layer of side
 * {@code 2n + 1} under the one above, up to four levels. The range and effect duration follow
 * {@code BeaconBlockEntity.applyEffects}.
 */
public final class BeaconPyramid {
  /** Most levels a pyramid counts ({@code MAX_LEVELS}). */
  public static final int MAX_LEVELS = 4;

  /** Level at which a second effect (or level II of the first) unlocks. */
  public static final int SECONDARY_LEVEL = 4;

  private static final int RANGE_BASE = 10;
  private static final int RANGE_PER_LEVEL = 10;
  private static final int SECONDS_BASE = 9;
  private static final int SECONDS_PER_LEVEL = 2;

  private BeaconPyramid() {}

  /** Blocks per side of layer {@code level}, counted from the top (1: the 3x3 under the beacon). */
  public static int layerSide(int level) {
    checkLevel(level);
    return 2 * level + 1;
  }

  /** Blocks of layer {@code level}. */
  public static int layerBlocks(int level) {
    int side = layerSide(level);
    return side * side;
  }

  /** Blocks a pyramid of {@code levels} levels needs in total. */
  public static int totalBlocks(int levels) {
    int total = 0;
    for (int level = 1; level <= levels; level++) {
      total += layerBlocks(level);
    }
    return total;
  }

  /** Horizontal reach of the effects, in blocks ({@code levels * 10 + 10}). */
  public static int range(int levels) {
    checkLevel(levels);
    return levels * RANGE_PER_LEVEL + RANGE_BASE;
  }

  /** How long each pulse of the effect lasts, in seconds ({@code 9 + levels * 2}). */
  public static int effectSeconds(int levels) {
    checkLevel(levels);
    return SECONDS_BASE + levels * SECONDS_PER_LEVEL;
  }

  private static void checkLevel(int level) {
    if (level < 1 || level > MAX_LEVELS) {
      throw new IllegalArgumentException("beacon level " + level);
    }
  }
}
