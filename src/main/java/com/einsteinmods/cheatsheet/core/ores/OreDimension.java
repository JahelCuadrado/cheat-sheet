package com.einsteinmods.cheatsheet.core.ores;

/**
 * Height limits of a dimension where ores generate, as vanilla's noise settings give them ({@code
 * WorldGenerationContext}: anchors resolve against {@code genMinY} and {@code genDepth}).
 *
 * @param genMinY lowest block of the world ({@code VerticalAnchor.bottom()})
 * @param genDepth generation height: {@code VerticalAnchor.top()} is {@code genMinY + genDepth - 1}
 * @param lowestMinableY lowest layer free of bedrock (bedrock fills the 5 bottom layers)
 * @param highestMinableY highest layer free of bedrock (the Nether has a bedrock roof)
 * @param surfaceY placements starting above this height only find stone in high terrain (sea level
 *     in the Overworld); {@link Integer#MAX_VALUE} when there is no such terrain
 */
public enum OreDimension {
  OVERWORLD(-64, 384, -59, 319, 63),
  NETHER(0, 128, 5, 122, Integer.MAX_VALUE);

  private final int genMinY;
  private final int genDepth;
  private final int lowestMinableY;
  private final int highestMinableY;
  private final int surfaceY;

  OreDimension(int genMinY, int genDepth, int lowestMinableY, int highestMinableY, int surfaceY) {
    this.genMinY = genMinY;
    this.genDepth = genDepth;
    this.lowestMinableY = lowestMinableY;
    this.highestMinableY = highestMinableY;
    this.surfaceY = surfaceY;
  }

  public int genMinY() {
    return genMinY;
  }

  public int genDepth() {
    return genDepth;
  }

  /** Lowest world height (inclusive). */
  public int minY() {
    return genMinY;
  }

  /** Highest world height where ores can generate (inclusive). */
  public int maxY() {
    return genMinY + genDepth - 1;
  }

  /** Number of block layers between {@link #minY()} and {@link #maxY()}. */
  public int heightLayers() {
    return genDepth;
  }

  public int lowestMinableY() {
    return lowestMinableY;
  }

  public int highestMinableY() {
    return highestMinableY;
  }

  public int surfaceY() {
    return surfaceY;
  }
}
