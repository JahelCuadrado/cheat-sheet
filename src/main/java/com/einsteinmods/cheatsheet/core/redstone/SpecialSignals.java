package com.einsteinmods.cheatsheet.core.redstone;

import java.util.ArrayList;
import java.util.List;

/**
 * Blocks a comparator reads by something other than fullness, with their signals as Minecraft 26.3
 * computes them ({@code getAnalogOutputSignal} of each block, cited per method). {@code
 * SpecialSignalsGameTest} places every state of {@link #checks()} and reads the game's signal.
 */
public final class SpecialSignals {
  /** {@code ComposterBlock}: the signal is its level, 0 to 8 (8: bone meal ready). */
  public static final int COMPOSTER_MAX = 8;

  /** {@code CakeBlock.MAX_BITES} + 1: slices of a whole cake. */
  public static final int CAKE_SLICES = 7;

  /** {@code LayeredCauldronBlock}: water and powder snow levels 1 to 3. */
  public static final int CAULDRON_MAX = 3;

  /** {@code LavaCauldronBlock#getAnalogOutputSignal}: always 3. */
  public static final int LAVA_CAULDRON = 3;

  /** {@code BeehiveBlock.MAX_HONEY_LEVELS}: honey levels 0 to 5. */
  public static final int BEEHIVE_MAX = 5;

  /** {@code ItemFrame.NUM_ROTATIONS}: an item frame gives 1 to 8. */
  public static final int ITEM_FRAME_ROTATIONS = 8;

  /** {@code BlockStateProperties.MAX_RESPAWN_ANCHOR_CHARGES}. */
  public static final int RESPAWN_ANCHOR_CHARGES = 4;

  /** {@code EndPortalFrameBlock#getAnalogOutputSignal}: 15 with an eye. */
  public static final int END_PORTAL_FRAME_EYE = RedstoneRules.MAX_SIGNAL;

  /** Music discs give 1 to 15 ({@code JukeboxSong.comparatorOutput}, one per disc). */
  public static final int JUKEBOX_MIN = 1;

  /** {@code ShelfBlock#getAnalogOutputSignal}: the left slot seen from the front is worth 1. */
  public static final int SHELF_LEFT = 1;

  /** The middle slot of a shelf is worth 2. */
  public static final int SHELF_MIDDLE = 2;

  /** The right slot of a shelf is worth 4. */
  public static final int SHELF_RIGHT = 4;

  /** {@code ChiseledBookShelfBlockEntity.MAX_BOOKS_IN_STORAGE}. */
  public static final int CHISELED_BOOKSHELF_SLOTS = 6;

  /** {@code CopperBulbBlock#getAnalogOutputSignal}: 15 lit, 0 off. */
  public static final int COPPER_BULB_LIT = RedstoneRules.MAX_SIGNAL;

  /** {@code CopperGolemStatueBlock.Pose}: standing, sitting, running, star. */
  public static final List<String> COPPER_GOLEM_POSES =
      List.of("standing", "sitting", "running", "star");

  /** {@code CreakingHeartBlockEntity#computeAnalogOutputSignal}: 0 from this many blocks on. */
  public static final int CREAKING_HEART_RANGE = 32;

  /** Highest scale of a lectern's book: {@code LecternBlockEntity#getRedstoneSignal}. */
  private static final float LECTERN_SCALE = 14.0F;

  /**
   * A block state and the signal a comparator reads from it.
   *
   * @param state written as in commands, {@code cake[bites=1]}
   */
  public record StateSignal(String state, int signal) {}

  private SpecialSignals() {}

  /** {@code CakeBlock#getOutputSignal}: 2 per slice left. */
  public static int cake(int bitesTaken) {
    if (bitesTaken < 0 || bitesTaken >= CAKE_SLICES) {
      throw new IllegalArgumentException("bites " + bitesTaken);
    }
    return (CAKE_SLICES - bitesTaken) * 2;
  }

  /**
   * {@code LecternBlockEntity#getRedstoneSignal}: 1 on the first page up to 15 on the last; {@code
   * page} counts from 0.
   */
  public static int lectern(int page, int pages) {
    if (pages <= 0 || page < 0 || page >= pages) {
      throw new IllegalArgumentException("page " + page + " of " + pages);
    }
    float progress = pages > 1 ? page / (pages - 1.0F) : 1.0F;
    return (int) Math.floor(progress * LECTERN_SCALE) + 1;
  }

