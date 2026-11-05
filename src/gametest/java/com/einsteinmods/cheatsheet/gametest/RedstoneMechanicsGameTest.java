package com.einsteinmods.cheatsheet.gametest;

import static com.einsteinmods.cheatsheet.gametest.DioramaPlacer.at;

import com.einsteinmods.cheatsheet.core.pictures.BlockSpec;
import com.einsteinmods.cheatsheet.core.pictures.Diorama;
import com.einsteinmods.cheatsheet.core.pictures.DioramaBlock;
import com.einsteinmods.cheatsheet.core.redstone.CrafterRules;
import com.einsteinmods.cheatsheet.core.redstone.PistonRules;
import com.einsteinmods.cheatsheet.core.redstone.RedstoneBuild;
import com.einsteinmods.cheatsheet.core.redstone.RedstoneDioramas;
import com.einsteinmods.cheatsheet.core.redstone.RedstoneDioramas.StickyStep;
import com.einsteinmods.cheatsheet.core.redstone.RedstoneRules;
import com.einsteinmods.cheatsheet.core.redstone.TransparentBlock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RedstoneWireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.PushReaction;

/**
 * The redstone tab's newer mechanics against the game: the transparent-block table, what pistons
 * and slime do to each kind of block and each step of the slime guide, every way the
 * vertical-signal card shows to move a signal up or down, and the crafter as its zoom explains it.
 * Builds are placed as their pictures draw them ({@link DioramaPlacer}), worked and checked.
 */
public final class RedstoneMechanicsGameTest {
  private static final int SETTLE = 10;

  /** Each block of the table carries power and holds dust exactly as the table says. */
  @GameTest
  public void transparentBlocksMatch(GameTestHelper helper) {
    List<String> problems = new ArrayList<>();
    BlockPos pos = new BlockPos(1, 1, 1);
    for (TransparentBlock block : TransparentBlock.ALL) {
      BlockState state = DioramaPlacer.state(block.state());
      helper.setBlock(pos, state);
      BlockPos absolute = helper.absolutePos(pos);
      boolean conducts = state.isRedstoneConductor(helper.getLevel(), absolute);
      boolean holdsDust =
          Blocks.REDSTONE_WIRE.defaultBlockState().canSurvive(helper.getLevel(), absolute.above());
      if (conducts != block.conducts()) {
        problems.add(block.state() + " conducts: " + conducts);
      }
      if (holdsDust != block.holdsDust()) {
        problems.add(block.state() + " holds dust: " + holdsDust);
      }
      if (!BuiltInRegistries.ITEM.containsKey(Identifier.withDefaultNamespace(block.itemId()))) {
        problems.add("no item " + block.itemId());
      }
    }
    helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
    helper.succeed();
  }

  /** Powered from the top, the climb stays dark below: the signal never comes down. */
  @GameTest(maxTicks = 60)
  public void signalDoesNotComeDownTransparentBlocks(GameTestHelper helper) {
    DioramaPlacer.place(helper, RedstoneBuild.TRANSPARENCY.steps().get(1));
    BlockPos source = at(RedstoneDioramas.CLIMB_TOP_SOURCE_X, RedstoneDioramas.CLIMB_TOP_Y, 0);
    helper
        .startSequence()
        .thenExecute(() -> helper.setBlock(source, Blocks.AIR))
        .thenIdle(SETTLE)
        .thenExecute(() -> helper.setBlock(source, Blocks.REDSTONE_BLOCK))
        .thenIdle(SETTLE)
        .thenExecute(
            () -> {
              expectPower(
                  helper,
                  at(RedstoneDioramas.CLIMB_GLASS_X, RedstoneDioramas.CLIMB_TOP_Y, 0),
                  MAX_SIGNAL);
              expectPower(helper, at(RedstoneDioramas.CLIMB_SLAB_X, 2, 0), 0);
              expectPower(helper, at(1, 1, 0), 0);
            })
        .thenSucceed();
  }

  private static final int MAX_SIGNAL = 15;

  @GameTest(maxTicks = 60)
  public void slimeDragsANormalBlock(GameTestHelper helper) {
    stickyStep(helper, StickyStep.DRAG);
  }

  @GameTest(maxTicks = 60)
  public void slimeLeavesHoneyBehind(GameTestHelper helper) {
    stickyStep(helper, StickyStep.HONEY);
  }

  @GameTest(maxTicks = 60)
  public void slimeLeavesGlazedTerracottaBehind(GameTestHelper helper) {
    stickyStep(helper, StickyStep.GLAZED_TERRACOTTA);
  }

  @GameTest(maxTicks = 60)
  public void slimeLeavesAPumpkinBehindWhole(GameTestHelper helper) {
    stickyStep(helper, StickyStep.PUMPKIN);
  }

  @GameTest(maxTicks = 60)
  public void obsidianOnTheSlimeStopsEverything(GameTestHelper helper) {
    stickyStep(helper, StickyStep.OBSIDIAN);
  }

