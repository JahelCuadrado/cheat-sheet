package com.einsteinmods.cheatsheet.gametest;

import com.einsteinmods.cheatsheet.core.trades.MerchantTrades;
import com.einsteinmods.cheatsheet.core.trades.RecipeIngredient;
import com.einsteinmods.cheatsheet.core.trades.VillagerTradeTable;
import com.einsteinmods.cheatsheet.core.trades.WorkstationRecipe;
import com.einsteinmods.cheatsheet.core.trades.WorkstationRecipeTable;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The workstation recipes the Villagers tab draws ({@link WorkstationRecipeTable}) against the
 * game's recipe manager: each must be the game's only recipe for its block, a shaped crafting
 * recipe with the same pattern and key (tags as tags); each item drawn for a tag must belong to it;
 * and every profession with trades must have its workstation's recipe in the table.
 */
public final class WorkstationRecipeGameTest {
  private static final String NAMESPACE = "minecraft:";
  private static final String TAG_PREFIX = "#";
  private static final String PATTERN = "pattern";
  private static final String KEY = "key";

  @GameTest
  public void workstationRecipesMatchTheGame(GameTestHelper helper) {
    List<JsonObject> recipes = RecipeJson.all(helper.getLevel().getServer());
    List<String> mismatches = new ArrayList<>();
    for (WorkstationRecipe recipe : WorkstationRecipeTable.RECIPES) {
      check(recipe, recipes, mismatches);
    }
    for (MerchantTrades profession : VillagerTradeTable.PROFESSIONS) {
      String workstation = workstationOf(profession.merchantId());
      if (workstation.isEmpty() || WorkstationRecipeTable.of(workstation).isEmpty()) {
        mismatches.add(
            profession.merchantId() + ": no recipe for its workstation '" + workstation + "'");
      }
    }
    helper.assertTrue(
        mismatches.isEmpty(),
        "workstation recipes differ from the game:\n" + String.join("\n", mismatches));
    helper.succeed();
  }

  private static void check(
      WorkstationRecipe recipe, List<JsonObject> recipes, List<String> mismatches) {
    String result = NAMESPACE + recipe.workstationId();
    List<JsonObject> found = new ArrayList<>();
    for (JsonObject candidate : recipes) {
      if (result.equals(RecipeJson.resultId(candidate))) {
        found.add(candidate);
      }
    }
    if (found.size() != 1) {
      mismatches.add(result + ": " + found.size() + " recipes in the game, the table shows one");
      return;
    }
    JsonObject game = found.getFirst();
    if (!RecipeJson.SHAPED.equals(RecipeJson.type(game))) {
      mismatches.add(result + ": the game's recipe is " + RecipeJson.type(game));
      return;
    }
    List<String> pattern = new ArrayList<>();
    for (JsonElement row : game.getAsJsonArray(PATTERN)) {
      pattern.add(row.getAsString());
    }
    if (!pattern.equals(recipe.pattern())) {
      mismatches.add(result + ": pattern " + pattern + ", table " + recipe.pattern());
    }
    Map<String, String> gameKey = new TreeMap<>();
    for (Map.Entry<String, JsonElement> entry : game.getAsJsonObject(KEY).entrySet()) {
      gameKey.put(entry.getKey(), entry.getValue().toString());
    }
    Map<String, String> tableKey = new TreeMap<>();
    for (Map.Entry<Character, RecipeIngredient> entry : recipe.key().entrySet()) {
      RecipeIngredient ingredient = entry.getValue();
      String id =
          ingredient
              .tagId()
              .map(tag -> TAG_PREFIX + NAMESPACE + tag)
              .orElse(NAMESPACE + ingredient.itemId());
      tableKey.put(String.valueOf(entry.getKey()), "\"" + id + "\"");
      checkShownItem(result, ingredient, mismatches);
    }
    if (!gameKey.equals(tableKey)) {
      mismatches.add(result + ": key " + gameKey + ", table " + tableKey);
    }
  }

  /** The item drawn for an ingredient exists and, for a tag, belongs to it. */
  private static void checkShownItem(
      String result, RecipeIngredient ingredient, List<String> mismatches) {
    Item item =
        BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(ingredient.itemId()));
    if (item == Items.AIR) {
      mismatches.add(result + ": unknown item " + ingredient.itemId());
      return;
    }
    ingredient
        .tagId()
        .ifPresent(
            tag -> {
              TagKey<Item> key =
                  TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace(tag));
              if (!new ItemStack(item).is(key)) {
                mismatches.add(result + ": " + ingredient.itemId() + " is not in #" + tag);
              }
            });
  }

  /** Item id of the block a profession works at (no namespace), or "" for none. */
  private static String workstationOf(String professionId) {
    VillagerProfession profession =
        BuiltInRegistries.VILLAGER_PROFESSION.getValue(
            Identifier.withDefaultNamespace(professionId));
    if (profession == null) {
      return "";
    }
    for (Holder<PoiType> poi : BuiltInRegistries.POINT_OF_INTEREST_TYPE.listElements().toList()) {
      if (profession.heldJobSite().test(poi) && !poi.value().matchingStates().isEmpty()) {
        Item item = poi.value().matchingStates().iterator().next().getBlock().asItem();
        return BuiltInRegistries.ITEM.getKey(item).getPath();
      }
    }
    return "";
  }
}
