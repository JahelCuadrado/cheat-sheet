package com.einsteinmods.cheatsheet.gametest;

import com.einsteinmods.cheatsheet.core.beacon.ConduitMaterials;
import com.einsteinmods.cheatsheet.core.beacon.GridRecipe;
import com.einsteinmods.cheatsheet.core.beacon.MaterialSource;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.BuiltInPackSource;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/**
 * The beacon tab's conduit data ({@link ConduitMaterials}) against the game: the conduit's and the
 * frame blocks' crafting recipes (the only crafting recipes that make them, same pattern and
 * items), frame recipes for exactly the blocks of {@code #conduit_activator_blocks}, every loot
 * table that gives each material (and the treasure map), the nautilus' drop chance, the drowned's
 * shell chance and the wandering trader's shell trade (read from the vanilla pack's files).
 */
public final class ConduitMaterialsGameTest {
  private static final String NAMESPACE = "minecraft:";
  private static final String PATTERN = "pattern";
  private static final String KEY = "key";
  private static final String EMPTY_SLOT = "-";
  private static final String INGREDIENTS = "ingredients";
  private static final String GIVES = "gives";
  private static final String WANTS = "wants";
  private static final String ID = "id";
  private static final String COUNT = "count";
  private static final String EMERALD = "minecraft:emerald";
  private static final String TRADE_DIRECTORY = "villager_trade";
  private static final String JSON_EXTENSION = ".json";
  private static final String NAUTILUS_TABLE = "entities/nautilus";
  private static final String CHANCE_FIELD = "\"unenchanted_chance\":";
  private static final double PERCENT = 100.0;

  @GameTest
  public void conduitMaterialsMatchTheGame(GameTestHelper helper) {
    MinecraftServer server = helper.getLevel().getServer();
    List<String> mismatches = new ArrayList<>();
    checkRecipes(RecipeJson.all(server), mismatches);
    checkFrameBlocks(mismatches);
    checkLoot(LootIndex.of(server), mismatches);
    checkDrowned(mismatches);
    checkTrades(server.getResourceManager(), mismatches);
    helper.assertTrue(
        mismatches.isEmpty(),
        "conduit materials differ from the game:\n" + String.join("\n", mismatches));
    helper.succeed();
  }

  private static void checkRecipes(List<JsonObject> recipes, List<String> mismatches) {
    List<GridRecipe> table = new ArrayList<>(ConduitMaterials.FRAME_RECIPES);
    table.add(ConduitMaterials.CONDUIT_RECIPE);
    for (GridRecipe recipe : table) {
      String result = NAMESPACE + recipe.resultId();
      List<JsonObject> game = new ArrayList<>();
      for (JsonObject candidate : recipes) {
        String type = RecipeJson.type(candidate);
        if (result.equals(RecipeJson.resultId(candidate))
            && (RecipeJson.SHAPED.equals(type) || RecipeJson.SHAPELESS.equals(type))) {
          game.add(candidate);
        }
      }
      if (game.size() != 1) {
        mismatches.add(result + ": " + game.size() + " crafting recipes in the game, table has 1");
        continue;
      }
      JsonObject found = game.getFirst();
      String expected = signature(recipe);
      String actual = signature(found);
      if (!expected.equals(actual) || RecipeJson.resultCount(found) != 1) {
        mismatches.add(result + ": table " + expected + ", game " + found);
      }
    }
  }

  /**
   * Shaped: the grid's rows with each symbol replaced by its item (symbols may differ from
   * vanilla's); shapeless: the sorted items. Ids with namespace.
   */
  private static String signature(GridRecipe recipe) {
    if (recipe.shapeless()) {
      return shapeless(recipe.counts());
    }
    Map<String, String> key = new TreeMap<>();
    recipe.key().forEach((symbol, item) -> key.put(symbol.toString(), NAMESPACE + item));
    return shaped(recipe.pattern(), key);
  }

  private static String signature(JsonObject recipe) {
    if (RecipeJson.SHAPELESS.equals(RecipeJson.type(recipe))) {
      Map<String, Integer> counts = new TreeMap<>();
      for (JsonElement ingredient : recipe.getAsJsonArray(INGREDIENTS)) {
        counts.merge(text(ingredient), 1, Integer::sum);
      }
      return shapelessWithNamespace(counts);
    }
    List<String> pattern = new ArrayList<>();
    for (JsonElement row : recipe.getAsJsonArray(PATTERN)) {
      pattern.add(row.getAsString());
    }
    Map<String, String> key = new TreeMap<>();
    for (Map.Entry<String, JsonElement> entry : recipe.getAsJsonObject(KEY).entrySet()) {
      key.put(entry.getKey(), text(entry.getValue()));
    }
    return shaped(pattern, key);
  }

  private static String shaped(List<String> pattern, Map<String, String> key) {
    List<List<String>> rows = new ArrayList<>();
    for (String row : pattern) {
      List<String> items = new ArrayList<>();
      for (char symbol : row.toCharArray()) {
        items.add(key.getOrDefault(String.valueOf(symbol), EMPTY_SLOT));
      }
      rows.add(items);
    }
    return RecipeJson.SHAPED + rows;
  }

  private static String shapeless(Map<String, Integer> counts) {
    Map<String, Integer> named = new TreeMap<>();
    counts.forEach((item, count) -> named.put(NAMESPACE + item, count));
    return shapelessWithNamespace(named);
  }

  private static String shapelessWithNamespace(Map<String, Integer> counts) {
    return RecipeJson.SHAPELESS + new TreeMap<>(counts);
  }

  /** An ingredient written as one item id; anything else (a tag, a list) as its JSON. */
  private static String text(JsonElement ingredient) {
    return ingredient.isJsonPrimitive() ? ingredient.getAsString() : ingredient.toString();
  }