  /**
   * A step of the slime guide does what its picture shows: built as before, the lever pulled, the
   * world then matches the step's picture block by block.
   */
  private static void stickyStep(GameTestHelper helper, StickyStep step) {
    DioramaPlacer.place(helper, step.before());
    helper
        .startSequence()
        .thenExecute(
            () ->
                helper.pullLever(
                    at(
                        RedstoneDioramas.STICKY_SLIME_X,
                        RedstoneDioramas.STICKY_PISTON_Y,
                        RedstoneDioramas.STICKY_LEVER_Z)))
        .thenIdle(SETTLE)
        .thenExecute(() -> expectDiorama(helper, step.after()))
        .thenSucceed();
  }

  /**
   * Every cell of {@code diorama}'s box holds the block it draws with each property it names, and
   * the cells it leaves empty are air.
   */
  private static void expectDiorama(GameTestHelper helper, Diorama diorama) {
    Map<BlockPos, BlockSpec> drawn = new HashMap<>();
    for (DioramaBlock cell : diorama.blocks()) {
      drawn.put(new BlockPos(cell.x(), cell.y(), cell.z()), cell.block());
    }
    List<String> problems = new ArrayList<>();
    for (int x = 0; x < diorama.width(); x++) {
      for (int y = 0; y < diorama.height(); y++) {
        for (int z = 0; z < diorama.depth(); z++) {
          BlockState actual = helper.getBlockState(at(x, y, z));
          BlockSpec spec = drawn.get(new BlockPos(x, y, z));
          if (spec == null) {
            if (!actual.isAir()) {
              problems.add(x + "," + y + "," + z + " is " + actual + ", drawn empty");
            }
            continue;
          }
          BlockState expected = DioramaPlacer.state(spec);
          if (!actual.is(expected.getBlock())) {
            problems.add(x + "," + y + "," + z + " is " + actual + ", drawn " + expected);
            continue;
          }
          for (String name : spec.properties().keySet()) {
            Property<?> property = expected.getBlock().getStateDefinition().getProperty(name);
            if (property != null
                && !actual.getValue(property).equals(expected.getValue(property))) {
              problems.add(x + "," + y + "," + z + " " + name + " is " + actual.getValue(property));
            }
          }
        }
      }
    }
    helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
  }

  /**
   * The push limit, and each listed block reacts to a piston as its group says: in front of a
   * moving block it moves, breaks or stops the piston; a sticky piston pulls it back or not; beside
   * a moving slime it is dragged or not.
   */
  @GameTest
  public void pistonGroupsMatch(GameTestHelper helper) {
    helper.assertValueEqual(
        PistonStructureResolver.MAX_PUSH_DEPTH, PistonRules.MAX_PUSHED, "push limit");
    BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
    Direction push = Direction.WEST;
    List<String> problems = new ArrayList<>();
    for (PistonRules.Group group : PistonRules.Group.values()) {
      for (String id : group.blockIds()) {
        BlockState state = block(id);
        boolean headOn =
            PistonBaseBlock.isPushable(state, helper.getLevel(), pos, push, false, push);
        boolean sideways =
            PistonBaseBlock.isPushable(state, helper.getLevel(), pos, push, false, Direction.UP);
        boolean piston = state.is(Blocks.PISTON) || state.is(Blocks.STICKY_PISTON);
        // PistonBaseBlock#triggerEvent pulls back only a PUSH_PULL block or a piston.
        boolean pulled =
            headOn && (state.getPistonPushReaction() == PushReaction.PUSH_PULL || piston);
        PistonRules.Push pushed =
            headOn
                ? PistonRules.Push.MOVES
                : state.getPistonPushReaction() == PushReaction.POPPED
                    ? PistonRules.Push.BREAKS
                    : PistonRules.Push.STOPS;
        PistonRules.Drag dragged =
            !sideways
                ? PistonRules.Drag.NO
                : state.is(Blocks.SLIME_BLOCK) || state.is(Blocks.HONEY_BLOCK)
                    ? PistonRules.Drag.SAME_KIND_ONLY
                    : PistonRules.Drag.YES;
        if (pushed != group.pushed() || pulled != group.pulled() || dragged != group.dragged()) {
          problems.add(id + ": " + pushed + ", pulled " + pulled + ", " + dragged);
        }
      }
    }
    helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
    helper.succeed();
  }

  private static BlockState block(String id) {
    return BuiltInRegistries.BLOCK
        .getValue(Identifier.withDefaultNamespace(id))
        .defaultBlockState();
  }

  /** The dust staircase on solid blocks carries the lever up, and a signal from the top down. */
  @GameTest(maxTicks = 80)
  public void dustStaircaseGoesUpAndDown(GameTestHelper helper) {
    DioramaPlacer.place(helper, RedstoneBuild.VERTICAL_SIGNAL.steps().get(0));
    BlockPos lever = at(RedstoneDioramas.VERTICAL_LEVER_X, 1, 0);
    BlockPos top = at(0, 3, 0);
    BlockPos bottom = at(2, 1, 0);
    helper
        .startSequence()
        .thenExecute(() -> helper.pullLever(lever))
        .thenIdle(SETTLE)
        .thenExecute(() -> expectPower(helper, top, 0))
        .thenExecute(() -> helper.pullLever(lever))
        .thenIdle(SETTLE)
        .thenExecute(() -> expectPowered(helper, top, true))
        .thenExecute(
            () -> {
              helper.pullLever(lever);
              helper.setBlock(top.west(), Blocks.REDSTONE_BLOCK);
            })
        .thenIdle(SETTLE)
        .thenExecute(() -> expectPowered(helper, bottom, true))
        .thenSucceed();
  }

