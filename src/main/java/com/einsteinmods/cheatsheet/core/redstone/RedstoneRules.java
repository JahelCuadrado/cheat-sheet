package com.einsteinmods.cheatsheet.core.redstone;

import java.util.Locale;

/**
 * The numbers of redstone components and circuits, copied from Minecraft 26.3 (each constant names
 * its source): signal strengths, delays and pulse lengths. {@code RedstoneRulesGameTest} compares
 * the ones vanilla makes public with the game's own constants and measures the others in a world;
 * {@code RedstoneBuildsGameTest} measures the circuits' timings.
 */
public final class RedstoneRules {
  /** Strongest signal: {@code BlockStateProperties.POWER} runs 0 to 15. */
  public static final int MAX_SIGNAL = 15;

  /** Game ticks per second. */
  public static final int TICKS_PER_SECOND = 20;

  /** A redstone tick is two game ticks: {@code RepeaterBlock#getDelay} is {@code delay * 2}. */
  public static final int GAME_TICKS_PER_REDSTONE_TICK = 2;

  /** Repeater delays in redstone ticks: {@code BlockStateProperties.DELAY} runs 1 to 4. */
  public static final int REPEATER_MIN_DELAY = 1;

  public static final int REPEATER_MAX_DELAY = 4;

  /** {@code ComparatorBlock#getDelay}: 2 game ticks. */
  public static final int COMPARATOR_DELAY_TICKS = 2;

  /** {@code RedstoneTorchBlock#neighborChanged} schedules the torch's switch 2 game ticks later. */
  public static final int TORCH_DELAY_TICKS = 2;

  /**
   * An observer turns on 2 game ticks after the change it sees and off 2 game ticks later: {@code
   * ObserverBlock#startSignal} and {@code ObserverBlock#tick} both schedule 2.
   */
  public static final int OBSERVER_PULSE_TICKS = 2;

  /** {@code RedstoneTorchBlock.MAX_RECENT_TOGGLES}: this many turn-offs burn a torch out... */
  public static final int TORCH_BURNOUT_TOGGLES = 8;

  /** ...within {@code RedstoneTorchBlock.RECENT_TOGGLE_TIMER} game ticks. */
  public static final int TORCH_BURNOUT_WINDOW_TICKS = 60;

  /** A burnt-out torch tries to light again after {@code RedstoneTorchBlock.RESTART_DELAY}. */
  public static final int TORCH_RESTART_TICKS = 160;

  /** {@code Blocks.STONE_BUTTON} (and the polished blackstone one) stays pressed 20 ticks. */
  public static final int STONE_BUTTON_TICKS = 20;

  /** Every wooden button ({@code Blocks.OAK_BUTTON} and the rest) stays pressed 30 ticks. */
  public static final int WOODEN_BUTTON_TICKS = 30;

  /**
   * A wooden or stone plate checks again this long after it is pressed, so even a touch keeps it on
   * that long: {@code BasePressurePlateBlock#getPressedTime}.
   */
  public static final int PLATE_PRESSED_TICKS = 20;

  /** The same for weighted plates: {@code WeightedPressurePlateBlock#getPressedTime}. */
  public static final int WEIGHTED_PLATE_PRESSED_TICKS = 10;

  /** {@code Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE}: {@code new WeightedPressurePlateBlock(15)}. */
  public static final int LIGHT_PLATE_MAX_ENTITIES = 15;

  /** {@code Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE}: {@code new WeightedPressurePlateBlock(150)}. */
  public static final int HEAVY_PLATE_MAX_ENTITIES = 150;

  /** {@code RedstoneLampBlock#neighborChanged}: a lamp goes off 4 game ticks after its power. */
  public static final int LAMP_OFF_DELAY_TICKS = 4;

  /** {@code DispenserBlock#neighborChanged}: droppers and dispensers fire 4 game ticks later. */
  public static final int DROPPER_DELAY_TICKS = 4;

  /** {@code PistonMovingBlockEntity#tick} moves a block half a block per game tick. */
  public static final int PISTON_MOVE_TICKS = 2;

  /** {@code HopperBlockEntity.MOVE_ITEM_SPEED}: one item every 8 game ticks. */
  public static final int HOPPER_TICKS_PER_ITEM = 8;

