package com.einsteinmods.cheatsheet.core.farming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FarmPlansTest {
  /**
   * Canes per layout on the 16x16 field, as the reference picture labels them: 50%, 62.5%, 74.2%
   * and 75%. Its drawing of the third holds 192 (12 free blocks); ours frees two more for a gate.
   */
  private static final List<Integer> CANES = List.of(128, 160, 190, 192);

  @Test
  void sugarCaneLayoutsFillTheFieldWithTheirCanesAndEveryCaneTouchesWater() {
    assertEquals(CANES.size(), FarmPlans.CANE_LAYOUTS.size());
    for (int layout = 0; layout < CANES.size(); layout++) {
      FarmPlan plan = FarmPlans.CANE_LAYOUTS.get(layout);
      assertEquals(FarmPlans.CANE_FIELD, plan.width());
      assertEquals(FarmPlans.CANE_FIELD, plan.height());
      assertEquals(CANES.get(layout), plan.count(PlanCell.SUGAR_CANE), "layout " + (layout + 1));
      for (int y = 0; y < plan.height(); y++) {
        for (int x = 0; x < plan.width(); x++) {
          PlanCell cell = plan.cell(x, y);
          if (cell == PlanCell.SUGAR_CANE) {
            assertTrue(FarmRules.caneHasWater(plan, x, y), "dry cane at " + x + "," + y);
          } else {
            assertTrue(cell == PlanCell.WATER || cell == PlanCell.DIRT, cell + " in a cane field");
          }
        }
      }
    }
  }

  @Test
  void theDensestLayoutLeavesNoBlockFreeAndTheCrossesLeaveSome() {
    assertEquals(0, FarmPlans.CANE_DENSEST.count(PlanCell.DIRT));
    assertEquals(64, FarmPlans.CANE_DENSEST.count(PlanCell.WATER));
    assertEquals(14, FarmPlans.CANE_CROSSES.count(PlanCell.DIRT));
    assertEquals(52, FarmPlans.CANE_CROSSES.count(PlanCell.WATER));
  }

  @Test
  void theCrossesOpenATwoBlockGateInTheMiddleOfTheNorthAndSouthEdges() {
    FarmPlan plan = FarmPlans.CANE_CROSSES;
    int middle = FarmPlans.CANE_FIELD / 2;
    int south = FarmPlans.CANE_FIELD - 1;
    assertEquals(PlanCell.DIRT, plan.cell(middle - 1, 0));
    assertEquals(PlanCell.DIRT, plan.cell(middle, 0));
    assertEquals(PlanCell.DIRT, plan.cell(middle - 1, south));
    assertEquals(PlanCell.DIRT, plan.cell(middle, south));
  }

  @Test
  void caneNeedsWaterOnASideNotADiagonal() {
    assertTrue(FarmRules.caneHasWater(FarmPlan.of("SW"), 0, 0));
    assertFalse(FarmRules.caneHasWater(FarmPlan.of("SD", "DW"), 0, 0));
  }

  @Test
  void theFieldIsOneWaterBlockAndAllOfItIsHydratedAtFullSpeed() {
    FarmPlan plan = FarmPlans.FIELD;
    assertEquals(FarmRules.HYDRATED_SIDE, plan.width());
    assertEquals(FarmRules.HYDRATED_SIDE, plan.height());
    int water = 0;
    for (int y = 0; y < plan.height(); y++) {
      for (int x = 0; x < plan.width(); x++) {
        if (plan.cell(x, y) == PlanCell.WATER) {
          water++;
          continue;
        }
        assertTrue(plan.cell(x, y).isCrop());
        assertTrue(FarmRules.isHydrated(plan, x, y), "dry at " + x + "," + y);
        assertTrue(FarmRules.growsAtFullSpeed(plan, x, y), "slowed at " + x + "," + y);
      }
    }
    assertEquals(1, water);
  }

  @Test
  void aFieldOfOneCropIsSlowedAndRowsOfOneCropAreNot() {
    FarmPlan block = FarmPlan.of("HHH", "HHH", "HHH");
    assertFalse(FarmRules.growsAtFullSpeed(block, 1, 1));
    FarmPlan diagonal = FarmPlan.of("HD", "DH");
    assertFalse(FarmRules.growsAtFullSpeed(diagonal, 0, 0));
    FarmPlan row = FarmPlan.of("HHH", "RRR");
    assertTrue(FarmRules.growsAtFullSpeed(row, 1, 0));
  }

  @Test
  void waterReachesFourBlocksAndNoFurther() {
    FarmPlan plan = FarmPlan.of("WDDDDD");
    assertTrue(FarmRules.isHydrated(plan, FarmRules.HYDRATION_REACH, 0));
    assertFalse(FarmRules.isHydrated(plan, FarmRules.HYDRATION_REACH + 1, 0));
  }

  @Test
  void everyStemHasAFreeBlockForItsFruitAndGrowsAtFullSpeed() {
    FarmPlan plan = FarmPlans.STEMS;
    Set<PlanCell> stems = new HashSet<>();
    for (int y = 0; y < plan.height(); y++) {
      for (int x = 0; x < plan.width(); x++) {
        PlanCell cell = plan.cell(x, y);
        if (cell == PlanCell.PUMPKIN_STEM || cell == PlanCell.MELON_STEM) {
          stems.add(cell);
          assertTrue(FarmRules.stemHasFruitSpot(plan, x, y), "no spot at " + x + "," + y);
          assertTrue(FarmRules.isHydrated(plan, x, y));
          assertTrue(FarmRules.growsAtFullSpeed(plan, x, y));
        }
      }
    }
    assertEquals(Set.of(PlanCell.PUMPKIN_STEM, PlanCell.MELON_STEM), stems);
    assertFalse(FarmRules.stemHasFruitSpot(FarmPlan.of("WpW"), 1, 0));
  }

  @Test
  void noCactusTouchesAnother() {
    FarmPlan plan = FarmPlans.CACTUS;
    int cactus = 0;
    for (int y = 0; y < plan.height(); y++) {
      for (int x = 0; x < plan.width(); x++) {
        if (plan.cell(x, y) == PlanCell.CACTUS) {
          assertTrue(FarmRules.cactusHasFreeSides(plan, x, y), "touching at " + x + "," + y);
          cactus++;
        } else {
          assertEquals(PlanCell.SAND, plan.cell(x, y));
        }
      }
    }
    assertTrue(cactus > 0);
    assertFalse(FarmRules.cactusHasFreeSides(FarmPlan.of("XX"), 0, 0));
  }

  @Test
  void plansRejectUnknownSymbolsAndRaggedRows() {
    assertThrows(IllegalArgumentException.class, () -> FarmPlan.of("W?"));
    assertThrows(IllegalArgumentException.class, () -> FarmPlan.of("WW", "W"));
  }
}
