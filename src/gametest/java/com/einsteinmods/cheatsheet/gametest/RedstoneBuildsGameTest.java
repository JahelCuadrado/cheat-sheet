package com.einsteinmods.cheatsheet.gametest;

import static com.einsteinmods.cheatsheet.gametest.DioramaPlacer.at;

import com.einsteinmods.cheatsheet.core.redstone.RedstoneBuild;
import com.einsteinmods.cheatsheet.core.redstone.RedstoneDioramas;
import com.einsteinmods.cheatsheet.core.redstone.RedstoneRules;
import com.einsteinmods.cheatsheet.core.redstone.SorterFilter;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CopperBulbBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RedstoneWireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Every circuit and mechanic the redstone tab teaches works: each build is placed as its picture
 * draws it ({@link DioramaPlacer}), its input is worked (a button, a lever, items, a block placed)
 * and its output checked. A picture that drew a broken machine fails here.
 */
public final class RedstoneBuildsGameTest {
  /** Enough game ticks for a short chain of redstone components to settle. */
  private static final int SETTLE = 10;

  private static final int CLOCK_TICKS = 40;
  private static final int CLOCK_MIN_PULSES = 4;

  @GameTest(maxTicks = CLOCK_TICKS + SETTLE)
  public void observerClockKeepsPulsing(GameTestHelper helper) {
    DioramaPlacer.place(helper, RedstoneBuild.OBSERVER_CLOCK.steps().get(1));
    BlockPos second = at(1, 1, 0);
    BlockPos first = at(2, 1, 0);
    // Placing the second observer starts the clock, as in the game.
    BlockState observer = helper.getBlockState(second);
    helper.setBlock(second, Blocks.AIR);
    helper.setBlock(second, observer);
    List<Long> rises = new ArrayList<>();
    List<Long> falls = new ArrayList<>();
    boolean[] last = {false};
    helper.onEachTick(
        () -> {
          boolean powered = helper.getBlockState(first).getValue(ObserverBlock.POWERED);
          if (powered != last[0]) {
            (powered ? rises : falls).add(helper.getTick());
            last[0] = powered;
          }
        });
    helper.runAtTickTime(
        CLOCK_TICKS,
        () -> {
          helper.assertTrue(
              rises.size() >= CLOCK_MIN_PULSES, "clock pulsed " + rises.size() + " times");
          long period = rises.get(rises.size() - 1) - rises.get(rises.size() - 2);
          long on = falls.getLast() - rises.get(falls.size() - 1);
          helper.assertValueEqual(
              (int) period, RedstoneRules.OBSERVER_CLOCK_PERIOD_TICKS, "clock period");
          helper.assertValueEqual((int) on, RedstoneRules.OBSERVER_PULSE_TICKS, "pulse length");
          helper.succeed();
        });
  }

  @GameTest(maxTicks = 80)
  public void copperBulbTogglesOnEachPress(GameTestHelper helper) {
    DioramaPlacer.place(helper, RedstoneBuild.T_FLIP_FLOP.steps().get(1));
    BlockPos button = at(3, 1, 0);
    BlockPos bulb = at(2, 1, 0);
    BlockPos output = at(0, 1, 0);
    helper
        .startSequence()
        .thenExecute(() -> helper.pressButton(button))
        .thenIdle(SETTLE)
        .thenExecute(() -> expectLit(helper, bulb, output, true))
        .thenIdle(RedstoneRules.STONE_BUTTON_TICKS + SETTLE)
        .thenExecute(() -> expectLit(helper, bulb, output, true))
        .thenExecute(() -> helper.pressButton(button))
        .thenIdle(SETTLE)
        .thenExecute(() -> expectLit(helper, bulb, output, false))
        .thenSucceed();
  }

  private static void expectLit(GameTestHelper helper, BlockPos bulb, BlockPos output, boolean on) {
    helper.assertValueEqual(
        helper.getBlockState(bulb).getValue(CopperBulbBlock.LIT), on, "bulb lit");
    helper.assertValueEqual(
        helper.getBlockState(output).getValue(RedstoneWireBlock.POWER) > 0, on, "output powered");
  }

  private static final int RING_TICKS = 160;
  private static final int RING_MIN_PULSES = 3;

