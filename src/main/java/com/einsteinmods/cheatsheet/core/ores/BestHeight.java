package com.einsteinmods.cheatsheet.core.ores;

/**
 * Where an ore is most common: the layers sharing the highest density, and the one to dig at.
 *
 * @param y layer to dig at: the middle of the densest layers
 * @param fromY lowest of the densest layers
 * @param toY highest of the densest layers
 */
public record BestHeight(int y, int fromY, int toY) {
  /** A flat spread this tall has no single best layer: the whole range is as good. */
  public static final int FLAT_MIN_LAYERS = 16;

  public BestHeight {
    if (fromY > y || y > toY) {
      throw new IllegalArgumentException("best " + y + " outside " + fromY + ".." + toY);
    }
  }

  public boolean isFlat() {
    return toY - fromY + 1 >= FLAT_MIN_LAYERS;
  }
}
