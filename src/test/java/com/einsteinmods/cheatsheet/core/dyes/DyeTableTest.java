package com.einsteinmods.cheatsheet.core.dyes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class DyeTableTest {
  @Test
  void sixteenColoursEachWithARecipe() {
    assertEquals(16, new HashSet<>(DyeTable.COLORS).size());
    for (String color : DyeTable.COLORS) {
      assertFalse(DyeTable.recipesFor(color).isEmpty(), color + " has no recipe");
    }
    for (DyeRecipe recipe : DyeTable.RECIPES) {
      assertTrue(DyeTable.COLORS.contains(recipe.color()), recipe.color());
    }
  }

  @Test
  void mixesAreMadeOfDyes() {
    for (DyeRecipe recipe : DyeTable.RECIPES) {
      if (recipe.isMix()) {
        for (String ingredient : recipe.ingredients()) {
          assertTrue(ingredient.endsWith("_dye"), ingredient + " in a mix for " + recipe.color());
        }
      }
    }
  }

  @Test
  void recipesForKeepsTableOrderAndResultIds() {
    List<DyeRecipe> lightGray = DyeTable.recipesFor("light_gray");
    assertEquals(5, lightGray.size());
    assertEquals(List.of("azure_bluet"), lightGray.getFirst().ingredients());
    assertEquals("light_gray_dye", lightGray.getFirst().resultItemId());
  }

  @Test
  void smeltingTakesOneIngredient() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new DyeRecipe("green", DyeRecipeKind.SMELTING, List.of("cactus", "cactus"), 1));
  }
}
