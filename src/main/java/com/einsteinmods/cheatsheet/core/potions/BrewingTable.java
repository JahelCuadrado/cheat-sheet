package com.einsteinmods.cheatsheet.core.potions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Vanilla brewing between drinkable potions, as a fixed table: brewing recipes ({@code
 * BrewingRecipe}, {@code data/minecraft/recipe/brewing}) are server data, never sent to the client.
 * {@code BrewingRecipeGameTest} checks the table against the game's recipes, including the splash
 * and lingering copies and the gunpowder and dragon's breath conversions every potion has.
 */
public final class BrewingTable {
  public static final String WATER = "water";
  public static final String AWKWARD = "awkward";

  /** Containers: drinkable, splash (gunpowder) and lingering (dragon's breath). */
  public static final String POTION_ITEM = "potion";

  public static final String SPLASH_POTION_ITEM = "splash_potion";
  public static final String LINGERING_POTION_ITEM = "lingering_potion";

  public static final String NETHER_WART = "nether_wart";
  public static final String REDSTONE = "redstone";
  public static final String GLOWSTONE = "glowstone_dust";
  public static final String FERMENTED_SPIDER_EYE = "fermented_spider_eye";
  public static final String GUNPOWDER = "gunpowder";
  public static final String DRAGON_BREATH = "dragon_breath";
  public static final String BLAZE_POWDER = "blaze_powder";

  /** Every vanilla mix between drinkable potions, grouped by input. */
  public static final List<BrewingMix> MIXES =
      List.of(
          mix(WATER, NETHER_WART, AWKWARD),
          mix(WATER, GLOWSTONE, "thick"),
          mix(WATER, FERMENTED_SPIDER_EYE, "weakness"),
          mix(WATER, REDSTONE, "mundane"),
          mix(WATER, "sugar", "mundane"),
          mix(WATER, "rabbit_foot", "mundane"),
          mix(WATER, "glistering_melon_slice", "mundane"),
          mix(WATER, "spider_eye", "mundane"),
          mix(WATER, "magma_cream", "mundane"),
          mix(WATER, BLAZE_POWDER, "mundane"),
          mix(WATER, "ghast_tear", "mundane"),
          mix(WATER, "breeze_rod", "mundane"),
          mix(WATER, "cobweb", "mundane"),
          mix(WATER, "slime_block", "mundane"),
          mix(WATER, "stone", "mundane"),
          mix(AWKWARD, "golden_carrot", "night_vision"),
          mix(AWKWARD, "magma_cream", "fire_resistance"),
          mix(AWKWARD, "pufferfish", "water_breathing"),
          mix(AWKWARD, "sugar", "swiftness"),
          mix(AWKWARD, "rabbit_foot", "leaping"),
          mix(AWKWARD, "glistering_melon_slice", "healing"),
          mix(AWKWARD, "ghast_tear", "regeneration"),
          mix(AWKWARD, BLAZE_POWDER, "strength"),
          mix(AWKWARD, "phantom_membrane", "slow_falling"),
          mix(AWKWARD, "turtle_helmet", "turtle_master"),
          mix(AWKWARD, "spider_eye", "poison"),
          mix(AWKWARD, "breeze_rod", "wind_charged"),
          mix(AWKWARD, "cobweb", "weaving"),
          mix(AWKWARD, "slime_block", "oozing"),
          mix(AWKWARD, "stone", "infested"),
          mix("night_vision", FERMENTED_SPIDER_EYE, "invisibility"),
          mix("long_night_vision", FERMENTED_SPIDER_EYE, "long_invisibility"),
          mix("healing", FERMENTED_SPIDER_EYE, "harming"),
          mix("poison", FERMENTED_SPIDER_EYE, "harming"),
          mix("long_poison", FERMENTED_SPIDER_EYE, "harming"),
          mix("strong_healing", FERMENTED_SPIDER_EYE, "strong_harming"),
          mix("strong_poison", FERMENTED_SPIDER_EYE, "strong_harming"),
          mix("swiftness", FERMENTED_SPIDER_EYE, "slowness"),
          mix("leaping", FERMENTED_SPIDER_EYE, "slowness"),
          mix("long_swiftness", FERMENTED_SPIDER_EYE, "long_slowness"),
          mix("long_leaping", FERMENTED_SPIDER_EYE, "long_slowness"),
          mix("fire_resistance", REDSTONE, "long_fire_resistance"),
          mix("invisibility", REDSTONE, "long_invisibility"),
          mix("leaping", REDSTONE, "long_leaping"),
          mix("night_vision", REDSTONE, "long_night_vision"),
          mix("poison", REDSTONE, "long_poison"),
          mix("regeneration", REDSTONE, "long_regeneration"),
          mix("slow_falling", REDSTONE, "long_slow_falling"),
          mix("slowness", REDSTONE, "long_slowness"),
          mix("strength", REDSTONE, "long_strength"),
          mix("swiftness", REDSTONE, "long_swiftness"),
          mix("turtle_master", REDSTONE, "long_turtle_master"),
          mix("water_breathing", REDSTONE, "long_water_breathing"),
          mix("weakness", REDSTONE, "long_weakness"),
          mix("harming", GLOWSTONE, "strong_harming"),
          mix("healing", GLOWSTONE, "strong_healing"),
          mix("leaping", GLOWSTONE, "strong_leaping"),
          mix("poison", GLOWSTONE, "strong_poison"),
          mix("regeneration", GLOWSTONE, "strong_regeneration"),
          mix("slowness", GLOWSTONE, "strong_slowness"),
          mix("strength", GLOWSTONE, "strong_strength"),
          mix("swiftness", GLOWSTONE, "strong_swiftness"),
          mix("turtle_master", GLOWSTONE, "strong_turtle_master"));

