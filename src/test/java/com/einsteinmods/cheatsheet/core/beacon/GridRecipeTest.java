package com.einsteinmods.cheatsheet.core.beacon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GridRecipeTest {
  @Test
  void conduitTakesEightShellsAroundAHeart() {
    GridRecipe conduit = ConduitMaterials.CONDUIT_RECIPE;
    assertEquals(
        Map.of(ConduitMaterials.NAUTILUS_SHELL, 8, ConduitMaterials.HEART_OF_THE_SEA, 1),
        conduit.counts());
    assertEquals(Optional.of(ConduitMaterials.HEART_OF_THE_SEA), conduit.slots().get(4));
  }

  @Test
  void smallPatternsFillTheTopLeftCorner() {
    GridRecipe prismarine = ConduitMaterials.FRAME_RECIPES.getFirst();
    List<Optional<String>> slots = prismarine.slots();
    assertEquals(GridRecipe.SIDE * GridRecipe.SIDE, slots.size());
    assertEquals(Optional.of(ConduitMaterials.PRISMARINE_SHARD), slots.get(4));
    assertEquals(Optional.empty(), slots.get(2));
    assertEquals(Optional.empty(), slots.get(8));
    assertEquals(Map.of(ConduitMaterials.PRISMARINE_SHARD, 4), prismarine.counts());
  }

  @Test
  void rejectsSymbolsWithoutAnItem() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new GridRecipe("stone", List.of("#?"), Map.of('#', "cobblestone"), false));
  }
}
