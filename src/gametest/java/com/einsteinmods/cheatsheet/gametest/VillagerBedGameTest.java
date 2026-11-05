package com.einsteinmods.cheatsheet.gametest;

import com.einsteinmods.cheatsheet.core.mobfarms.FarmRules;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;

/**
 * What a villager's bed needs over it, measured with real villagers: sleeping asks for nothing, a
 * baby's bed asks for {@link FarmRules#BED_FREE_BLOCKS_ABOVE} free blocks, and waking up in an
 * enclosed bed with one free block suffocates. Each test runs its beds one after the other (a
 * villager looks for beds 48 blocks around, so tests running side by side would share theirs).
 */
public final class VillagerBedGameTest {
  /** The time the villagers are asleep and awake, by test environment. */
  private static final String NIGHT = "cheatsheet-gametest:night";

  private static final String MORNING = "cheatsheet-gametest:morning";

  private static final int SIDE = 8;
  private static final int FLOOR_Y = 1;
  private static final int RIM_TOP_Y = 6;
  private static final BlockPos HEAD = new BlockPos(4, 2, 3);
  private static final BlockPos FOOT = HEAD.south();
  private static final BlockPos START = new BlockPos(1, 2, 5);

  /** Parents are told their home is here, so that they leave the bed under test unclaimed. */
  private static final BlockPos ELSEWHERE = new BlockPos(1, 2, 1);

  private static final int SETTLE_TICKS = 6;
  private static final int POLL_TICKS = 5;

  /** Long enough for a claim and a walk: seen within 95 ticks. */
  private static final int SLEEP_TICKS = 400;

  /** Two rounds of breeding, seen within 360 ticks; a failed round must also have run. */
  private static final int BIRTH_TICKS = 1500;

  private static final int WAKE_TICKS = 50;
  private static final int PARENT_BREAD = 32;

  /** Blocks over the head and foot of the bed, by height above it (1 = touching it). */
  private record Roof(int overHead, int overFoot) {
    static final int NONE = 0;
  }

  private static void clear(GameTestHelper helper) {
    helper.killAllEntities();
    for (int x = 0; x < SIDE; x++) {
      for (int z = 0; z < SIDE; z++) {
        for (int y = FLOOR_Y; y < SIDE; y++) {
          boolean rim = x == 0 || z == 0 || x == SIDE - 1 || z == SIDE - 1;
          boolean solid = y == FLOOR_Y || (rim && y <= RIM_TOP_Y);
          helper.setBlock(new BlockPos(x, y, z), solid ? Blocks.STONE : Blocks.AIR);
        }
      }
    }
  }

  private static void bed(GameTestHelper helper, Roof roof) {
    helper.setBlock(HEAD, DioramaPlacer.state("red_bed[facing=north,part=head]"));
    helper.setBlock(FOOT, DioramaPlacer.state("red_bed[facing=north,part=foot]"));
    if (roof.overHead() != Roof.NONE) {
      helper.setBlock(HEAD.above(roof.overHead()), Blocks.STONE);
    }
    if (roof.overFoot() != Roof.NONE) {
      helper.setBlock(FOOT.above(roof.overFoot()), Blocks.STONE);
    }
  }