  /** The zigzag of upside-down slabs carries the lever up; a signal at its top stays there. */
  @GameTest(maxTicks = 80)
  public void slabZigzagGoesUpOnly(GameTestHelper helper) {
    DioramaPlacer.place(helper, RedstoneBuild.VERTICAL_SIGNAL.steps().get(1));
    BlockPos lever = at(RedstoneDioramas.VERTICAL_LEVER_X, 1, 0);
    int column = RedstoneDioramas.VERTICAL_COLUMN_X;
    int slabs = RedstoneDioramas.ZIGZAG_SLABS;
    BlockPos top = at(slabs % 2 == 0 ? column + 1 : column, slabs + 1, 0);
    BlockPos belowTop = at(slabs % 2 == 0 ? column : column + 1, slabs, 0);
    helper
        .startSequence()
        .thenExecute(() -> helper.pullLever(lever))
        .thenIdle(SETTLE)
        .thenExecute(() -> expectPower(helper, top, 0))
        .thenExecute(() -> helper.pullLever(lever))
        .thenIdle(SETTLE)
        .thenExecute(() -> expectPowered(helper, top, true))
        .thenExecute(
            () -> {
              helper.pullLever(lever);
              helper.setBlock(top.east(), Blocks.REDSTONE_BLOCK);
            })
        .thenIdle(SETTLE)
        .thenExecute(
            () -> {
              expectPower(helper, top, MAX_SIGNAL);
              expectPower(helper, belowTop, 0);
            })
        .thenSucceed();
  }

  /** Two torches up the tower: the lamp on top follows the lever (each torch flips it). */
  @GameTest(maxTicks = 80)
  public void torchTowerCarriesTheLeverUp(GameTestHelper helper) {
    DioramaPlacer.place(helper, RedstoneBuild.VERTICAL_SIGNAL.steps().get(2));
    int column = RedstoneDioramas.VERTICAL_COLUMN_X;
    BlockPos lever = at(column + 1, 1, 0);
    BlockPos lamp = at(column, RedstoneDioramas.TORCH_TOWER_LAMP_Y, 0);
    helper
        .startSequence()
        .thenExecute(() -> helper.pullLever(lever))
        .thenIdle(SETTLE)
        .thenExecute(() -> expectLamp(helper, lamp, false))
        .thenExecute(() -> helper.pullLever(lever))
        .thenIdle(SETTLE)
        .thenExecute(() -> expectLamp(helper, lamp, true))
        .thenSucceed();
  }

  /** Observers stacked facing down pass a flip of the lever up as a pulse. */
  @GameTest(maxTicks = 60)
  public void observersPulseUp(GameTestHelper helper) {
    DioramaPlacer.place(helper, RedstoneBuild.VERTICAL_SIGNAL.steps().get(3));
    int column = RedstoneDioramas.VERTICAL_COLUMN_X;
    BlockPos lamp = at(column, RedstoneDioramas.OBSERVER_LAMP_Y, 0);
    boolean[] lit = {false};
    helper.onEachTick(() -> lit[0] |= isLit(helper, lamp));
    helper.runAfterDelay(2, () -> helper.pullLever(at(column, 1, 0)));
    helper.runAfterDelay(
        SETTLE * 2,
        () -> {
          helper.assertTrue(lit[0], "the lamp on top never lit");
          helper.succeed();
        });
  }

  /** Observers stacked facing up pass a flip of a lever on top down to a lamp under them. */
  @GameTest(maxTicks = 60)
  public void observersPulseDown(GameTestHelper helper) {
    DioramaPlacer.place(helper, RedstoneBuild.VERTICAL_SIGNAL.steps().get(3));
    int x = RedstoneDioramas.VERTICAL_LEVER_X + 2;
    BlockPos lamp = at(x, 0, 0);
    helper.setBlock(lamp, Blocks.REDSTONE_LAMP);
    helper.setBlock(at(x, 1, 0), DioramaPlacer.state("observer[facing=up]"));
    helper.setBlock(at(x, 2, 0), DioramaPlacer.state("observer[facing=up]"));
    helper.setBlock(at(x, 3, 0), DioramaPlacer.state("lever[face=floor,facing=east]"));
    // Placing the observers may pulse the lamp once: only watch it after the lever is pulled.
    boolean[] pulled = {false};
    boolean[] lit = {false};
    helper.onEachTick(() -> lit[0] |= pulled[0] && isLit(helper, lamp));
    helper.runAfterDelay(
        SETTLE,
        () -> {
          pulled[0] = true;
          helper.pullLever(at(x, 3, 0));
        });
    helper.runAfterDelay(
        SETTLE * 3,
        () -> {
          helper.assertTrue(lit[0], "the lamp below never lit");
          helper.succeed();
        });
  }

