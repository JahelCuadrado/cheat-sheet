package com.einsteinmods.cheatsheet.core.trades;

/**
 * What a trade does to the item it gives ({@code given_item_modifier} in the trade's JSON), which
 * the sheet explains in the trade's tooltip.
 */
public enum TradeKind {
  /** The item as listed. */
  PLAIN,
  /** Random enchantments ({@code enchant_with_levels}); the levels are added to the price. */
  ENCHANTED_ITEM,
  /** A random enchanted book ({@code enchant_randomly}); its price depends on the enchantment. */
  ENCHANTED_BOOK,
  /** Leather dyed with random colours ({@code set_random_dyes}). */
  DYED_ARMOR,
  /** Tipped arrows of a random potion ({@code set_random_potion}). */
  RANDOM_POTION,
  /** A suspicious stew with a random effect ({@code set_stew_effect}). */
  SUSPICIOUS_STEW,
  /** A map to the nearest structure of its kind ({@code exploration_map}). */
  EXPLORER_MAP,
  /** A fixed potion ({@code set_potion}); the potion is on the given item. */
  POTION
}
