package com.einsteinmods.cheatsheet.gametest;

import com.einsteinmods.cheatsheet.core.redstone.ComparatorSignal;
import com.einsteinmods.cheatsheet.core.redstone.CrafterRules;
import com.einsteinmods.cheatsheet.core.redstone.RedstoneRules;
import com.einsteinmods.cheatsheet.core.redstone.SignalContainer;
import com.einsteinmods.cheatsheet.core.redstone.SpecialSignals;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.entity.CreakingHeartBlockEntity;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The redstone tab's comparator table ({@link SignalContainer}) and special blocks ({@link
 * SpecialSignals}) against the game: real containers are filled with the table's counts (and one
 * item fewer) and a comparator behind each is read; every special state is placed and its signal
 * read.
 */
public final class ComparatorSignalGameTest {
  private static final int SETTLE = 6;

  /** Where cases stand: a row along z, two blocks apart so chests never join. */
  private static final int CASE_SPACING = 2;

  /** Signals checked per container: the first, a middle one (and one item fewer), the last. */
  private static final int MIDDLE = 8;

  /** Long enough for a heart's or a detector rail's update to reach a comparator. */
  private static final int ENTITY_SETTLE = 12;

  /** A distance from a heart to its creaking, inside the test's box. */
  private static final int CREAKING_DISTANCE = 6;

  /** Armour stands a command runs on, one success each. */
  private static final int COMMAND_TARGETS = 3;

  /** Near the edges of a block's face (block fractions), to aim at its outer slots. */
  private static final double FACE_LOW = 0.1;

  private static final double FACE_HIGH = 0.9;
  private static final double FACE_MIDDLE = 0.5;

  @GameTest(maxTicks = SETTLE * 2)
  public void chestCounts(GameTestHelper helper) {
    check(helper, SignalContainer.CHEST, Blocks.CHEST);
  }

  @GameTest(maxTicks = SETTLE * 2)
  public void doubleChestCounts(GameTestHelper helper) {
    check(helper, SignalContainer.DOUBLE_CHEST, Blocks.CHEST);
  }

  @GameTest(maxTicks = SETTLE * 2)
  public void barrelCounts(GameTestHelper helper) {
    check(helper, SignalContainer.CHEST, Blocks.BARREL);
  }

  @GameTest(maxTicks = SETTLE * 2)
  public void shulkerBoxCounts(GameTestHelper helper) {
    check(helper, SignalContainer.CHEST, Blocks.SHULKER_BOX);
  }

  @GameTest(maxTicks = SETTLE * 2)
  public void dropperCounts(GameTestHelper helper) {
    check(helper, SignalContainer.DISPENSER, Blocks.DROPPER);
  }

  @GameTest(maxTicks = SETTLE * 2)
  public void hopperCounts(GameTestHelper helper) {
    check(helper, SignalContainer.HOPPER, Blocks.HOPPER);
  }

  @GameTest(maxTicks = SETTLE * 2)
  public void brewingStandCounts(GameTestHelper helper) {
    check(helper, SignalContainer.HOPPER, Blocks.BREWING_STAND);
  }

  @GameTest(maxTicks = SETTLE * 2)
  public void furnaceCounts(GameTestHelper helper) {
    check(helper, SignalContainer.FURNACE, Blocks.FURNACE);
  }

  @GameTest(maxTicks = SETTLE * 2)
  public void decoratedPotCounts(GameTestHelper helper) {
    check(helper, SignalContainer.DECORATED_POT, Blocks.DECORATED_POT);
  }

  @GameTest(maxTicks = ENTITY_SETTLE * 2)
  public void chestMinecartCounts(GameTestHelper helper) {
    checkMinecart(helper, SignalContainer.CHEST, EntityTypes.CHEST_MINECART);
  }

  @GameTest(maxTicks = ENTITY_SETTLE * 2)
  public void hopperMinecartCounts(GameTestHelper helper) {
    checkMinecart(helper, SignalContainer.HOPPER, EntityTypes.HOPPER_MINECART);
  }

