package com.einsteinmods.cheatsheet.core.farming;

/**
 * The vanilla rules the farming plans show, checked on a {@link FarmPlan} (one layer, seen from
 * above). Each follows the vanilla method it names.
 */
public final class FarmRules {
  /**
   * Farmland is hydrated by water up to this many blocks away, in a square ({@code
   * FarmlandBlock#isNearWater}: offsets -4..4 on both axes, same height or one block up).
   */
  public static final int HYDRATION_REACH = 4;

  /** Side of the square one water block hydrates: 9. */
  public static final int HYDRATED_SIDE = 2 * HYDRATION_REACH + 1;

  /** Sugar cane and cactus stop growing at this height ({@code randomTick}: grow while below 3). */
  public static final int MAX_PLANT_HEIGHT = 3;

  private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
  private static final int[][] DIAGONALS = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

  private FarmRules() {}

  /**
   * Whether sugar cane at ({@code x}, {@code y}) has water right beside it, not diagonally ({@code
   * SugarCaneBlock#canSurvive} looks at the four sides of the block below; the plan is that layer).
   */
  public static boolean caneHasWater(FarmPlan plan, int x, int y) {
    return hasSide(plan, x, y, PlanCell.WATER);
  }

  /** Whether some water of the plan is within {@link #HYDRATION_REACH} on both axes. */
  public static boolean isHydrated(FarmPlan plan, int x, int y) {
    for (int dy = -HYDRATION_REACH; dy <= HYDRATION_REACH; dy++) {
      for (int dx = -HYDRATION_REACH; dx <= HYDRATION_REACH; dx++) {
        if (plan.contains(x + dx, y + dy) && plan.cell(x + dx, y + dy) == PlanCell.WATER) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Whether a crop grows at full speed by its neighbours ({@code CropBlock#getGrowthSpeed}, also
   * used by stems): it is halved when the same crop is beside it both east-west and north-south,
   * or, failing that, on any diagonal. Rows of one crop beside rows of another stay at full speed.
   */
  public static boolean growsAtFullSpeed(FarmPlan plan, int x, int y) {
    PlanCell crop = plan.cell(x, y);
    boolean eastWest = is(plan, x + 1, y, crop) || is(plan, x - 1, y, crop);
    boolean northSouth = is(plan, x, y + 1, crop) || is(plan, x, y - 1, crop);
    if (eastWest && northSouth) {
      return false;
    }
    for (int[] diagonal : DIAGONALS) {
      if (is(plan, x + diagonal[0], y + diagonal[1], crop)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Whether a cactus at ({@code x}, {@code y}) survives its sides ({@code CactusBlock#canSurvive}:
   * no solid block on any side, and another cactus is solid). Ground cells (sand, dirt) are the
   * layer below, so the air above them is free.
   */
  public static boolean cactusHasFreeSides(FarmPlan plan, int x, int y) {
    for (int[] side : SIDES) {
      int sideX = x + side[0];
      int sideY = y + side[1];
      if (plan.contains(sideX, sideY) && isSolidAtPlantHeight(plan.cell(sideX, sideY))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Whether a stem at ({@code x}, {@code y}) has a side where its fruit can grow ({@code
   * StemBlock#randomTick}: a random side that is air above dirt, grass or farmland): bare ground or
   * its fruit already there.
   */
  public static boolean stemHasFruitSpot(FarmPlan plan, int x, int y) {
    PlanCell fruit = plan.cell(x, y).fruit();
    return hasSide(plan, x, y, PlanCell.DIRT) || hasSide(plan, x, y, fruit);
  }

  private static boolean isSolidAtPlantHeight(PlanCell cell) {
    return cell == PlanCell.CACTUS || cell == PlanCell.PUMPKIN || cell == PlanCell.MELON;
  }

  private static boolean hasSide(FarmPlan plan, int x, int y, PlanCell wanted) {
    for (int[] side : SIDES) {
      if (is(plan, x + side[0], y + side[1], wanted)) {
        return true;
      }
    }
    return false;
  }

  private static boolean is(FarmPlan plan, int x, int y, PlanCell wanted) {
    return plan.contains(x, y) && plan.cell(x, y) == wanted;
  }
}
