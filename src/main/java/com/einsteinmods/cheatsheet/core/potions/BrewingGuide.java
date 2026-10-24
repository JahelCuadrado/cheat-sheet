package com.einsteinmods.cheatsheet.core.potions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The Potions tab's tiles, from {@link BrewingTable}: every potion brewed from the awkward potion,
 * each followed by its fermented spider eye opposite the first time one appears, then any opposite
 * brewed straight from water (weakness). Upgraded versions (redstone, glowstone) are not tiles of
 * their own: they are the {@link PotionRecipe#longer()} and {@link PotionRecipe#stronger()} marks.
 */
public final class BrewingGuide {
  private BrewingGuide() {}

  /** One recipe per potion, in tab order. */
  public static List<PotionRecipe> recipes() {
    Map<String, List<String>> oppositeBases = oppositeBases();
    List<PotionRecipe> recipes = new ArrayList<>();
    List<String> listed = new ArrayList<>();
    for (BrewingMix mix : BrewingTable.mixesFrom(BrewingTable.AWKWARD)) {
      add(recipes, listed, mix.output(), List.of(BrewingTable.AWKWARD), mix.reagent());
      String opposite =
          BrewingTable.result(mix.output(), BrewingTable.FERMENTED_SPIDER_EYE).orElse(null);
      if (opposite != null && oppositeBases.containsKey(opposite)) {
        add(
            recipes,
            listed,
            opposite,
            oppositeBases.get(opposite),
            BrewingTable.FERMENTED_SPIDER_EYE);
      }
    }
    for (Map.Entry<String, List<String>> entry : oppositeBases.entrySet()) {
      add(recipes, listed, entry.getKey(), entry.getValue(), BrewingTable.FERMENTED_SPIDER_EYE);
    }
    return List.copyOf(recipes);
  }

  /**
   * The awkward potion's own recipe (water + nether wart), the start of almost every potion; its
   * marks are empty.
   */
  public static PotionRecipe start() {
    return new PotionRecipe(
        BrewingTable.AWKWARD,
        List.of(BrewingTable.WATER),
        BrewingTable.NETHER_WART,
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
  }

  /** Fermented spider eye results and their bases: base potions and water, table order. */
  private static Map<String, List<String>> oppositeBases() {
    Map<String, List<String>> bases = new LinkedHashMap<>();
    for (Corruption corruption : BrewingTable.corruptions()) {
      bases.put(corruption.output(), corruption.inputs());
    }
    for (BrewingMix mix : BrewingTable.mixesFrom(BrewingTable.WATER)) {
      if (mix.reagent().equals(BrewingTable.FERMENTED_SPIDER_EYE)) {
        bases.computeIfAbsent(mix.output(), output -> List.of(BrewingTable.WATER));
      }
    }
    return bases;
  }

  private static void add(
      List<PotionRecipe> recipes,
      List<String> listed,
      String potion,
      List<String> bases,
      String reagent) {
    if (listed.contains(potion)) {
      return;
    }
    listed.add(potion);
    recipes.add(
        new PotionRecipe(
            potion,
            bases,
            reagent,
            BrewingTable.result(potion, BrewingTable.REDSTONE),
            BrewingTable.result(potion, BrewingTable.GLOWSTONE),
            BrewingTable.result(potion, BrewingTable.FERMENTED_SPIDER_EYE)));
  }
}
