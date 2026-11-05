package com.einsteinmods.cheatsheet.gametest;

import com.einsteinmods.cheatsheet.core.redstone.RedstoneRules;
import com.einsteinmods.cheatsheet.core.redstone.SpecialSignals;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.WeightedPressurePlateBlock;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * The redstone tab's numbers ({@link RedstoneRules}, {@link SpecialSignals}) against the game: the
 * constants vanilla makes public are compared, and delays and pulse lengths are measured in a world
 * (game test callbacks run after the level's tick, so an action at tick T whose effect is scheduled
 * d ticks later is first seen at T + d).
 */
public final class RedstoneRulesGameTest {
  private static final int MEASURE_TICKS = 50;
  private static final int SETTLE_TICKS = 10;

  @GameTest
  public void publicConstantsMatch(GameTestHelper helper) {
    expect(helper, RedstoneTorchBlock.MAX_RECENT_TOGGLES, RedstoneRules.TORCH_BURNOUT_TOGGLES);
    expect(
        helper, RedstoneTorchBlock.RECENT_TOGGLE_TIMER, RedstoneRules.TORCH_BURNOUT_WINDOW_TICKS);
    expect(helper, RedstoneTorchBlock.RESTART_DELAY, RedstoneRules.TORCH_RESTART_TICKS);
    expect(helper, HopperBlockEntity.MOVE_ITEM_SPEED, RedstoneRules.HOPPER_TICKS_PER_ITEM);
    expect(helper, max(BlockStateProperties.POWER), RedstoneRules.MAX_SIGNAL);
    expect(helper, min(RepeaterBlock.DELAY), RedstoneRules.REPEATER_MIN_DELAY);
    expect(helper, max(RepeaterBlock.DELAY), RedstoneRules.REPEATER_MAX_DELAY);
    expect(helper, ItemFrame.NUM_ROTATIONS, SpecialSignals.ITEM_FRAME_ROTATIONS);
    expect(helper, BeehiveBlock.MAX_HONEY_LEVELS, SpecialSignals.BEEHIVE_MAX);
    expect(helper, CakeBlock.MAX_BITES + 1, SpecialSignals.CAKE_SLICES);
    expect(
        helper,
        BlockStateProperties.MAX_RESPAWN_ANCHOR_CHARGES,
        SpecialSignals.RESPAWN_ANCHOR_CHARGES);
    expect(helper, max(BlockStateProperties.LEVEL_COMPOSTER), SpecialSignals.COMPOSTER_MAX);
    expect(helper, max(BlockStateProperties.LEVEL_CAULDRON), SpecialSignals.CAULDRON_MAX);
    expect(
        helper,
        ChiseledBookShelfBlockEntity.MAX_BOOKS_IN_STORAGE,
        SpecialSignals.CHISELED_BOOKSHELF_SLOTS);
    expect(
        helper,
        CopperGolemStatueBlock.Pose.values().length,
        SpecialSignals.COPPER_GOLEM_POSES.size());
    helper.succeed();
  }

  /** A stone and a wooden button stay pressed their own time. */
  @GameTest(maxTicks = MEASURE_TICKS)
  public void buttonsStayPressed(GameTestHelper helper) {
    BlockPos stone = new BlockPos(1, 1, 1);
    BlockPos wood = new BlockPos(3, 1, 1);
    floor(helper);
    helper.setBlock(stone, DioramaPlacer.state("stone_button[face=floor,facing=north]"));
    helper.setBlock(wood, DioramaPlacer.state("oak_button[face=floor,facing=north]"));
    long pressed = helper.getTick();
    helper.pressButton(stone);
    helper.pressButton(wood);
    long[] released = new long[2];
    helper.onEachTick(
        () -> {
          record(helper, stone, state -> !state.getValue(ButtonBlock.POWERED), released, 0);
          record(helper, wood, state -> !state.getValue(ButtonBlock.POWERED), released, 1);
        });
    helper.runAtTickTime(
        MEASURE_TICKS - 1,
        () -> {
          expect(helper, (int) (released[0] - pressed), RedstoneRules.STONE_BUTTON_TICKS);
          expect(helper, (int) (released[1] - pressed), RedstoneRules.WOODEN_BUTTON_TICKS);
          helper.succeed();
        });
  }