  /** The sculk sensor up its pillar hears the lever below and lights the lamp on top of it. */
  @GameTest(maxTicks = 60)
  public void sculkSensorHearsTheLever(GameTestHelper helper) {
    DioramaPlacer.place(helper, RedstoneBuild.VERTICAL_SIGNAL.steps().get(4));
    int column = RedstoneDioramas.VERTICAL_COLUMN_X;
    BlockPos lamp = at(column, RedstoneDioramas.SCULK_SENSOR_Y + 1, 0);
    boolean[] lit = {false};
    helper.onEachTick(() -> lit[0] |= isLit(helper, lamp));
    helper.runAfterDelay(2, () -> helper.pullLever(at(RedstoneDioramas.VERTICAL_LEVER_X, 1, 0)));
    helper.runAfterDelay(
        SETTLE * 3,
        () -> {
          helper.assertTrue(lit[0], "the sensor never lit the lamp");
          helper.succeed();
        });
  }

  /** Planks fed one by one into the crafter of the sheet's example. */
  private static final int FED_PLANKS = 10;

  private static final int MAX_STACK = 64;

  /**
   * The crafter of the zoom, the example recipe set (the centre slot disabled): each plank a hopper
   * gives goes where {@link CrafterRules#nextSlot} says, never into the disabled slot; full, the
   * comparator reads 9; a button press crafts one chest, the delay later, out of the front into the
   * hopper and chest there, using one plank per slot; the comparator then reads what is left.
   */
  @GameTest(maxTicks = 120)
  public void crafterFillsEvenlyAndCraftsOncePerPulse(GameTestHelper helper) {
    DioramaPlacer.place(helper, RedstoneDioramas.crafter());
    int row = RedstoneDioramas.CRAFTER_ROW;
    int y = RedstoneDioramas.CRAFTER_Y;
    CrafterBlockEntity crafter =
        helper.getBlockEntity(at(RedstoneDioramas.CRAFTER_X, y, row), CrafterBlockEntity.class);
    Container out = container(helper, at(RedstoneDioramas.CRAFTER_OUT_HOPPER_X, y, row));
    Container chest = container(helper, at(RedstoneDioramas.CRAFTER_CHEST_X, y, row));
    BlockPos dust = at(RedstoneDioramas.CRAFTER_DUST_X, y, row);
    BlockPos button = at(RedstoneDioramas.CRAFTER_X, y, RedstoneDioramas.CRAFTER_BUTTON_Z);
    int disabled = CrafterRules.EXAMPLE_DISABLED_SLOT;
    long[] pressed = {-1};
    long[] crafted = {-1};
    helper.onEachTick(
        () -> {
          if (crafted[0] < 0 && out.countItem(Items.CHEST) + chest.countItem(Items.CHEST) > 0) {
            crafted[0] = helper.getTick();
          }
        });
    helper
        .startSequence()
        .thenExecute(
            () -> {
              crafter.setSlotState(disabled, false);
              int[] counts = new int[CrafterRules.SLOTS];
              boolean[] off = new boolean[CrafterRules.SLOTS];
              off[disabled] = true;
              for (int plank = 0; plank < FED_PLANKS; plank++) {
                int expected = CrafterRules.nextSlot(counts, off, MAX_STACK);
                ItemStack rest =
                    HopperBlockEntity.addItem(
                        null, crafter, new ItemStack(Items.OAK_PLANKS), Direction.DOWN);
                helper.assertTrue(rest.isEmpty(), "plank " + plank + " refused");
                counts[expected]++;
                for (int slot = 0; slot < CrafterRules.SLOTS; slot++) {
                  helper.assertValueEqual(
                      crafter.getItem(slot).getCount(),
                      counts[slot],
                      "plank " + plank + " slot " + slot);
                }
              }
            })
        .thenIdle(SETTLE)
        .thenExecute(
            () -> {
              expectPower(helper, dust, CrafterRules.comparatorSignal(CrafterRules.SLOTS - 1, 1));
              pressed[0] = helper.getTick();
              helper.pressButton(button);
            })
        .thenIdle(SETTLE * 3)
        .thenExecute(
            () -> {
              helper.assertValueEqual(
                  out.countItem(Items.CHEST) + chest.countItem(Items.CHEST), 1, "chests made");
              helper.assertValueEqual(
                  (int) (crafted[0] - pressed[0]), CrafterRules.CRAFT_DELAY_TICKS, "craft delay");
              int left = FED_PLANKS - (CrafterRules.SLOTS - 1);
              helper.assertValueEqual(
                  crafter.countItem(Items.OAK_PLANKS), left, "planks left, one per slot used");
              helper.assertTrue(crafter.isSlotDisabled(disabled), "the disabled slot came back on");
              expectPower(helper, dust, CrafterRules.comparatorSignal(left, 1));
            })
        .thenIdle(RedstoneRules.STONE_BUTTON_TICKS + SETTLE)
        .thenExecute(
            () ->
                helper.assertValueEqual(
                    out.countItem(Items.CHEST) + chest.countItem(Items.CHEST),
                    1,
                    "one press crafted more than once"))
        .thenSucceed();
  }

