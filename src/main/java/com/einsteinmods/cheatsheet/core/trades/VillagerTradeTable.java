package com.einsteinmods.cheatsheet.core.trades;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Vanilla villager and wandering trader trades of Minecraft 26.3, default (non-experimental) data:
 * trade sets ({@code data/minecraft/trade_set}), their trade tags ({@code
 * data/minecraft/tags/villager_trade}) and the trades ({@code data/minecraft/villager_trade}).
 * Trades are server data, never sent to the client, so the client reads this fixed copy; {@code
 * VillagerTradeGameTest} compares it with the game's registries and fails the build when an update
 * changes a trade.
 */
public final class VillagerTradeTable {
  /** Villager professions with trades, in id order; each has levels 1 to 5 in order. */
  public static final List<MerchantTrades> PROFESSIONS =
      List.of(
          merchant(
              "armorer",
              tier(
                  "armorer/level_1",
                  2,
                  trade("smith/1/coal_emerald", "coal", 15, "emerald", 1),
                  trade("armorer/1/emerald_iron_leggings", "emerald", 7, "iron_leggings", 1),
                  trade("armorer/1/emerald_iron_boots", "emerald", 4, "iron_boots", 1),
                  trade("armorer/1/emerald_iron_helmet", "emerald", 5, "iron_helmet", 1),
                  trade("armorer/1/emerald_iron_chestplate", "emerald", 9, "iron_chestplate", 1)),
              tier(
                  "armorer/level_2",
                  2,
                  trade("smith/2/iron_ingot_emerald", "iron_ingot", 4, "emerald", 1),
                  trade("smith/2/emerald_bell", "emerald", 36, "bell", 1),
                  trade("armorer/2/emerald_chainmail_boots", "emerald", 1, "chainmail_boots", 1),
                  trade(
                      "armorer/2/emerald_chainmail_leggings",
                      "emerald",
                      3,
                      "chainmail_leggings",
                      1)),
              tier(
                  "armorer/level_3",
                  2,
                  trade("armorer/3/lava_bucket_emerald", "lava_bucket", 1, "emerald", 1),
                  trade("armorer/3/emerald_chainmail_helmet", "emerald", 1, "chainmail_helmet", 1),
                  trade(
                      "armorer/3/emerald_chainmail_chestplate",
                      "emerald",
                      4,
                      "chainmail_chestplate",
                      1),
                  trade("armorer/3/emerald_shield", "emerald", 5, "shield", 1),
                  trade("armorer/3/diamond_emerald", "diamond", 1, "emerald", 1)),
              tier(
                  "armorer/level_4",
                  2,
                  trade(
                          "armorer/4/emerald_enchanted_diamond_leggings",
                          "emerald",
                          14,
                          "diamond_leggings",
                          1)
                      .as(TradeKind.ENCHANTED_ITEM),
                  trade(
                          "armorer/4/emerald_enchanted_diamond_boots",
                          "emerald",
                          8,
                          "diamond_boots",
                          1)
                      .as(TradeKind.ENCHANTED_ITEM)),
              tier(
                  "armorer/level_5",
                  2,
                  trade(
                          "armorer/5/emerald_enchanted_diamond_helmet",
                          "emerald",
                          8,
                          "diamond_helmet",
                          1)
                      .as(TradeKind.ENCHANTED_ITEM),
                  trade(
                          "armorer/5/emerald_enchanted_diamond_chestplate",
                          "emerald",
                          16,
                          "diamond_chestplate",
                          1)
                      .as(TradeKind.ENCHANTED_ITEM))),
          merchant(
              "butcher",
              tier(
                  "butcher/level_1",
                  2,
                  trade("butcher/1/chicken_emerald", "chicken", 14, "emerald", 1),
                  trade("butcher/1/porkchop_emerald", "porkchop", 7, "emerald", 1),
                  trade("butcher/1/rabbit_emerald", "rabbit", 4, "emerald", 1),
                  trade("butcher/1/emerald_rabbit_stew", "emerald", 1, "rabbit_stew", 1)),
              tier(
                  "butcher/level_2",
                  2,
                  trade("butcher/2/coal_emerald", "coal", 15, "emerald", 1),
                  trade("butcher/2/emerald_cooked_porkchop", "emerald", 1, "cooked_porkchop", 5),
                  trade("butcher/2/emerald_cooked_chicken", "emerald", 1, "cooked_chicken", 8)),
              tier(
                  "butcher/level_3",
                  2,
                  trade("butcher/3/mutton_emerald", "mutton", 7, "emerald", 1),
                  trade("butcher/3/beef_emerald", "beef", 10, "emerald", 1)),
              tier(
                  "butcher/level_4",
                  2,
                  trade(
                      "butcher/4/dried_kelp_block_emerald", "dried_kelp_block", 10, "emerald", 1)),
              tier(
                  "butcher/level_5",
                  2,
                  trade("butcher/5/sweet_berries_emerald", "sweet_berries", 10, "emerald", 1))),
          merchant(
              "cartographer",
              tier(
                  "cartographer/level_1",
                  2,
                  trade("cartographer/1/paper_emerald", "paper", 24, "emerald", 1),
                  trade("cartographer/1/emerald_map", "emerald", 7, "map", 1)),
              tier(
                  "cartographer/level_2",
                  2,
                  trade("cartographer/2/glass_pane_emerald", "glass_pane", 11, "emerald", 1),
                  trade(
                          "cartographer/2/emerald_and_compass_village_taiga_map",
                          "emerald",
                          8,
                          "compass",
                          1,
                          "taiga_village_map",
                          1)
                      .as(TradeKind.EXPLORER_MAP)
                      .onlyFrom("swamp", "snow", "plains"),
                  trade(
                          "cartographer/2/emerald_and_compass_explorer_swamp_map",
                          "emerald",
                          8,
                          "compass",
                          1,
                          "swamp_hut_map",
                          1)
                      .as(TradeKind.EXPLORER_MAP)
                      .onlyFrom("taiga", "snow", "jungle"),
                  trade(
                          "cartographer/2/emerald_and_compass_village_snowy_map",
                          "emerald",
                          8,
                          "compass",
                          1,
                          "snowy_village_map",
                          1)
                      .as(TradeKind.EXPLORER_MAP)
                      .onlyFrom("taiga", "swamp"),
                  trade(
                          "cartographer/2/emerald_and_compass_village_savanna_map",
                          "emerald",
                          8,
                          "compass",
                          1,
                          "savanna_village_map",
                          1)
                      .as(TradeKind.EXPLORER_MAP)
                      .onlyFrom("plains", "jungle", "desert"),
                  trade(
                          "cartographer/2/emerald_and_compass_village_plains_map",
                          "emerald",
                          8,
                          "compass",
                          1,
                          "plains_village_map",
                          1)
                      .as(TradeKind.EXPLORER_MAP)
                      .onlyFrom("taiga", "snow", "savanna", "desert"),
                  trade(
                          "cartographer/2/emerald_and_compass_explorer_jungle_map",
                          "emerald",
                          8,
                          "compass",
                          1,
                          "jungle_pyramid_map",
                          1)
                      .as(TradeKind.EXPLORER_MAP)
                      .onlyFrom("swamp", "savanna", "desert"),
                  trade(
                          "cartographer/2/emerald_and_compass_village_desert_map",
                          "emerald",
                          8,
                          "compass",
                          1,
                          "desert_village_map",
                          1)
                      .as(TradeKind.EXPLORER_MAP)
                      .onlyFrom("savanna", "jungle")),
              tier(
                  "cartographer/level_3",
                  2,
                  trade("cartographer/3/compass_emerald", "compass", 1, "emerald", 1),
                  trade(
                          "cartographer/3/emerald_and_compass_ocean_monument_map",
                          "emerald",
                          13,
                          "compass",
                          1,
                          "ocean_monument_map",
                          1)
                      .as(TradeKind.EXPLORER_MAP),
                  trade(
                          "cartographer/3/emerald_and_compass_buried_trial_chambers_map",
                          "emerald",
                          12,
                          "compass",
                          1,
                          "buried_trial_chambers_map",
                          1)
                      .as(TradeKind.EXPLORER_MAP)),
              tier(
                  "cartographer/level_4",
                  2,
                  trade("cartographer/4/emerald_item_frame", "emerald", 7, "item_frame", 1),
                  trade("cartographer/4/emerald_white_banner", "emerald", 2, "white_banner", 1)
                      .onlyFrom("snow", "plains"),
                  trade("cartographer/4/emerald_orange_banner", "emerald", 2, "orange_banner", 1)
                      .onlyFrom("savanna", "desert"),
                  trade("cartographer/4/emerald_magenta_banner", "emerald", 2, "magenta_banner", 1)
                      .onlyFrom("savanna"),
                  trade("cartographer/4/emerald_blue_banner", "emerald", 2, "blue_banner", 1)
                      .onlyFrom("snow", "taiga"),
                  trade(
                          "cartographer/4/emerald_light_blue_banner",
                          "emerald",
                          2,
                          "light_blue_banner",
                          1)
                      .onlyFrom("snow", "swamp"),
                  trade("cartographer/4/emerald_yellow_banner", "emerald", 2, "yellow_banner", 1)
                      .onlyFrom("plains", "jungle"),
                  trade("cartographer/4/emerald_lime_banner", "emerald", 2, "lime_banner", 1)
                      .onlyFrom("desert", "taiga"),
                  trade("cartographer/4/emerald_pink_banner", "emerald", 2, "pink_banner", 1)
                      .onlyFrom("taiga", "plains"),
                  trade("cartographer/4/emerald_gray_banner", "emerald", 2, "gray_banner", 1)
                      .onlyFrom("desert"),
                  trade("cartographer/4/emerald_cyan_banner", "emerald", 2, "cyan_banner", 1)
                      .onlyFrom("desert", "snow"),
                  trade("cartographer/4/emerald_purple_banner", "emerald", 2, "purple_banner", 1)
                      .onlyFrom("taiga", "swamp"),
                  trade("cartographer/4/emerald_brown_banner", "emerald", 2, "brown_banner", 1)
                      .onlyFrom("plains", "jungle"),
                  trade("cartographer/4/emerald_green_banner", "emerald", 2, "green_banner", 1)
                      .onlyFrom("desert", "savanna", "jungle"),
                  trade("cartographer/4/emerald_red_banner", "emerald", 2, "red_banner", 1)
                      .onlyFrom("snow", "savanna"),
                  trade("cartographer/4/emerald_black_banner", "emerald", 2, "black_banner", 1)
                      .onlyFrom("swamp")),
              tier(
                  "cartographer/level_5",
                  2,
                  trade(
                      "cartographer/5/emerald_globe_banner_pattern",
                      "emerald",
                      8,
                      "globe_banner_pattern",
                      1),
                  trade(
                          "cartographer/5/emerald_and_compass_woodland_mansion_map",
                          "emerald",
                          14,
                          "compass",
                          1,
                          "woodland_mansion_map",
                          1)
                      .as(TradeKind.EXPLORER_MAP))),
          merchant(
              "cleric",
              tier(
                  "cleric/level_1",
                  2,
                  trade("cleric/1/rotten_flesh_emerald", "rotten_flesh", 32, "emerald", 1),
                  trade("cleric/1/emerald_redstone", "emerald", 1, "redstone", 2)),
              tier(
                  "cleric/level_2",
                  2,
                  trade("cleric/2/gold_ingot_emerald", "gold_ingot", 3, "emerald", 1),
                  trade("cleric/2/emerald_lapis_lazuli", "emerald", 1, "lapis_lazuli", 1)),
              tier(
                  "cleric/level_3",
                  2,
                  trade("cleric/3/rabbit_foot_emerald", "rabbit_foot", 2, "emerald", 1),
                  trade("cleric/3/emerald_glowstone", "emerald", 4, "glowstone", 1)),
              tier(
                  "cleric/level_4",
                  2,
                  trade("cleric/4/turtle_scute_emerald", "turtle_scute", 4, "emerald", 1),
                  trade("cleric/4/glass_bottle_emerald", "glass_bottle", 9, "emerald", 1),
                  trade("cleric/4/emerald_ender_pearl", "emerald", 5, "ender_pearl", 1)),
              tier(
                  "cleric/level_5",
                  2,
                  trade("cleric/5/nether_wart_emerald", "nether_wart", 22, "emerald", 1),
                  trade(
                      "cleric/5/emerald_experience_bottle", "emerald", 3, "experience_bottle", 1))),
          merchant(
              "farmer",
              tier(
                  "farmer/level_1",
                  2,
                  trade("farmer/1/wheat_emerald", "wheat", 20, "emerald", 1),
                  trade("farmer/1/potato_emerald", "potato", 26, "emerald", 1),
                  trade("farmer/1/carrot_emerald", "carrot", 22, "emerald", 1),
                  trade("farmer/1/beetroot_emerald", "beetroot", 15, "emerald", 1),
                  trade("farmer/1/emerald_bread", "emerald", 1, "bread", 6)),
              tier(
                  "farmer/level_2",
                  2,
                  trade("farmer/2/pumpkin_emerald", "pumpkin", 6, "emerald", 1),
                  trade("farmer/2/emerald_pumpkin_pie", "emerald", 1, "pumpkin_pie", 4),
                  trade("farmer/2/emerald_apple", "emerald", 1, "apple", 4)),
              tier(
                  "farmer/level_3",
                  2,
                  trade("farmer/3/emerald_cookie", "emerald", 3, "cookie", 18),
                  trade("farmer/3/melon_emerald", "melon", 4, "emerald", 1)),
              tier(
                  "farmer/level_4",
                  2,
                  trade("farmer/4/emerald_cake", "emerald", 3, "cake", 1),
                  trade("farmer/4/emerald_suspicious_stew", "emerald", 1, "suspicious_stew", 1)
                      .as(TradeKind.SUSPICIOUS_STEW)),
              tier(
                  "farmer/level_5",
                  2,
                  trade("farmer/5/emerald_golden_carrot", "emerald", 3, "golden_carrot", 3),
                  trade(
                      "farmer/5/emerald_glistening_melon_slice",
                      "emerald",
                      4,
                      "glistering_melon_slice",
                      3))),
          merchant(
              "fisherman",
              tier(
                  "fisherman/level_1",
                  2,
                  trade("fisherman/1/string_emerald", "string", 20, "emerald", 1),
                  trade("fisherman/1/coal_emerald", "coal", 10, "emerald", 1),
                  trade(
                      "fisherman/1/raw_cod_and_emerald_cooked_cod",
                      "cod",
                      6,
                      "emerald",
                      1,
                      "cooked_cod",
                      6),
                  trade("fisherman/1/emerald_cod_bucket", "emerald", 3, "cod_bucket", 1)),
              tier(
                  "fisherman/level_2",
                  2,
                  trade("fisherman/2/cod_emerald", "cod", 15, "emerald", 1),
                  trade(
                      "fisherman/2/salmon_and_emerald_cooked_salmon",
                      "salmon",
                      6,
                      "emerald",
                      1,
                      "cooked_salmon",
                      6),
                  trade("fisherman/2/emerald_campfire", "emerald", 2, "campfire", 1)),
              tier(
                  "fisherman/level_3",
                  2,
                  trade("fisherman/3/salmon_emerald", "salmon", 13, "emerald", 1),
                  trade("fisherman/3/emerald_enchanted_fishing_rod", "emerald", 3, "fishing_rod", 1)
                      .as(TradeKind.ENCHANTED_ITEM)),
              tier(
                  "fisherman/level_4",
                  2,
                  trade("fisherman/4/tropical_fish_emerald", "tropical_fish", 6, "emerald", 1)),
              tier(
                  "fisherman/level_5",
                  2,
                  trade("fisherman/5/pufferfish_emerald", "pufferfish", 4, "emerald", 1),
                  trade("fisherman/5/oak_boat_emerald", "oak_boat", 1, "emerald", 1)
                      .onlyFrom("plains"),
                  trade("fisherman/5/spruce_boat_emerald", "spruce_boat", 1, "emerald", 1)
                      .onlyFrom("taiga", "snow"),
                  trade("fisherman/5/jungle_boat_emerald", "jungle_boat", 1, "emerald", 1)
                      .onlyFrom("desert", "jungle"),
                  trade("fisherman/5/acacia_boat_emerald", "acacia_boat", 1, "emerald", 1)
                      .onlyFrom("savanna"),
                  trade("fisherman/5/dark_oak_boat_emerald", "dark_oak_boat", 1, "emerald", 1)
                      .onlyFrom("swamp"))),
          merchant(
              "fletcher",
              tier(
                  "fletcher/level_1",
                  2,
                  trade("fletcher/1/stick_emerald", "stick", 32, "emerald", 1),
                  trade("fletcher/1/emerald_arrow", "emerald", 1, "arrow", 16),
                  trade(
                      "fletcher/1/gravel_and_emerald_flint",
                      "gravel",
                      10,
                      "emerald",
                      1,
                      "flint",
                      10)),
              tier(
                  "fletcher/level_2",
                  2,
                  trade("fletcher/2/flint_emerald", "flint", 26, "emerald", 1),
                  trade("fletcher/2/emerald_bow", "emerald", 2, "bow", 1)),
              tier(
                  "fletcher/level_3",
                  2,
                  trade("fletcher/3/string_emerald", "string", 14, "emerald", 1),
                  trade("fletcher/3/emerald_crossbow", "emerald", 3, "crossbow", 1)),
              tier(
                  "fletcher/level_4",
                  2,
                  trade("fletcher/4/feather_emerald", "feather", 24, "emerald", 1),
                  trade("fletcher/4/emerald_enchanted_bow", "emerald", 2, "bow", 1)
                      .as(TradeKind.ENCHANTED_ITEM)),
              tier(
                  "fletcher/level_5",
                  2,
                  trade("fletcher/5/tripwire_hook_emerald", "tripwire_hook", 8, "emerald", 1),
                  trade("fletcher/5/emerald_enchanted_crossbow", "emerald", 3, "crossbow", 1)
                      .as(TradeKind.ENCHANTED_ITEM),
                  trade(
                          "fletcher/5/arrow_and_emerald_tipped_arrow",
                          "emerald",
                          2,
                          "arrow",
                          5,
                          "tipped_arrow",
                          5)
                      .as(TradeKind.RANDOM_POTION))),
          merchant(
              "leatherworker",
              tier(
                  "leatherworker/level_1",
                  2,
                  trade("leatherworker/1/leather_emerald", "leather", 6, "emerald", 1),
                  trade(
                          "leatherworker/1/emerald_dyed_leather_leggings",
                          "emerald",
                          3,
                          "leather_leggings",
                          1)
                      .as(TradeKind.DYED_ARMOR),
                  trade(
                          "leatherworker/1/emerald_dyed_leather_chestplate",
                          "emerald",
                          7,
                          "leather_chestplate",
                          1)
                      .as(TradeKind.DYED_ARMOR)),
              tier(
                  "leatherworker/level_2",
                  2,
                  trade("leatherworker/2/flint_emerald", "flint", 26, "emerald", 1),
                  trade(
                          "leatherworker/2/emerald_dyed_leather_helmet",
                          "emerald",
                          5,
                          "leather_helmet",
                          1)
                      .as(TradeKind.DYED_ARMOR),
                  trade(
                          "leatherworker/2/emerald_dyed_leather_boots",
                          "emerald",
                          4,
                          "leather_boots",
                          1)
                      .as(TradeKind.DYED_ARMOR)),
              tier(
                  "leatherworker/level_3",
                  2,
                  trade("leatherworker/3/rabbit_hide_emerald", "rabbit_hide", 9, "emerald", 1),
                  trade(
                          "leatherworker/3/emerald_dyed_leather_chestplate",
                          "emerald",
                          7,
                          "leather_chestplate",
                          1)
                      .as(TradeKind.DYED_ARMOR)),
              tier(
                  "leatherworker/level_4",
                  2,
                  trade("leatherworker/4/turtle_scute_emerald", "turtle_scute", 4, "emerald", 1),
                  trade(
                          "leatherworker/4/emerald_dyed_leather_horse_armor",
                          "emerald",
                          6,
                          "leather_horse_armor",
                          1)
                      .as(TradeKind.DYED_ARMOR)),
              tier(
                  "leatherworker/level_5",
                  2,
                  trade("leatherworker/5/emerald_saddle", "emerald", 6, "saddle", 1),
                  trade(
                          "leatherworker/5/emerald_dyed_leather_helmet",
                          "emerald",
                          5,
                          "leather_helmet",
                          1)
                      .as(TradeKind.DYED_ARMOR))),
          merchant(
              "librarian",
              tier(
                  "librarian/level_1",
                  2,
                  trade("librarian/1/paper_emerald", "paper", 24, "emerald", 1),
                  trade(
                          "librarian/1/emerald_and_book_enchanted_book",
                          "emerald",
                          0,
                          "book",
                          1,
                          "enchanted_book",
                          1)
                      .as(TradeKind.ENCHANTED_BOOK),
                  trade("librarian/1/emerald_bookshelf", "emerald", 9, "bookshelf", 1)),
              tier(
                  "librarian/level_2",
                  2,
                  trade("librarian/2/book_emerald", "book", 4, "emerald", 1),
                  trade(
                          "librarian/2/emerald_and_book_enchanted_book",
                          "emerald",
                          0,
                          "book",
                          1,
                          "enchanted_book",
                          1)
                      .as(TradeKind.ENCHANTED_BOOK),
                  trade("librarian/2/emerald_lantern", "emerald", 1, "lantern", 1)),
              tier(
                  "librarian/level_3",
                  2,
                  trade("librarian/3/ink_sac_emerald", "ink_sac", 5, "emerald", 1),
                  trade(
                          "librarian/3/emerald_and_book_enchanted_book",
                          "emerald",
                          0,
                          "book",
                          1,
                          "enchanted_book",
                          1)
                      .as(TradeKind.ENCHANTED_BOOK),
                  trade("librarian/3/emerald_glass", "emerald", 1, "glass", 4)),
              tier(
                  "librarian/level_4",
                  2,
                  trade("librarian/4/writable_book_emerald", "writable_book", 2, "emerald", 1),
                  trade(
                          "librarian/4/emerald_book_and_enchanted_book",
                          "emerald",
                          0,
                          "book",
                          1,
                          "enchanted_book",
                          1)
                      .as(TradeKind.ENCHANTED_BOOK),
                  trade("librarian/4/emerald_clock", "emerald", 5, "clock", 1),
                  trade("librarian/4/emerald_compass", "emerald", 4, "compass", 1)),
              tier(
                  "librarian/level_5",
                  3,
                  trade("librarian/5/emerald_yellow_candle", "emerald", 3, "yellow_candle", 1),
                  trade("librarian/5/emerald_red_candle", "emerald", 3, "red_candle", 1))),
          merchant(
              "mason",
              tier(
                  "mason/level_1",
                  2,
                  trade("mason/1/clay_ball_emerald", "clay_ball", 10, "emerald", 1),
                  trade("mason/1/emerald_brick", "emerald", 1, "brick", 10)),
              tier(
                  "mason/level_2",
                  2,
                  trade("mason/2/stone_emerald", "stone", 20, "emerald", 1),
                  trade(
                      "mason/2/emerald_chiseled_stone_bricks",
                      "emerald",
                      1,
                      "chiseled_stone_bricks",
                      4)),
              tier(
                  "mason/level_3",
                  2,
                  trade("mason/3/granite_emerald", "granite", 16, "emerald", 1),
                  trade("mason/3/andesite_emerald", "andesite", 16, "emerald", 1),
                  trade("mason/3/diorite_emerald", "diorite", 16, "emerald", 1),
                  trade("mason/3/emerald_dripstone_block", "emerald", 1, "dripstone_block", 4),
                  trade("mason/3/emerald_polished_andesite", "emerald", 1, "polished_andesite", 4),
                  trade("mason/3/emerald_polished_diorite", "emerald", 1, "polished_diorite", 4),
                  trade("mason/3/emerald_polished_granite", "emerald", 1, "polished_granite", 4)),
              tier(
                  "mason/level_4",
                  2,
                  trade("mason/4/quartz_emerald", "quartz", 12, "emerald", 1),
                  trade("mason/4/emerald_white_terracotta", "emerald", 1, "white_terracotta", 1),
                  trade("mason/4/emerald_orange_terracotta", "emerald", 1, "orange_terracotta", 1),
                  trade(
                      "mason/4/emerald_magenta_terracotta", "emerald", 1, "magenta_terracotta", 1),
                  trade(
                      "mason/4/emerald_light_blue_terracotta",
                      "emerald",
                      1,
                      "light_blue_terracotta",
                      1),
                  trade("mason/4/emerald_yellow_terracotta", "emerald", 1, "yellow_terracotta", 1),
                  trade("mason/4/emerald_lime_terracotta", "emerald", 1, "lime_terracotta", 1),
                  trade("mason/4/emerald_pink_terracotta", "emerald", 1, "pink_terracotta", 1),
                  trade("mason/4/emerald_gray_terracotta", "emerald", 1, "gray_terracotta", 1),
                  trade(
                      "mason/4/emerald_light_gray_terracotta",
                      "emerald",
                      1,
                      "light_gray_terracotta",
                      1),
                  trade("mason/4/emerald_cyan_terracotta", "emerald", 1, "cyan_terracotta", 1),
                  trade("mason/4/emerald_purple_terracotta", "emerald", 1, "purple_terracotta", 1),
                  trade("mason/4/emerald_blue_terracotta", "emerald", 1, "blue_terracotta", 1),
                  trade("mason/4/emerald_brown_terracotta", "emerald", 1, "brown_terracotta", 1),
                  trade("mason/4/emerald_green_terracotta", "emerald", 1, "green_terracotta", 1),
                  trade("mason/4/emerald_red_terracotta", "emerald", 1, "red_terracotta", 1),
                  trade("mason/4/emerald_black_terracotta", "emerald", 1, "black_terracotta", 1),
                  trade(
                      "mason/4/emerald_white_glazed_terracotta",
                      "emerald",
                      1,
                      "white_glazed_terracotta",
                      1),
                  trade(
                      "mason/4/emerald_orange_glazed_terracotta",
                      "emerald",
                      1,
                      "orange_glazed_terracotta",
                      1),
                  trade(
                      "mason/4/emerald_magenta_glazed_terracotta",
                      "emerald",
                      1,
                      "magenta_glazed_terracotta",
                      1),
                  trade(
                      "mason/4/emerald_light_blue_glazed_terracotta",
                      "emerald",
                      1,
                      "light_blue_glazed_terracotta",
                      1),
                  trade(
                      "mason/4/emerald_yellow_glazed_terracotta",
                      "emerald",
                      1,
                      "yellow_glazed_terracotta",
                      1),
                  trade(
                      "mason/4/emerald_lime_glazed_terracotta",
                      "emerald",
                      1,
                      "lime_glazed_terracotta",
                      1),
                  trade(
                      "mason/4/emerald_pink_glazed_terracotta",
                      "emerald",
                      1,
                      "pink_glazed_terracotta",
                      1),
                  trade(
                      "mason/4/emerald_gray_glazed_terracotta",
                      "emerald",
                      1,
                      "gray_glazed_terracotta",
                      1),
                  trade(
                      "mason/4/emerald_light_gray_glazed_terracotta",
                      "emerald",
                      1,
                      "light_gray_glazed_terracotta",
                      1),
                  trade(
                      "mason/4/emerald_cyan_glazed_terracotta",
                      "emerald",
                      1,
                      "cyan_glazed_terracotta",
                      1),
                  trade(
                      "mason/4/emerald_purple_glazed_terracotta",
                      "emerald",
                      1,
                      "purple_glazed_terracotta",
                      1),
                  trade(
                      "mason/4/emerald_blue_glazed_terracotta",
                      "emerald",
                      1,
                      "blue_glazed_terracotta",
                      1),
                  trade(
                      "mason/4/emerald_brown_glazed_terracotta",
                      "emerald",
                      1,
                      "brown_glazed_terracotta",
                      1),
                  trade(
                      "mason/4/emerald_green_glazed_terracotta",
                      "emerald",
                      1,
                      "green_glazed_terracotta",
                      1),
                  trade(
                      "mason/4/emerald_red_glazed_terracotta",
                      "emerald",
                      1,
                      "red_glazed_terracotta",
                      1),
                  trade(
                      "mason/4/emerald_black_glazed_terracotta",
                      "emerald",
                      1,
                      "black_glazed_terracotta",
                      1)),
              tier(
                  "mason/level_5",
                  2,
                  trade("mason/5/emerald_quartz_pillar", "emerald", 1, "quartz_pillar", 1),
                  trade("mason/5/emerald_quartz_block", "emerald", 1, "quartz_block", 1))),
          merchant(
              "shepherd",
              tier(
                  "shepherd/level_1",
                  2,
                  trade("shepherd/1/white_wool_emerald", "white_wool", 18, "emerald", 1),
                  trade("shepherd/1/brown_wool_emerald", "brown_wool", 18, "emerald", 1),
                  trade("shepherd/1/gray_wool_emerald", "gray_wool", 18, "emerald", 1),
                  trade("shepherd/1/black_wool_emerald", "black_wool", 18, "emerald", 1),
                  trade("shepherd/1/emerald_shears", "emerald", 2, "shears", 1)),
              tier(
                  "shepherd/level_2",
                  2,
                  trade("shepherd/2/white_dye_emerald", "white_dye", 12, "emerald", 1),
                  trade("shepherd/2/gray_dye_emerald", "gray_dye", 12, "emerald", 1),
                  trade("shepherd/2/black_dye_emerald", "black_dye", 12, "emerald", 1),
                  trade("shepherd/2/light_blue_dye_emerald", "light_blue_dye", 12, "emerald", 1),
                  trade("shepherd/2/lime_dye_emerald", "lime_dye", 12, "emerald", 1),
                  trade("shepherd/2/emerald_white_wool", "emerald", 1, "white_wool", 1),
                  trade("shepherd/2/emerald_orange_wool", "emerald", 1, "orange_wool", 1),
                  trade("shepherd/2/emerald_magenta_wool", "emerald", 1, "magenta_wool", 1),
                  trade("shepherd/2/emerald_light_blue_wool", "emerald", 1, "light_blue_wool", 1),
                  trade("shepherd/2/emerald_yellow_wool", "emerald", 1, "yellow_wool", 1),
                  trade("shepherd/2/emerald_lime_wool", "emerald", 1, "lime_wool", 1),
                  trade("shepherd/2/emerald_pink_wool", "emerald", 1, "pink_wool", 1),
                  trade("shepherd/2/emerald_gray_wool", "emerald", 1, "gray_wool", 1),
                  trade("shepherd/2/emerald_light_gray_wool", "emerald", 1, "light_gray_wool", 1),
                  trade("shepherd/2/emerald_cyan_wool", "emerald", 1, "cyan_wool", 1),
                  trade("shepherd/2/emerald_purple_wool", "emerald", 1, "purple_wool", 1),
                  trade("shepherd/2/emerald_blue_wool", "emerald", 1, "blue_wool", 1),
                  trade("shepherd/2/emerald_brown_wool", "emerald", 1, "brown_wool", 1),
                  trade("shepherd/2/emerald_green_wool", "emerald", 1, "green_wool", 1),
                  trade("shepherd/2/emerald_red_wool", "emerald", 1, "red_wool", 1),
                  trade("shepherd/2/emerald_black_wool", "emerald", 1, "black_wool", 1),
                  trade("shepherd/2/emerald_white_carpet", "emerald", 1, "white_carpet", 4),
                  trade("shepherd/2/emerald_orange_carpet", "emerald", 1, "orange_carpet", 4),
                  trade("shepherd/2/emerald_magenta_carpet", "emerald", 1, "magenta_carpet", 4),
                  trade(
                      "shepherd/2/emerald_light_blue_carpet", "emerald", 1, "light_blue_carpet", 4),
                  trade("shepherd/2/emerald_yellow_carpet", "emerald", 1, "yellow_carpet", 4),
                  trade("shepherd/2/emerald_lime_carpet", "emerald", 1, "lime_carpet", 4),
                  trade("shepherd/2/emerald_pink_carpet", "emerald", 1, "pink_carpet", 4),
                  trade("shepherd/2/emerald_gray_carpet", "emerald", 1, "gray_carpet", 4),
                  trade(
                      "shepherd/2/emerald_light_gray_carpet", "emerald", 1, "light_gray_carpet", 4),
                  trade("shepherd/2/emerald_cyan_carpet", "emerald", 1, "cyan_carpet", 4),
                  trade("shepherd/2/emerald_purple_carpet", "emerald", 1, "purple_carpet", 4),
                  trade("shepherd/2/emerald_blue_carpet", "emerald", 1, "blue_carpet", 4),
                  trade("shepherd/2/emerald_brown_carpet", "emerald", 1, "brown_carpet", 4),
                  trade("shepherd/2/emerald_green_carpet", "emerald", 1, "green_carpet", 4),
                  trade("shepherd/2/emerald_red_carpet", "emerald", 1, "red_carpet", 4),
                  trade("shepherd/2/emerald_black_carpet", "emerald", 1, "black_carpet", 4)),
              tier(
                  "shepherd/level_3",
                  2,
                  trade("shepherd/3/yellow_dye_emerald", "yellow_dye", 12, "emerald", 1),
                  trade("shepherd/3/light_gray_dye_emerald", "light_gray_dye", 12, "emerald", 1),
                  trade("shepherd/3/orange_dye_emerald", "orange_dye", 12, "emerald", 1),
                  trade("shepherd/3/red_dye_emerald", "red_dye", 12, "emerald", 1),
                  trade("shepherd/3/pink_dye_emerald", "pink_dye", 12, "emerald", 1),
                  trade("shepherd/3/emerald_white_bed", "emerald", 3, "white_bed", 1),
                  trade("shepherd/3/emerald_orange_bed", "emerald", 3, "orange_bed", 1),
                  trade("shepherd/3/emerald_magenta_bed", "emerald", 3, "magenta_bed", 1),
                  trade("shepherd/3/emerald_light_blue_bed", "emerald", 3, "light_blue_bed", 1),
                  trade("shepherd/3/emerald_yellow_bed", "emerald", 3, "yellow_bed", 1),
                  trade("shepherd/3/emerald_lime_bed", "emerald", 3, "lime_bed", 1),
                  trade("shepherd/3/emerald_pink_bed", "emerald", 3, "pink_bed", 1),
                  trade("shepherd/3/emerald_gray_bed", "emerald", 3, "gray_bed", 1),
                  trade("shepherd/3/emerald_light_gray_bed", "emerald", 3, "light_gray_bed", 1),
                  trade("shepherd/3/emerald_cyan_bed", "emerald", 3, "cyan_bed", 1),
                  trade("shepherd/3/emerald_purple_bed", "emerald", 3, "purple_bed", 1),
                  trade("shepherd/3/emerald_blue_bed", "emerald", 3, "blue_bed", 1),
                  trade("shepherd/3/emerald_brown_bed", "emerald", 3, "brown_bed", 1),
                  trade("shepherd/3/emerald_green_bed", "emerald", 3, "green_bed", 1),
                  trade("shepherd/3/emerald_red_bed", "emerald", 3, "red_bed", 1),
                  trade("shepherd/3/emerald_black_bed", "emerald", 3, "black_bed", 1)),
              tier(
                  "shepherd/level_4",
                  2,
                  trade("shepherd/4/brown_dye_emerald", "brown_dye", 12, "emerald", 1),
                  trade("shepherd/4/purple_dye_emerald", "purple_dye", 12, "emerald", 1),
                  trade("shepherd/4/blue_dye_emerald", "blue_dye", 12, "emerald", 1),
                  trade("shepherd/4/green_dye_emerald", "green_dye", 12, "emerald", 1),
                  trade("shepherd/4/magenta_dye_emerald", "magenta_dye", 12, "emerald", 1),
                  trade("shepherd/4/cyan_dye_emerald", "cyan_dye", 12, "emerald", 1),
                  trade("shepherd/4/emerald_white_banner", "emerald", 3, "white_banner", 1),
                  trade("shepherd/4/emerald_orange_banner", "emerald", 3, "orange_banner", 1),
                  trade("shepherd/4/emerald_magenta_banner", "emerald", 3, "magenta_banner", 1),
                  trade(
                      "shepherd/4/emerald_light_blue_banner", "emerald", 3, "light_blue_banner", 1),
                  trade("shepherd/4/emerald_yellow_banner", "emerald", 3, "yellow_banner", 1),
                  trade("shepherd/4/emerald_lime_banner", "emerald", 3, "lime_banner", 1),
                  trade("shepherd/4/emerald_pink_banner", "emerald", 3, "pink_banner", 1),
                  trade("shepherd/4/emerald_gray_banner", "emerald", 3, "gray_banner", 1),
                  trade(
                      "shepherd/4/emerald_light_gray_banner", "emerald", 3, "light_gray_banner", 1),
                  trade("shepherd/4/emerald_cyan_banner", "emerald", 3, "cyan_banner", 1),
                  trade("shepherd/4/emerald_purple_banner", "emerald", 3, "purple_banner", 1),
                  trade("shepherd/4/emerald_blue_banner", "emerald", 3, "blue_banner", 1),
                  trade("shepherd/4/emerald_brown_banner", "emerald", 3, "brown_banner", 1),
                  trade("shepherd/4/emerald_green_banner", "emerald", 3, "green_banner", 1),
                  trade("shepherd/4/emerald_red_banner", "emerald", 3, "red_banner", 1),
                  trade("shepherd/4/emerald_black_banner", "emerald", 3, "black_banner", 1)),
              tier(
                  "shepherd/level_5",
                  2,
                  trade("shepherd/5/emerald_painting", "emerald", 2, "painting", 3))),
          merchant(
              "toolsmith",
              tier(
                  "toolsmith/level_1",
                  2,
                  trade("smith/1/coal_emerald", "coal", 15, "emerald", 1),
                  trade("toolsmith/1/emerald_stone_axe", "emerald", 1, "stone_axe", 1),
                  trade("toolsmith/1/emerald_stone_shovel", "emerald", 1, "stone_shovel", 1),
                  trade("toolsmith/1/emerald_stone_pickaxe", "emerald", 1, "stone_pickaxe", 1),
                  trade("toolsmith/1/emerald_stone_hoe", "emerald", 1, "stone_hoe", 1)),
              tier(
                  "toolsmith/level_2",
                  2,
                  trade("smith/2/iron_ingot_emerald", "iron_ingot", 4, "emerald", 1),
                  trade("smith/2/emerald_bell", "emerald", 36, "bell", 1)),
              tier(
                  "toolsmith/level_3",
                  2,
                  trade("toolsmith/3/flint_emerald", "flint", 30, "emerald", 1),
                  trade("toolsmith/3/emerald_enchanted_iron_axe", "emerald", 1, "iron_axe", 1)
                      .as(TradeKind.ENCHANTED_ITEM),
                  trade("toolsmith/3/emerald_enchanted_iron_shovel", "emerald", 2, "iron_shovel", 1)
                      .as(TradeKind.ENCHANTED_ITEM),
                  trade(
                          "toolsmith/3/emerald_enchanted_iron_pickaxe",
                          "emerald",
                          3,
                          "iron_pickaxe",
                          1)
                      .as(TradeKind.ENCHANTED_ITEM),
                  trade("toolsmith/3/emerald_diamond_hoe", "emerald", 4, "diamond_hoe", 1)),
              tier(
                  "toolsmith/level_4",
                  2,
                  trade(
                          "toolsmith/4/emerald_enchanted_diamond_axe",
                          "emerald",
                          12,
                          "diamond_axe",
                          1)
                      .as(TradeKind.ENCHANTED_ITEM),
                  trade(
                          "toolsmith/4/emerald_enchanted_diamond_shovel",
                          "emerald",
                          5,
                          "diamond_shovel",
                          1)
                      .as(TradeKind.ENCHANTED_ITEM),
                  trade("toolsmith/4/diamond_emerald", "diamond", 1, "emerald", 1)),
              tier(
                  "toolsmith/level_5",
                  2,
                  trade(
                          "toolsmith/5/emerald_enchanted_diamond_pickaxe",
                          "emerald",
                          13,
                          "diamond_pickaxe",
                          1)
                      .as(TradeKind.ENCHANTED_ITEM))),
          merchant(
              "weaponsmith",
              tier(
                  "weaponsmith/level_1",
                  2,
                  trade("smith/1/coal_emerald", "coal", 15, "emerald", 1),
                  trade("weaponsmith/1/emerald_iron_axe", "emerald", 3, "iron_axe", 1),
                  trade("weaponsmith/1/emerald_enchanted_iron_sword", "emerald", 2, "iron_sword", 1)
                      .as(TradeKind.ENCHANTED_ITEM)),
              tier(
                  "weaponsmith/level_2",
                  2,
                  trade("smith/2/iron_ingot_emerald", "iron_ingot", 4, "emerald", 1),
                  trade("smith/2/emerald_bell", "emerald", 36, "bell", 1)),
              tier(
                  "weaponsmith/level_3",
                  2,
                  trade("weaponsmith/3/flint_emerald", "flint", 24, "emerald", 1)),
              tier(
                  "weaponsmith/level_4",
                  2,
                  trade(
                          "weaponsmith/4/emerald_enchanted_diamond_axe",
                          "emerald",
                          12,
                          "diamond_axe",
                          1)
                      .as(TradeKind.ENCHANTED_ITEM),
                  trade("weaponsmith/4/diamond_emerald", "diamond", 1, "emerald", 1)),
              tier(
                  "weaponsmith/level_5",
                  2,
                  trade(
                          "weaponsmith/5/emerald_enchanted_diamond_sword",
                          "emerald",
                          8,
                          "diamond_sword",
                          1)
                      .as(TradeKind.ENCHANTED_ITEM))));