  /** A full stack counts as one slot; a disabled slot counts too; all nine give 9. */
  @GameTest(maxTicks = SETTLE * 2)
  public void crafterCountsSlotsNotItems(GameTestHelper helper) {
    BlockPos oneStack = new BlockPos(1, 1, 1);
    BlockPos everySlot = new BlockPos(1, 1, 3);
    CrafterBlockEntity first = crafterWithComparator(helper, oneStack);
    CrafterBlockEntity second = crafterWithComparator(helper, everySlot);
    first.setItem(0, new ItemStack(Items.COBBLESTONE, Items.COBBLESTONE.getDefaultMaxStackSize()));
    first.setSlotState(1, false);
    second.setSlotState(0, false);
    for (int slot = 1; slot < CrafterRules.SLOTS; slot++) {
      second.setItem(slot, new ItemStack(Items.COBBLESTONE));
    }
    first.setChanged();
    second.setChanged();
    helper.runAfterDelay(
        SETTLE,
        () -> {
          helper.assertValueEqual(
              comparatorOutput(helper, oneStack.east()),
              CrafterRules.comparatorSignal(1, 1),
              "a full stack and a disabled slot");
          helper.assertValueEqual(
              comparatorOutput(helper, everySlot.east()),
              CrafterRules.comparatorSignal(CrafterRules.SLOTS - 1, 1),
              "every slot in use");
          helper.succeed();
        });
  }

  /** Worked example: the input alone comes out. */
  @GameTest(maxTicks = SETTLE * 3)
  public void exampleInputAlone(GameTestHelper helper) {
    checkExample(helper, null, false, 0);
  }

  /** Worked example: a side as strong as the input lets it through. */
  @GameTest(maxTicks = SETTLE * 3)
  public void exampleSideEqual(GameTestHelper helper) {
    checkExample(helper, "cake[bites=2]", false, ComparatorSignal.EXAMPLE_INPUT);
  }

  /** Worked example: a stronger side turns the output off. */
  @GameTest(maxTicks = SETTLE * 3)
  public void exampleSideStronger(GameTestHelper helper) {
    checkExample(
        helper, "respawn_anchor[charges=3]", false, ComparatorSignal.EXAMPLE_STRONGER_SIDE);
  }

  /** Worked example: subtract takes the side away. */
  @GameTest(maxTicks = SETTLE * 3)
  public void exampleSubtract(GameTestHelper helper) {
    checkExample(helper, "cake[bites=5]", true, ComparatorSignal.EXAMPLE_SUBTRACTED_SIDE);
  }

