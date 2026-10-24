package com.einsteinmods.cheatsheet.gametest;

import com.einsteinmods.cheatsheet.core.trades.BarterRow;
import com.einsteinmods.cheatsheet.core.trades.BarterTable;
import com.einsteinmods.cheatsheet.core.trades.CountRange;
import com.einsteinmods.cheatsheet.core.trades.MerchantTrades;
import com.einsteinmods.cheatsheet.core.trades.TradeItem;
import com.einsteinmods.cheatsheet.core.trades.TradeKind;
import com.einsteinmods.cheatsheet.core.trades.TradePrices;
import com.einsteinmods.cheatsheet.core.trades.TradeRow;
import com.einsteinmods.cheatsheet.core.trades.TradeTier;
import com.einsteinmods.cheatsheet.core.trades.VillagerTradeTable;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.repository.BuiltInPackSource;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.TradeSets;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

/**
 * The cheat sheet's fixed trade tables ({@link VillagerTradeTable}, {@link BarterTable}) against
 * the game's own data: a Minecraft update that changes a villager or wandering trader trade, the
 * trades a level draws from, or piglin bartering fails the build here, so the tables (and the
 * Villagers tab) get updated with it.
 *
 * <p>Trades are compared with the default data, the vanilla pack's files (experimental packs stay
 * out); trades and the bartering loot table go through the game's codecs to normalised JSON, then
 * are read field by field.
 */
public final class VillagerTradeGameTest {
  private static final String TYPE = "type";
  private static final String ID = "id";
  private static final String COUNT = "count";
  private static final String COMPONENTS = "components";
  private static final String POTION_CONTENTS = "minecraft:potion_contents";
  private static final String POTION = "potion";
  private static final String WANTS = "wants";
  private static final String EXTRA_WANTS = "additional_wants";
  private static final String GIVES = "gives";
  private static final String MODIFIER = "given_item_modifier";
  private static final String PREDICATE = "merchant_predicate";
  private static final String ENTITY_PROPERTIES = "minecraft:entity_properties";
  private static final String VILLAGER_VARIANT = "minecraft:villager/variant";
  private static final String FILTERED = "minecraft:filtered";
  private static final String UNIFORM = "minecraft:uniform";
  private static final String SET_COUNT = "minecraft:set_count";
  private static final String SET_POTION = "minecraft:set_potion";
  private static final String ENCHANT_RANDOMLY = "minecraft:enchant_randomly";
  private static final String TRADEABLE_TAG = "#minecraft:tradeable";
  private static final String NAMESPACE = "minecraft:";
  private static final String TAG_PREFIX = "#";
  private static final String JSON_EXTENSION = ".json";
  private static final String TRADE_SET_DIRECTORY = "trade_set/";
  private static final String TRADE_TAG_DIRECTORY = "tags/villager_trade/";
  private static final String TRADE_DIRECTORY = "villager_trade/";

  @GameTest
  public void tradeTablesMatchTheGame(GameTestHelper helper) {
    RegistryAccess registries = helper.getLevel().registryAccess();
    RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
    Registry<TradeSet> tradeSets = registries.lookupOrThrow(Registries.TRADE_SET);
    ResourceManager resources = helper.getLevel().getServer().getResourceManager();
    List<String> mismatches = new ArrayList<>();

    checkProfessions(mismatches);
    checkWanderingTrader(mismatches);
    for (MerchantTrades merchant : VillagerTradeTable.all()) {
      for (TradeTier tier : merchant.tiers()) {
        checkTier(tier, resources, tradeSets, ops, mismatches);
      }
    }
    checkBookLevels(registries.lookupOrThrow(Registries.ENCHANTMENT), mismatches);
    LootTable bartering =
        helper
            .getLevel()
            .getServer()
            .reloadableRegistries()
            .getLootTable(BuiltInLootTables.PIGLIN_BARTERING);
    checkBartering(LootTable.DIRECT_CODEC.encodeStart(ops, bartering).getOrThrow(), mismatches);

    helper.assertTrue(
        mismatches.isEmpty(),
        "trade tables differ from the game:\n" + String.join("\n", mismatches));
    helper.succeed();
  }

