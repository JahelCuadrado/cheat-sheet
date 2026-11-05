package com.einsteinmods.cheatsheet.core.redstone;

import java.util.List;

/**
 * Whether a pickaxe breaks a piston in one tick, standing on the ground (Minecraft 26.3: {@code
 * BlockBehaviour#getDestroyProgress} is the tool's speed, plus level² + 1 for Efficiency ({@code
 * enchantment/efficiency.json}), times 1 + 0.2 per Haste level ({@code Player#getDestroySpeed}),
 * over the piston's hardness 1.5 and 30; one tick at 1 or more). {@code BedrockBreakingGameTest}
 * checks every row against the game.
 *
 * @param pickaxe the pickaxe's item id
 * @param speed the pickaxe's mining speed ({@code ToolMaterial})
 * @param efficiency its Efficiency level
 * @param haste the Haste level, 0 for none
 */
public record InstantMining(String pickaxe, float speed, int efficiency, int haste) {
  private static final float PISTON_HARDNESS = 1.5F;
  private static final float TICKS_PER_HARDNESS = 30.0F;
  private static final float HASTE_PER_LEVEL = 0.2F;

  /** Efficiency V, the level every row uses. */
  public static final int EFFICIENCY_V = 5;

  public static final List<InstantMining> ROWS =
      List.of(
          new InstantMining("netherite_pickaxe", 9.0F, EFFICIENCY_V, 2),
          new InstantMining("diamond_pickaxe", 8.0F, EFFICIENCY_V, 2),
          new InstantMining("golden_pickaxe", 12.0F, EFFICIENCY_V, 1),
          new InstantMining("netherite_pickaxe", 9.0F, EFFICIENCY_V, 1),
          new InstantMining("netherite_pickaxe", 9.0F, EFFICIENCY_V, 0));

  /** Share of the piston broken per tick. */
  public float progress() {
    float withEfficiency = speed + efficiency * efficiency + 1;
    return withEfficiency * (1 + HASTE_PER_LEVEL * haste) / PISTON_HARDNESS / TICKS_PER_HARDNESS;
  }

  public boolean instant() {
    return progress() >= 1.0F;
  }
}