  /** A cake's milk buckets come back empty, out of the front like the cake. */
  @GameTest(maxTicks = 120)
  public void crafterPutsOutLeftoversToo(GameTestHelper helper) {
    DioramaPlacer.place(helper, RedstoneDioramas.crafter());
    int row = RedstoneDioramas.CRAFTER_ROW;
    int y = RedstoneDioramas.CRAFTER_Y;
    CrafterBlockEntity crafter =
        helper.getBlockEntity(at(RedstoneDioramas.CRAFTER_X, y, row), CrafterBlockEntity.class);
    Container out = container(helper, at(RedstoneDioramas.CRAFTER_OUT_HOPPER_X, y, row));
    Container chest = container(helper, at(RedstoneDioramas.CRAFTER_CHEST_X, y, row));
    List<Item> cake =
        List.of(
            Items.MILK_BUCKET,
            Items.MILK_BUCKET,
            Items.MILK_BUCKET,
            Items.SUGAR,
            Items.EGG,
            Items.SUGAR,
            Items.WHEAT,
            Items.WHEAT,
            Items.WHEAT);
    helper
        .startSequence()
        .thenExecute(
            () -> {
              for (int slot = 0; slot < cake.size(); slot++) {
                crafter.setItem(slot, new ItemStack(cake.get(slot)));
              }
              helper.pressButton(
                  at(RedstoneDioramas.CRAFTER_X, y, RedstoneDioramas.CRAFTER_BUTTON_Z));
            })
        .thenIdle(SETTLE * 6)
        .thenExecute(
            () -> {
              helper.assertValueEqual(
                  out.countItem(Items.CAKE) + chest.countItem(Items.CAKE), 1, "cakes");
              helper.assertValueEqual(
                  out.countItem(Items.BUCKET) + chest.countItem(Items.BUCKET),
                  CAKE_BUCKETS,
                  "empty buckets");
            })
        .thenSucceed();
  }

  private static final int CAKE_BUCKETS = 3;

  /** A hopper under a crafter takes its ingredients out. */
  @GameTest(maxTicks = 60)
  public void hopperUnderACrafterEmptiesIt(GameTestHelper helper) {
    BlockPos hopperPos = new BlockPos(1, 1, 1);
    BlockPos crafterPos = hopperPos.above();
    helper.setBlock(hopperPos, DioramaPlacer.state("hopper[facing=east]"));
    helper.setBlock(crafterPos, DioramaPlacer.state("crafter[orientation=north_up]"));
    CrafterBlockEntity crafter = helper.getBlockEntity(crafterPos, CrafterBlockEntity.class);
    crafter.setItem(0, new ItemStack(Items.OAK_PLANKS));
    Container hopper = container(helper, hopperPos);
    helper.succeedWhen(
        () -> {
          helper.assertTrue(crafter.isEmpty(), "the plank is still in the crafter");
          helper.assertValueEqual(hopper.countItem(Items.OAK_PLANKS), 1, "planks in the hopper");
        });
  }

  /** Ingots fed to the machines of the guide. */
  private static final int INGOTS = 81;

  private static final int IRON_BLOCKS = 9;

  /** Room for every item of a test's run, plus the first one's wait. */
  private static final int MACHINE_RUN_TICKS = INGOTS * RedstoneRules.HOPPER_TICKS_PER_ITEM + 60;

  /** The guide's machine as shown: 81 ingots through one hopper, 9 blocks and never a nugget. */
  @GameTest(maxTicks = MACHINE_RUN_TICKS)
  public void ironMachineCraftsOnlyBlocks(GameTestHelper helper) {
    DioramaPlacer.place(helper, RedstoneDioramas.ironMachine().getLast());
    runIronLikeMachine(
        helper,
        at(RedstoneDioramas.IRON_CRAFTER_X, RedstoneDioramas.GUIDE_Y, RedstoneDioramas.GUIDE_ROW),
        at(RedstoneDioramas.IRON_CHEST_X, RedstoneDioramas.GUIDE_Y, RedstoneDioramas.GUIDE_ROW));
  }

  /** The compact version gives the same result, a crafter with nine disabled slots as its 9. */
  @GameTest(maxTicks = MACHINE_RUN_TICKS)
  public void compactMachineCraftsOnlyBlocks(GameTestHelper helper) {
    DioramaPlacer.place(helper, RedstoneDioramas.compactMachine());
    CrafterBlockEntity spare =
        helper.getBlockEntity(
            at(
                RedstoneDioramas.COMPACT_SPARE_X,
                RedstoneDioramas.GUIDE_Y,
                RedstoneDioramas.COMPACT_SPARE_Z),
            CrafterBlockEntity.class);
    for (int slot = 0; slot < CrafterRules.SLOTS; slot++) {
      spare.setSlotState(slot, false);
    }
    BlockPos crafter =
        at(
            RedstoneDioramas.COMPACT_CRAFTER_X,
            RedstoneDioramas.GUIDE_Y,
            RedstoneDioramas.COMPACT_ROW);
    BlockPos chest =
        at(
            RedstoneDioramas.COMPACT_CHEST_X,
            RedstoneDioramas.GUIDE_Y,
            RedstoneDioramas.COMPACT_ROW);
    runIronLikeMachine(helper, crafter, chest);
  }