  /**
   * A shelf gives a bit per slot in use, only to a comparator behind it; the left slot seen from
   * the front is the first.
   */
  @GameTest(maxTicks = SETTLE * 2)
  public void shelfGivesBitsFromBehind(GameTestHelper helper) {
    BlockPos shelf = new BlockPos(1, 1, 1);
    BlockPos absolute = helper.absolutePos(shelf);
    BlockPos behind = shelf.south();
    BlockPos inFront = shelf.north();
    helper.setBlock(behind.below(), Blocks.STONE);
    helper.setBlock(inFront.below(), Blocks.STONE);
    helper.setBlock(behind, DioramaPlacer.state("comparator[facing=north]"));
    helper.setBlock(inFront, DioramaPlacer.state("comparator[facing=south]"));
    helper.setBlock(shelf, DioramaPlacer.state("oak_shelf[facing=north]"));
    ShelfBlockEntity entity = helper.getBlockEntity(shelf, ShelfBlockEntity.class);
    BlockState state = helper.getBlockState(shelf);
    helper.assertValueEqual(entity.getContainerSize(), 3, "shelf slots");
    for (int bits = 0; bits < 1 << entity.getContainerSize(); bits++) {
      boolean[] used = new boolean[entity.getContainerSize()];
      for (int slot = 0; slot < used.length; slot++) {
        used[slot] = (bits >> slot & 1) != 0;
        entity.setItem(slot, used[slot] ? new ItemStack(Items.BOOK) : ItemStack.EMPTY);
      }
      helper.assertValueEqual(
          state.getAnalogOutputSignal(helper.getLevel(), absolute, Direction.SOUTH),
          SpecialSignals.shelf(used[0], used[1], used[2]),
          "shelf from behind with " + bits);
    }
    ShelfBlock block = (ShelfBlock) state.getBlock();
    // Looking at a north face the viewer's left is the east.
    helper.assertValueEqual(
        block.getHitSlot(hitNorthFace(FACE_HIGH, FACE_MIDDLE), Direction.NORTH).orElseThrow(),
        0,
        "left slot");
    helper.assertValueEqual(
        block.getHitSlot(hitNorthFace(FACE_LOW, FACE_MIDDLE), Direction.NORTH).orElseThrow(),
        entity.getContainerSize() - 1,
        "right slot");
    entity.setItem(0, ItemStack.EMPTY);
    entity.setItem(1, ItemStack.EMPTY);
    entity.setItem(2, new ItemStack(Items.BOOK));
    entity.setChanged();
    helper.runAfterDelay(
        SETTLE,
        () -> {
          helper.assertValueEqual(
              comparatorOutput(helper, behind), SpecialSignals.SHELF_RIGHT, "comparator behind");
          helper.assertValueEqual(comparatorOutput(helper, inFront), 0, "comparator in front");
          helper.succeed();
        });
  }

  /**
   * A chiseled bookshelf gives its last used slot, books taken out too; slots count from the top.
   */
  @GameTest(maxTicks = SETTLE * 2)
  public void chiseledBookshelfGivesItsLastSlot(GameTestHelper helper) {
    BlockPos shelf = new BlockPos(1, 1, 1);
    BlockPos comparator = shelf.east();
    helper.setBlock(comparator.below(), Blocks.STONE);
    helper.setBlock(comparator, DioramaPlacer.state("comparator[facing=west]"));
    helper.setBlock(shelf, DioramaPlacer.state("chiseled_bookshelf[facing=north]"));
    ChiseledBookShelfBlockEntity entity =
        helper.getBlockEntity(shelf, ChiseledBookShelfBlockEntity.class);
    BlockPos absolute = helper.absolutePos(shelf);
    helper.assertValueEqual(
        helper
            .getBlockState(shelf)
            .getAnalogOutputSignal(helper.getLevel(), absolute, Direction.EAST),
        0,
        "never used");
    ChiseledBookShelfBlock block = (ChiseledBookShelfBlock) helper.getBlockState(shelf).getBlock();
    helper.assertValueEqual(
        block.getHitSlot(hitNorthFace(FACE_HIGH, FACE_HIGH), Direction.NORTH).orElseThrow(),
        0,
        "top left slot");
    helper.assertValueEqual(
        block.getHitSlot(hitNorthFace(FACE_LOW, FACE_LOW), Direction.NORTH).orElseThrow(),
        SpecialSignals.CHISELED_BOOKSHELF_SLOTS - 1,
        "bottom right slot");
    int last = SpecialSignals.CHISELED_BOOKSHELF_SLOTS - 1;
    for (int slot = 0; slot <= last; slot++) {
      entity.setItem(slot, new ItemStack(Items.BOOK));
      helper.assertValueEqual(
          helper
              .getBlockState(shelf)
              .getAnalogOutputSignal(helper.getLevel(), absolute, Direction.EAST),
          SpecialSignals.chiseledBookshelf(slot),
          "book put in slot " + slot);
    }
    entity.removeItem(1, 1);
    helper.runAfterDelay(
        SETTLE,
        () -> {
          helper.assertValueEqual(
              comparatorOutput(helper, comparator),
              SpecialSignals.chiseledBookshelf(1),
              "book taken out");
          helper.succeed();
        });
  }