  /** One press starts a pulse that runs round the repeater loop: on as long as the button. */
  @GameTest(maxTicks = RING_TICKS + SETTLE)
  public void repeaterClockKeepsItsPulse(GameTestHelper helper) {
    DioramaPlacer.place(helper, RedstoneBuild.REPEATER_CLOCK.steps().get(1));
    BlockPos probe = at(RedstoneDioramas.RING_PROBE_X, 1, 0);
    Edges edges = new Edges();
    helper.onEachTick(
        () ->
            edges.see(
                helper.getBlockState(probe).getValue(RedstoneWireBlock.POWER) > 0,
                helper.getTick()));
    helper.runAfterDelay(1, () -> helper.pressButton(at(0, 1, RedstoneDioramas.RING_BUTTON_Z)));
    helper.runAtTickTime(
        RING_TICKS,
        () -> {
          helper.assertTrue(
              edges.rises.size() >= RING_MIN_PULSES, "loop pulsed " + edges.rises.size());
          long period = edges.rises.getLast() - edges.rises.get(edges.rises.size() - 2);
          long on = edges.falls.getFirst() - edges.rises.getFirst();
          helper.assertValueEqual(
              (int) period, RedstoneRules.repeaterClockPeriodTicks(), "clock period");
          helper.assertValueEqual((int) on, RedstoneRules.STONE_BUTTON_TICKS, "pulse length");
          helper.succeed();
        });
  }

  private static final int FEW_CLOCK_ITEMS = 4;
  private static final int MORE_CLOCK_ITEMS = 8;

  @GameTest(maxTicks = 160)
  public void hopperClockCyclesWithFewItems(GameTestHelper helper) {
    hopperClockCycles(helper, FEW_CLOCK_ITEMS);
  }

  @GameTest(maxTicks = 280)
  public void hopperClockCyclesWithMoreItems(GameTestHelper helper) {
    hopperClockCycles(helper, MORE_CLOCK_ITEMS);
  }

  /**
   * Items in the right hopper: the redstone block goes back and forth over the hoppers, a full
   * cycle growing with the items as {@link RedstoneRules#hopperClockCycleTicks} says.
   */
  private static void hopperClockCycles(GameTestHelper helper, int items) {
    DioramaPlacer.place(helper, RedstoneBuild.HOPPER_CLOCK.steps().get(1));
    Container right =
        container(
            helper, at(RedstoneDioramas.CLOCK_RIGHT_HOPPER_X, RedstoneDioramas.CLOCK_HOPPER_Y, 0));
    right.setItem(0, new ItemStack(Items.STICK, items));
    right.setChanged();
    BlockPos left = at(RedstoneDioramas.CLOCK_LEFT_HOPPER_X, RedstoneDioramas.CLOCK_PISTON_Y, 0);
    Edges overLeft = new Edges();
    overLeft.on = true;
    helper.onEachTick(
        () -> overLeft.see(helper.getBlockState(left).is(Blocks.REDSTONE_BLOCK), helper.getTick()));
    helper.succeedWhen(
        () -> {
          helper.assertTrue(overLeft.rises.size() >= 2, "the block came back " + overLeft.rises);
          helper.assertValueEqual(
              (int) (overLeft.rises.get(1) - overLeft.rises.getFirst()),
              RedstoneRules.hopperClockCycleTicks(items),
              "cycle with " + items + " items");
        });
  }

  /** Each flip gives one short pulse: the sticky piston drops its block, then takes it back. */
  @GameTest(maxTicks = 60)
  public void pulseLimiterDropsAndTakesBackTheBlock(GameTestHelper helper) {
    DioramaPlacer.place(helper, RedstoneBuild.PULSE_LIMITER.steps().get(1));
    BlockPos lever = at(RedstoneDioramas.LIMITER_LEVER_X, 1, 0);
    BlockPos block = at(RedstoneDioramas.LIMITER_BLOCK_X, 1, 0);
    BlockPos dropped = at(RedstoneDioramas.LIMITER_DROPPED_X, 1, 0);
    helper
        .startSequence()
        .thenExecute(() -> helper.pullLever(lever))
        .thenIdle(SETTLE)
        .thenExecute(
            () -> {
              helper.assertBlockPresent(Blocks.STONE_BRICKS, dropped);
              helper.assertBlockPresent(Blocks.AIR, block);
            })
        .thenExecute(() -> helper.pullLever(lever))
        .thenIdle(SETTLE)
        .thenExecute(
            () -> {
              helper.assertBlockPresent(Blocks.STONE_BRICKS, block);
              helper.assertBlockPresent(Blocks.AIR, dropped);
            })
        .thenSucceed();
  }

