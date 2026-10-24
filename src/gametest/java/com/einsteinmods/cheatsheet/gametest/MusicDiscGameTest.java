package com.einsteinmods.cheatsheet.gametest;

import com.einsteinmods.cheatsheet.core.discs.Disc;
import com.einsteinmods.cheatsheet.core.discs.DiscSource;
import com.einsteinmods.cheatsheet.core.discs.DiscTable;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;

/**
 * The cheat sheet's disc table ({@link DiscTable}) against the game: each disc drops from exactly
 * the loot tables of its sources, every disc item is listed, mob drops have the condition the sheet
 * describes (killed by a skeleton; a ghast killed by a fireball; a baby zombie riding a chicken),
 * biome-bound chest loot has its biome (Bounce: mineshafts in sulfur caves only), only the tables
 * the sheet names roll a source's table (a trial chamber vault and its reward chests share one),
 * fragments come only from their listed source and nine of them craft disc 5.
 */
public final class MusicDiscGameTest {
  private static final String NAMESPACE = "minecraft:";
  private static final String INGREDIENTS = "ingredients";

  /**
   * Text each conditional drop's conditions must contain (their JSON as encoded by the loot codec).
   */
  private static final Map<DiscSource, List<String>> CONDITIONS =
      Map.of(
          DiscSource.CREEPER_KILLED_BY_SKELETON,
          List.of("\"attacker\"", "#minecraft:skeletons"),
          DiscSource.GHAST_OWN_FIREBALL,
          List.of("minecraft:fireball", "minecraft:killed_by_player"),
          DiscSource.CHICKEN_JOCKEY,
          List.of("\"is_baby\":true", "minecraft:chicken", "minecraft:killed_by_player"),
          DiscSource.MINESHAFT,
          List.of("minecraft:location_check", "minecraft:sulfur_caves"));

  /**
   * Tables that roll a source's whole table, as its text says: the vault's table ({@code
   * chests/trial_chambers/reward}) is also the loot table of the reward chests in trial chamber
   * hallways (structure templates {@code hallway/rubble}, {@code hallway/rubble_chamber}, {@code
   * corridor/entrance_1}); the ominous vault's is used by ominous vaults only.
   */
  private static final Map<DiscSource, Set<String>> ROLLED_BY =
      Map.of(
          DiscSource.TRIAL_VAULT,
          Set.of(NAMESPACE + "chests/trial_chambers/reward"),
          DiscSource.OMINOUS_VAULT,
          Set.of(NAMESPACE + "chests/trial_chambers/reward_ominous"));

  @GameTest
  public void discTableMatchesTheGame(GameTestHelper helper) {
    MinecraftServer server = helper.getLevel().getServer();
    LootIndex loot = LootIndex.of(server);
    List<String> mismatches = new ArrayList<>();

    Set<String> listed = new HashSet<>();
    for (Disc disc : DiscTable.DISCS) {
      String itemId = NAMESPACE + disc.itemId();
      listed.add(itemId);
      Set<String> expected = new TreeSet<>();
      for (DiscSource source : disc.sources()) {
        source.lootTable().ifPresent(table -> expected.add(NAMESPACE + table));
      }
      Set<String> actual = loot.tablesOf(itemId);
      if (!expected.equals(actual)) {
        mismatches.add(itemId + ": table says " + expected + ", game drops it from " + actual);
      }
      for (DiscSource source : disc.sources()) {
        checkCondition(loot, itemId, source, mismatches);
      }
    }
    for (DiscSource source : DiscSource.values()) {
      source
          .lootTable()
          .ifPresent(
              table -> {
                Set<String> rolledBy = loot.tablesNesting(NAMESPACE + table);
                Set<String> expected = ROLLED_BY.getOrDefault(source, Set.of());
                if (!rolledBy.equals(expected)) {
                  mismatches.add(table + " is rolled by " + rolledBy + ", sheet says " + expected);
                }
              });
    }
    for (Item item : BuiltInRegistries.ITEM) {
      String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
      boolean playable = item.components().has(DataComponents.JUKEBOX_PLAYABLE);
      if (playable != listed.contains(itemId)) {
        mismatches.add(itemId + (playable ? " is a disc missing from the table" : " is no disc"));
      }
    }

    String fragment = NAMESPACE + DiscTable.FRAGMENT_ITEM_ID;
    Set<String> fragmentTables = loot.tablesOf(fragment);
    String fragmentTable = NAMESPACE + DiscTable.FRAGMENT_SOURCE.lootTable().orElseThrow();
    if (!fragmentTables.equals(Set.of(fragmentTable))) {
      mismatches.add(fragment + " drops from " + fragmentTables + ", table says " + fragmentTable);
    }
    checkCraftedDiscs(RecipeJson.all(server), fragment, mismatches);

    helper.assertTrue(
        mismatches.isEmpty(),
        "disc table differs from the game:\n" + String.join("\n", mismatches));
    helper.succeed();
  }

  private static void checkCondition(
      LootIndex loot, String itemId, DiscSource source, List<String> mismatches) {
    List<String> needed = CONDITIONS.get(source);
    if (needed == null) {
      return;
    }
    String table = NAMESPACE + source.lootTable().orElseThrow();
    for (LootIndex.Drop drop : loot.dropsOf(itemId)) {
      if (drop.lootTable().equals(table)) {
        for (String text : needed) {
          if (!drop.conditions().contains(text)) {
            mismatches.add(itemId + " from " + table + ": condition lacks " + text + ": " + drop);
          }
        }
      }
    }
  }

  /** Discs from crafting: one shapeless recipe of nine fragments, making one disc. */
  private static void checkCraftedDiscs(
      List<JsonObject> recipes, String fragment, List<String> mismatches) {
    for (Disc disc : DiscTable.discsFrom(DiscSource.CRAFTED)) {
      String itemId = NAMESPACE + disc.itemId();
      List<JsonObject> found = new ArrayList<>();
      for (JsonObject recipe : recipes) {
        if (itemId.equals(RecipeJson.resultId(recipe))) {
          found.add(recipe);
        }
      }
      boolean matches =
          found.size() == 1 && RecipeJson.SHAPELESS.equals(RecipeJson.type(found.getFirst()));
      if (matches) {
        int fragments = 0;
        for (JsonElement ingredient : found.getFirst().getAsJsonArray(INGREDIENTS)) {
          if (ingredient.isJsonPrimitive() && fragment.equals(ingredient.getAsString())) {
            fragments++;
          } else {
            matches = false;
          }
        }
        matches &=
            fragments == DiscTable.FRAGMENTS_PER_DISC
                && RecipeJson.resultCount(found.getFirst()) == 1;
      }
      if (!matches) {
        mismatches.add(itemId + ": expected nine fragments crafting one disc, found " + found);
      }
    }
    for (Disc disc : DiscTable.DISCS) {
      if (!disc.sources().contains(DiscSource.CRAFTED)) {
        for (JsonObject recipe : recipes) {
          if ((NAMESPACE + disc.itemId()).equals(RecipeJson.resultId(recipe))) {
            mismatches.add(disc.itemId() + " has a recipe the table does not list: " + recipe);
          }
        }
      }
    }
  }
}