  /** A heart gives more the nearer its creaking; a heart without one gives 0. */
  @GameTest(maxTicks = ENTITY_SETTLE * 2)
  public void creakingHeartGivesItsCreakingsDistance(GameTestHelper helper) {
    BlockPos heart = new BlockPos(1, 2, 1);
    BlockPos alone = new BlockPos(1, 2, 3);
    BlockPos comparator = heart.east();
    helper.setBlock(comparator.below(), Blocks.STONE);
    helper.setBlock(comparator, DioramaPlacer.state("comparator[facing=west]"));
    for (BlockPos pos : List.of(heart, alone)) {
      helper.setBlock(pos.below(), DioramaPlacer.state("pale_oak_log[axis=y]"));
      helper.setBlock(pos.above(), DioramaPlacer.state("pale_oak_log[axis=y]"));
      helper.setBlock(
          pos, DioramaPlacer.state("creaking_heart[axis=y,creaking_heart_state=awake]"));
    }
    Creaking creaking =
        helper.spawnWithNoFreeWill(EntityTypes.CREAKING, heart.east(CREAKING_DISTANCE));
    creaking.setPersistenceRequired();
    helper.getBlockEntity(heart, CreakingHeartBlockEntity.class).setCreakingInfo(creaking);
    helper.runAfterDelay(
        ENTITY_SETTLE,
        () -> {
          helper.assertValueEqual(
              comparatorOutput(helper, comparator),
              SpecialSignals.creakingHeart(CREAKING_DISTANCE),
              "with its creaking " + CREAKING_DISTANCE + " blocks away");
          helper.assertValueEqual(
              helper
                  .getBlockEntity(alone, CreakingHeartBlockEntity.class)
                  .computeAnalogOutputSignal(),
              0,
              "no creaking");
          helper.succeed();
        });
  }

  /** A command block gives its command's successes: one per target, none when it fails. */
  @GameTest(maxTicks = ENTITY_SETTLE * 2)
  public void commandBlockGivesItsSuccessCount(GameTestHelper helper) {
    BlockPos forEach = new BlockPos(1, 1, 1);
    BlockPos failing = new BlockPos(1, 1, 5);
    for (int target = 0; target < COMMAND_TARGETS; target++) {
      helper.spawn(EntityTypes.ARMOR_STAND, new BlockPos(5, 1, 1 + target));
    }
    runCommand(
        helper,
        forEach,
        "execute as @e[type=minecraft:armor_stand,distance=..8] run tag @s add cheatsheet");
    runCommand(helper, failing, "kill @e[type=minecraft:bee,distance=..1]");
    helper.runAfterDelay(
        ENTITY_SETTLE,
        () -> {
          helper.assertValueEqual(
              comparatorOutput(helper, forEach.east()), COMMAND_TARGETS, "one success per target");
          helper.assertValueEqual(comparatorOutput(helper, failing.east()), 0, "a failed command");
          helper.succeed();
        });
  }

  /** Every special state gives the signal the tab says. */
  @GameTest
  public void specialBlocksGiveTheirSignals(GameTestHelper helper) {
    List<String> problems = new ArrayList<>();
    BlockPos pos = new BlockPos(1, 1, 1);
    BlockPos absolute = helper.absolutePos(pos);
    for (SpecialSignals.StateSignal check : SpecialSignals.checks()) {
      BlockState state = DioramaPlacer.state(check.state());
      helper.getLevel().setBlock(absolute, state, Block.UPDATE_CLIENTS);
      int signal = state.getAnalogOutputSignal(helper.getLevel(), absolute, Direction.NORTH);
      if (signal != check.signal()) {
        problems.add(check.state() + " gives " + signal + ", not " + check.signal());
      }
    }
    helper.assertTrue(problems.isEmpty(), String.join("\n", problems));
    helper.succeed();
  }

