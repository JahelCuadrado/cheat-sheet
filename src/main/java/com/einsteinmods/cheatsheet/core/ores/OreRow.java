package com.einsteinmods.cheatsheet.core.ores;

/**
 * One vanilla placed feature that generates an ore, with its heights already resolved to absolute Y
 * values in its dimension (they may reach outside the world: those tries are lost).
 *
 * @param featureId placed feature id ({@code minecraft:} namespace implied)
 * @param ore ore it places
 * @param minY lowest height drawn (inclusive)
 * @param maxY highest height drawn (inclusive)
 * @param shape how heights spread between the limits
 * @param count tries per chunk
 * @param biomes biomes that carry it
 */
public record OreRow(
    String featureId,
    Ore ore,
    int minY,
    int maxY,
    HeightShape shape,
    PlacementCount count,
    BiomeRule biomes) {
  public OreRow {
    if (maxY < minY) {
      throw new IllegalArgumentException(featureId + ": empty range " + minY + ".." + maxY);
    }
  }

  /**
   * Chance that one try lands at {@code y}: vanilla's discrete distribution ({@code UniformHeight}:
   * {@code randomBetweenInclusive(min, max)}; {@code TrapezoidHeight} without plateau: {@code min +
   * randomBetweenInclusive(0, range - range / 2) + randomBetweenInclusive(0, range / 2)}).
   */
  public double chanceAt(int y) {
    if (y < minY || y > maxY) {
      return 0.0;
    }
    int range = maxY - minY;
    if (shape == HeightShape.UNIFORM) {
      return 1.0 / (range + 1);
    }
    int lowerHalf = range / 2;
    int upperHalf = range - lowerHalf;
    int offset = y - minY;
    int pairs = Math.min(upperHalf, offset) - Math.max(0, offset - lowerHalf) + 1;
    return (double) pairs / ((upperHalf + 1) * (double) (lowerHalf + 1));
  }

  /** Average tries per chunk landing at {@code y}. */
  public double densityAt(int y) {
    return chanceAt(y) * count.perChunk();
  }

  /** Starts above the dimension's surface: only high terrain (mountains) has stone there. */
  public boolean isHighTerrain() {
    return minY > ore.dimension().surfaceY();
  }
}