  /**
   * Two observers facing each other: each turns on every this many game ticks, measured in a world
   * by {@code RedstoneBuildsGameTest} (on {@link #OBSERVER_PULSE_TICKS}, off the rest).
   */
  public static final int OBSERVER_CLOCK_PERIOD_TICKS = 6;

  /** Repeaters in the repeater clock's loop, each set to the longest delay. */
  public static final int REPEATER_CLOCK_REPEATERS = 4;

  /** A hopper clock's cycle grows this much per item: two hopper moves, there and back. */
  public static final int HOPPER_CLOCK_TICKS_PER_ITEM = HOPPER_TICKS_PER_ITEM * 2;

  /**
   * A hopper clock's cycle is this much shorter than its items' moves: each side starts sending
   * before the other side's last item has settled. Measured by {@code RedstoneBuildsGameTest}.
   */
  public static final int HOPPER_CLOCK_OVERLAP_TICKS = 12;

  /** Items in the hopper clock the guide gives as an example. */
  public static final int HOPPER_CLOCK_EXAMPLE_ITEMS = 10;

  /**
   * The pulse extender keeps its lamp on this long after its button lets go: measured by {@code
   * RedstoneBuildsGameTest} (the signal fades around the comparator loop, plus the lamp's delay).
   */
  public static final int PULSE_EXTENDER_HOLD_TICKS = 52;

  /** Game ticks in a tenth of a second: a tick count divisible by it prints one decimal. */
  private static final int TICKS_PER_TENTH = 2;

  private RedstoneRules() {}

  /**
   * Seconds in {@code gameTicks}, printed with as few decimals as it needs: {@code 1}, {@code 0.4},
   * {@code 0.05}.
   */
  public static String seconds(int gameTicks) {
    double seconds = (double) gameTicks / TICKS_PER_SECOND;
    if (gameTicks % TICKS_PER_SECOND == 0) {
      return Integer.toString(gameTicks / TICKS_PER_SECOND);
    }
    return String.format(Locale.ROOT, gameTicks % TICKS_PER_TENTH == 0 ? "%.1f" : "%.2f", seconds);
  }

  /** Game ticks in a repeater's delay of {@code redstoneTicks}. */
  public static int repeaterTicks(int redstoneTicks) {
    if (redstoneTicks < REPEATER_MIN_DELAY || redstoneTicks > REPEATER_MAX_DELAY) {
      throw new IllegalArgumentException("repeater delay " + redstoneTicks);
    }
    return redstoneTicks * GAME_TICKS_PER_REDSTONE_TICK;
  }

  /** The repeater clock's period: its pulse goes once round the loop. */
  public static int repeaterClockPeriodTicks() {
    return REPEATER_CLOCK_REPEATERS * repeaterTicks(REPEATER_MAX_DELAY);
  }

  /** How long the repeater clock stays off each period, after its button's pulse. */
  public static int repeaterClockOffTicks() {
    return repeaterClockPeriodTicks() - STONE_BUTTON_TICKS;
  }

  /** A hopper clock's full cycle (its block there and back) with {@code items} in its hoppers. */
  public static int hopperClockCycleTicks(int items) {
    if (items < 1) {
      throw new IllegalArgumentException("items " + items);
    }
    return items * HOPPER_CLOCK_TICKS_PER_ITEM - HOPPER_CLOCK_OVERLAP_TICKS;
  }

  /**
   * A weighted pressure plate's signal with {@code entities} on it, as {@code
   * WeightedPressurePlateBlock#getSignalStrength} computes it (in floats): the share of {@code
   * maxEntities} on the plate, times 15, rounded up.
   */
  public static int weightedPlateSignal(int entities, int maxEntities) {
    if (entities < 0 || maxEntities <= 0) {
      throw new IllegalArgumentException(entities + " entities of " + maxEntities);
    }
    int count = Math.min(entities, maxEntities);
    if (count == 0) {
      return 0;
    }
    float percent = (float) count / maxEntities;
    return (int) Math.ceil(percent * (float) MAX_SIGNAL);
  }

  /** Fewest entities that give a weighted plate's {@code signal}. */
  public static int entitiesForPlateSignal(int signal, int maxEntities) {
    for (int entities = 1; entities <= maxEntities; entities++) {
      if (weightedPlateSignal(entities, maxEntities) >= signal) {
        return entities;
      }
    }
    throw new IllegalArgumentException("signal " + signal + " out of reach");
  }
}