  /**
   * A repeater set to 4 and a comparator turn on their delay after their input; an observer turns
   * on and off its pulse after a change; a lamp goes off its delay after its power.
   */
  @GameTest(maxTicks = MEASURE_TICKS)
  public void delaysAndPulses(GameTestHelper helper) {
    floor(helper);
    BlockPos repeater = new BlockPos(1, 1, 1);
    BlockPos comparator = new BlockPos(1, 1, 3);
    BlockPos observer = new BlockPos(1, 1, 5);
    BlockPos lamp = new BlockPos(5, 1, 1);
    BlockPos lampPower = new BlockPos(6, 1, 1);
    helper.setBlock(repeater, DioramaPlacer.state("repeater[facing=east,delay=4]"));
    helper.setBlock(comparator, DioramaPlacer.state("comparator[facing=east]"));
    helper.setBlock(observer, DioramaPlacer.state("observer[facing=east]"));
    helper.setBlock(lamp, Blocks.REDSTONE_LAMP);
    helper.setBlock(lampPower, Blocks.REDSTONE_BLOCK);
    long[] seen = new long[5];
    long[] start = new long[1];
    helper
        .startSequence()
        .thenIdle(RedstoneRules.LAMP_OFF_DELAY_TICKS * 2)
        .thenExecute(
            () -> {
              helper.assertTrue(
                  helper.getBlockState(lamp).getValue(RedstoneLampBlock.LIT), "lamp not lit");
              start[0] = helper.getTick();
              helper.setBlock(repeater.east(), Blocks.REDSTONE_BLOCK);
              helper.setBlock(comparator.east(), Blocks.REDSTONE_BLOCK);
              helper.setBlock(observer.east(), Blocks.STONE);
              helper.setBlock(lampPower, Blocks.AIR);
              helper.onEachTick(
                  () -> {
                    record(
                        helper, repeater, state -> state.getValue(RepeaterBlock.POWERED), seen, 0);
                    record(
                        helper,
                        comparator,
                        state -> state.getValue(ComparatorBlock.POWERED),
                        seen,
                        1);
                    record(
                        helper, observer, state -> state.getValue(ObserverBlock.POWERED), seen, 2);
                    if (seen[2] > 0) {
                      record(
                          helper,
                          observer,
                          state -> !state.getValue(ObserverBlock.POWERED),
                          seen,
                          3);
                    }
                    record(helper, lamp, state -> !state.getValue(RedstoneLampBlock.LIT), seen, 4);
                  });
            })
        .thenIdle(MEASURE_TICKS / 2)
        .thenExecute(
            () -> {
              long at = start[0];
              expect(helper, (int) (seen[0] - at), RedstoneRules.repeaterTicks(4));
              expect(helper, (int) (seen[1] - at), RedstoneRules.COMPARATOR_DELAY_TICKS);
              expect(helper, (int) (seen[2] - at), RedstoneRules.OBSERVER_PULSE_TICKS);
              expect(helper, (int) (seen[3] - seen[2]), RedstoneRules.OBSERVER_PULSE_TICKS);
              expect(helper, (int) (seen[4] - at), RedstoneRules.LAMP_OFF_DELAY_TICKS);
            })
        .thenSucceed();
  }

  /**
   * A torch switches its delay after the block it stands on is powered; a dropper fires its delay
   * after its power; a piston's block is moving for its move time.
   */
  @GameTest(maxTicks = MEASURE_TICKS)
  public void torchDropperAndPistonDelays(GameTestHelper helper) {
    floor(helper);
    BlockPos torchBlock = new BlockPos(1, 1, 1);
    BlockPos torch = torchBlock.above();
    BlockPos lever = torchBlock.west();
    BlockPos dropper = new BlockPos(4, 1, 1);
    BlockPos piston = new BlockPos(1, 1, 4);
    BlockPos pushed = piston.above();
    helper.setBlock(torchBlock, Blocks.STONE_BRICKS);
    helper.setBlock(torch, Blocks.REDSTONE_TORCH);
    helper.setBlock(lever, DioramaPlacer.state("lever[face=wall,facing=west]"));
    helper.setBlock(dropper, DioramaPlacer.state("dropper[facing=up]"));
    helper
        .getBlockEntity(dropper, DispenserBlockEntity.class)
        .setItem(0, new ItemStack(Items.STICK));
    helper.setBlock(piston, DioramaPlacer.state("piston[facing=up]"));
    helper.setBlock(pushed, Blocks.STONE_BRICKS);
    long[] seen = new long[4];
    long[] start = new long[1];
    helper
        .startSequence()
        .thenIdle(RedstoneRules.TORCH_DELAY_TICKS * 2)
        .thenExecute(
            () -> {
              start[0] = helper.getTick();
              helper.pullLever(lever);
              helper.setBlock(dropper.east(), Blocks.REDSTONE_BLOCK);
              helper.setBlock(piston.east(), Blocks.REDSTONE_BLOCK);
              helper.onEachTick(
                  () -> {
                    record(
                        helper, torch, state -> !state.getValue(RedstoneTorchBlock.LIT), seen, 0);
                    if (seen[1] == 0
                        && helper.getBlockEntity(dropper, DispenserBlockEntity.class).isEmpty()) {
                      seen[1] = helper.getTick();
                    }
                    record(
                        helper, pushed.above(), state -> state.is(Blocks.MOVING_PISTON), seen, 2);
                    record(helper, pushed.above(), state -> state.is(Blocks.STONE_BRICKS), seen, 3);
                  });
            })
        .thenIdle(MEASURE_TICKS / 2)
        .thenExecute(
            () -> {
              expect(helper, (int) (seen[0] - start[0]), RedstoneRules.TORCH_DELAY_TICKS);
              expect(helper, (int) (seen[1] - start[0]), RedstoneRules.DROPPER_DELAY_TICKS);
              expect(helper, (int) (seen[3] - seen[2]), RedstoneRules.PISTON_MOVE_TICKS);
            })
        .thenSucceed();
  }

