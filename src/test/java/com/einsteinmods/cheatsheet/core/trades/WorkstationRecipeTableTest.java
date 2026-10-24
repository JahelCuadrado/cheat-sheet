package com.einsteinmods.cheatsheet.core.trades;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WorkstationRecipeTableTest {
  private static final int VILLAGER_WORKSTATIONS = 13;

  @Test
  void everyWorkstationOnce() {
    Set<String> seen = new HashSet<>();
    for (WorkstationRecipe recipe : WorkstationRecipeTable.RECIPES) {
      assertTrue(seen.add(recipe.workstationId()), recipe.workstationId());
    }
    assertEquals(VILLAGER_WORKSTATIONS, seen.size());
  }

  @Test
  void patternSitsInTheTopLeftCorner() {
    List<Optional<RecipeIngredient>> grid = WorkstationRecipeTable.of("loom").orElseThrow().grid();
    assertEquals(WorkstationRecipe.SIZE * WorkstationRecipe.SIZE, grid.size());
    assertEquals("string", grid.get(0).orElseThrow().itemId());
    assertEquals("string", grid.get(1).orElseThrow().itemId());
    assertTrue(grid.get(2).isEmpty());
    assertEquals(Optional.of("planks"), grid.get(3).orElseThrow().tagId());
    assertEquals(Optional.of("planks"), grid.get(4).orElseThrow().tagId());
    for (int slot = 5; slot < grid.size(); slot++) {
      assertTrue(grid.get(slot).isEmpty(), "slot " + slot);
    }
  }

  @Test
  void spacesAreEmptySlots() {
    List<Optional<RecipeIngredient>> grid =
        WorkstationRecipeTable.of("smoker").orElseThrow().grid();
    assertTrue(grid.get(0).isEmpty());
    assertEquals("furnace", grid.get(4).orElseThrow().itemId());
    assertEquals("oak_log", grid.get(7).orElseThrow().itemId());
    assertTrue(grid.get(8).isEmpty());
  }

  @Test
  void unknownKeyIsRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new WorkstationRecipe("x", List.of("#?"), Map.of('#', RecipeIngredient.item("a"))));
  }

  @Test
  void unknownWorkstationHasNoRecipe() {
    assertTrue(WorkstationRecipeTable.of("crafting_table").isEmpty());
  }
}
