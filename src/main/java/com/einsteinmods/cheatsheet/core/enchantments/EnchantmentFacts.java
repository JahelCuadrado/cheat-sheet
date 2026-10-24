package com.einsteinmods.cheatsheet.core.enchantments;

import java.util.Set;

/**
 * What the Enchantments tab needs to place one enchantment, read from the synced registry.
 *
 * @param id enchantment id ({@code namespace:path})
 * @param supported gear it can go on
 * @param curse in the {@code curse} tag
 * @param treasure not in the {@code in_enchanting_table} tag: only from loot, trades or fishing
 * @param order position in the {@code tooltip_order} tag; {@link Integer#MAX_VALUE} when absent
 */
public record EnchantmentFacts(
    String id, Set<Gear> supported, boolean curse, boolean treasure, int order) {
  public EnchantmentFacts {
    supported = Set.copyOf(supported);
  }
}
