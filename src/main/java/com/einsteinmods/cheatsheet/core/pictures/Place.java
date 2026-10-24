package com.einsteinmods.cheatsheet.core.pictures;

/**
 * Places the sheets show in miniature: structures where loot is found and biomes with ore bonuses.
 * Each is a hand-built diorama of a few blocks that reads at a glance (see {@link PlaceDioramas}).
 */
public enum Place {
  PILLAGER_OUTPOST(PlaceDioramas.pillagerOutpost()),
  DESERT_PYRAMID(PlaceDioramas.desertPyramid()),
  SHIPWRECK(PlaceDioramas.shipwreck()),
  JUNGLE_TEMPLE(PlaceDioramas.jungleTemple()),
  OCEAN_MONUMENT(PlaceDioramas.oceanMonument()),
  WOODLAND_MANSION(PlaceDioramas.woodlandMansion()),
  STRONGHOLD(PlaceDioramas.stronghold()),
  ANCIENT_CITY(PlaceDioramas.ancientCity()),
  TRAIL_RUINS(PlaceDioramas.trailRuins()),
  TRIAL_CHAMBERS(PlaceDioramas.trialChambers()),
  NETHER_FORTRESS(PlaceDioramas.netherFortress()),
  BASTION(PlaceDioramas.bastion()),
  END_CITY(PlaceDioramas.endCity()),
  DUNGEON(PlaceDioramas.dungeon()),
  MINESHAFT(PlaceDioramas.mineshaft()),
  BADLANDS(PlaceDioramas.badlands()),
  MOUNTAINS(PlaceDioramas.mountains()),
  DRIPSTONE_CAVES(PlaceDioramas.dripstoneCaves()),
  BASALT_DELTAS(PlaceDioramas.basaltDeltas());

  private final Diorama diorama;

  Place(Diorama diorama) {
    this.diorama = diorama;
  }

  public Diorama diorama() {
    return diorama;
  }
}