  private static GlobalPos home(GameTestHelper helper, BlockPos pos) {
    return GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(pos));
  }

  /** {@code VillagerMakeLove#canReach}: the same call, for the bed's POI range of 1. */
  private static boolean countsForABaby(GameTestHelper helper, Villager villager) {
    Path path = villager.getNavigation().createPath(helper.absolutePos(HEAD), 1);
    return path != null && path.canReach();
  }

  /** A bed with no free block over it, one, or the sheet's: all are slept in, unclaimed before. */
  @GameTest(environment = NIGHT, maxTicks = SLEEP_TICKS * 4)
  public void villagersSleepWithBlocksOverTheBed(GameTestHelper helper) {
    int free = FarmRules.BED_FREE_BLOCKS_ABOVE;
    sleepIn(helper, List.of(new Roof(1, 1), new Roof(free, free), new Roof(free + 1, free + 1)), 0);
  }

  private static void sleepIn(GameTestHelper helper, List<Roof> roofs, int index) {
    if (index == roofs.size()) {
      helper.succeed();
      return;
    }
    clear(helper);
    bed(helper, roofs.get(index));
    helper.runAfterDelay(
        POLL_TICKS,
        () -> waitForSleep(helper, helper.spawn(EntityTypes.VILLAGER, START), roofs, index, 0));
  }

  private static void waitForSleep(
      GameTestHelper helper, Villager villager, List<Roof> roofs, int index, int waited) {
    if (villager.isSleeping()) {
      sleepIn(helper, roofs, index + 1);
    } else if (waited >= SLEEP_TICKS) {
      helper.fail("no sleep under " + roofs.get(index));
    } else {
      helper.runAfterDelay(
          POLL_TICKS, () -> waitForSleep(helper, villager, roofs, index, waited + POLL_TICKS));
    }
  }

  /**
   * The rule the text gives, with real parents: a block on the pillow or a single free block over
   * the whole bed means no baby, however long they try; the free blocks asked for mean one, also
   * with a block right over the foot.
   */
  @GameTest(environment = MORNING, maxTicks = BIRTH_TICKS * 4)
  public void babiesNeedFreeBlocksOverTheBed(GameTestHelper helper) {
    int free = FarmRules.BED_FREE_BLOCKS_ABOVE;
    List<Roof> refused = List.of(new Roof(1, Roof.NONE), new Roof(free, free));
    List<Roof> accepted = List.of(new Roof(free + 1, free + 1), new Roof(Roof.NONE, 1));
    List<Roof> roofs = new ArrayList<>(refused);
    roofs.addAll(accepted);
    breedUnder(helper, roofs, refused.size(), 0);
  }

  private static void breedUnder(GameTestHelper helper, List<Roof> roofs, int refused, int index) {
    if (index == roofs.size()) {
      helper.succeed();
      return;
    }
    clear(helper);
    bed(helper, roofs.get(index));
    helper.runAfterDelay(
        POLL_TICKS,
        () -> {
          Villager parent = helper.spawn(EntityTypes.VILLAGER, START);
          Villager other = helper.spawn(EntityTypes.VILLAGER, START.south());
          for (Villager villager : List.of(parent, other)) {
            villager.getInventory().addItem(new ItemStack(Items.BREAD, PARENT_BREAD));
            villager.getBrain().setMemory(MemoryModuleType.HOME, home(helper, ELSEWHERE));
          }
          boolean expected = index >= refused;
          helper.runAfterDelay(
              SETTLE_TICKS,
              () ->
                  helper.assertValueEqual(
                      countsForABaby(helper, parent), expected, "reach " + roofs.get(index)));
          waitForBaby(helper, roofs, refused, index, 0);
        });
  }

  private static void waitForBaby(
      GameTestHelper helper, List<Roof> roofs, int refused, int index, int waited) {
    boolean born = helper.getEntities(EntityTypes.VILLAGER).stream().anyMatch(Villager::isBaby);
    boolean expected = index >= refused;
    if (born && !expected) {
      helper.fail("a baby under " + roofs.get(index));
    } else if (born || (waited >= BIRTH_TICKS && !expected)) {
      breedUnder(helper, roofs, refused, index + 1);
    } else if (waited >= BIRTH_TICKS) {
      helper.fail("no baby under " + roofs.get(index));
    } else {
      helper.runAfterDelay(
          POLL_TICKS, () -> waitForBaby(helper, roofs, refused, index, waited + POLL_TICKS));
    }
  }

  /**
   * With every block around the bed taken, a villager wakes on top of it: one free block over the
   * bed puts its head in the roof; the free blocks asked for leave it unhurt.
   */
  @GameTest(maxTicks = WAKE_TICKS * 4)
  public void wakingInAnEnclosedBedNeedsRoom(GameTestHelper helper) {
    int free = FarmRules.BED_FREE_BLOCKS_ABOVE;
    wakeUnder(helper, free, true, () -> wakeUnder(helper, free + 1, false, helper::succeed));
  }

  private static void wakeUnder(
      GameTestHelper helper, int roofHeight, boolean hurts, Runnable next) {
    clear(helper);
    bed(helper, new Roof(roofHeight, roofHeight));
    for (int x = HEAD.getX() - 1; x <= HEAD.getX() + 1; x++) {
      for (int z = HEAD.getZ() - 1; z <= FOOT.getZ() + 1; z++) {
        BlockPos column = new BlockPos(x, HEAD.getY(), z);
        if (!column.equals(HEAD) && !column.equals(FOOT)) {
          for (int up = 0; up <= roofHeight; up++) {
            helper.setBlock(column.above(up), Blocks.STONE);
          }
        }
      }
    }
    helper.runAfterDelay(
        POLL_TICKS,
        () -> {
          Villager villager = helper.spawn(EntityTypes.VILLAGER, HEAD);
          helper.assertTrue(villager.startSleeping(helper.absolutePos(HEAD)), "goes to sleep");
          helper.runAfterDelay(
              SETTLE_TICKS,
              () -> {
                villager.stopSleeping();
                helper.runAfterDelay(
                    WAKE_TICKS,
                    () -> {
                      helper.assertValueEqual(
                          villager.getHealth() < villager.getMaxHealth(),
                          hurts,
                          "hurt waking under a block at " + roofHeight);
                      next.run();
                    });
              });
        });
  }
}
