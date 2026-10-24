package com.einsteinmods.cheatsheet.core.trims;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Every smithing template and where it is found. Loot tables and recipes are server data, never
 * sent to the client, so this is a copy of vanilla; {@code TrimTemplateGameTest} checks it against
 * the game's loot tables and copy recipes.
 */
public final class TrimTable {
  /** Diamonds in every template copy recipe. */
  public static final int COPY_DIAMONDS = 7;

  /** Templates a copy recipe makes. */
  public static final int COPY_RESULT_COUNT = 2;

  private static final String COBBLESTONE = "cobblestone";
  private static final String COBBLED_DEEPSLATE = "cobbled_deepslate";
  private static final String NETHERRACK = "netherrack";
  private static final String TERRACOTTA = "terracotta";

  public static final List<TrimTemplate> TEMPLATES =
      List.of(
          TrimTemplate.trim("sentry", TrimSource.PILLAGER_OUTPOST, COBBLESTONE),
          TrimTemplate.trim("dune", TrimSource.DESERT_PYRAMID, "sandstone"),
          TrimTemplate.trim("coast", TrimSource.SHIPWRECK, COBBLESTONE),
          TrimTemplate.trim("wild", TrimSource.JUNGLE_TEMPLE, "mossy_cobblestone"),
          TrimTemplate.trim("tide", TrimSource.OCEAN_MONUMENT, "prismarine"),
          TrimTemplate.trim("vex", TrimSource.WOODLAND_MANSION, COBBLESTONE),
          TrimTemplate.trim("eye", TrimSource.STRONGHOLD, "end_stone"),
          TrimTemplate.trim("ward", TrimSource.ANCIENT_CITY, COBBLED_DEEPSLATE),
          TrimTemplate.trim("silence", TrimSource.ANCIENT_CITY, COBBLED_DEEPSLATE),
          TrimTemplate.trim("wayfinder", TrimSource.TRAIL_RUINS, TERRACOTTA),
          TrimTemplate.trim("shaper", TrimSource.TRAIL_RUINS, TERRACOTTA),
          TrimTemplate.trim("raiser", TrimSource.TRAIL_RUINS, TERRACOTTA),
          TrimTemplate.trim("host", TrimSource.TRAIL_RUINS, TERRACOTTA),
          TrimTemplate.trim("bolt", TrimSource.TRIAL_VAULT, "copper_block"),
          TrimTemplate.trim("flow", TrimSource.OMINOUS_VAULT, "breeze_rod"),
          TrimTemplate.trim("rib", TrimSource.NETHER_FORTRESS, NETHERRACK),
          TrimTemplate.trim("snout", TrimSource.BASTION, "blackstone"),
          new TrimTemplate(
              "netherite_upgrade_smithing_template",
              Optional.empty(),
              TrimSource.BASTION,
              NETHERRACK),
          TrimTemplate.trim("spire", TrimSource.END_CITY, "purpur_block"));

  private TrimTable() {}

  /** Templates found at {@code source}, in table order. */
  public static List<TrimTemplate> templatesFrom(TrimSource source) {
    List<TrimTemplate> found = new ArrayList<>();
    for (TrimTemplate template : TEMPLATES) {
      if (template.source() == source) {
        found.add(template);
      }
    }
    return List.copyOf(found);
  }
}
