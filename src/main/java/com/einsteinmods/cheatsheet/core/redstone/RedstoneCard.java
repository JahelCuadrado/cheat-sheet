package com.einsteinmods.cheatsheet.core.redstone;

import com.einsteinmods.cheatsheet.core.pictures.Diorama;
import java.util.Locale;

/**
 * The redstone tab's reference cards, in reading order: each shows a small diorama and one key
 * fact, and zooms into all its numbers.
 */
public enum RedstoneCard {
  PRESSURE_PLATES(RedstoneDioramas.pressurePlate()),
  COMPARATOR(RedstoneDioramas.comparator()),
  SPECIAL_SIGNALS(RedstoneDioramas.specialBlocks()),
  SCULK_SENSORS(RedstoneDioramas.sculkSensors()),
  TICKS(RedstoneDioramas.repeater()),
  CRAFTER(RedstoneDioramas.crafterCard());

  private final Diorama picture;

  RedstoneCard(Diorama picture) {
    this.picture = picture;
  }

  public Diorama picture() {
    return picture;
  }

  /** Lower-case id for lang keys. */
  public String key() {
    return name().toLowerCase(Locale.ROOT);
  }
}