  /** The lamp stays lit its hold time after the button lets go. */
  @GameTest(maxTicks = 120)
  public void pulseExtenderHoldsAfterTheButton(GameTestHelper helper) {
    DioramaPlacer.place(helper, RedstoneBuild.PULSE_EXTENDER.steps().get(1));
    BlockPos lamp = at(RedstoneDioramas.EXTENDER_LAMP_X, 0, RedstoneDioramas.EXTENDER_LAMP_Z);
    Edges lit = new Edges();
    long[] pressed = new long[1];
    helper.onEachTick(
        () ->
            lit.see(helper.getBlockState(lamp).getValue(RedstoneLampBlock.LIT), helper.getTick()));
    helper.runAfterDelay(
        1,
        () -> {
          pressed[0] = helper.getTick();
          helper.pressButton(at(RedstoneDioramas.EXTENDER_BUTTON_X, 1, 0));
        });
    helper.succeedWhen(
        () -> {
          helper.assertFalse(lit.falls.isEmpty(), "the lamp is still lit");
          helper.assertValueEqual(
              (int) (lit.falls.getFirst() - pressed[0] - RedstoneRules.STONE_BUTTON_TICKS),
              RedstoneRules.PULSE_EXTENDER_HOLD_TICKS,
              "lamp lit after the button");
        });
  }

  /**
   * The redstone block powers the spot above the piston: alone it moves nothing, but once there the
   * next block update makes the piston push. Without the redstone block the update does not.
   */
  @GameTest(maxTicks = 60)
  public void quasiConnectedPistonWaitsForAnUpdate(GameTestHelper helper) {
    DioramaPlacer.place(helper, RedstoneBuild.QUASI_CONNECTIVITY.steps().getFirst());
    int row = RedstoneDioramas.QC_ROW;
    BlockPos piston = at(RedstoneDioramas.QC_PISTON_X, 1, row);
    BlockPos source = at(RedstoneDioramas.QC_SOURCE_X, 2, row);
    BlockPos beside = at(RedstoneDioramas.QC_PISTON_X, 1, row - 1);
    helper
        .startSequence()
        .thenExecute(
            () -> {
              helper.setBlock(source, Blocks.AIR);
              helper.setBlock(beside, Blocks.OAK_PLANKS);
            })
        .thenIdle(SETTLE)
        .thenExecute(() -> expectExtended(helper, piston, false))
        .thenExecute(
            () -> {
              helper.setBlock(beside, Blocks.AIR);
              helper.setBlock(source, Blocks.REDSTONE_BLOCK);
            })
        .thenIdle(SETTLE)
        .thenExecute(() -> expectExtended(helper, piston, false))
        .thenExecute(() -> helper.setBlock(beside, Blocks.OAK_PLANKS))
        .thenIdle(SETTLE)
        .thenExecute(
            () -> {
              expectExtended(helper, piston, true);
              helper.assertBlockPresent(
                  Blocks.STONE_BRICKS, at(RedstoneDioramas.QC_PUSHED_X - 1, 1, row));
            })
        .thenSucceed();
  }

  private static void expectExtended(GameTestHelper helper, BlockPos piston, boolean extended) {
    helper.assertValueEqual(
        helper.getBlockState(piston).getValue(PistonBaseBlock.EXTENDED), extended, "extended");
  }

