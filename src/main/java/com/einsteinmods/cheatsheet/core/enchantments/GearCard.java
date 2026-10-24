package com.einsteinmods.cheatsheet.core.enchantments;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Cards of the Enchantments tab in display order: two shared cards (every gear, every armor piece),
 * one per gear, and a last card for enchantments that fit none of the tested gear.
 */
public enum GearCard {
  ANY(EnumSet.allOf(Gear.class)),
  ARMOR(EnumSet.of(Gear.HELMET, Gear.CHESTPLATE, Gear.LEGGINGS, Gear.BOOTS)),
  HELMET(EnumSet.of(Gear.HELMET)),
  CHESTPLATE(EnumSet.of(Gear.CHESTPLATE)),
  LEGGINGS(EnumSet.of(Gear.LEGGINGS)),
  BOOTS(EnumSet.of(Gear.BOOTS)),
  SWORD(EnumSet.of(Gear.SWORD)),
  SPEAR(EnumSet.of(Gear.SPEAR)),
  AXE(EnumSet.of(Gear.AXE)),
  TOOL(EnumSet.of(Gear.TOOL)),
  BOW(EnumSet.of(Gear.BOW)),
  CROSSBOW(EnumSet.of(Gear.CROSSBOW)),
  TRIDENT(EnumSet.of(Gear.TRIDENT)),
  MACE(EnumSet.of(Gear.MACE)),
  FISHING_ROD(EnumSet.of(Gear.FISHING_ROD)),
  OTHER(EnumSet.noneOf(Gear.class));

  private final Set<Gear> gears;

  GearCard(EnumSet<Gear> gears) {
    this.gears = Collections.unmodifiableSet(gears);
  }

  /** Gear the card stands for; empty for {@link #OTHER}. */
  public Set<Gear> gears() {
    return gears;
  }

  /** Lower-case id for lang keys. */
  public String id() {
    return name().toLowerCase(Locale.ROOT);
  }
}