  /**
   * Fills a {@code block} of {@code container}'s size with the table's count for the first, a
   * middle and the last signal, and one item fewer than the middle's, each read by a comparator.
   */
  private static void check(GameTestHelper helper, SignalContainer container, Block block) {
    int[][] cases = cases(container);
    boolean doubleChest = container == SignalContainer.DOUBLE_CHEST;
    List<BlockPos> comparators = new ArrayList<>();
    for (int index = 0; index < cases.length; index++) {
      int z = index * CASE_SPACING;
      BlockPos target = new BlockPos(1, 1, z);
      BlockPos comparator = new BlockPos(2, 1, z);
      helper.setBlock(new BlockPos(2, 0, z), Blocks.STONE);
      if (doubleChest) {
        helper.setBlock(
            new BlockPos(0, 1, z), DioramaPlacer.state("chest[facing=north,type=left]"));
        helper.setBlock(target, DioramaPlacer.state("chest[facing=north,type=right]"));
      } else {
        helper.setBlock(target, block);
      }
      // The comparator first: filling the container then updates it, as in the game.
      helper.setBlock(comparator, DioramaPlacer.state("comparator[facing=west]"));
      comparators.add(comparator);
      Container inventory = container(helper, target);
      helper.assertValueEqual(inventory.getContainerSize(), container.slots(), "slots");
      fill(inventory, cases[index][0]);
    }
    helper.runAfterDelay(
        SETTLE,
        () -> {
          for (int index = 0; index < cases.length; index++) {
            helper.assertValueEqual(
                comparatorOutput(helper, comparators.get(index)),
                cases[index][1],
                block + " with " + cases[index][0] + " items");
          }
          helper.succeed();
        });
  }

  /**
   * Rails with a {@code type} minecart of {@code container}'s size on each, filled as in {@link
   * #check}, each rail read by a comparator ({@code DetectorRailBlock#getAnalogOutputSignal}).
   */
  private static void checkMinecart(
      GameTestHelper helper,
      SignalContainer container,
      EntityType<? extends AbstractMinecart> type) {
    int[][] cases = cases(container);
    List<BlockPos> comparators = new ArrayList<>();
    for (int index = 0; index < cases.length; index++) {
      int z = index * CASE_SPACING;
      BlockPos rail = new BlockPos(1, 1, z);
      BlockPos comparator = rail.east();
      helper.setBlock(rail.below(), Blocks.STONE);
      helper.setBlock(comparator.below(), Blocks.STONE);
      helper.setBlock(comparator, DioramaPlacer.state("comparator[facing=west]"));
      helper.setBlock(rail, DioramaPlacer.state("detector_rail[shape=north_south]"));
      comparators.add(comparator);
      AbstractMinecart minecart = helper.spawn(type, rail);
      if (!(minecart instanceof Container inventory)) {
        throw new IllegalStateException(type + " holds no items");
      }
      helper.assertValueEqual(inventory.getContainerSize(), container.slots(), "slots");
      fill(inventory, cases[index][0]);
    }
    helper.runAfterDelay(
        ENTITY_SETTLE,
        () -> {
          for (int index = 0; index < cases.length; index++) {
            helper.assertValueEqual(
                comparatorOutput(helper, comparators.get(index)),
                cases[index][1],
                type + " with " + cases[index][0] + " items");
          }
          helper.succeed();
        });
  }

  /** Item counts and the signal each gives: the first, the middle (and below it), the last. */
  private static int[][] cases(SignalContainer container) {
    int[] items = container.itemsPerSignal();
    return new int[][] {
      {items[0], 1},
      {items[MIDDLE - 1], MIDDLE},
      {items[MIDDLE - 1] - 1, MIDDLE - 1},
      {items[RedstoneRules.MAX_SIGNAL - 1], RedstoneRules.MAX_SIGNAL}
    };
  }

