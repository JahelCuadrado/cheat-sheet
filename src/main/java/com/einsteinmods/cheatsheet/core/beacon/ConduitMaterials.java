package com.einsteinmods.cheatsheet.core.beacon;

import com.einsteinmods.cheatsheet.core.dyes.DyeRecipe;
import com.einsteinmods.cheatsheet.core.dyes.DyeTable;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

/**
 * The conduit's recipe, the recipes of its frame blocks and where their materials come from, copied
 * from vanilla data (recipes, loot tables and trades are server data, never sent to the client).
 * {@code ConduitMaterialsGameTest} checks every row against the running game.
 */
public final class ConduitMaterials {
  public static final String CONDUIT = "conduit";
  public static final String HEART_OF_THE_SEA = "heart_of_the_sea";
  public static final String NAUTILUS_SHELL = "nautilus_shell";
  public static final String PRISMARINE_SHARD = "prismarine_shard";
  public static final String PRISMARINE_CRYSTALS = "prismarine_crystals";

  /** Comes from the dye recipes ({@link DyeTable}), not from this table. */
  public static final String BLACK_DYE_COLOR = "black";

  public static final String BLACK_DYE = DyeRecipe.dyeItemId(BLACK_DYE_COLOR);

  /** The map that leads to buried treasure, and the chests it is found in. */
  public static final String TREASURE_MAP = "buried_treasure_map";

  public static final List<String> TREASURE_MAP_TABLES =
      List.of("chests/shipwreck_map", "chests/underwater_ruin_big", "chests/underwater_ruin_small");

  /** Chance, in percent, that a nautilus killed by a player drops a shell (without Looting). */
  public static final int NAUTILUS_SHELL_PERCENT = 5;

  /** Chance, in percent, that a drowned spawns holding a shell, which it always drops. */
  public static final int DROWNED_SHELL_PERCENT = 3;

  /** Emeralds the wandering trader asks for one shell. */
  public static final int TRADER_SHELL_EMERALDS = 5;

  private static final char SYMBOL = '#';
  private static final char CENTRE = 'X';
  private static final char SECOND = 'C';
  private static final String FULL_ROW = "###";

  public static final GridRecipe CONDUIT_RECIPE =
      new GridRecipe(
          CONDUIT,
          List.of(FULL_ROW, "#X#", FULL_ROW),
          Map.of(SYMBOL, NAUTILUS_SHELL, CENTRE, HEART_OF_THE_SEA),
          false);

  /** Recipes of the frame blocks ({@code #conduit_activator_blocks}), in the tag's order. */
  public static final List<GridRecipe> FRAME_RECIPES =
      List.of(
          new GridRecipe(
              "prismarine", List.of("##", "##"), Map.of(SYMBOL, PRISMARINE_SHARD), false),
          new GridRecipe(
              "prismarine_bricks",
              List.of(FULL_ROW, FULL_ROW, FULL_ROW),
              Map.of(SYMBOL, PRISMARINE_SHARD),
              true),
          new GridRecipe(
              "sea_lantern",
              List.of("#C#", "CCC", "#C#"),
              Map.of(SYMBOL, PRISMARINE_SHARD, SECOND, PRISMARINE_CRYSTALS),
              false),
          new GridRecipe(
              "dark_prismarine",
              List.of(FULL_ROW, "#X#", FULL_ROW),
              Map.of(SYMBOL, PRISMARINE_SHARD, CENTRE, BLACK_DYE),
              false));

  private static final MaterialSource GUARDIAN =
      MaterialSource.loot("guardian", "guardian_spawn_egg", "entities/guardian");
  private static final MaterialSource ELDER_GUARDIAN =
      MaterialSource.loot("elder_guardian", "elder_guardian_spawn_egg", "entities/elder_guardian");
  private static final MaterialSource BURIED_TREASURE =
      MaterialSource.loot("buried_treasure", TREASURE_MAP, "chests/buried_treasure");

  /**
   * Every place each material comes from (material ids without namespace). The loot tables listed
   * for a material are all the loot tables that give it.
   */
  public static final Map<String, List<MaterialSource>> SOURCES =
      Map.of(
          HEART_OF_THE_SEA,
          List.of(BURIED_TREASURE),
          NAUTILUS_SHELL,
          List.of(
              MaterialSource.loot("fishing", "fishing_rod", "gameplay/fishing/treasure"),
              new MaterialSource(
                  "nautilus",
                  "nautilus_spawn_egg",
                  MaterialSource.Kind.LOOT,
                  List.of("entities/nautilus"),
                  OptionalInt.of(NAUTILUS_SHELL_PERCENT)),
              new MaterialSource(
                  "drowned",
                  "drowned_spawn_egg",
                  MaterialSource.Kind.HELD_ITEM,
                  List.of(),
                  OptionalInt.of(DROWNED_SHELL_PERCENT)),
              new MaterialSource(
                  "wandering_trader",
                  "wandering_trader_spawn_egg",
                  MaterialSource.Kind.TRADE,
                  List.of("wandering_trader/emerald_nautilus_shell"),
                  OptionalInt.of(TRADER_SHELL_EMERALDS))),
          PRISMARINE_SHARD,
          List.of(GUARDIAN, ELDER_GUARDIAN),
          PRISMARINE_CRYSTALS,
          List.of(
              GUARDIAN,
              ELDER_GUARDIAN,
              BURIED_TREASURE,
              MaterialSource.loot("sea_lantern", "sea_lantern", "blocks/sea_lantern")));

  private ConduitMaterials() {}

  /** Where {@code itemId} comes from; empty for items this table does not cover. */
  public static List<MaterialSource> sourcesOf(String itemId) {
    return SOURCES.getOrDefault(itemId, List.of());
  }
}