  /** One frame recipe per block of the tag, in the tag's order. */
  private static void checkFrameBlocks(List<String> mismatches) {
    List<String> tag = new ArrayList<>();
    for (Holder<Block> block :
        BuiltInRegistries.BLOCK.getTagOrEmpty(BlockTags.CONDUIT_EFFECT_BLOCK)) {
      tag.add(block.unwrapKey().orElseThrow().identifier().getPath());
    }
    List<String> table = new ArrayList<>();
    for (GridRecipe recipe : ConduitMaterials.FRAME_RECIPES) {
      table.add(recipe.resultId());
    }
    if (!tag.equals(table)) {
      mismatches.add("frame blocks: tag " + tag + ", table " + table);
    }
  }

  private static void checkLoot(LootIndex loot, List<String> mismatches) {
    for (Map.Entry<String, List<MaterialSource>> material : ConduitMaterials.SOURCES.entrySet()) {
      Set<String> expected = new TreeSet<>();
      for (MaterialSource source : material.getValue()) {
        checkIcon(source.iconId(), mismatches);
        if (source.kind() == MaterialSource.Kind.LOOT) {
          for (String table : source.references()) {
            expected.add(NAMESPACE + table);
          }
        }
      }
      expectTables(loot, material.getKey(), expected, mismatches);
    }
    Set<String> mapTables = new TreeSet<>();
    for (String table : ConduitMaterials.TREASURE_MAP_TABLES) {
      mapTables.add(NAMESPACE + table);
    }
    expectTables(loot, ConduitMaterials.TREASURE_MAP, mapTables, mismatches);

    String chance = CHANCE_FIELD + ConduitMaterials.NAUTILUS_SHELL_PERCENT / PERCENT;
    String shell = NAMESPACE + ConduitMaterials.NAUTILUS_SHELL;
    String nautilus = NAMESPACE + NAUTILUS_TABLE;
    boolean found = false;
    for (LootIndex.Drop drop : loot.dropsOf(shell)) {
      found |= drop.lootTable().equals(nautilus) && drop.conditions().contains(chance);
    }
    if (!found) {
      mismatches.add(nautilus + ": no shell drop with " + chance + " in " + loot.dropsOf(shell));
    }
  }

  private static void expectTables(
      LootIndex loot, String itemId, Set<String> expected, List<String> mismatches) {
    Set<String> actual = loot.tablesOf(NAMESPACE + itemId);
    if (!expected.equals(actual)) {
      mismatches.add(itemId + ": table says " + expected + ", game gives it from " + actual);
    }
  }

  private static void checkIcon(String itemId, List<String> mismatches) {
    if (!BuiltInRegistries.ITEM.containsKey(Identifier.withDefaultNamespace(itemId))) {
      mismatches.add("no item " + itemId + " to show");
    }
  }

  private static void checkDrowned(List<String> mismatches) {
    long percent = Math.round(Drowned.NAUTILUS_SHELL_CHANCE * PERCENT);
    if (percent != ConduitMaterials.DROWNED_SHELL_PERCENT) {
      mismatches.add("drowned shell chance " + percent + " %");
    }
  }

  /**
   * Every vanilla trade that gives a material is listed (and only those), with the price the sheet
   * shows for the shell.
   */
  private static void checkTrades(ResourceManager resources, List<String> mismatches) {
    Map<String, Set<String>> game = new TreeMap<>();
    resources
        .listResourceStacks(TRADE_DIRECTORY, file -> file.getPath().endsWith(JSON_EXTENSION))
        .forEach(
            (file, stack) -> {
              JsonObject trade = vanilla(file, stack);
              if (trade == null || !trade.has(GIVES)) {
                return;
              }
              String path = file.getPath();
              String id =
                  path.substring(
                      TRADE_DIRECTORY.length() + 1, path.length() - JSON_EXTENSION.length());
              String gives = trade.getAsJsonObject(GIVES).get(ID).getAsString();
              game.computeIfAbsent(gives, item -> new TreeSet<>()).add(id);
              if (gives.equals(NAMESPACE + ConduitMaterials.NAUTILUS_SHELL)) {
                checkShellPrice(id, trade.getAsJsonObject(WANTS), mismatches);
              }
            });
    for (Map.Entry<String, List<MaterialSource>> material : ConduitMaterials.SOURCES.entrySet()) {
      Set<String> expected = new TreeSet<>();
      for (MaterialSource source : material.getValue()) {
        if (source.kind() == MaterialSource.Kind.TRADE) {
          expected.addAll(source.references());
        }
      }
      Set<String> actual = game.getOrDefault(NAMESPACE + material.getKey(), Set.of());
      if (!expected.equals(actual)) {
        mismatches.add(material.getKey() + ": trades " + expected + ", game " + actual);
      }
    }
  }

  private static void checkShellPrice(String id, JsonObject wants, List<String> mismatches) {
    int count = wants.has(COUNT) ? wants.get(COUNT).getAsInt() : 1;
    if (!EMERALD.equals(wants.get(ID).getAsString())
        || count != ConduitMaterials.TRADER_SHELL_EMERALDS) {
      mismatches.add(id + ": shell costs " + wants);
    }
  }

  private static @Nullable JsonObject vanilla(Identifier file, List<Resource> stack) {
    for (Resource resource : stack) {
      if (BuiltInPackSource.VANILLA_ID.equals(resource.sourcePackId())) {
        try (Reader reader = resource.openAsReader()) {
          return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException exception) {
          throw new UncheckedIOException("cannot read " + file, exception);
        }
      }
    }
    return null;
  }
}
