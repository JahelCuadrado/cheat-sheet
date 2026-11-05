package com.einsteinmods.cheatsheet.core.redstone;

import java.util.List;
import java.util.Locale;

/**
 * What a piston, and the slime and honey blocks it moves, can take along (Minecraft 26.3), sorted
 * the way the game sorts blocks.
 *
 * <p>{@code PistonBaseBlock#isPushable}: none moves with a destroy time of -1 (bedrock), a block
 * entity (chests, furnaces, hoppers) or the {@code IMMOVEABLE} push reaction (obsidian); {@code
 * POPPED} ones only break, when a moving block runs into them; {@code PUSH} ones move only when
 * pushed head-on. A sticky piston pulls back only a {@code PUSH_PULL} block ({@code
 * PistonBaseBlock#triggerEvent}). {@code PistonStructureResolver#addBranchingBlocks} drags every
 * {@code PUSH_PULL} neighbour of a moving slime or honey block, except honey for slime and slime
 * for honey ({@code canStickToEachOther}); any other neighbour stays where it is, whole, and stops
 * nothing. A piston moves at most {@link #MAX_PUSHED} blocks. {@code RedstoneMechanicsGameTest}
 * checks every listed block and each step of the slime guide in a world.
 */
public final class PistonRules {
  /** {@code PistonStructureResolver.MAX_PUSH_DEPTH}: blocks one piston moves at most. */
  public static final int MAX_PUSHED = 12;

  private PistonRules() {}

  /** What happens to a block in front of something a piston moves. */
  public enum Push {
    /** It moves along. */
    MOVES,
    /** It breaks and drops. */
    BREAKS,
    /** Nothing moves: the piston does not fire. */
    STOPS
  }

  /** Whether a moving slime or honey block takes a block it touches along. */
  public enum Drag {
    YES,
    NO,
    /** Slime takes slime and honey takes honey, but neither takes the other. */
    SAME_KIND_ONLY
  }

  /** A kind of block for pistons, with the blocks players meet most. Block ids. */
  public enum Group {
    NORMAL(
        Push.MOVES,
        true,
        Drag.YES,
        List.of(
            "stone",
            "dirt",
            "sand",
            "oak_planks",
            "oak_log",
            "glass",
            "white_wool",
            "iron_block",
            "redstone_block",
            "observer",
            "piston")),
    STICKY(Push.MOVES, true, Drag.SAME_KIND_ONLY, List.of("slime_block", "honey_block")),
    PUSH_ONLY(Push.MOVES, false, Drag.NO, List.of("white_glazed_terracotta")),
    BREAK(
        Push.BREAKS,
        false,
        Drag.NO,
        List.of(
            "pumpkin",
            "carved_pumpkin",
            "jack_o_lantern",
            "melon",
            "oak_leaves",
            "cactus",
            "sugar_cane",
            "bamboo",
            "cobweb",
            "moss_block",
            "torch",
            "lantern",
            "redstone_wire",
            "repeater",
            "comparator",
            "lever",
            "stone_button",
            "oak_pressure_plate",
            "oak_door",
            "red_bed",
            "ladder",
            "scaffolding",
            "poppy",
            "cake",
            "decorated_pot",
            "shulker_box",
            "dragon_egg")),
    IMMOVABLE(
        Push.STOPS,
        false,
        Drag.NO,
        List.of(
            "obsidian",
            "crying_obsidian",
            "bedrock",
            "respawn_anchor",
            "reinforced_deepslate",
            "end_portal_frame",
            "anvil",
            "grindstone",
            "lodestone",
            "chest",
            "barrel",
            "furnace",
            "hopper",
            "dispenser",
            "dropper",
            "crafter",
            "beacon",
            "enchanting_table",
            "spawner"));

    private final Push pushed;
    private final boolean pulled;
    private final Drag dragged;
    private final List<String> blockIds;

    Group(Push pushed, boolean pulled, Drag dragged, List<String> blockIds) {
      this.pushed = pushed;
      this.pulled = pulled;
      this.dragged = dragged;
      this.blockIds = List.copyOf(blockIds);
    }

    /** In front of a moving block. */
    public Push pushed() {
      return pushed;
    }

    /** Pulled back by the sticky piston it touches. */
    public boolean pulled() {
      return pulled;
    }

    /** Beside a moving slime or honey block. */
    public Drag dragged() {
      return dragged;
    }

    public List<String> blockIds() {
      return blockIds;
    }

    /** Lower-case id for lang keys. */
    public String key() {
      return name().toLowerCase(Locale.ROOT);
    }
  }
}