  private static void runIronLikeMachine(
      GameTestHelper helper, BlockPos crafterPos, BlockPos chestPos) {
    Container hopper = container(helper, crafterPos.above());
    Container crafter = container(helper, crafterPos);
    Container chest = container(helper, chestPos);
    hopper.setItem(0, new ItemStack(Items.IRON_INGOT, MAX_STACK));
    hopper.setItem(1, new ItemStack(Items.IRON_INGOT, INGOTS - MAX_STACK));
    long[] lastBlock = {-1};
    int[] blocksSeen = {0};
    List<Long> gaps = new ArrayList<>();
    helper.onEachTick(
        () -> {
          int nuggets =
              chest.countItem(Items.IRON_NUGGET)
                  + crafter.countItem(Items.IRON_NUGGET)
                  + hopper.countItem(Items.IRON_NUGGET);
          helper.assertValueEqual(nuggets, 0, "iron nuggets");
          int blocks = chest.countItem(Items.IRON_BLOCK);
          if (blocks > blocksSeen[0]) {
            blocksSeen[0] = blocks;
            if (lastBlock[0] >= 0) {
              gaps.add(helper.getTick() - lastBlock[0]);
            }
            lastBlock[0] = helper.getTick();
          }
        });
    helper.succeedWhen(
        () -> {
          helper.assertValueEqual(blocksSeen[0], IRON_BLOCKS, "iron blocks");
          helper.assertValueEqual(chest.countItem(Items.IRON_INGOT), 0, "ingots in the chest");
          helper.assertTrue(crafter.isEmpty(), "ingots left in the crafter");
          helper.assertTrue(hopper.isEmpty(), "ingots left in the hopper");
          for (long gap : gaps) {
            helper.assertValueEqual((int) gap, CrafterRules.ticksPerCraft(1), "ticks per block");
          }
        });
  }

  /**
   * Why nine dust: eight ingots read 8, which dies one dust short (the block is never powered and
   * nothing is crafted); the ninth reads 9, which reaches the last dust with 1 left.
   */
  @GameTest(maxTicks = 200)
  public void ironMachineWaitsForTheFullGrid(GameTestHelper helper) {
    DioramaPlacer.place(helper, RedstoneDioramas.ironMachine().getLast());
    int y = RedstoneDioramas.GUIDE_Y;
    BlockPos crafterPos = at(RedstoneDioramas.IRON_CRAFTER_X, y, RedstoneDioramas.GUIDE_ROW);
    BlockPos last = at(RedstoneDioramas.IRON_LAST_DUST_X, y, RedstoneDioramas.IRON_LAST_DUST_Z);
    Container hopper = container(helper, crafterPos.above());
    Container crafter = container(helper, crafterPos);
    Container chest =
        container(helper, at(RedstoneDioramas.IRON_CHEST_X, y, RedstoneDioramas.GUIDE_ROW));
    int[] strongest = {0};
    helper.onEachTick(
        () ->
            strongest[0] =
                Math.max(
                    strongest[0], helper.getBlockState(last).getValue(RedstoneWireBlock.POWER)));
    hopper.setItem(0, new ItemStack(Items.IRON_INGOT, CrafterRules.SLOTS - 1));
    helper
        .startSequence()
        .thenIdle(CrafterRules.SLOTS * RedstoneRules.HOPPER_TICKS_PER_ITEM + SETTLE)
        .thenExecute(
            () -> {
              helper.assertValueEqual(
                  crafter.countItem(Items.IRON_INGOT), CrafterRules.SLOTS - 1, "ingots in place");
              helper.assertValueEqual(strongest[0], 0, "power at the last dust with 8");
              hopper.setItem(0, new ItemStack(Items.IRON_INGOT));
            })
        .thenIdle(SETTLE * 2)
        .thenExecute(
            () -> {
              helper.assertValueEqual(strongest[0], 1, "power at the last dust with 9");
              helper.assertValueEqual(chest.countItem(Items.IRON_BLOCK), 1, "iron blocks");
              expectPower(helper, last, 0);
            })
        .thenSucceed();
  }

  /** What the ninth dust prevents: one ingot alone is a recipe already, nine nuggets. */
  @GameTest(maxTicks = 60)
  public void oneIngotAloneCraftsNuggets(GameTestHelper helper) {
    DioramaPlacer.place(helper, RedstoneDioramas.crafter());
    int row = RedstoneDioramas.CRAFTER_ROW;
    int y = RedstoneDioramas.CRAFTER_Y;
    CrafterBlockEntity crafter =
        helper.getBlockEntity(at(RedstoneDioramas.CRAFTER_X, y, row), CrafterBlockEntity.class);
    Container out = container(helper, at(RedstoneDioramas.CRAFTER_OUT_HOPPER_X, y, row));
    Container chest = container(helper, at(RedstoneDioramas.CRAFTER_CHEST_X, y, row));
    crafter.setItem(0, new ItemStack(Items.IRON_INGOT));
    helper.pressButton(at(RedstoneDioramas.CRAFTER_X, y, RedstoneDioramas.CRAFTER_BUTTON_Z));
    helper.succeedWhen(
        () ->
            helper.assertValueEqual(
                out.countItem(Items.IRON_NUGGET) + chest.countItem(Items.IRON_NUGGET),
                CrafterRules.SLOTS,
                "nuggets"));
  }