  /** The wandering trader: what it buys, its common and its uncommon offers. */
  public static final MerchantTrades WANDERING_TRADER =
      merchant(
          "wandering_trader",
          tier(
              "wandering_trader/buying",
              2,
              trade("wandering_trader/water_bottle_emerald", "potion", 1, "emerald", 1)
                  .wantsPotion("water"),
              trade("wandering_trader/water_bucket_emerald", "water_bucket", 1, "emerald", 2),
              trade("wandering_trader/milk_bucket_emerald", "milk_bucket", 1, "emerald", 2),
              trade(
                  "wandering_trader/fermented_spider_eye_emerald",
                  "fermented_spider_eye",
                  1,
                  "emerald",
                  3),
              trade("wandering_trader/baked_potato_emerald", "baked_potato", 4, "emerald", 1),
              trade("wandering_trader/hay_block_emerald", "hay_block", 1, "emerald", 1)),
          tier(
              "wandering_trader/common",
              5,
              trade("wandering_trader/emerald_white_dye", "emerald", 1, "white_dye", 3),
              trade("wandering_trader/emerald_orange_dye", "emerald", 1, "orange_dye", 3),
              trade("wandering_trader/emerald_magenta_dye", "emerald", 1, "magenta_dye", 3),
              trade("wandering_trader/emerald_light_blue_dye", "emerald", 1, "light_blue_dye", 3),
              trade("wandering_trader/emerald_yellow_dye", "emerald", 1, "yellow_dye", 3),
              trade("wandering_trader/emerald_lime_dye", "emerald", 1, "lime_dye", 3),
              trade("wandering_trader/emerald_pink_dye", "emerald", 1, "pink_dye", 3),
              trade("wandering_trader/emerald_gray_dye", "emerald", 1, "gray_dye", 3),
              trade("wandering_trader/emerald_light_gray_dye", "emerald", 1, "light_gray_dye", 3),
              trade("wandering_trader/emerald_cyan_dye", "emerald", 1, "cyan_dye", 3),
              trade("wandering_trader/emerald_purple_dye", "emerald", 1, "purple_dye", 3),
              trade("wandering_trader/emerald_blue_dye", "emerald", 1, "blue_dye", 3),
              trade("wandering_trader/emerald_brown_dye", "emerald", 1, "brown_dye", 3),
              trade("wandering_trader/emerald_green_dye", "emerald", 1, "green_dye", 3),
              trade("wandering_trader/emerald_red_dye", "emerald", 1, "red_dye", 3),
              trade("wandering_trader/emerald_black_dye", "emerald", 1, "black_dye", 3),
              trade(
                  "wandering_trader/emerald_fish_bucket", "emerald", 3, "tropical_fish_bucket", 1),
              trade(
                  "wandering_trader/emerald_pufferfish_bucket",
                  "emerald",
                  3,
                  "pufferfish_bucket",
                  1),
              trade("wandering_trader/emerald_sea_pickle", "emerald", 2, "sea_pickle", 1),
              trade("wandering_trader/emerald_slime_ball", "emerald", 4, "slime_ball", 1),
              trade("wandering_trader/emerald_glowstone", "emerald", 2, "glowstone", 1),
              trade("wandering_trader/emerald_nautilus_shell", "emerald", 5, "nautilus_shell", 1),
              trade("wandering_trader/emerald_fern", "emerald", 1, "fern", 1),
              trade("wandering_trader/emerald_sugar_cane", "emerald", 1, "sugar_cane", 1),
              trade("wandering_trader/emerald_pumpkin", "emerald", 1, "pumpkin", 1),
              trade("wandering_trader/emerald_kelp", "emerald", 3, "kelp", 1),
              trade("wandering_trader/emerald_cactus", "emerald", 3, "cactus", 1),
              trade("wandering_trader/emerald_dandelion", "emerald", 1, "dandelion", 1),
              trade("wandering_trader/emerald_poppy", "emerald", 1, "poppy", 1),
              trade("wandering_trader/emerald_blue_orchid", "emerald", 1, "blue_orchid", 1),
              trade("wandering_trader/emerald_allium", "emerald", 1, "allium", 1),
              trade("wandering_trader/emerald_azure_bluet", "emerald", 1, "azure_bluet", 1),
              trade("wandering_trader/emerald_red_tulip", "emerald", 1, "red_tulip", 1),
              trade("wandering_trader/emerald_orange_tulip", "emerald", 1, "orange_tulip", 1),
              trade("wandering_trader/emerald_white_tulip", "emerald", 1, "white_tulip", 1),
              trade("wandering_trader/emerald_pink_tulip", "emerald", 1, "pink_tulip", 1),
              trade("wandering_trader/emerald_oxeye_daisy", "emerald", 1, "oxeye_daisy", 1),
              trade("wandering_trader/emerald_cornflower", "emerald", 1, "cornflower", 1),
              trade(
                  "wandering_trader/emerald_lily_of_the_valley",
                  "emerald",
                  1,
                  "lily_of_the_valley",
                  1),
              trade("wandering_trader/emerald_open_eyeblossom", "emerald", 1, "open_eyeblossom", 1),
              trade("wandering_trader/emerald_wheat_seeds", "emerald", 1, "wheat_seeds", 1),
              trade("wandering_trader/emerald_beetroot_seeds", "emerald", 1, "beetroot_seeds", 1),
              trade("wandering_trader/emerald_pumpkin_seeds", "emerald", 1, "pumpkin_seeds", 1),
              trade("wandering_trader/emerald_melon_seeds", "emerald", 1, "melon_seeds", 1),
              trade("wandering_trader/emerald_acacia_sapling", "emerald", 5, "acacia_sapling", 1),
              trade("wandering_trader/emerald_birch_sapling", "emerald", 5, "birch_sapling", 1),
              trade(
                  "wandering_trader/emerald_dark_oak_sapling", "emerald", 5, "dark_oak_sapling", 1),
              trade("wandering_trader/emerald_jungle_sapling", "emerald", 5, "jungle_sapling", 1),
              trade("wandering_trader/emerald_oak_sapling", "emerald", 5, "oak_sapling", 1),
              trade("wandering_trader/emerald_spruce_sapling", "emerald", 5, "spruce_sapling", 1),
              trade("wandering_trader/emerald_cherry_sapling", "emerald", 5, "cherry_sapling", 1),
              trade(
                  "wandering_trader/emerald_pale_oak_sapling", "emerald", 5, "pale_oak_sapling", 1),
              trade(
                  "wandering_trader/emerald_mangrove_propagule",
                  "emerald",
                  5,
                  "mangrove_propagule",
                  1),
              trade("wandering_trader/emerald_poplar_sapling", "emerald", 5, "poplar_sapling", 1),
              trade(
                  "wandering_trader/emerald_brain_coral_block",
                  "emerald",
                  3,
                  "brain_coral_block",
                  1),
              trade(
                  "wandering_trader/emerald_bubble_coral_block",
                  "emerald",
                  3,
                  "bubble_coral_block",
                  1),
              trade(
                  "wandering_trader/emerald_fire_coral_block", "emerald", 3, "fire_coral_block", 1),
              trade(
                  "wandering_trader/emerald_horn_coral_block", "emerald", 3, "horn_coral_block", 1),
              trade(
                  "wandering_trader/emerald_tube_coral_block", "emerald", 3, "tube_coral_block", 1),
              trade("wandering_trader/emerald_vine", "emerald", 1, "vine", 3),
              trade(
                  "wandering_trader/emerald_pale_hanging_moss",
                  "emerald",
                  1,
                  "pale_hanging_moss",
                  3),
              trade("wandering_trader/emerald_brown_mushroom", "emerald", 1, "brown_mushroom", 3),
              trade("wandering_trader/emerald_red_mushroom", "emerald", 1, "red_mushroom", 3),
              trade("wandering_trader/emerald_lily_pad", "emerald", 1, "lily_pad", 5),
              trade("wandering_trader/emerald_small_dripleaf", "emerald", 1, "small_dripleaf", 2),
              trade("wandering_trader/emerald_sand", "emerald", 1, "sand", 8),
              trade("wandering_trader/emerald_red_sand", "emerald", 1, "red_sand", 4),
              trade(
                  "wandering_trader/emerald_pointed_dripstone",
                  "emerald",
                  1,
                  "pointed_dripstone",
                  2),
              trade("wandering_trader/emerald_sulfur_spike", "emerald", 1, "sulfur_spike", 2),
              trade("wandering_trader/emerald_rooted_dirt", "emerald", 1, "rooted_dirt", 2),
              trade("wandering_trader/emerald_moss_block", "emerald", 1, "moss_block", 2),
              trade("wandering_trader/emerald_pale_moss_block", "emerald", 1, "pale_moss_block", 2),
              trade("wandering_trader/emerald_wildflowers", "emerald", 1, "wildflowers", 1),
              trade("wandering_trader/emerald_dry_tall_grass", "emerald", 1, "tall_dry_grass", 1),
              trade("wandering_trader/emerald_firefly_bush", "emerald", 3, "firefly_bush", 1),
              trade(
                  "wandering_trader/emerald_golden_dandelion", "emerald", 2, "golden_dandelion", 1),
              trade("wandering_trader/emerald_name_tag", "emerald", 1, "name_tag", 1),
              trade("wandering_trader/emerald_shelf_mushroom", "emerald", 1, "shelf_mushroom", 3)),
          tier(
              "wandering_trader/uncommon",
              2,
              trade("wandering_trader/emerald_packed_ice", "emerald", 1, "packed_ice", 1),
              trade("wandering_trader/emerald_blue_ice", "emerald", 6, "blue_ice", 1),
              trade("wandering_trader/emerald_gunpowder", "emerald", 1, "gunpowder", 4),
              trade("wandering_trader/emerald_podzol", "emerald", 3, "podzol", 3),
              trade("wandering_trader/emerald_acacia_log", "emerald", 1, "acacia_log", 8),
              trade("wandering_trader/emerald_birch_log", "emerald", 1, "birch_log", 8),
              trade("wandering_trader/emerald_dark_oak_log", "emerald", 1, "dark_oak_log", 8),
              trade("wandering_trader/emerald_jungle_log", "emerald", 1, "jungle_log", 8),
              trade("wandering_trader/emerald_oak_log", "emerald", 1, "oak_log", 8),
              trade("wandering_trader/emerald_spruce_log", "emerald", 1, "spruce_log", 8),
              trade("wandering_trader/emerald_cherry_log", "emerald", 1, "cherry_log", 8),
              trade("wandering_trader/emerald_mangrove_log", "emerald", 1, "mangrove_log", 8),
              trade("wandering_trader/emerald_pale_oak_log", "emerald", 1, "pale_oak_log", 8),
              trade("wandering_trader/emerald_poplar_log", "emerald", 1, "poplar_log", 8),
              trade(
                      "wandering_trader/emerald_enchanted_iron_pickaxe",
                      "emerald",
                      1,
                      "iron_pickaxe",
                      1)
                  .as(TradeKind.ENCHANTED_ITEM),
              trade("wandering_trader/emerald_long_invisibility_potion", "emerald", 5, "potion", 1)
                  .givesPotion("long_invisibility")));

