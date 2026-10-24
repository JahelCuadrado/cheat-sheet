package com.einsteinmods.cheatsheet.gametest;

import com.einsteinmods.cheatsheet.core.dyes.DyeRecipe;
import com.einsteinmods.cheatsheet.core.dyes.DyeRecipeKind;
import com.einsteinmods.cheatsheet.core.dyes.DyeTable;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * The cheat sheet's dye recipes ({@link DyeTable}) against the game's recipe manager: every recipe
 * whose result is one of the sixteen dyes must be in the table (same kind, ingredients and count)
 * and every table row must be a recipe of the game.
 */
public final class DyeRecipeGameTest {
  private static final String NAMESPACE = "minecraft:";
  private static final String INGREDIENTS = "ingredients";
  private static final String INGREDIENT = "ingredient";

  @GameTest
  public void dyeTableMatchesTheGame(GameTestHelper helper) {
    Map<String, String> dyeColors = new HashMap<>();
    for (String color : DyeTable.COLORS) {
      dyeColors.put(NAMESPACE + DyeRecipe.dyeItemId(color), color);
    }
    List<String> mismatches = new ArrayList<>();
    Map<String, Integer> game = new TreeMap<>();
    for (JsonObject recipe : RecipeJson.all(helper.getLevel().getServer())) {
      String color = dyeColors.get(RecipeJson.resultId(recipe));
      if (color == null) {
        continue;
      }
      String signature = signature(color, recipe, mismatches);
      if (signature != null) {
        game.merge(signature, 1, Integer::sum);
      }
    }
    Map<String, Integer> table = new TreeMap<>();
    for (DyeRecipe recipe : DyeTable.RECIPES) {
      table.merge(signature(recipe), 1, Integer::sum);
    }
    for (Map.Entry<String, Integer> entry : game.entrySet()) {
      if (!entry.getValue().equals(table.get(entry.getKey()))) {
        mismatches.add("game recipe missing from the table: " + entry.getKey());
      }
    }
    for (Map.Entry<String, Integer> entry : table.entrySet()) {
      if (!entry.getValue().equals(game.get(entry.getKey()))) {
        mismatches.add("table recipe not in the game: " + entry.getKey());
      }
    }
    helper.assertTrue(
        mismatches.isEmpty(), "dye table differs from the game:\n" + String.join("\n", mismatches));
    helper.succeed();
  }

  /** {@code color|KIND|sorted ingredients|count}, or null (and a mismatch) for unknown shapes. */
  private static String signature(String color, JsonObject recipe, List<String> mismatches) {
    String type = RecipeJson.type(recipe);
    List<JsonElement> ingredients = new ArrayList<>();
    DyeRecipeKind kind;
    if (RecipeJson.SHAPELESS.equals(type)) {
      kind = DyeRecipeKind.CRAFTING;
      recipe.getAsJsonArray(INGREDIENTS).forEach(ingredients::add);
    } else if (RecipeJson.SMELTING.equals(type)) {
      kind = DyeRecipeKind.SMELTING;
      ingredients.add(recipe.get(INGREDIENT));
    } else {
      mismatches.add(color + " dye from a " + type + " recipe the table cannot show: " + recipe);
      return null;
    }
    List<String> ids = new ArrayList<>();
    for (JsonElement ingredient : ingredients) {
      String id = ingredient.isJsonPrimitive() ? ingredient.getAsString() : "";
      if (!id.startsWith(NAMESPACE)) {
        mismatches.add(color + " dye recipe with an ingredient the table cannot show: " + recipe);
        return null;
      }
      ids.add(id.substring(NAMESPACE.length()));
    }
    return signature(color, kind, ids, RecipeJson.resultCount(recipe));
  }

  private static String signature(DyeRecipe recipe) {
    return signature(recipe.color(), recipe.kind(), recipe.ingredients(), recipe.count());
  }

  private static String signature(
      String color, DyeRecipeKind kind, List<String> ingredients, int count) {
    List<String> sorted = new ArrayList<>(ingredients);
    Collections.sort(sorted);
    return color + "|" + kind + "|" + String.join("+", sorted) + "|" + count;
  }
}
