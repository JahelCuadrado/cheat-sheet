package com.einsteinmods.cheatsheet.core.potions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BrewingGuideTest {
  @Test
  void everyAwkwardPotionAndOppositeHasOneTile() {
    List<String> potions = new ArrayList<>();
    for (PotionRecipe recipe : BrewingGuide.recipes()) {
      potions.add(recipe.potion());
    }
    assertEquals(
        List.of(
            "night_vision",
            "invisibility",
            "fire_resistance",
            "water_breathing",
            "swiftness",
            "slowness",
            "leaping",
            "healing",
            "harming",
            "regeneration",
            "strength",
            "slow_falling",
            "turtle_master",
            "poison",
            "wind_charged",
            "weaving",
            "oozing",
            "infested",
            "weakness"),
        potions);
    assertEquals(potions.size(), new HashSet<>(potions).size());
  }

  @Test
  void oppositesListEveryBase() {
    PotionRecipe harming = recipe("harming");
    assertEquals(List.of("healing", "poison"), harming.bases());
    assertEquals(BrewingTable.FERMENTED_SPIDER_EYE, harming.reagent());
    assertEquals(Optional.of("strong_harming"), harming.stronger());
    assertEquals(List.of(BrewingTable.WATER), recipe("weakness").bases());
  }

  @Test
  void marksExistOnlyWhereTheTableHasThem() {
    PotionRecipe poison = recipe("poison");
    assertEquals(List.of(BrewingTable.AWKWARD), poison.bases());
    assertEquals("spider_eye", poison.reagent());
    assertEquals(Optional.of("long_poison"), poison.longer());
    assertEquals(Optional.of("strong_poison"), poison.stronger());
    assertEquals(Optional.of("harming"), poison.opposite());
    PotionRecipe fireResistance = recipe("fire_resistance");
    assertEquals(Optional.empty(), fireResistance.stronger());
    assertEquals(Optional.empty(), fireResistance.opposite());
  }

  @Test
  void startIsWaterAndNetherWart() {
    PotionRecipe start = BrewingGuide.start();
    assertEquals(BrewingTable.AWKWARD, start.potion());
    assertEquals(
        Optional.of(start.potion()),
        BrewingTable.result(start.bases().getFirst(), start.reagent()));
  }

  @Test
  void everyBaseBrewsTheTile() {
    Set<String> checked = new HashSet<>();
    for (PotionRecipe recipe : BrewingGuide.recipes()) {
      for (String base : recipe.bases()) {
        assertEquals(Optional.of(recipe.potion()), BrewingTable.result(base, recipe.reagent()));
        checked.add(base);
      }
    }
    assertTrue(checked.contains(BrewingTable.AWKWARD));
  }

  private static PotionRecipe recipe(String potion) {
    for (PotionRecipe recipe : BrewingGuide.recipes()) {
      if (recipe.potion().equals(potion)) {
        return recipe;
      }
    }
    throw new AssertionError("no tile for " + potion);
  }
}