  /**
   * A lever strongly powers the block it is on: switched off, the dust and lamp beside it are off;
   * switched on, they turn on.
   */
  @GameTest(maxTicks = 60)
  public void leverPowersItsBlockStrongly(GameTestHelper helper) {
    DioramaPlacer.place(helper, RedstoneBuild.STRONG_AND_WEAK.steps().get(0));
    int x = RedstoneDioramas.POWERED_BLOCK_X;
    BlockPos lever = at(x, 2, 0);
    BlockPos dust = at(x - 1, 1, 0);
    BlockPos lamp = at(x + 1, 1, 0);
    helper
        .startSequence()
        .thenExecute(() -> helper.pullLever(lever))
        .thenIdle(SETTLE)
        .thenExecute(
            () -> {
              expectPower(helper, dust, 0);
              expectLamp(helper, lamp, false);
              helper.pullLever(lever);
            })
        .thenIdle(SETTLE)
        .thenExecute(
            () -> {
              expectPower(helper, dust, RedstoneRules.MAX_SIGNAL);
              expectLamp(helper, lamp, true);
            })
        .thenSucceed();
  }

  /** Dust weakly powers the block it points into: the lamp on it lights, the dust beyond not. */
  @GameTest(maxTicks = 60)
  public void dustPowersItsBlockWeakly(GameTestHelper helper) {
    DioramaPlacer.place(helper, RedstoneBuild.STRONG_AND_WEAK.steps().get(1));
    BlockPos source = at(0, 1, 0);
    helper
        .startSequence()
        .thenExecute(() -> helper.setBlock(source, Blocks.AIR))
        .thenIdle(SETTLE)
        .thenExecute(() -> helper.setBlock(source, Blocks.REDSTONE_BLOCK))
        .thenIdle(SETTLE)
        .thenExecute(
            () -> {
              expectLamp(helper, at(RedstoneDioramas.WEAK_BLOCK_X, 2, 0), true);
              expectPower(helper, at(RedstoneDioramas.WEAK_FAR_DUST_X, 1, 0), 0);
            })
        .thenSucceed();
  }

  /** A torch strongly powers the block above it, not the one it stands on. */
  @GameTest(maxTicks = 60)
  public void torchPowersTheBlockAboveIt(GameTestHelper helper) {
    DioramaPlacer.place(helper, RedstoneBuild.STRONG_AND_WEAK.steps().get(2));
    BlockPos torch = at(RedstoneDioramas.POWERED_BLOCK_X, RedstoneDioramas.TORCH_Y, 0);
    BlockPos upper = at(RedstoneDioramas.TORCH_LAMP_X, RedstoneDioramas.TORCH_Y + 1, 0);
    BlockPos lower = at(RedstoneDioramas.TORCH_LAMP_X, RedstoneDioramas.TORCH_Y - 1, 0);
    helper
        .startSequence()
        .thenExecute(() -> helper.setBlock(torch, Blocks.AIR))
        .thenIdle(SETTLE)
        .thenExecute(() -> expectLamp(helper, upper, false))
        .thenExecute(() -> helper.setBlock(torch, Blocks.REDSTONE_TORCH))
        .thenIdle(SETTLE)
        .thenExecute(
            () -> {
              expectLamp(helper, upper, true);
              expectLamp(helper, lower, false);
            })
        .thenSucceed();
  }

  /**
   * Dust climbs an upside-down slab and glass, losing a step per block; powered from the top
   * instead, the signal does not come back down.
   */
  @GameTest(maxTicks = 60)
  public void dustClimbsGlassAndSlabsButNotDown(GameTestHelper helper) {
    DioramaPlacer.place(helper, RedstoneBuild.TRANSPARENCY.steps().get(0));
    BlockPos source = at(0, 1, 0);
    BlockPos top = at(RedstoneDioramas.CLIMB_GLASS_X, RedstoneDioramas.CLIMB_TOP_Y, 0);
    BlockPos onSlab = at(RedstoneDioramas.CLIMB_SLAB_X, 2, 0);
    helper
        .startSequence()
        .thenExecute(() -> helper.setBlock(source, Blocks.AIR))
        .thenIdle(SETTLE)
        .thenExecute(() -> helper.setBlock(source, Blocks.REDSTONE_BLOCK))
        .thenIdle(SETTLE)
        .thenExecute(() -> expectPower(helper, top, RedstoneRules.MAX_SIGNAL - 2))
        .thenExecute(
            () -> {
              helper.setBlock(source, Blocks.AIR);
              helper.setBlock(
                  at(RedstoneDioramas.CLIMB_TOP_SOURCE_X, RedstoneDioramas.CLIMB_TOP_Y, 0),
                  Blocks.REDSTONE_BLOCK);
            })
        .thenIdle(SETTLE)
        .thenExecute(
            () -> {
              expectPower(helper, top, RedstoneRules.MAX_SIGNAL);
              expectPower(helper, onSlab, 0);
            })
        .thenSucceed();
  }

