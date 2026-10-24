package com.einsteinmods.cheatsheet.core.enchantments;

/**
 * A kind of gear the Enchantments tab sorts enchantments by, with the item tested against each
 * enchantment's supported items. Shovels and hoes take the same enchantments as pickaxes in
 * vanilla, so one tool stands for the three.
 */
public enum Gear {
  HELMET("diamond_helmet"),
  CHESTPLATE("diamond_chestplate"),
  LEGGINGS("diamond_leggings"),
  BOOTS("diamond_boots"),
  SWORD("diamond_sword"),
  SPEAR("diamond_spear"),
  AXE("diamond_axe"),
  TOOL("diamond_pickaxe"),
  BOW("bow"),
  CROSSBOW("crossbow"),
  TRIDENT("trident"),
  MACE("mace"),
  FISHING_ROD("fishing_rod");

  private final String itemId;

  Gear(String itemId) {
    this.itemId = itemId;
  }

  /** Item tested and shown for this gear ({@code minecraft:} namespace implied). */
  public String itemId() {
    return itemId;
  }
}