  /** The guide's tip: disable what stays empty and the same machine crafts any recipe. */
  @GameTest(maxTicks = 300)
  public void ironMachineCraftsAnyRecipeWithItsEmptySlotsDisabled(GameTestHelper helper) {
    DioramaPlacer.place(helper, RedstoneDioramas.ironMachine().getLast());
    int y = RedstoneDioramas.GUIDE_Y;
    BlockPos crafterPos = at(RedstoneDioramas.IRON_CRAFTER_X, y, RedstoneDioramas.GUIDE_ROW);
    CrafterBlockEntity crafter = helper.getBlockEntity(crafterPos, CrafterBlockEntity.class);
    crafter.setSlotState(CrafterRules.EXAMPLE_DISABLED_SLOT, false);
    Container hopper = container(helper, crafterPos.above());
    Container chest =
        container(helper, at(RedstoneDioramas.IRON_CHEST_X, y, RedstoneDioramas.GUIDE_ROW));
    hopper.setItem(0, new ItemStack(Items.OAK_PLANKS, (CrafterRules.SLOTS - 1) * EXAMPLE_CHESTS));
    helper.succeedWhen(
        () -> {
          helper.assertValueEqual(chest.countItem(Items.CHEST), EXAMPLE_CHESTS, "chests");
          helper.assertTrue(crafter.isEmpty(), "planks left");
        });
  }

  private static final int EXAMPLE_CHESTS = 2;

  /** The guide's tip: a pulse on an incomplete recipe clicks and spends nothing. */
  @GameTest(maxTicks = 60)
  public void crafterWithAnIncompleteRecipeSpendsNothing(GameTestHelper helper) {
    DioramaPlacer.place(helper, RedstoneDioramas.crafter());
    int row = RedstoneDioramas.CRAFTER_ROW;
    int y = RedstoneDioramas.CRAFTER_Y;
    CrafterBlockEntity crafter =
        helper.getBlockEntity(at(RedstoneDioramas.CRAFTER_X, y, row), CrafterBlockEntity.class);
    Container out = container(helper, at(RedstoneDioramas.CRAFTER_OUT_HOPPER_X, y, row));
    Container chest = container(helper, at(RedstoneDioramas.CRAFTER_CHEST_X, y, row));
    crafter.setItem(0, new ItemStack(Items.OAK_PLANKS));
    crafter.setItem(1, new ItemStack(Items.OAK_PLANKS));
    crafter.setItem(3, new ItemStack(Items.OAK_PLANKS));
    helper.pressButton(at(RedstoneDioramas.CRAFTER_X, y, RedstoneDioramas.CRAFTER_BUTTON_Z));
    helper.runAfterDelay(
        SETTLE * 2,
        () -> {
          helper.assertValueEqual(crafter.countItem(Items.OAK_PLANKS), 3, "planks kept");
          helper.assertTrue(out.isEmpty() && chest.isEmpty(), "something came out");
          helper.succeed();
        });
  }

  /** The guide's tip: fed in the recipe's slot order it crafts, in another order it does not. */
  @GameTest(maxTicks = 120)
  public void aRecipeOfTwoIngredientsNeedsThemInSlotOrder(GameTestHelper helper) {
    DioramaPlacer.place(helper, RedstoneDioramas.ironMachine().getLast());
    int y = RedstoneDioramas.GUIDE_Y;
    BlockPos crafterPos = at(RedstoneDioramas.IRON_CRAFTER_X, y, RedstoneDioramas.GUIDE_ROW);
    CrafterBlockEntity crafter = helper.getBlockEntity(crafterPos, CrafterBlockEntity.class);
    for (int slot = 0; slot < CrafterRules.SLOTS; slot++) {
      if (slot != TORCH_COAL_SLOT && slot != TORCH_STICK_SLOT) {
        crafter.setSlotState(slot, false);
      }
    }
    Container hopper = container(helper, crafterPos.above());
    Container chest =
        container(helper, at(RedstoneDioramas.IRON_CHEST_X, y, RedstoneDioramas.GUIDE_ROW));
    hopper.setItem(0, new ItemStack(Items.STICK));
    hopper.setItem(1, new ItemStack(Items.COAL));
    helper
        .startSequence()
        .thenIdle(SETTLE * 4)
        .thenExecute(
            () -> {
              helper.assertValueEqual(chest.countItem(Items.TORCH), 0, "torches, wrong order");
              helper.assertValueEqual(
                  crafter.getItem(TORCH_COAL_SLOT).getItem(), Items.STICK, "the stick went first");
              crafter.clearContent();
              hopper.setItem(0, new ItemStack(Items.COAL));
              hopper.setItem(1, new ItemStack(Items.STICK));
            })
        .thenIdle(SETTLE * 4)
        .thenExecute(
            () -> helper.assertValueEqual(chest.countItem(Items.TORCH), 4, "torches, right order"))
        .thenSucceed();
  }

  private static final int TORCH_COAL_SLOT = 1;
  private static final int TORCH_STICK_SLOT = 4;

  private static final int HOPPERS = 2;

  private static final int SIDE_HOPPER_LATE_TICKS = 3;

  /** The guide's tip: a second hopper in step with the first doubles the speed. */
  @GameTest(maxTicks = MACHINE_RUN_TICKS)
  public void secondHopperInStepDoublesTheSpeed(GameTestHelper helper) {
    runWithTwoHoppers(helper, 0);
  }

  /** The same with hoppers out of step: extra ingots wait in a slot and never jam the signal. */
  @GameTest(maxTicks = MACHINE_RUN_TICKS)
  public void secondHopperOutOfStepDoublesTheSpeed(GameTestHelper helper) {
    runWithTwoHoppers(helper, SIDE_HOPPER_LATE_TICKS);
  }