  /**
   * A comparator with {@link ComparatorSignal#EXAMPLE_INPUT} at its input end, a cake with two
   * bites eaten, and on one side (when {@code sideSource} is given) a second comparator reading
   * {@code sideSource}, which must give {@code side}.
   */
  private static void checkExample(
      GameTestHelper helper, @Nullable String sideSource, boolean subtract, int side) {
    BlockPos input = new BlockPos(1, 1, 1);
    BlockPos comparator = input.east();
    BlockPos sideComparator = comparator.south();
    BlockPos sideInput = sideComparator.south();
    for (BlockPos pos : List.of(input, comparator, sideComparator, sideInput)) {
      helper.setBlock(pos.below(), Blocks.STONE);
    }
    helper.setBlock(
        comparator,
        DioramaPlacer.state(
            "comparator[facing=west,mode=" + (subtract ? "subtract" : "compare") + "]"));
    if (sideSource != null) {
      helper.setBlock(sideComparator, DioramaPlacer.state("comparator[facing=south]"));
      helper.setBlock(sideInput, DioramaPlacer.state(sideSource));
    }
    helper.setBlock(input, DioramaPlacer.state("cake[bites=2]"));
    helper.runAfterDelay(
        SETTLE * 2,
        () -> {
          if (sideSource != null) {
            helper.assertValueEqual(comparatorOutput(helper, sideComparator), side, "side signal");
          }
          helper.assertValueEqual(
              comparatorOutput(helper, comparator),
              ComparatorSignal.output(ComparatorSignal.EXAMPLE_INPUT, side, subtract),
              "output");
          helper.succeed();
        });
  }

  /** A crafter at {@code pos} and a comparator east of it reading it. */
  private static CrafterBlockEntity crafterWithComparator(GameTestHelper helper, BlockPos pos) {
    helper.setBlock(pos.east().below(), Blocks.STONE);
    helper.setBlock(pos.east(), DioramaPlacer.state("comparator[facing=west]"));
    helper.setBlock(pos, Blocks.CRAFTER);
    return helper.getBlockEntity(pos, CrafterBlockEntity.class);
  }

  /** Sets a command block at {@code pos} to {@code command} and powers it, a comparator east. */
  private static void runCommand(GameTestHelper helper, BlockPos pos, String command) {
    helper.setBlock(pos.east().below(), Blocks.STONE);
    helper.setBlock(pos.east(), DioramaPlacer.state("comparator[facing=west]"));
    helper.setBlock(pos, Blocks.COMMAND_BLOCK);
    helper.getBlockEntity(pos, CommandBlockEntity.class).getCommandBlock().setCommand(command);
    helper.setBlock(pos.north(), Blocks.REDSTONE_BLOCK);
  }

  /** A click on the north face of the block at the origin, at block fractions x and y. */
  private static BlockHitResult hitNorthFace(double x, double y) {
    return new BlockHitResult(new Vec3(x, y, 0), Direction.NORTH, BlockPos.ZERO, false);
  }

  private static int comparatorOutput(GameTestHelper helper, BlockPos pos) {
    return helper.getBlockEntity(pos, ComparatorBlockEntity.class).getOutputSignal();
  }

  /** {@code items} cobblestone (stacks of 64), slot by slot. */
  private static void fill(Container inventory, int items) {
    int left = items;
    for (int slot = 0; slot < inventory.getContainerSize() && left > 0; slot++) {
      int count = Math.min(left, Items.COBBLESTONE.getDefaultMaxStackSize());
      inventory.setItem(slot, new ItemStack(Items.COBBLESTONE, count));
      left -= count;
    }
    inventory.setChanged();
  }

  /** The container at {@code pos}: a double chest's whole inventory for a chest. */
  private static Container container(GameTestHelper helper, BlockPos pos) {
    BlockPos absolute = helper.absolutePos(pos);
    BlockState state = helper.getLevel().getBlockState(absolute);
    if (state.getBlock() instanceof ChestBlock chest) {
      Container combined = ChestBlock.getContainer(chest, state, helper.getLevel(), absolute, true);
      if (combined != null) {
        return combined;
      }
    }
    BlockEntity entity = helper.getLevel().getBlockEntity(absolute);
    if (!(entity instanceof Container inventory)) {
      throw new IllegalStateException("no container at " + pos);
    }
    return inventory;
  }
}