  /** {@code RespawnAnchorBlock#getScaledChargeLevel(state, 15)}. */
  public static int respawnAnchor(int charges) {
    if (charges < 0 || charges > RESPAWN_ANCHOR_CHARGES) {
      throw new IllegalArgumentException("charges " + charges);
    }
    return (int) Math.floor(charges / (float) RESPAWN_ANCHOR_CHARGES * RedstoneRules.MAX_SIGNAL);
  }

  /** The respawn anchor's signal at each charge, 0 to 4. */
  public static List<Integer> respawnAnchorSignals() {
    List<Integer> signals = new ArrayList<>(RESPAWN_ANCHOR_CHARGES + 1);
    for (int charges = 0; charges <= RESPAWN_ANCHOR_CHARGES; charges++) {
      signals.add(respawnAnchor(charges));
    }
    return List.copyOf(signals);
  }

  /** {@code ShelfBlock#getAnalogOutputSignal}, read from behind: one bit per slot holding items. */
  public static int shelf(boolean left, boolean middle, boolean right) {
    return (left ? SHELF_LEFT : 0) | (middle ? SHELF_MIDDLE : 0) | (right ? SHELF_RIGHT : 0);
  }

  /** {@code ChiseledBookShelfBlock#getAnalogOutputSignal}: {@code lastSlot} counts from 0. */
  public static int chiseledBookshelf(int lastSlot) {
    if (lastSlot < 0 || lastSlot >= CHISELED_BOOKSHELF_SLOTS) {
      throw new IllegalArgumentException("slot " + lastSlot);
    }
    return lastSlot + 1;
  }

  /**
   * {@code CreakingHeartBlockEntity#computeAnalogOutputSignal}: with {@code distance} blocks to its
   * creaking, 15 close by down to 0 at {@link #CREAKING_HEART_RANGE}.
   */
  public static int creakingHeart(double distance) {
    if (distance < 0) {
      throw new IllegalArgumentException("distance " + distance);
    }
    double scaled = Math.min(distance, CREAKING_HEART_RANGE) / CREAKING_HEART_RANGE;
    return RedstoneRules.MAX_SIGNAL - (int) Math.floor(scaled * RedstoneRules.MAX_SIGNAL);
  }

  /**
   * Every state the cheat sheet gives a signal for that needs no block entity, with that signal.
   */
  public static List<StateSignal> checks() {
    List<StateSignal> checks = new ArrayList<>();
    for (int level = 0; level <= COMPOSTER_MAX; level++) {
      checks.add(new StateSignal("composter[level=" + level + "]", level));
    }
    for (int bites = 0; bites < CAKE_SLICES; bites++) {
      checks.add(new StateSignal("cake[bites=" + bites + "]", cake(bites)));
    }
    checks.add(new StateSignal("cauldron", 0));
    for (int level = 1; level <= CAULDRON_MAX; level++) {
      checks.add(new StateSignal("water_cauldron[level=" + level + "]", level));
      checks.add(new StateSignal("powder_snow_cauldron[level=" + level + "]", level));
    }
    checks.add(new StateSignal("lava_cauldron", LAVA_CAULDRON));
    for (int charges = 0; charges <= RESPAWN_ANCHOR_CHARGES; charges++) {
      checks.add(
          new StateSignal("respawn_anchor[charges=" + charges + "]", respawnAnchor(charges)));
    }
    for (int honey = 0; honey <= BEEHIVE_MAX; honey++) {
      checks.add(new StateSignal("beehive[honey_level=" + honey + "]", honey));
    }
    checks.add(new StateSignal("end_portal_frame[eye=false]", 0));
    checks.add(new StateSignal("end_portal_frame[eye=true]", END_PORTAL_FRAME_EYE));
    checks.add(new StateSignal("copper_bulb[lit=false]", 0));
    checks.add(new StateSignal("copper_bulb[lit=true]", COPPER_BULB_LIT));
    for (int pose = 0; pose < COPPER_GOLEM_POSES.size(); pose++) {
      checks.add(
          new StateSignal(
              "copper_golem_statue[copper_golem_pose=" + COPPER_GOLEM_POSES.get(pose) + "]",
              pose + 1));
    }
    return List.copyOf(checks);
  }
}
