package com.einsteinmods.cheatsheet.core.ores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class OreProfileTest {
  private static final double TOLERANCE = 1.0e-12;

  @Test
  void everyPlacementSpreadsOneTryOverItsRange() {
    for (OreRow row : OreTable.ROWS) {
      double total = 0.0;
      for (int y = row.minY(); y <= row.maxY(); y++) {
        total += row.chanceAt(y);
      }
      assertEquals(1.0, total, TOLERANCE, row.featureId());
      assertEquals(0.0, row.chanceAt(row.minY() - 1), row.featureId());
      assertEquals(0.0, row.chanceAt(row.maxY() + 1), row.featureId());
    }
  }

  @Test
  void trianglePeaksInTheMiddleAndFadesToTheEnds() {
    OreRow debris = OreTable.rowsOf(Ore.ANCIENT_DEBRIS).getLast();
    assertEquals(HeightShape.TRIANGLE, debris.shape());
    // 8..24: two draws of 0..8; 9 ways to sum 8 of 81.
    assertEquals(9.0 / 81.0, debris.chanceAt(16), TOLERANCE);
    assertEquals(1.0 / 81.0, debris.chanceAt(8), TOLERANCE);
    assertEquals(1.0 / 81.0, debris.chanceAt(24), TOLERANCE);
  }

  @Test
  void bestHeightsMatchWhatPlayersDigAt() {
    assertBest(Ore.COAL, 96, OptionalInt.of(136));
    assertBest(Ore.COPPER, 48, OptionalInt.empty());
    assertBest(Ore.IRON, 16, OptionalInt.of(232));
    assertBest(Ore.LAPIS, 0, OptionalInt.empty());
    assertBest(Ore.GOLD, -16, OptionalInt.empty());
    assertBest(Ore.REDSTONE, -59, OptionalInt.empty());
    assertBest(Ore.DIAMOND, -59, OptionalInt.empty());
    assertBest(Ore.EMERALD, 232, OptionalInt.empty());
    assertBest(Ore.ANCIENT_DEBRIS, 16, OptionalInt.empty());
  }

  @Test
  void netherGoldAndQuartzAreEquallyCommonAtEveryHeight() {
    for (Ore ore : new Ore[] {Ore.NETHER_GOLD, Ore.NETHER_QUARTZ}) {
      BestHeight best = OreProfile.of(ore).best();
      assertTrue(best.isFlat(), ore + " is flat");
      assertEquals(10, best.fromY(), ore + " from");
      assertEquals(117, best.toY(), ore + " to");
    }
  }

  @Test
  void biomeBonusesStayOutOfTheMainSpread() {
    OreProfile gold = OreProfile.of(Ore.GOLD);
    assertEquals(1, gold.bonusRows().size());
    assertEquals("ore_gold_extra", gold.bonusRows().getFirst().featureId());
    assertEquals(-64, gold.minY());
    assertEquals(32, gold.maxY());
    // Emerald only exists in mountain biomes: those placements are its main spread.
    OreProfile emerald = OreProfile.of(Ore.EMERALD);
    assertTrue(emerald.bonusRows().isEmpty());
    assertEquals(-16, emerald.minY());
    assertEquals(319, emerald.maxY());
  }

  @Test
  void relativeDensityIsOneAtTheDensestLayerAndZeroOutside() {
    OreProfile copper = OreProfile.of(Ore.COPPER);
    assertEquals(1.0, copper.relativeDensityAt(48), TOLERANCE);
    assertEquals(0.0, copper.relativeDensityAt(113));
    assertEquals(0.0, copper.relativeDensityAt(-1000));
    assertFalse(copper.relativeDensityAt(0) >= 1.0);
  }

  @Test
  void deepOresShowTheirDeepslateVariant() {
    assertEquals("minecraft:deepslate_diamond_ore", Ore.DIAMOND.itemAt(-59));
    assertEquals("minecraft:coal_ore", Ore.COAL.itemAt(96));
    assertEquals("minecraft:deepslate_lapis_ore", Ore.LAPIS.itemAt(0));
  }

  private static void assertBest(Ore ore, int expectedY, OptionalInt expectedMountainY) {
    OreProfile profile = OreProfile.of(ore);
    assertEquals(expectedY, profile.best().y(), ore + " best Y");
    assertFalse(profile.best().isFlat(), ore + " has a peak");
    assertEquals(expectedMountainY, profile.mountainBestY(), ore + " mountain Y");
  }
}