  /** Glass over the lower dust leaves the climb whole; a solid block there cuts it. */
  @GameTest(maxTicks = 60)
  public void glassOverDustLeavesTheClimb(GameTestHelper helper) {
    DioramaPlacer.place(helper, RedstoneBuild.TRANSPARENCY.steps().get(2));
    BlockPos source = at(0, 1, 0);
    BlockPos top = at(RedstoneDioramas.CLIMB_GLASS_X, RedstoneDioramas.CLIMB_TOP_Y, 0);
    BlockPos onSlab = at(RedstoneDioramas.CLIMB_SLAB_X, 2, 0);
    helper
        .startSequence()
        .thenExecute(() -> helper.setBlock(source, Blocks.AIR))
        .thenIdle(SETTLE)
        .thenExecute(() -> helper.setBlock(source, Blocks.REDSTONE_BLOCK))
        .thenIdle(SETTLE)
        .thenExecute(() -> expectPower(helper, top, RedstoneRules.MAX_SIGNAL - 2))
        .thenExecute(
            () -> helper.setBlock(at(RedstoneDioramas.CLIMB_COVER_X, 2, 0), Blocks.STONE_BRICKS))
        .thenIdle(SETTLE)
        .thenExecute(() -> expectPower(helper, onSlab, 0))
        .thenSucceed();
  }

  /** A lever on glass lights nothing beside the glass; on a solid block it would. */
  @GameTest(maxTicks = 60)
  public void glassCarriesNoPower(GameTestHelper helper) {
    DioramaPlacer.place(helper, RedstoneBuild.TRANSPARENCY.steps().get(3));
    BlockPos lever = at(RedstoneDioramas.LEVER_GLASS_X, 2, 0);
    BlockPos lamp = at(RedstoneDioramas.LEVER_LAMP_X, 1, 0);
    helper
        .startSequence()
        .thenExecute(
            () -> helper.setBlock(lever, DioramaPlacer.state("lever[face=floor,facing=east]")))
        .thenIdle(SETTLE)
        .thenExecute(() -> helper.pullLever(lever))
        .thenIdle(SETTLE)
        .thenExecute(() -> expectLamp(helper, lamp, false))
        .thenExecute(
            () -> helper.setBlock(at(RedstoneDioramas.LEVER_GLASS_X, 1, 0), Blocks.STONE_BRICKS))
        .thenIdle(SETTLE)
        .thenExecute(() -> expectLamp(helper, lamp, true))
        .thenSucceed();
  }

  private static void expectPower(GameTestHelper helper, BlockPos dust, int power) {
    helper.assertValueEqual(
        helper.getBlockState(dust).getValue(RedstoneWireBlock.POWER), power, "dust at " + dust);
  }

  private static void expectLamp(GameTestHelper helper, BlockPos lamp, boolean lit) {
    helper.assertValueEqual(
        helper.getBlockState(lamp).getValue(RedstoneLampBlock.LIT), lit, "lamp at " + lamp);
  }

  /** The ticks a condition turns true (rises) and false (falls). */
  private static final class Edges {
    private final List<Long> rises = new ArrayList<>();
    private final List<Long> falls = new ArrayList<>();
    private boolean on;

    void see(boolean now, long tick) {
      if (now != on) {
        (now ? rises : falls).add(tick);
        on = now;
      }
    }
  }

