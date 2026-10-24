package com.einsteinmods.cheatsheet.core.trades;

import java.util.List;
import java.util.Optional;

/**
 * Piglin bartering of Minecraft 26.3 ({@code data/minecraft/loot_table/gameplay/piglin_bartering
 * .json}: one roll, one entry by weight). Loot tables are server data, so the client reads this
 * copy; {@code VillagerTradeGameTest} compares it with the game's loot table.
 */
public final class BarterTable {
  private static final String SOUL_SPEED = "soul_speed";
  private static final String FIRE_RESISTANCE = "fire_resistance";

  /** Every entry, in the loot table's order (rarest first). */
  public static final List<BarterRow> ROWS =
      List.of(
          enchanted("book", 5, SOUL_SPEED),
          enchanted("iron_boots", 8, SOUL_SPEED),
          potion("potion", 8, FIRE_RESISTANCE),
          potion("splash_potion", 8, FIRE_RESISTANCE),
          potion("potion", 10, "water"),
          row("iron_nugget", 10, 36, 10),
          row("ender_pearl", 2, 4, 10),
          row("dried_ghast", 1, 1, 10),
          row("string", 3, 9, 20),
          row("quartz", 5, 12, 20),
          row("obsidian", 1, 1, 40),
          row("crying_obsidian", 1, 3, 40),
          row("fire_charge", 1, 1, 40),
          row("leather", 2, 4, 40),
          row("soul_sand", 2, 8, 40),
          row("nether_brick", 2, 8, 40),
          row("spectral_arrow", 6, 12, 40),
          row("gravel", 8, 16, 40),
          row("blackstone", 8, 16, 40));

  private BarterTable() {}

  public static int totalWeight() {
    int total = 0;
    for (BarterRow row : ROWS) {
      total += row.weight();
    }
    return total;
  }

  /** Chance of {@code row} per gold ingot, from 0 to 1. */
  public static double chance(BarterRow row) {
    return (double) row.weight() / totalWeight();
  }

  private static BarterRow row(String itemId, int min, int max, int weight) {
    return new BarterRow(
        itemId, new CountRange(min, max), weight, Optional.empty(), Optional.empty());
  }

  private static BarterRow potion(String itemId, int weight, String potion) {
    return new BarterRow(
        itemId, CountRange.exactly(1), weight, Optional.of(potion), Optional.empty());
  }

  private static BarterRow enchanted(String itemId, int weight, String enchantment) {
    return new BarterRow(
        itemId, CountRange.exactly(1), weight, Optional.empty(), Optional.of(enchantment));
  }
}
