package com.einsteinmods.cheatsheet.core.redstone;

import com.einsteinmods.cheatsheet.core.pictures.Diorama;
import java.util.List;
import java.util.Locale;

/**
 * Blocks players build redstone with, sorted as the game sorts them (Minecraft 26.3): whether the
 * block carries power ({@code BlockBehaviour.Properties#isRedstoneConductor}: by default a block
 * whose collision shape is a full cube, unless {@code Blocks} says {@code never} or {@code always})
 * and whether dust stays on top of it ({@code RedstoneWireBlock#canSurviveOn}: a sturdy top face,
 * or a hopper). {@code RedstoneRulesGameTest} checks both against the game for every block.
 *
 * <p>Dust on a block that carries no power sends its signal up a step, never down: {@code
 * RedstoneWireEvaluator#getIncomingWireSignal} reads the dust a step below only through a block
 * that carries no power, and the dust a step above only through one that does.
 *
 * @param state the block as written in commands ({@code smooth_stone_slab[type=top]})
 * @param itemId the item whose name and icon stand for it
 * @param conducts whether it carries power (a conductor)
 * @param holdsDust whether dust can be placed on it
 */
public record TransparentBlock(String state, String itemId, boolean conducts, boolean holdsDust) {
  /**
   * The blocks the zoom shows, in its order: transparent ones that hold dust, those that do not,
   * then solid ones.
   */
  public static final List<TransparentBlock> ALL =
      List.of(
          transparent("glass", "glass", true),
          transparent("glowstone", "glowstone", true),
          transparent("sea_lantern", "sea_lantern", true),
          transparent("ice", "ice", true),
          transparent("tnt", "tnt", true),
          transparent("redstone_block", "redstone_block", true),
          transparent("observer", "observer", true),
          transparent("piston", "piston", true),
          transparent("hopper", "hopper", true),
          transparent("smooth_stone_slab[type=top]", "smooth_stone_slab", true),
          transparent("stone_brick_stairs[half=top,facing=north]", "stone_brick_stairs", true),
          transparent("oak_trapdoor[half=top]", "oak_trapdoor", true),
          transparent("oak_leaves[persistent=true]", "oak_leaves", false),
          transparent("honey_block", "honey_block", false),
          transparent("smooth_stone_slab[type=bottom]", "smooth_stone_slab", false),
          transparent("stone_brick_stairs[half=bottom,facing=north]", "stone_brick_stairs", false),
          solid("stone"),
          solid("oak_planks"),
          solid("white_wool"),
          solid("slime_block"),
          solid("soul_sand"),
          solid("mud"));

  private static TransparentBlock transparent(String state, String itemId, boolean holdsDust) {
    return new TransparentBlock(state, itemId, false, holdsDust);
  }

  private static TransparentBlock solid(String id) {
    return new TransparentBlock(id, id, true, true);
  }

  /** The block id without its properties. */
  public String blockId() {
    int bracket = state.indexOf('[');
    return bracket < 0 ? state : state.substring(0, bracket);
  }

  /**
   * Which half a slab, stairs or trapdoor takes, read from its state: the top half, the bottom
   * half, or none (a whole block).
   */
  public Half half() {
    if (state.contains("type=top") || state.contains("half=top")) {
      return Half.TOP;
    }
    if (state.contains("type=bottom") || state.contains("half=bottom")) {
      return Half.BOTTOM;
    }
    return Half.WHOLE;
  }

  /** Which half of its cell a block fills. */
  public enum Half {
    WHOLE,
    TOP,
    BOTTOM;

    /** Lower-case id for lang keys. */
    public String key() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  /** The block alone, with unpowered dust on top when dust can stand there. */
  public Diorama picture() {
    Diorama.Builder builder = Diorama.builder(1, 2, 1).block(state, 0, 0, 0);
    if (holdsDust) {
      builder.block(RedstoneDioramas.dust(0), 0, 1, 0);
    }
    return builder.build();
  }
}
