package com.einsteinmods.cheatsheet.core.farming;

import com.einsteinmods.cheatsheet.core.farming.BreedingFood.FixedFood;
import com.einsteinmods.cheatsheet.core.farming.BreedingFood.TagFood;
import java.util.List;
import java.util.Set;

/**
 * Every animal bred by feeding it, in the farming tab's order: farm animals first. Most read their
 * food from the item tag their {@code isFood} tests; horses, donkeys and llamas breed only with the
 * items their {@code handleEating} names. {@code BreedingFoodGameTest} (part of {@code build})
 * checks the table against the game, and that no breedable animal is missing.
 */
public final class BreedingTable {
  private static final String NAMESPACE = "minecraft:";

  /**
   * {@code AbstractHorse#handleEating}: the only items that put a tamed horse or donkey in love.
   */
  private static final FixedFood HORSE_BREEDING =
      new FixedFood(
          List.of(
              NAMESPACE + "golden_carrot",
              NAMESPACE + "golden_apple",
              NAMESPACE + "enchanted_golden_apple"));

  /** {@code Llama#handleEating}: only hay bales put a tamed llama in love. */
  private static final FixedFood LLAMA_BREEDING = new FixedFood(List.of(NAMESPACE + "hay_block"));

  public static final List<BreedingFood> ANIMALS =
      List.of(
          tagged(List.of("cow", "mooshroom"), "cow_food", false),
          tagged(List.of("sheep"), "sheep_food", false),
          tagged(List.of("pig"), "pig_food", false),
          tagged(List.of("chicken"), "chicken_food", false),
          tagged(List.of("rabbit"), "rabbit_food", false),
          tagged(List.of("goat"), "goat_food", false),
          new BreedingFood(List.of(NAMESPACE + "horse"), HORSE_BREEDING, true),
          new BreedingFood(List.of(NAMESPACE + "donkey"), HORSE_BREEDING, true),
          new BreedingFood(
              List.of(NAMESPACE + "llama", NAMESPACE + "trader_llama"), LLAMA_BREEDING, true),
          tagged(List.of("wolf"), "wolf_food", true),
          tagged(List.of("cat"), "cat_food", true),
          tagged(List.of("ocelot"), "ocelot_food", false),
          tagged(List.of("fox"), "fox_food", false),
          tagged(List.of("panda"), "panda_food", false),
          tagged(List.of("bee"), "bee_food", false),
          tagged(List.of("turtle"), "turtle_food", false),
          tagged(List.of("frog"), "frog_food", false),
          tagged(List.of("axolotl"), "axolotl_food", false),
          tagged(List.of("armadillo"), "armadillo_food", false),
          tagged(List.of("camel"), "camel_food", false),
          tagged(List.of("sniffer"), "sniffer_food", false),
          tagged(List.of("strider"), "strider_food", false),
          tagged(List.of("hoglin"), "hoglin_food", false),
          tagged(List.of("nautilus"), "nautilus_food", true));

  /**
   * Animals never bred by feeding: mules, skeleton and zombie horses, camel husks, parrots, polar
   * bears, happy ghasts and zombie nautiluses.
   */
  public static final Set<String> NOT_BRED_BY_FEEDING =
      Set.of(
          NAMESPACE + "mule",
          NAMESPACE + "skeleton_horse",
          NAMESPACE + "zombie_horse",
          NAMESPACE + "camel_husk",
          NAMESPACE + "parrot",
          NAMESPACE + "polar_bear",
          NAMESPACE + "happy_ghast",
          NAMESPACE + "zombie_nautilus");

  private BreedingTable() {}

  private static BreedingFood tagged(List<String> paths, String tagPath, boolean tamed) {
    return new BreedingFood(
        paths.stream().map(path -> NAMESPACE + path).toList(),
        new TagFood(NAMESPACE + tagPath),
        tamed);
  }
}