  @GameTest(maxTicks = 120)
  public void sorterKeepsItsFilterAndPassesTheRest(GameTestHelper helper) {
    DioramaPlacer.place(helper, RedstoneBuild.ITEM_SORTER.finished());
    Container filter =
        container(
            helper, at(RedstoneDioramas.SORTER_FILTER_X, RedstoneDioramas.SORTER_FILTER_Y, 0));
    Container chest =
        container(helper, at(RedstoneDioramas.SORTER_CHEST_X, RedstoneDioramas.SORTER_LOCKED_Y, 0));
    filter.setItem(0, new ItemStack(Items.DIAMOND, SorterFilter.ITEMS));
    for (int slot = 1; slot <= SorterFilter.JUNK_SLOTS; slot++) {
      filter.setItem(slot, new ItemStack(Items.STICK));
    }
    filter.setChanged();
    Container locked =
        container(
            helper, at(RedstoneDioramas.SORTER_FILTER_X, RedstoneDioramas.SORTER_LOCKED_Y, 0));
    helper
        .startSequence()
        .thenIdle(SETTLE * 2)
        .thenExecute(
            () -> {
              helper.assertTrue(locked.isEmpty() && chest.isEmpty(), "the filter let items go");
              addOne(filter);
            })
        .thenIdle(SETTLE * 2)
        .thenExecute(
            () -> {
              // The hopper takes one and locks again before passing it on: it waits there.
              helper.assertValueEqual(
                  filter.getItem(0).getCount(), SorterFilter.ITEMS, "diamonds kept");
              helper.assertValueEqual(
                  locked.countItem(Items.DIAMOND) + chest.countItem(Items.DIAMOND),
                  1,
                  "diamonds taken");
              addOne(filter);
            })
        .thenIdle(SETTLE * 2)
        .thenExecute(
            () -> {
              helper.assertValueEqual(
                  filter.getItem(0).getCount(), SorterFilter.ITEMS, "diamonds kept");
              helper.assertTrue(
                  chest.countItem(Items.DIAMOND) >= 1, "no diamond reached the chest");
              helper.assertValueEqual(
                  locked.countItem(Items.STICK) + chest.countItem(Items.STICK), 0, "junk taken");
            })
        .thenSucceed();
  }

  /** A full stack for the self-emptying dropper, then one more item after it has rested. */
  private static final int DROPPER_STACK = 64;

  private static final int DROPPER_RUN_TICKS =
      (DROPPER_STACK + 1) * RedstoneRules.HOPPER_TICKS_PER_ITEM + SETTLE * 20;

  /**
   * The self-emptying dropper passes a whole stack on, rests empty, and starts again by itself when
   * one more item arrives.
   */
  @GameTest(maxTicks = DROPPER_RUN_TICKS)
  public void autoDropperEmptiesItself(GameTestHelper helper) {
    DioramaPlacer.place(helper, RedstoneBuild.AUTO_DROPPER.finished());
    BlockPos dropperPos = at(RedstoneDioramas.DROPPER_X, 1, RedstoneDioramas.DROPPER_ROW);
    Container hopper = container(helper, dropperPos.above());
    Container dropper = container(helper, dropperPos);
    Container chest =
        container(helper, at(RedstoneDioramas.DROPPER_CHEST_X, 1, RedstoneDioramas.DROPPER_ROW));
    hopper.setItem(0, new ItemStack(Items.COBBLESTONE, DROPPER_STACK));
    helper
        .startSequence()
        .thenWaitUntil(
            () ->
                helper.assertValueEqual(
                    chest.countItem(Items.COBBLESTONE), DROPPER_STACK, "the stack passed on"))
        .thenIdle(SETTLE * 4)
        .thenExecute(
            () -> {
              helper.assertTrue(dropper.isEmpty() && hopper.isEmpty(), "items left behind");
              hopper.setItem(0, new ItemStack(Items.COBBLESTONE));
            })
        .thenWaitUntil(
            () ->
                helper.assertValueEqual(
                    chest.countItem(Items.COBBLESTONE), DROPPER_STACK + 1, "the late item"))
        .thenExecute(() -> helper.assertTrue(dropper.isEmpty(), "the late item stayed"))
        .thenSucceed();
  }

  /** One more sorted item in the filter's first slot. */
  private static void addOne(Container filter) {
    filter.getItem(0).grow(1);
    filter.setChanged();
  }

  private static Container container(GameTestHelper helper, BlockPos pos) {
    BlockEntity entity = helper.getBlockEntity(pos, BlockEntity.class);
    if (!(entity instanceof Container container)) {
      throw new IllegalStateException("no container at " + pos);
    }
    return container;
  }
}
