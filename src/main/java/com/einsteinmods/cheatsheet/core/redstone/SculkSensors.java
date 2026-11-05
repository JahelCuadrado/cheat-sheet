package com.einsteinmods.cheatsheet.core.redstone;

import java.util.List;

/**
 * Sculk sensors as Minecraft 26.3 makes them (each constant names its source): how far they hear,
 * how strong and how long their signal is, and the frequency a comparator reads for each vibration
 * ({@code VibrationSystem.VIBRATION_FREQUENCY_FOR_EVENT}). {@code SculkSensorGameTest} compares
 * every number and the whole table with the game.
 */
public final class SculkSensors {
  /** {@code SculkSensorBlockEntity.VibrationUser#getListenerRadius}: blocks it hears. */
  public static final int RANGE = 8;

  /** {@code CalibratedSculkSensorBlockEntity.VibrationUser#getListenerRadius}. */
  public static final int CALIBRATED_RANGE = 16;

  /** {@code SculkSensorBlock#getActiveTicks} ({@code SculkSensorBlock.ACTIVE_TICKS}). */
  public static final int ACTIVE_TICKS = 30;

  /** {@code CalibratedSculkSensorBlock#getActiveTicks}. */
  public static final int CALIBRATED_ACTIVE_TICKS = 10;

  /** {@code SculkSensorBlock#deactivate}: it rests this long before it hears again. */
  public static final int COOLDOWN_TICKS = 10;

  /**
   * A vibration takes a game tick per block to arrive: {@code
   * VibrationSystem.User#calculateTravelTimeInTicks} is the distance rounded down.
   */
  public static final int TRAVEL_TICKS_PER_BLOCK = 1;

  /**
   * One vibration frequency and the game events that give it (ids without namespace).
   *
   * @param frequency what a comparator reads from an active sensor, 1 to 15
   */
  public record Frequency(int frequency, List<String> gameEvents) {
    public Frequency {
      gameEvents = List.copyOf(gameEvents);
    }
  }

  /**
   * Every frequency in order, as {@code VibrationSystem.VIBRATION_FREQUENCY_FOR_EVENT} maps game
   * events to them (the amethyst {@code resonate_N} events left out: each gives its own N).
   */
  public static final List<Frequency> FREQUENCIES =
      List.of(
          new Frequency(1, List.of("step", "swim", "flap")),
          new Frequency(2, List.of("projectile_land", "hit_ground", "splash", "bounce")),
          new Frequency(3, List.of("item_interact_finish", "projectile_shoot", "instrument_play")),
          new Frequency(4, List.of("entity_action", "elytra_glide", "unequip")),
          new Frequency(5, List.of("entity_dismount", "equip")),
          new Frequency(6, List.of("entity_interact", "shear", "entity_mount")),
          new Frequency(7, List.of("entity_damage")),
          new Frequency(8, List.of("drink", "eat")),
          new Frequency(
              9, List.of("container_close", "block_close", "block_deactivate", "block_detach")),
          new Frequency(
              10,
              List.of(
                  "container_open",
                  "block_open",
                  "block_activate",
                  "block_attach",
                  "prime_fuse",
                  "note_block_play")),
          new Frequency(11, List.of("block_change")),
          new Frequency(12, List.of("block_destroy", "fluid_pickup")),
          new Frequency(13, List.of("block_place", "fluid_place")),
          new Frequency(14, List.of("entity_place", "lightning_strike", "teleport")),
          new Frequency(15, List.of("entity_die", "explode")));

  private SculkSensors() {}

  /**
   * The signal a sensor of {@code range} gives for a vibration {@code distance} blocks away, as
   * {@code VibrationSystem#getRedstoneStrengthForDistance} computes it: 15 next to it, a step less
   * every {@code range / 15} blocks, never under 1.
   */
  public static int signalAtDistance(float distance, int range) {
    if (distance < 0 || range <= 0) {
      throw new IllegalArgumentException("distance " + distance + " of " + range);
    }
    double perBlock = (double) RedstoneRules.MAX_SIGNAL / range;
    return Math.max(1, RedstoneRules.MAX_SIGNAL - (int) Math.floor(perBlock * distance));
  }
}