  /** Lever flips between one turn-off of the torch and the next: each lets the switch land. */
  private static final int TORCH_FLIP_TICKS = 3;

  /** Enough for an unpowered torch to light again, unless it is burnt out. */
  private static final int TORCH_LIT_CHECK_TICKS = 6;

  /**
   * Only a torch's turn-offs count towards its burnout ({@code RedstoneTorchBlock#tick} records
   * them): one short of the limit, with as many turn-ons, it still lights; the next turn-off leaves
   * it dark for {@link RedstoneRules#TORCH_RESTART_TICKS} though nothing powers it.
   */
  @GameTest(maxTicks = RedstoneRules.TORCH_RESTART_TICKS * 2)
  public void torchBurnsOutOnItsTurnOffs(GameTestHelper helper) {
    floor(helper);
    BlockPos block = new BlockPos(1, 2, 1);
    BlockPos torch = block.above();
    BlockPos lever = block.west();
    helper.setBlock(block, Blocks.STONE_BRICKS);
    helper.setBlock(torch, Blocks.REDSTONE_TORCH);
    helper.setBlock(lever, DioramaPlacer.state("lever[face=wall,facing=west]"));
    var sequence = helper.startSequence().thenIdle(TORCH_LIT_CHECK_TICKS);
    for (int turnOff = 1; turnOff < RedstoneRules.TORCH_BURNOUT_TOGGLES; turnOff++) {
      sequence
          .thenExecute(() -> helper.pullLever(lever))
          .thenIdle(TORCH_FLIP_TICKS)
          .thenExecute(() -> helper.pullLever(lever))
          .thenIdle(TORCH_FLIP_TICKS);
    }
    sequence
        .thenExecute(() -> helper.assertBlockProperty(torch, RedstoneTorchBlock.LIT, Boolean.TRUE))
        .thenExecute(() -> helper.pullLever(lever))
        .thenIdle(TORCH_FLIP_TICKS)
        .thenExecute(() -> helper.pullLever(lever))
        .thenIdle(TORCH_LIT_CHECK_TICKS)
        .thenExecute(() -> helper.assertBlockProperty(torch, RedstoneTorchBlock.LIT, Boolean.FALSE))
        .thenIdle(RedstoneRules.TORCH_RESTART_TICKS)
        .thenExecute(() -> helper.assertBlockProperty(torch, RedstoneTorchBlock.LIT, Boolean.TRUE))
        .thenSucceed();
  }

