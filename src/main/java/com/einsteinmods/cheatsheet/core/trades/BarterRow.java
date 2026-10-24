package com.einsteinmods.cheatsheet.core.trades;

import java.util.Optional;

/**
 * One item a piglin may give for a gold ingot.
 *
 * @param itemId item id ({@code minecraft:} namespace implied)
 * @param count how many ({@code set_count})
 * @param weight loot weight; the chance is the weight over the sum of all weights
 * @param potion potion it holds ({@code set_potion})
 * @param enchantment enchantment it carries ({@code enchant_randomly} with one option)
 */
public record BarterRow(
    String itemId,
    CountRange count,
    int weight,
    Optional<String> potion,
    Optional<String> enchantment) {
  public BarterRow {
    if (weight < 1) {
      throw new IllegalArgumentException(itemId + ": weight " + weight);
    }
  }
}
