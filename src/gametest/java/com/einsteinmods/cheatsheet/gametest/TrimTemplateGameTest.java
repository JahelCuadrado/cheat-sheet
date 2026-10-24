package com.einsteinmods.cheatsheet.gametest;

import com.einsteinmods.cheatsheet.core.trims.TrimSource;
import com.einsteinmods.cheatsheet.core.trims.TrimTable;
import com.einsteinmods.cheatsheet.core.trims.TrimTemplate;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;

/**
 * The cheat sheet's template table ({@link TrimTable}) against the game: each template drops from
 * exactly the loot tables of its source, only the tables the sheet names roll a source's table (a
 * trial chamber vault and its reward chests share one), every trim pattern and every template found
 * in loot is in the table, and each copy recipe is 7 diamonds + the template + the listed block,
 * making two.
 */
public final class TrimTemplateGameTest {
  private static final String NAMESPACE = "minecraft:";
  private static final String TEMPLATE_SUFFIX = "_smithing_template";
  private static final String DIAMOND = "minecraft:diamond";
  private static final String KEY = "key";
  private static final String PATTERN = "pattern";

  /**
   * Tables that roll a source's whole table, as its text says: the vault's table ({@code
   * chests/trial_chambers/reward}) is also the loot table of the reward chests in trial chamber
   * hallways (structure templates {@code hallway/rubble}, {@code hallway/rubble_chamber}, {@code
   * corridor/entrance_1}); the ominous vault's is used by ominous vaults only.
   */
  private static final Map<TrimSource, Set<String>> ROLLED_BY =
      Map.of(
          TrimSource.TRIAL_VAULT,
          Set.of(NAMESPACE + "chests/trial_chambers/reward"),
          TrimSource.OMINOUS_VAULT,
          Set.of(NAMESPACE + "chests/trial_chambers/reward_ominous"));

  @GameTest
  public void templateTableMatchesTheGame(GameTestHelper helper) {
    MinecraftServer server = helper.getLevel().getServer();
    LootIndex loot = LootIndex.of(server);
    List<String> mismatches = new ArrayList<>();

    Set<String> listed = new HashSet<>();
    Set<String> listedPatterns = new HashSet<>();
    for (TrimTemplate template : TrimTable.TEMPLATES) {
      String itemId = NAMESPACE + template.itemId();
      listed.add(itemId);
      template.patternId().ifPresent(pattern -> listedPatterns.add(NAMESPACE + pattern));
      Set<String> expected = new TreeSet<>();
      for (String table : template.source().lootTables()) {
        expected.add(NAMESPACE + table);
      }
      Set<String> actual = loot.tablesOf(itemId);
      if (!expected.equals(actual)) {
        mismatches.add(itemId + ": table says " + expected + ", game drops it from " + actual);
      }
    }
    for (TrimSource source : TrimSource.values()) {
      for (String table : source.lootTables()) {
        Set<String> rolledBy = loot.tablesNesting(NAMESPACE + table);
        Set<String> expected = ROLLED_BY.getOrDefault(source, Set.of());
        if (!rolledBy.equals(expected)) {
          mismatches.add(table + " is rolled by " + rolledBy + ", sheet says " + expected);
        }
      }
    }
    for (String itemId : loot.items()) {
      if (itemId.endsWith(TEMPLATE_SUFFIX) && !listed.contains(itemId)) {
        mismatches.add(itemId + " drops from " + loot.tablesOf(itemId) + " but is not listed");
      }
    }
    server
        .registryAccess()
        .lookupOrThrow(Registries.TRIM_PATTERN)
        .keySet()
        .forEach(
            pattern -> {
              if (!listedPatterns.remove(pattern.toString())) {
                mismatches.add("pattern " + pattern + " has no template in the table");
              }
            });
    for (String pattern : listedPatterns) {
      mismatches.add("pattern " + pattern + " is not in the game");
    }

    checkCopyRecipes(RecipeJson.all(server), mismatches);
    helper.assertTrue(
        mismatches.isEmpty(),
        "template table differs from the game:\n" + String.join("\n", mismatches));
    helper.succeed();
  }

  private static void checkCopyRecipes(List<JsonObject> recipes, List<String> mismatches) {
    for (TrimTemplate template : TrimTable.TEMPLATES) {
      String itemId = NAMESPACE + template.itemId();
      String block = NAMESPACE + template.copyBlockId();
      List<JsonObject> copies = new ArrayList<>();
      for (JsonObject recipe : recipes) {
        if (itemId.equals(RecipeJson.resultId(recipe))) {
          copies.add(recipe);
        }
      }
      if (copies.size() != 1 || !RecipeJson.SHAPED.equals(RecipeJson.type(copies.getFirst()))) {
        mismatches.add(itemId + ": expected one shaped copy recipe, found " + copies);
        continue;
      }
      JsonObject copy = copies.getFirst();
      if (RecipeJson.resultCount(copy) != TrimTable.COPY_RESULT_COUNT) {
        mismatches.add(itemId + ": copy makes " + RecipeJson.resultCount(copy));
      }
      Set<String> others = new TreeSet<>();
      String diamondSymbol = null;
      boolean usesTemplate = false;
      for (Map.Entry<String, JsonElement> key : copy.getAsJsonObject(KEY).entrySet()) {
        Set<String> accepted = accepted(key.getValue());
        if (accepted.equals(Set.of(DIAMOND))) {
          diamondSymbol = key.getKey();
        } else if (accepted.equals(Set.of(itemId))) {
          usesTemplate = true;
        } else {
          others.addAll(accepted);
        }
      }
      int diamonds = 0;
      for (JsonElement row : copy.getAsJsonArray(PATTERN)) {
        for (char symbol : row.getAsString().toCharArray()) {
          if (diamondSymbol != null && diamondSymbol.equals(String.valueOf(symbol))) {
            diamonds++;
          }
        }
      }
      if (!usesTemplate || diamonds != TrimTable.COPY_DIAMONDS || !others.contains(block)) {
        mismatches.add(
            itemId
                + ": copy recipe is "
                + diamonds
                + " diamonds + "
                + others
                + (usesTemplate ? " + template" : " without template")
                + ", table says "
                + block);
      }
    }
  }

  /** Items an ingredient accepts: one id or a list of ids. */
  private static Set<String> accepted(JsonElement ingredient) {
    Set<String> items = new TreeSet<>();
    if (ingredient.isJsonArray()) {
      for (JsonElement item : ingredient.getAsJsonArray()) {
        items.add(item.getAsString());
      }
    } else {
      items.add(ingredient.getAsString());
    }
    return items;
  }
}