  /**
   * A touch keeps a wooden plate on its pressed time and a weighted plate on its own: an item
   * presses each and is removed at once.
   */
  @GameTest(maxTicks = MEASURE_TICKS)
  public void platesStayPressedAfterATouch(GameTestHelper helper) {
    floor(helper);
    BlockPos wood = new BlockPos(1, 1, 1);
    BlockPos gold = new BlockPos(4, 1, 4);
    helper.setBlock(wood, Blocks.OAK_PRESSURE_PLATE);
    helper.setBlock(gold, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE);
    ItemEntity onWood = helper.spawnItem(Items.STICK, wood);
    ItemEntity onGold = helper.spawnItem(Items.STICK, gold);
    long[] seen = new long[4];
    helper.onEachTick(
        () -> {
          long tick = helper.getTick();
          boolean woodOn = helper.getBlockState(wood).getValue(PressurePlateBlock.POWERED);
          boolean goldOn =
              helper.getBlockState(gold).getValue(WeightedPressurePlateBlock.POWER) > 0;
          if (seen[0] == 0 && woodOn) {
            seen[0] = tick;
            onWood.discard();
          } else if (seen[0] > 0 && seen[1] == 0 && !woodOn) {
            seen[1] = tick;
          }
          if (seen[2] == 0 && goldOn) {
            seen[2] = tick;
            onGold.discard();
          } else if (seen[2] > 0 && seen[3] == 0 && !goldOn) {
            seen[3] = tick;
          }
        });
    helper.runAtTickTime(
        MEASURE_TICKS - 1,
        () -> {
          expect(helper, (int) (seen[1] - seen[0]), RedstoneRules.PLATE_PRESSED_TICKS);
          expect(helper, (int) (seen[3] - seen[2]), RedstoneRules.WEIGHTED_PLATE_PRESSED_TICKS);
          helper.succeed();
        });
  }

  /** A stone plate ignores items but feels players, mobs and armour stands. */
  @GameTest(maxTicks = MEASURE_TICKS)
  public void stonePlatesIgnoreItems(GameTestHelper helper) {
    floor(helper);
    BlockPos stone = new BlockPos(1, 1, 1);
    helper.setBlock(stone, Blocks.STONE_PRESSURE_PLATE);
    helper.spawnItem(Items.STICK, stone);
    helper
        .startSequence()
        .thenIdle(SETTLE_TICKS)
        .thenExecute(
            () -> {
              helper.assertFalse(
                  helper.getBlockState(stone).getValue(PressurePlateBlock.POWERED),
                  "an item pressed a stone plate");
              helper.spawn(EntityTypes.ARMOR_STAND, stone);
            })
        .thenIdle(SETTLE_TICKS)
        .thenExecute(
            () ->
                helper.assertTrue(
                    helper.getBlockState(stone).getValue(PressurePlateBlock.POWERED),
                    "an armour stand did not press a stone plate"))
        .thenSucceed();
  }

  /** The light plate counts each entity on it, the heavy one each ten. */
  @GameTest(maxTicks = MEASURE_TICKS)
  public void weightedPlatesCountEntities(GameTestHelper helper) {
    floor(helper);
    BlockPos light = new BlockPos(1, 1, 1);
    BlockPos heavy = new BlockPos(4, 1, 4);
    helper.setBlock(light, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE);
    helper.setBlock(heavy, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE);
    int onLight = 5;
    int onHeavy = 11;
    helper.spawn(EntityTypes.ARMOR_STAND, light, onLight);
    helper.spawn(EntityTypes.ARMOR_STAND, heavy, onHeavy);
    helper.succeedWhen(
        () -> {
          expect(
              helper,
              helper.getBlockState(light).getValue(WeightedPressurePlateBlock.POWER),
              RedstoneRules.weightedPlateSignal(onLight, RedstoneRules.LIGHT_PLATE_MAX_ENTITIES));
          expect(
              helper,
              helper.getBlockState(heavy).getValue(WeightedPressurePlateBlock.POWER),
              RedstoneRules.weightedPlateSignal(onHeavy, RedstoneRules.HEAVY_PLATE_MAX_ENTITIES));
        });
  }

  /** Stores the first tick {@code condition} holds at {@code pos} into {@code ticks[index]}. */
  private static void record(
      GameTestHelper helper,
      BlockPos pos,
      Predicate<BlockState> condition,
      long[] ticks,
      int index) {
    if (ticks[index] == 0 && condition.test(helper.getBlockState(pos))) {
      ticks[index] = helper.getTick();
    }
  }

  private static void floor(GameTestHelper helper) {
    for (int x = 0; x < 8; x++) {
      for (int z = 0; z < 8; z++) {
        helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
      }
    }
  }

  private static int min(IntegerProperty property) {
    List<Integer> values = property.getPossibleValues();
    return values.getFirst();
  }

  private static int max(IntegerProperty property) {
    List<Integer> values = property.getPossibleValues();
    return values.getLast();
  }

  private static void expect(GameTestHelper helper, int actual, int expected) {
    helper.assertValueEqual(actual, expected, "value");
  }
}