  private BrewingTable() {}

  /** Mixes starting from {@code input}, in table order. */
  public static List<BrewingMix> mixesFrom(String input) {
    List<BrewingMix> mixes = new ArrayList<>();
    for (BrewingMix mix : MIXES) {
      if (mix.input().equals(input)) {
        mixes.add(mix);
      }
    }
    return List.copyOf(mixes);
  }

  /** What {@code reagent} turns {@code input} into, if anything. */
  public static Optional<String> result(String input, String reagent) {
    for (BrewingMix mix : MIXES) {
      if (mix.input().equals(input) && mix.reagent().equals(reagent)) {
        return Optional.of(mix.output());
      }
    }
    return Optional.empty();
  }

  /**
   * Fermented spider eye mixes from base potions (those not made by redstone or glowstone), grouped
   * by result in table order: the upgraded versions follow the same pattern.
   */
  public static List<Corruption> corruptions() {
    Set<String> upgraded = new LinkedHashSet<>();
    for (BrewingMix mix : MIXES) {
      if (mix.reagent().equals(REDSTONE) || mix.reagent().equals(GLOWSTONE)) {
        upgraded.add(mix.output());
      }
    }
    Map<String, List<String>> inputsByOutput = new LinkedHashMap<>();
    for (BrewingMix mix : MIXES) {
      if (mix.reagent().equals(FERMENTED_SPIDER_EYE)
          && !mix.input().equals(WATER)
          && !upgraded.contains(mix.input())) {
        inputsByOutput.computeIfAbsent(mix.output(), output -> new ArrayList<>()).add(mix.input());
      }
    }
    List<Corruption> corruptions = new ArrayList<>(inputsByOutput.size());
    for (Map.Entry<String, List<String>> entry : inputsByOutput.entrySet()) {
      corruptions.add(new Corruption(entry.getValue(), entry.getKey()));
    }
    return List.copyOf(corruptions);
  }

  /** Every potion the table names (inputs and outputs), water included, in table order. */
  public static Set<String> potions() {
    Set<String> potions = new LinkedHashSet<>();
    for (BrewingMix mix : MIXES) {
      potions.add(mix.input());
      potions.add(mix.output());
    }
    return Collections.unmodifiableSet(potions);
  }

  private static BrewingMix mix(String input, String reagent, String output) {
    return new BrewingMix(input, reagent, output);
  }
}
