package com.einsteinmods.cheatsheet.core.discs;

import java.util.Locale;
import java.util.Optional;

/**
 * Where music discs come from, in display order, with the vanilla loot table (id without the {@code
 * minecraft:} namespace) that drops them; crafting has none.
 */
public enum DiscSource {
  CREEPER_KILLED_BY_SKELETON("entities/creeper"),
  DUNGEON("chests/simple_dungeon"),
  ANCIENT_CITY("chests/ancient_city"),
  WOODLAND_MANSION("chests/woodland_mansion"),
  STRONGHOLD("chests/stronghold_corridor"),
  MINESHAFT("chests/abandoned_mineshaft"),
  TRIAL_VAULT("chests/trial_chambers/reward_unique"),
  OMINOUS_VAULT("chests/trial_chambers/reward_ominous_unique"),
  TRIAL_POTS("pots/trial_chambers/corridor"),
  TRAIL_RUINS("archaeology/trail_ruins_rare"),
  BASTION("chests/bastion_other"),
  GHAST_OWN_FIREBALL("entities/ghast"),
  CHICKEN_JOCKEY("entities/zombie"),
  CRAFTED;

  private final Optional<String> lootTable;

  DiscSource(String lootTable) {
    this.lootTable = Optional.of(lootTable);
  }

  DiscSource() {
    this.lootTable = Optional.empty();
  }

  public Optional<String> lootTable() {
    return lootTable;
  }

  /** Lower-case name for lang keys. */
  public String key() {
    return name().toLowerCase(Locale.ROOT);
  }
}
