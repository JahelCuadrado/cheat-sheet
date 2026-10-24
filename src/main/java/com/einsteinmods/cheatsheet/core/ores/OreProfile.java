package com.einsteinmods.cheatsheet.core.ores;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * An ore's spread over its dimension's heights, summed over the placements found in most biomes
 * (biome-only bonuses such as badlands gold are kept apart), and where to dig for it.
 *
 * <p>The best height is searched between the bedrock layers and ignores placements that start above
 * the surface (only mountains have stone there); when an ore has such placements, the best height
 * counting them is {@link #mountainBestY()}. An ore found only in some biomes (emerald) uses those
 * placements as its main ones.
 */
public final class OreProfile {
  /** Densities this close to the maximum count as equal (floating point sums). */
  private static final double SAME_DENSITY = 1.0e-9;

  private final Ore ore;
  private final double[] density;
  private final double maxDensity;
  private final int minY;
  private final int maxY;
  private final BestHeight best;
  private final OptionalInt mountainBestY;
  private final List<OreRow> bonusRows;

  private OreProfile(
      Ore ore,
      double[] density,
      int minY,
      int maxY,
      BestHeight best,
      OptionalInt mountainBestY,
      List<OreRow> bonusRows) {
    this.ore = ore;
    this.density = density;
    this.minY = minY;
    this.maxY = maxY;
    this.best = best;
    this.mountainBestY = mountainBestY;
    this.bonusRows = List.copyOf(bonusRows);
    double max = 0.0;
    for (double value : density) {
      max = Math.max(max, value);
    }
    this.maxDensity = max;
  }

  /** Profile of {@code ore} from {@link OreTable#ROWS}. */
  public static OreProfile of(Ore ore) {
    return of(ore, OreTable.rowsOf(ore));
  }

  /** Profile of {@code ore} from the given placements (all of that ore). */
  public static OreProfile of(Ore ore, List<OreRow> rows) {
    if (rows.isEmpty()) {
      throw new IllegalArgumentException("no placements for " + ore);
    }
    OreDimension dimension = ore.dimension();
    List<OreRow> main = new ArrayList<>();
    List<OreRow> bonus = new ArrayList<>();
    for (OreRow row : rows) {
      (row.biomes().isBiomeOnly() ? bonus : main).add(row);
    }
    if (main.isEmpty()) {
      main.addAll(bonus);
      bonus.clear();
    }
    List<OreRow> underground = new ArrayList<>();
    for (OreRow row : main) {
      if (!row.isHighTerrain()) {
        underground.add(row);
      }
    }
    boolean hasMountainRows = !underground.isEmpty() && underground.size() < main.size();
    if (underground.isEmpty()) {
      underground = main;
    }

    double[] density = densities(main, dimension);
    BestHeight best = bestHeight(densities(underground, dimension), dimension);
    OptionalInt mountainBestY = OptionalInt.empty();
    if (hasMountainRows) {
      int mountainY = bestHeight(density, dimension).y();
      if (mountainY != best.y()) {
        mountainBestY = OptionalInt.of(mountainY);
      }
    }
    int minY = dimension.maxY();
    int maxY = dimension.minY();
    for (OreRow row : main) {
      minY = Math.min(minY, Math.max(row.minY(), dimension.minY()));
      maxY = Math.max(maxY, Math.min(row.maxY(), dimension.maxY()));
    }
    return new OreProfile(ore, density, minY, maxY, best, mountainBestY, bonus);
  }

  public Ore ore() {
    return ore;
  }

  /** Lowest height of the main placements, inside the world. */
  public int minY() {
    return minY;
  }

  /** Highest height of the main placements, inside the world. */
  public int maxY() {
    return maxY;
  }

  public BestHeight best() {
    return best;
  }

  /** Best height counting the placements only mountains reach, when it differs. */
  public OptionalInt mountainBestY() {
    return mountainBestY;
  }

  /** Biome-only placements of an ore also found elsewhere (badlands gold, dripstone copper). */
  public List<OreRow> bonusRows() {
    return bonusRows;
  }

  /** Density at {@code y} relative to the densest layer: 0 (none) to 1 (densest). */
  public double relativeDensityAt(int y) {
    int index = y - ore.dimension().minY();
    if (index < 0 || index >= density.length || maxDensity <= 0.0) {
      return 0.0;
    }
    return density[index] / maxDensity;
  }

  private static double[] densities(List<OreRow> rows, OreDimension dimension) {
    double[] density = new double[dimension.heightLayers()];
    for (int index = 0; index < density.length; index++) {
      int y = dimension.minY() + index;
      for (OreRow row : rows) {
        density[index] += row.densityAt(y);
      }
    }
    return density;
  }

  /** Densest layers between the bedrock layers; their middle is the one to dig at. */
  private static BestHeight bestHeight(double[] density, OreDimension dimension) {
    double max = 0.0;
    for (int y = dimension.lowestMinableY(); y <= dimension.highestMinableY(); y++) {
      max = Math.max(max, density[y - dimension.minY()]);
    }
    int fromY = Integer.MAX_VALUE;
    int toY = Integer.MIN_VALUE;
    for (int y = dimension.lowestMinableY(); y <= dimension.highestMinableY(); y++) {
      if (density[y - dimension.minY()] >= max - max * SAME_DENSITY) {
        fromY = Math.min(fromY, y);
        toY = Math.max(toY, y);
      }
    }
    return new BestHeight(fromY + (toY - fromY) / 2, fromY, toY);
  }
}
