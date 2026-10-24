package com.einsteinmods.cheatsheet.core.ores;

import static com.einsteinmods.cheatsheet.core.ores.HeightShape.TRIANGLE;
import static com.einsteinmods.cheatsheet.core.ores.HeightShape.UNIFORM;

import java.util.ArrayList;
import java.util.List;

/**
 * Vanilla ore placements of Minecraft 26.3 ({@code OrePlacements.bootstrap}, biomes from {@code
 * BiomeDefaultFeatures} and {@code NetherBiomes}). Placed features live on the server only (they
 * are not synced), so the client reads this fixed copy; {@code OrePlacementGameTest} compares it
 * with the game's registry and fails the build when an update changes ore generation.
 */
public final class OreTable {
  private static final String BASALT_DELTAS = "basalt_deltas";
  private static final String DRIPSTONE_CAVES = "dripstone_caves";

  /** Every row, grouped by ore in chart order. */
  public static final List<OreRow> ROWS =
      List.of(
          row("ore_coal_upper", Ore.COAL, 136, 319, UNIFORM, PlacementCount.exactly(30)),
          row("ore_coal_lower", Ore.COAL, 0, 192, TRIANGLE, PlacementCount.exactly(20)),
          new OreRow(
              "ore_copper",
              Ore.COPPER,
              -16,
              112,
              TRIANGLE,
              PlacementCount.exactly(16),
              BiomeRule.except(DRIPSTONE_CAVES)),
          new OreRow(
              "ore_copper_large",
              Ore.COPPER,
              -16,
              112,
              TRIANGLE,
              PlacementCount.exactly(16),
              BiomeRule.only(DRIPSTONE_CAVES)),
          row("ore_iron_upper", Ore.IRON, 80, 384, TRIANGLE, PlacementCount.exactly(90)),
          row("ore_iron_middle", Ore.IRON, -24, 56, TRIANGLE, PlacementCount.exactly(10)),
          row("ore_iron_small", Ore.IRON, -64, 72, UNIFORM, PlacementCount.exactly(10)),
          row("ore_lapis", Ore.LAPIS, -32, 32, TRIANGLE, PlacementCount.exactly(2)),
          row("ore_lapis_buried", Ore.LAPIS, -64, 64, UNIFORM, PlacementCount.exactly(4)),
          row("ore_gold", Ore.GOLD, -64, 32, TRIANGLE, PlacementCount.exactly(4)),
          row("ore_gold_lower", Ore.GOLD, -64, -48, UNIFORM, PlacementCount.between(0, 1)),
          new OreRow(
              "ore_gold_extra",
              Ore.GOLD,
              32,
              256,
              UNIFORM,
              PlacementCount.exactly(50),
              BiomeRule.only("badlands", "eroded_badlands", "wooded_badlands")),
          row("ore_redstone", Ore.REDSTONE, -64, 15, UNIFORM, PlacementCount.exactly(4)),
          row("ore_redstone_lower", Ore.REDSTONE, -96, -32, TRIANGLE, PlacementCount.exactly(8)),
          row("ore_diamond", Ore.DIAMOND, -144, 16, TRIANGLE, PlacementCount.exactly(7)),
          row("ore_diamond_buried", Ore.DIAMOND, -144, 16, TRIANGLE, PlacementCount.exactly(4)),
          row("ore_diamond_medium", Ore.DIAMOND, -64, -4, UNIFORM, PlacementCount.exactly(2)),
          row("ore_diamond_large", Ore.DIAMOND, -144, 16, TRIANGLE, PlacementCount.oneInChunks(9)),
          new OreRow(
              "ore_emerald",
              Ore.EMERALD,
              -16,
              480,
              TRIANGLE,
              PlacementCount.exactly(100),
              BiomeRule.only(
                  "windswept_hills",
                  "windswept_gravelly_hills",
                  "windswept_forest",
                  "meadow",
                  "grove",
                  "snowy_slopes",
                  "cherry_grove",
                  "jagged_peaks",
                  "frozen_peaks",
                  "stony_peaks")),
          new OreRow(
              "ore_gold_nether",
              Ore.NETHER_GOLD,
              10,
              117,
              UNIFORM,
              PlacementCount.exactly(10),
              BiomeRule.except(BASALT_DELTAS)),
          new OreRow(
              "ore_gold_deltas",
              Ore.NETHER_GOLD,
              10,
              117,
              UNIFORM,
              PlacementCount.exactly(20),
              BiomeRule.only(BASALT_DELTAS)),
          new OreRow(
              "ore_quartz_nether",
              Ore.NETHER_QUARTZ,
              10,
              117,
              UNIFORM,
              PlacementCount.exactly(16),
              BiomeRule.except(BASALT_DELTAS)),
          new OreRow(
              "ore_quartz_deltas",
              Ore.NETHER_QUARTZ,
              10,
              117,
              UNIFORM,
              PlacementCount.exactly(32),
              BiomeRule.only(BASALT_DELTAS)),
          row("ore_debris_small", Ore.ANCIENT_DEBRIS, 8, 119, UNIFORM, PlacementCount.exactly(1)),
          row(
              "ore_ancient_debris_large",
              Ore.ANCIENT_DEBRIS,
              8,
              24,
              TRIANGLE,
              PlacementCount.exactly(1)));

  private OreTable() {}

  /** Rows of one ore, in table order. */
  public static List<OreRow> rowsOf(Ore ore) {
    List<OreRow> rows = new ArrayList<>();
    for (OreRow row : ROWS) {
      if (row.ore() == ore) {
        rows.add(row);
      }
    }
    return List.copyOf(rows);
  }

  private static OreRow row(
      String featureId, Ore ore, int minY, int maxY, HeightShape shape, PlacementCount count) {
    return new OreRow(featureId, ore, minY, maxY, shape, count, BiomeRule.ALL);
  }
}