  /** Every profession with trades is in the table, with its level trade sets in order. */
  private static void checkProfessions(List<String> mismatches) {
    Set<String> withTrades = new HashSet<>();
    for (VillagerProfession profession : BuiltInRegistries.VILLAGER_PROFESSION) {
      if (profession.tradeSetsByLevel().isEmpty()) {
        continue;
      }
      String id = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession).getPath();
      withTrades.add(id);
      MerchantTrades table = merchant(id);
      if (table == null) {
        mismatches.add(id + ": profession missing from the table");
        continue;
      }
      List<String> gameSets = new ArrayList<>();
      for (int level = 1; level <= profession.tradeSetsByLevel().size(); level++) {
        ResourceKey<TradeSet> key = profession.tradeSetsByLevel().get(level);
        gameSets.add(key == null ? "none" : key.identifier().getPath());
      }
      List<String> tableSets = new ArrayList<>();
      for (TradeTier tier : table.tiers()) {
        tableSets.add(tier.tradeSetId());
      }
      if (!gameSets.equals(tableSets)) {
        mismatches.add(id + ": game trade sets " + gameSets + ", table " + tableSets);
      }
    }
    for (MerchantTrades profession : VillagerTradeTable.PROFESSIONS) {
      if (!withTrades.contains(profession.merchantId())) {
        mismatches.add(profession.merchantId() + ": no such profession with trades");
      }
    }
  }

  private static void checkWanderingTrader(List<String> mismatches) {
    List<String> game =
        List.of(
            TradeSets.WANDERING_TRADER_BUYING.identifier().getPath(),
            TradeSets.WANDERING_TRADER_COMMON.identifier().getPath(),
            TradeSets.WANDERING_TRADER_UNCOMMON.identifier().getPath());
    List<String> table = new ArrayList<>();
    for (TradeTier tier : VillagerTradeTable.WANDERING_TRADER.tiers()) {
      table.add(tier.tradeSetId());
    }
    if (!game.equals(table)) {
      mismatches.add("wandering trader: game trade sets " + game + ", table " + table);
    }
  }

  /**
   * One trade set of the default data: its amount and its trades, read from the vanilla pack's own
   * files (the test server also enables experimental packs, such as the villager trade rebalance,
   * which override tags and trades in the registries). Each trade is decoded with the game's codec,
   * which checks its items and tags exist, and encoded back to compare normalised JSON.
   */
  private static void checkTier(
      TradeTier tier,
      ResourceManager resources,
      Registry<TradeSet> tradeSets,
      RegistryOps<JsonElement> ops,
      List<String> mismatches) {
    String setId = tier.tradeSetId();
    if (!tradeSets.containsKey(Identifier.withDefaultNamespace(setId))) {
      mismatches.add(setId + ": no such trade set");
      return;
    }
    JsonObject setJson = vanillaJson(resources, TRADE_SET_DIRECTORY + setId);
    if (setJson == null) {
      mismatches.add(setId + ": no default trade set file");
      return;
    }
    JsonElement amount = setJson.get("amount");
    if (!amount.isJsonPrimitive() || amount.getAsInt() != tier.amount()) {
      mismatches.add(setId + ": game amount " + amount + ", table " + tier.amount());
    }
    if (setJson.has("allow_duplicates") && setJson.get("allow_duplicates").getAsBoolean()) {
      mismatches.add(setId + ": now allows duplicates");
    }
    String trades = setJson.get("trades").getAsString();
    if (!trades.startsWith(TAG_PREFIX)) {
      mismatches.add(setId + ": trades are not a tag " + trades);
      return;
    }
    List<String> gameIds = new ArrayList<>();
    expandTag(resources, path(trades.substring(TAG_PREFIX.length())), gameIds, mismatches);
    List<String> tableIds = new ArrayList<>();
    for (TradeRow trade : tier.trades()) {
      tableIds.add(trade.id());
    }
    if (!gameIds.equals(tableIds)) {
      mismatches.add(setId + ": game trades " + gameIds + ", table " + tableIds);
      return;
    }
    for (TradeRow trade : tier.trades()) {
      JsonObject file = vanillaJson(resources, TRADE_DIRECTORY + trade.id());
      if (file == null) {
        mismatches.add(trade.id() + ": no default trade file");
        continue;
      }
      VillagerTrade decoded = VillagerTrade.CODEC.parse(ops, file).getOrThrow();
      checkTrade(
          trade,
          VillagerTrade.CODEC.encodeStart(ops, decoded).getOrThrow().getAsJsonObject(),
          mismatches);
    }
  }

  /** Trade ids of a villager trade tag, nested tags expanded in place, as the game orders them. */
  private static void expandTag(
      ResourceManager resources, String tag, List<String> ids, List<String> mismatches) {
    JsonObject json = vanillaJson(resources, TRADE_TAG_DIRECTORY + tag);
    if (json == null) {
      mismatches.add(tag + ": no default trade tag file");
      return;
    }
    for (JsonElement value : json.getAsJsonArray("values")) {
      if (!value.isJsonPrimitive()) {
        mismatches.add(tag + ": unexpected tag entry " + value);
        continue;
      }
      String entry = value.getAsString();
      if (entry.startsWith(TAG_PREFIX)) {
        expandTag(resources, path(entry.substring(TAG_PREFIX.length())), ids, mismatches);
      } else {
        ids.add(path(entry));
      }
    }
  }

  /** The vanilla pack's version of a data file ({@code data/minecraft/<path>.json}), if any. */
  private static @Nullable JsonObject vanillaJson(ResourceManager resources, String path) {
    Identifier file = Identifier.withDefaultNamespace(path + JSON_EXTENSION);
    for (Resource resource : resources.getResourceStack(file)) {
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

  private static void checkTrade(TradeRow trade, JsonObject json, List<String> mismatches) {
    String id = trade.id();
    expect(id, WANTS, cost(json.getAsJsonObject(WANTS)), trade.wants(), mismatches);
    Optional<TradeItem> extra =
        json.has(EXTRA_WANTS)
            ? Optional.of(cost(json.getAsJsonObject(EXTRA_WANTS)))
            : Optional.empty();
    expect(id, EXTRA_WANTS, extra, trade.extraWants(), mismatches);

    JsonObject gives = json.getAsJsonObject(GIVES);
    if (gives.has(COMPONENTS)) {
      mismatches.add(id + ": gives an item with components " + gives.get(COMPONENTS));
    }
    TradeKind kind = TradeKind.PLAIN;
    Optional<String> givenPotion = Optional.empty();
    for (JsonObject modifier : modifiers(json.get(MODIFIER))) {
      String type = modifier.get(TYPE).getAsString();
      switch (type) {
        case FILTERED -> {
          // Discards failed results; does not change the item.
        }
        case "minecraft:enchant_with_levels" -> {
          kind = TradeKind.ENCHANTED_ITEM;
          CountRange levels = range(modifier.get("levels"));
          if (!TradePrices.ENCHANT_LEVELS.equals(levels)) {
            mismatches.add(id + ": enchantment levels " + levels);
          }
        }
        case ENCHANT_RANDOMLY -> {
          kind = TradeKind.ENCHANTED_BOOK;
          if (!TRADEABLE_TAG.equals(modifier.get("options").getAsString())
              || !json.has("double_trade_price_enchantments")) {
            mismatches.add(id + ": book enchantments changed " + modifier);
          }
        }
        case "minecraft:set_random_dyes" -> kind = TradeKind.DYED_ARMOR;
        case "minecraft:set_random_potion" -> kind = TradeKind.RANDOM_POTION;
        case "minecraft:set_stew_effect" -> kind = TradeKind.SUSPICIOUS_STEW;
        case "minecraft:exploration_map" -> kind = TradeKind.EXPLORER_MAP;
        case SET_POTION -> {
          kind = TradeKind.POTION;
          givenPotion = Optional.of(path(modifier.get(ID).getAsString()));
        }
        default -> mismatches.add(id + ": unknown modifier " + type);
      }
    }
    TradeItem given =
        new TradeItem(
            path(gives.get(ID).getAsString()),
            gives.has(COUNT) ? gives.get(COUNT).getAsInt() : 1,
            givenPotion);
    expect(id, GIVES, given, trade.gives(), mismatches);
    expect(id, "kind", kind, trade.kind(), mismatches);
    expect(
        id,
        PREDICATE,
        villagerTypes(id, json.get(PREDICATE), mismatches),
        Set.copyOf(trade.villagerTypes()),
        mismatches);
  }

  /** A trade cost: item, constant count (default 1) and an optional potion. */
  private static TradeItem cost(JsonObject json) {
    String item = path(json.get(ID).getAsString());
    int count = json.has(COUNT) ? json.get(COUNT).getAsInt() : 1;
    Optional<String> potion = Optional.empty();
    if (json.has(COMPONENTS)) {
      JsonObject components = json.getAsJsonObject(COMPONENTS);
      potion =
          Optional.of(path(components.getAsJsonObject(POTION_CONTENTS).get(POTION).getAsString()));
    }
    return new TradeItem(item, count, potion);
  }

  private static Set<String> villagerTypes(
      String id, @Nullable JsonElement predicate, List<String> mismatches) {
    if (predicate == null) {
      return Set.of();
    }
    JsonObject json = predicate.getAsJsonObject();
    if (!ENTITY_PROPERTIES.equals(json.get(TYPE).getAsString())) {
      mismatches.add(id + ": unknown merchant predicate " + json);
      return Set.of();
    }
    JsonElement variants =
        json.getAsJsonObject("predicate")
            .getAsJsonObject("minecraft:predicates")
            .get(VILLAGER_VARIANT);
    Set<String> types = new HashSet<>();
    if (variants.isJsonArray()) {
      for (JsonElement variant : variants.getAsJsonArray()) {
        types.add(path(variant.getAsString()));
      }
    } else {
      types.add(path(variants.getAsString()));
    }
    return types;
  }

  /** The tradeable enchantments' highest level, which bounds the book price. */
  private static void checkBookLevels(Registry<Enchantment> enchantments, List<String> mismatches) {
    int highest = 0;
    for (Holder<Enchantment> enchantment : enchantments.getTagOrEmpty(EnchantmentTags.TRADEABLE)) {
      highest = Math.max(highest, enchantment.value().getMaxLevel());
    }
    expect("tradeable books", "max level", highest, TradePrices.MAX_BOOK_LEVEL, mismatches);
  }

  private static void checkBartering(JsonElement table, List<String> mismatches) {
    JsonArray pools = table.getAsJsonObject().getAsJsonArray("pools");
    if (pools.size() != 1) {
      mismatches.add("bartering: " + pools.size() + " pools");
      return;
    }
    JsonObject pool = pools.get(0).getAsJsonObject();
    if (pool.get("rolls").getAsInt() != 1) {
      mismatches.add("bartering: rolls " + pool.get("rolls"));
    }
    JsonArray entries = pool.getAsJsonArray("entries");
    List<BarterRow> game = new ArrayList<>();
    for (JsonElement element : entries) {
      JsonObject entry = element.getAsJsonObject();
      CountRange count = CountRange.exactly(1);
      Optional<String> potion = Optional.empty();
      Optional<String> enchantment = Optional.empty();
      for (JsonObject modifier : modifiers(entry.get("modifier"))) {
        String type = modifier.get(TYPE).getAsString();
        switch (type) {
          case SET_COUNT -> count = range(modifier.get(COUNT));
          case SET_POTION -> potion = Optional.of(path(modifier.get(ID).getAsString()));
          case ENCHANT_RANDOMLY ->
              enchantment = Optional.of(path(modifier.get("options").getAsString()));
          default -> mismatches.add("bartering: unknown modifier " + type);
        }
      }
      game.add(
          new BarterRow(
              path(entry.get("name").getAsString()),
              count,
              entry.has("weight") ? entry.get("weight").getAsInt() : 1,
              potion,
              enchantment));
    }
    if (game.size() != BarterTable.ROWS.size()) {
      mismatches.add("bartering: game has " + game.size() + " entries: " + game);
      return;
    }
    for (int index = 0; index < game.size(); index++) {
      expect(
          "bartering", "entry " + index, game.get(index), BarterTable.ROWS.get(index), mismatches);
    }
  }

  /** A modifier field: absent, one object or a list. */
  private static List<JsonObject> modifiers(@Nullable JsonElement element) {
    List<JsonObject> modifiers = new ArrayList<>();
    if (element == null) {
      return modifiers;
    }
    if (element.isJsonArray()) {
      for (JsonElement modifier : element.getAsJsonArray()) {
        modifiers.add(modifier.getAsJsonObject());
      }
    } else {
      modifiers.add(element.getAsJsonObject());
    }
    return modifiers;
  }

  /** A constant or a {@code uniform} number provider. */
  private static CountRange range(JsonElement element) {
    if (element.isJsonPrimitive()) {
      return CountRange.exactly(element.getAsInt());
    }
    JsonObject provider = element.getAsJsonObject();
    if (!UNIFORM.equals(provider.get(TYPE).getAsString())) {
      throw new IllegalStateException("unexpected number provider " + provider);
    }
    return new CountRange(provider.get("min").getAsInt(), provider.get("max").getAsInt());
  }

  private static <T> void expect(
      String id, String field, T game, T table, List<String> mismatches) {
    if (!game.equals(table)) {
      mismatches.add(id + ": game " + field + " " + game + ", table " + table);
    }
  }

  private static String path(String id) {
    return id.startsWith(NAMESPACE) ? id.substring(NAMESPACE.length()) : id;
  }

  private static @Nullable MerchantTrades merchant(String merchantId) {
    for (MerchantTrades merchant : VillagerTradeTable.PROFESSIONS) {
      if (merchant.merchantId().equals(merchantId)) {
        return merchant;
      }
    }
    return null;
  }
}
