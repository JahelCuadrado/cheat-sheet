package com.einsteinmods.cheatsheet.core.ores;

import java.util.Locale;

/**
 * An ore shown as one column of the height charts, in chart order.
 *
 * @param dimension chart it belongs to
 * @param stoneItem block item id of the stone (or Nether) variant
 * @param deepslateItem block item id of the deepslate variant (same as the stone one in the Nether)
 */
public enum Ore {
  COAL(OreDimension.OVERWORLD, "minecraft:coal_ore", "minecraft:deepslate_coal_ore"),
  COPPER(OreDimension.OVERWORLD, "minecraft:copper_ore", "minecraft:deepslate_copper_ore"),
  IRON(OreDimension.OVERWORLD, "minecraft:iron_ore", "minecraft:deepslate_iron_ore"),
  LAPIS(OreDimension.OVERWORLD, "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore"),
  GOLD(OreDimension.OVERWORLD, "minecraft:gold_ore", "minecraft:deepslate_gold_ore"),
  REDSTONE(OreDimension.OVERWORLD, "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore"),
  DIAMOND(OreDimension.OVERWORLD, "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore"),
  EMERALD(OreDimension.OVERWORLD, "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore"),
  NETHER_GOLD(OreDimension.NETHER, "minecraft:nether_gold_ore", "minecraft:nether_gold_ore"),
  NETHER_QUARTZ(OreDimension.NETHER, "minecraft:nether_quartz_ore", "minecraft:nether_quartz_ore"),
  ANCIENT_DEBRIS(OreDimension.NETHER, "minecraft:ancient_debris", "minecraft:ancient_debris");

  /** Deepslate replaces stone at and below this height (it fades out up to Y 8). */
  public static final int DEEPSLATE_TOP_Y = 0;

  private final OreDimension dimension;
  private final String stoneItem;
  private final String deepslateItem;

  Ore(OreDimension dimension, String stoneItem, String deepslateItem) {
    this.dimension = dimension;
    this.stoneItem = stoneItem;
    this.deepslateItem = deepslateItem;
  }

  public OreDimension dimension() {
    return dimension;
  }

  /** Lower-case name, used in lang keys. */
  public String id() {
    return name().toLowerCase(Locale.ROOT);
  }

  /** The block item a player finds at {@code y}: the deepslate variant deep down. */
  public String itemAt(int y) {
    return y <= DEEPSLATE_TOP_Y ? deepslateItem : stoneItem;
  }
}