  private VillagerTradeTable() {}

  /** Every merchant: the professions, then the wandering trader. */
  public static List<MerchantTrades> all() {
    List<MerchantTrades> all = new ArrayList<>(PROFESSIONS);
    all.add(WANDERING_TRADER);
    return List.copyOf(all);
  }

  private static MerchantTrades merchant(String merchantId, TradeTier... tiers) {
    return new MerchantTrades(merchantId, List.of(tiers));
  }

  private static TradeTier tier(String tradeSetId, int amount, TradeRow... trades) {
    return new TradeTier(tradeSetId, amount, List.of(trades));
  }

  private static TradeRow trade(
      String id, String wants, int wantsCount, String gives, int givesCount) {
    return new TradeRow(
        id,
        TradeItem.of(wants, wantsCount),
        Optional.empty(),
        TradeItem.of(gives, givesCount),
        TradeKind.PLAIN,
        List.of());
  }

  private static TradeRow trade(
      String id,
      String wants,
      int wantsCount,
      String extraWants,
      int extraCount,
      String gives,
      int givesCount) {
    return new TradeRow(
        id,
        TradeItem.of(wants, wantsCount),
        Optional.of(TradeItem.of(extraWants, extraCount)),
        TradeItem.of(gives, givesCount),
        TradeKind.PLAIN,
        List.of());
  }
}
