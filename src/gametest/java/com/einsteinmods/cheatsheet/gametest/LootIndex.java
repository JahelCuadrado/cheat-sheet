package com.einsteinmods.cheatsheet.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Every item that any loot table of the running game can give, with where and under which
 * conditions. Loot tables are read through their codec ({@code LootTable.DIRECT_CODEC} to JSON):
 * {@code minecraft:item} entries name the item, {@code minecraft:tag} entries are expanded with the
 * server's item tags. Conditions met on the way to an entry (pool and entry {@code condition(s)})
 * are kept as their JSON text, so tests can check a drop's condition. {@code minecraft:loot_table}
 * entries (a table rolled inside another, such as a trial chamber reward chest rolling {@code
 * reward_unique}) are not followed: their items count for the inner table only, and {@link
 * #tablesNesting} names the outer ones.
 */
final class LootIndex {
  private static final String TYPE = "type";
  private static final String ITEM_ENTRY = "minecraft:item";
  private static final String TAG_ENTRY = "minecraft:tag";
  private static final String NAME = "name";
  private static final String ITEMS = "items";
  private static final String LOOT_TABLE_ENTRY = "minecraft:loot_table";
  private static final String VALUE = "value";
  private static final String TAG_PREFIX = "#";
  private static final List<String> CONDITION_KEYS = List.of("condition", "conditions");

  /** One place an item drops: the loot table id and the conditions leading to its entry. */
  record Drop(String lootTable, String conditions) {}

  private final Map<String, List<Drop>> drops;

  /** Each loot table rolled by a {@code minecraft:loot_table} entry, to the tables holding it. */
  private final Map<String, Set<String>> nestedIn;

  private LootIndex(Map<String, List<Drop>> drops, Map<String, Set<String>> nestedIn) {
    this.drops = drops;
    this.nestedIn = nestedIn;
  }

  static LootIndex of(MinecraftServer server) {
    HolderLookup.Provider lookup = server.reloadableRegistries().lookup();
    RegistryOps<JsonElement> ops = lookup.createSerializationContext(JsonOps.INSTANCE);
    Map<String, List<Drop>> drops = new HashMap<>();
    Map<String, Set<String>> nestedIn = new HashMap<>();
    lookup
        .lookupOrThrow(Registries.LOOT_TABLE)
        .listElements()
        .forEach(
            table -> {
              JsonElement json =
                  LootTable.DIRECT_CODEC.encodeStart(ops, table.value()).getOrThrow();
              walk(json, "", table.key().identifier().toString(), drops, nestedIn);
            });
    return new LootIndex(drops, nestedIn);
  }

  /** Every drop of the item with this id ({@code minecraft:music_disc_cat}). */
  List<Drop> dropsOf(String itemId) {
    return drops.getOrDefault(itemId, List.of());
  }

  /** Loot tables that can give the item, sorted. */
  Set<String> tablesOf(String itemId) {
    Set<String> tables = new TreeSet<>();
    for (Drop drop : dropsOf(itemId)) {
      tables.add(drop.lootTable());
    }
    return tables;
  }

  /** Loot tables with an entry that rolls the whole {@code table}, sorted. */
  Set<String> tablesNesting(String table) {
    return new TreeSet<>(nestedIn.getOrDefault(table, Set.of()));
  }

  /** Every item id that some loot table gives. */
  Set<String> items() {
    return drops.keySet();
  }

  private static void walk(
      JsonElement element,
      String conditions,
      String table,
      Map<String, List<Drop>> drops,
      Map<String, Set<String>> nestedIn) {
    if (element.isJsonArray()) {
      for (JsonElement child : element.getAsJsonArray()) {
        walk(child, conditions, table, drops, nestedIn);
      }
      return;
    }
    if (!element.isJsonObject()) {
      return;
    }
    JsonObject object = element.getAsJsonObject();
    StringBuilder reached = new StringBuilder(conditions);
    for (String key : CONDITION_KEYS) {
      if (object.has(key)) {
        reached.append(object.get(key));
      }
    }
    String type =
        object.has(TYPE) && object.get(TYPE).isJsonPrimitive()
            ? object.get(TYPE).getAsString()
            : "";
    if (ITEM_ENTRY.equals(type) && object.has(NAME)) {
      add(drops, object.get(NAME).getAsString(), new Drop(table, reached.toString()));
    } else if (TAG_ENTRY.equals(type) && object.has(ITEMS)) {
      for (String itemId : itemsOf(object.get(ITEMS))) {
        add(drops, itemId, new Drop(table, reached.toString()));
      }
    } else if (LOOT_TABLE_ENTRY.equals(type)
        && object.has(VALUE)
        && object.get(VALUE).isJsonPrimitive()) {
      nestedIn.computeIfAbsent(object.get(VALUE).getAsString(), key -> new TreeSet<>()).add(table);
    }
    for (Map.Entry<String, JsonElement> child : object.entrySet()) {
      if (!CONDITION_KEYS.contains(child.getKey())) {
        walk(child.getValue(), reached.toString(), table, drops, nestedIn);
      }
    }
  }

  /** Items of a tag entry's holder set: {@code #tag} (expanded), one id, or a list of ids. */
  private static List<String> itemsOf(JsonElement items) {
    List<String> ids = new ArrayList<>();
    if (items.isJsonArray()) {
      for (JsonElement item : items.getAsJsonArray()) {
        ids.add(item.getAsString());
      }
      return ids;
    }
    String text = items.getAsString();
    if (!text.startsWith(TAG_PREFIX)) {
      ids.add(text);
      return ids;
    }
    TagKey<Item> tag =
        TagKey.create(Registries.ITEM, Identifier.parse(text.substring(TAG_PREFIX.length())));
    for (Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
      ids.add(item.unwrapKey().orElseThrow().identifier().toString());
    }
    return ids;
  }

  private static void add(Map<String, List<Drop>> drops, String itemId, Drop drop) {
    drops.computeIfAbsent(itemId, id -> new ArrayList<>()).add(drop);
  }
}
