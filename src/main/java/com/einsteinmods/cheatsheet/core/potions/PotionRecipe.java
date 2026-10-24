package com.einsteinmods.cheatsheet.core.potions;

import java.util.List;
import java.util.Optional;

/**
 * How one drinkable potion is brewed and what it turns into: its tile on the Potions tab.
 *
 * @param potion potion id ({@code minecraft:} namespace implied)
 * @param bases potions it is brewed from with {@code reagent}, in table order (never empty)
 * @param reagent item id that brews it from any of {@code bases}
 * @param longer what redstone turns it into, if anything
 * @param stronger what glowstone dust turns it into, if anything
 * @param opposite what a fermented spider eye turns it into, if anything
 */
public record PotionRecipe(
    String potion,
    List<String> bases,
    String reagent,
    Optional<String> longer,
    Optional<String> stronger,
    Optional<String> opposite) {
  public PotionRecipe {
    bases = List.copyOf(bases);
    if (bases.isEmpty()) {
      throw new IllegalArgumentException("a recipe needs a base potion: " + potion);
    }
  }
}
