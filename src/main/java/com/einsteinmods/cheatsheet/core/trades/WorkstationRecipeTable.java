package com.einsteinmods.cheatsheet.core.trades;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Crafting recipes of the villager workstations in Minecraft 26.3 ({@code data/minecraft/recipe}).
 * Recipes are server data, never sent to the client, so the Villagers tab reads this copy; {@code
 * WorkstationRecipeGameTest} compares it with the game's recipe manager and fails the build on any
 * difference. A tag is drawn as its oak or plain stone member.
 */
public final class WorkstationRecipeTable {
  private static final RecipeIngredient PLANKS = RecipeIngredient.tag("planks", "oak_planks");
  private static final RecipeIngredient WOODEN_SLABS =
      RecipeIngredient.tag("wooden_slabs", "oak_slab");
  private static final RecipeIngredient LOGS = RecipeIngredient.tag("logs", "oak_log");
  private static final RecipeIngredient STONE_CRAFTING_MATERIALS =
      RecipeIngredient.tag("stone_crafting_materials", "cobblestone");
  private static final RecipeIngredient IRON_INGOT = RecipeIngredient.item("iron_ingot");
  private static final RecipeIngredient FURNACE = RecipeIngredient.item("furnace");

  /** One recipe per workstation, in the order of the professions that use them. */
  public static final List<WorkstationRecipe> RECIPES =
      List.of(
          recipe(
              "blast_furnace",
              List.of("III", "IXI", "###"),
              Map.of(
                  'I', IRON_INGOT,
                  'X', FURNACE,
                  '#', RecipeIngredient.item("smooth_stone"))),
          recipe("smoker", List.of(" # ", "#X#", " # "), Map.of('#', LOGS, 'X', FURNACE)),
          recipe(
              "cartography_table",
              List.of("@@", "##", "##"),
              Map.of('@', RecipeIngredient.item("paper"), '#', PLANKS)),
          recipe(
              "brewing_stand",
              List.of(" B ", "###"),
              Map.of('B', RecipeIngredient.item("blaze_rod"), '#', STONE_CRAFTING_MATERIALS)),
          recipe("composter", List.of("# #", "# #", "###"), Map.of('#', WOODEN_SLABS)),
          recipe("barrel", List.of("PSP", "P P", "PSP"), Map.of('P', PLANKS, 'S', WOODEN_SLABS)),
          recipe(
              "fletching_table",
              List.of("@@", "##", "##"),
              Map.of('@', RecipeIngredient.item("flint"), '#', PLANKS)),
          recipe("cauldron", List.of("# #", "# #", "###"), Map.of('#', IRON_INGOT)),
          recipe(
              "lectern",
              List.of("SSS", " B ", " S "),
              Map.of('S', WOODEN_SLABS, 'B', RecipeIngredient.item("bookshelf"))),
          recipe(
              "stonecutter",
              List.of(" I ", "###"),
              Map.of('I', IRON_INGOT, '#', RecipeIngredient.item("stone"))),
          recipe(
              "loom",
              List.of("@@", "##"),
              Map.of('@', RecipeIngredient.item("string"), '#', PLANKS)),
          recipe("smithing_table", List.of("@@", "##", "##"), Map.of('@', IRON_INGOT, '#', PLANKS)),
          recipe(
              "grindstone",
              List.of("I-I", "# #"),
              Map.of(
                  'I', RecipeIngredient.item("stick"),
                  '-', RecipeIngredient.item("stone_slab"),
                  '#', PLANKS)));

  private WorkstationRecipeTable() {}

  /** The recipe that crafts {@code workstationId}, if the table has it. */
  public static Optional<WorkstationRecipe> of(String workstationId) {
    for (WorkstationRecipe recipe : RECIPES) {
      if (recipe.workstationId().equals(workstationId)) {
        return Optional.of(recipe);
      }
    }
    return Optional.empty();
  }

  private static WorkstationRecipe recipe(
      String workstationId, List<String> pattern, Map<Character, RecipeIngredient> key) {
    return new WorkstationRecipe(workstationId, pattern, key);
  }
}