  /**
   * The iron machine with a second hopper on the crafter's free side, filled {@code lateTicks}
   * after the first: every block is crafted at twice the speed of one hopper, never a nugget. Each
   * hopper holds one stack.
   */
  private static void runWithTwoHoppers(GameTestHelper helper, int lateTicks) {
    DioramaPlacer.place(helper, RedstoneDioramas.ironMachine().getLast());
    int y = RedstoneDioramas.GUIDE_Y;
    int row = RedstoneDioramas.GUIDE_ROW;
    BlockPos crafterPos = at(RedstoneDioramas.IRON_CRAFTER_X, y, row);
    BlockPos sideHopper = crafterPos.south();
    helper.setBlock(sideHopper, DioramaPlacer.state("hopper[facing=north]"));
    Container top = container(helper, crafterPos.above());
    Container side = container(helper, sideHopper);
    Container chest = container(helper, at(RedstoneDioramas.IRON_CHEST_X, y, row));
    int stacks = HOPPERS * MAX_STACK;
    int blocks = stacks / CrafterRules.SLOTS;
    top.setItem(0, new ItemStack(Items.IRON_INGOT, MAX_STACK));
    helper.runAfterDelay(
        lateTicks, () -> side.setItem(0, new ItemStack(Items.IRON_INGOT, MAX_STACK)));
    long started = helper.getTick();
    long[] finished = {-1};
    helper.onEachTick(
        () -> {
          helper.assertValueEqual(chest.countItem(Items.IRON_NUGGET), 0, "iron nuggets");
          if (finished[0] < 0 && chest.countItem(Items.IRON_BLOCK) == blocks) {
            finished[0] = helper.getTick();
          }
        });
    helper.succeedWhen(
        () -> {
          helper.assertTrue(finished[0] >= 0, "blocks still being crafted");
          helper.assertTrue(
              finished[0] - started
                  <= blocks * CrafterRules.ticksPerCraft(HOPPERS) + TWO_HOPPER_SLACK_TICKS,
              "took " + (finished[0] - started) + " ticks for " + blocks + " blocks");
        });
  }

  /** The first items' wait plus one craft's delay, on top of a two-hopper run's whole time. */
  private static final int TWO_HOPPER_SLACK_TICKS = 2 * RedstoneRules.HOPPER_TICKS_PER_ITEM;

  /** The chain of two crafters: 81 nuggets through one hopper, one gold block. */
  @GameTest(maxTicks = MACHINE_RUN_TICKS)
  public void goldMachineCraftsNuggetsIntoABlock(GameTestHelper helper) {
    DioramaPlacer.place(helper, RedstoneDioramas.goldMachine().getLast());
    int y = RedstoneDioramas.GUIDE_Y;
    int row = RedstoneDioramas.GUIDE_ROW;
    Container hopper = container(helper, at(RedstoneDioramas.GOLD_FIRST_X, y + 1, row));
    Container first = container(helper, at(RedstoneDioramas.GOLD_FIRST_X, y, row));
    Container second = container(helper, at(RedstoneDioramas.GOLD_SECOND_X, y, row));
    Container chest = container(helper, at(RedstoneDioramas.GOLD_CHEST_X, y, row));
    hopper.setItem(0, new ItemStack(Items.GOLD_NUGGET, MAX_STACK));
    hopper.setItem(1, new ItemStack(Items.GOLD_NUGGET, INGOTS - MAX_STACK));
    helper.succeedWhen(
        () -> {
          helper.assertValueEqual(chest.countItem(Items.GOLD_BLOCK), 1, "gold blocks");
          helper.assertValueEqual(chest.countItem(Items.GOLD_NUGGET), 0, "nuggets out");
          helper.assertValueEqual(chest.countItem(Items.GOLD_INGOT), 0, "ingots out");
          helper.assertTrue(first.isEmpty(), "nuggets left in the first crafter");
          helper.assertTrue(second.isEmpty(), "ingots left in the second crafter");
          helper.assertTrue(hopper.isEmpty(), "nuggets left in the hopper");
        });
  }

  private static Container container(GameTestHelper helper, BlockPos pos) {
    BlockEntity entity = helper.getBlockEntity(pos, BlockEntity.class);
    if (!(entity instanceof Container container)) {
      throw new IllegalStateException("no container at " + pos);
    }
    return container;
  }

  private static boolean isLit(GameTestHelper helper, BlockPos lamp) {
    BlockState state = helper.getBlockState(lamp);
    return state.hasProperty(RedstoneLampBlock.LIT) && state.getValue(RedstoneLampBlock.LIT);
  }

  private static void expectLamp(GameTestHelper helper, BlockPos lamp, boolean lit) {
    helper.assertValueEqual(isLit(helper, lamp), lit, "lamp at " + lamp);
  }

  private static void expectPower(GameTestHelper helper, BlockPos dust, int power) {
    helper.assertValueEqual(
        helper.getBlockState(dust).getValue(RedstoneWireBlock.POWER), power, "dust at " + dust);
  }

  private static void expectPowered(GameTestHelper helper, BlockPos dust, boolean powered) {
    helper.assertValueEqual(
        helper.getBlockState(dust).getValue(RedstoneWireBlock.POWER) > 0,
        powered,
        "dust powered at " + dust);
  }
}
