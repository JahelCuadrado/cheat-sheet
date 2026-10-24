package com.einsteinmods.cheatsheet.core.potions;

import java.util.List;

/**
 * Potions a fermented spider eye turns into the same opposite one.
 *
 * @param inputs base potions (no redstone or glowstone upgrade), in table order
 * @param output the potion they become
 */
public record Corruption(List<String> inputs, String output) {
  public Corruption {
    inputs = List.copyOf(inputs);
  }
}
