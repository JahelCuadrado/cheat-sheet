package com.einsteinmods.cheatsheet.core.redstone;

/**
 * The Nether's bedrock ceiling in Minecraft 26.3: {@code worldgen/material_rule/bedrock_roof.json},
 * {@code not(vertical_gradient(below_top 5, top))} over the Nether's generation height (128), so
 * the top layer {@link #ALWAYS_Y} is always bedrock and each layer below it one fifth less likely,
 * one block at a time ({@code VerticalGradientCondition}). Pure arithmetic.
 */
public final class NetherRoof {
  /** {@code top}: the ceiling's last layer, always bedrock. */
  public static final int ALWAYS_Y = 127;

  /** {@code below_top 5}: from this layer down, never bedrock. */
  public static final int NEVER_Y = 122;

  /** The layer you walk on above the ceiling: your feet stand here. */
  public static final int WALK_Y = ALWAYS_Y + 1;

  /** On the ladder your head is just under the bedrock: feet this many blocks below it. */
  public static final int FEET_BELOW_BEDROCK = 2;

  private static final int PERCENT = 100;

  private NetherRoof() {}

  /** Chance in percent that the block at {@code y} of the ceiling is bedrock. */
  public static int bedrockPercent(int y) {
    if (y >= ALWAYS_Y) {
      return PERCENT;
    }
    if (y <= NEVER_Y) {
      return 0;
    }
    return (y - NEVER_Y) * PERCENT / (ALWAYS_Y - NEVER_Y);
  }

  /** Where your feet are on the ladder when the lowest bedrock above you is at {@code y}. */
  public static int ladderFeetY(int bedrockY) {
    return bedrockY - FEET_BELOW_BEDROCK;
  }
}
