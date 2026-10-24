package com.einsteinmods.cheatsheet.core.trims;

import java.util.List;
import java.util.Locale;

/**
 * Where smithing templates are found, in display order, with the vanilla loot tables (ids without
 * the {@code minecraft:} namespace) that hold them.
 */
public enum TrimSource {
  PILLAGER_OUTPOST("chests/pillager_outpost"),
  DESERT_PYRAMID("chests/desert_pyramid"),
  SHIPWRECK("chests/shipwreck_map", "chests/shipwreck_supply", "chests/shipwreck_treasure"),
  JUNGLE_TEMPLE("chests/jungle_temple"),
  OCEAN_MONUMENT("entities/elder_guardian"),
  WOODLAND_MANSION("chests/woodland_mansion"),
  STRONGHOLD("chests/stronghold_corridor", "chests/stronghold_library"),
  ANCIENT_CITY("chests/ancient_city"),
  TRAIL_RUINS("archaeology/trail_ruins_rare"),
  TRIAL_VAULT("chests/trial_chambers/reward_unique"),
  OMINOUS_VAULT("chests/trial_chambers/reward_ominous_unique"),
  NETHER_FORTRESS("chests/nether_bridge"),
  BASTION(
      "chests/bastion_treasure",
      "chests/bastion_other",
      "chests/bastion_bridge",
      "chests/bastion_hoglin_stable"),
  END_CITY("chests/end_city_treasure");

  private final List<String> lootTables;

  TrimSource(String... lootTables) {
    this.lootTables = List.of(lootTables);
  }

  public List<String> lootTables() {
    return lootTables;
  }

  /** Lower-case name for lang keys. */
  public String key() {
    return name().toLowerCase(Locale.ROOT);
  }
}
