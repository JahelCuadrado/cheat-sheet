package com.einsteinmods.cheatsheet.core.commands;

import java.util.Locale;

/**
 * The card a built-in command is listed on, in display order: first the single commands, then the
 * popular combos.
 */
public enum CommandGroup {
  /** Game mode, time, weather, teleport, effects, spawn and the common game rules. */
  BASICS(false),
  /** Filling, cloning and clearing areas of blocks. */
  BUILDING(false),
  /** Items you cannot get in survival, or not like this: maxed gear, technical blocks. */
  ITEMS(false),
  /** Finding places, travelling, execute chains, summons with data, messages and effects. */
  ADVANCED(false),
  /** Combos that fill the inventory: armour, tools, weapons, food. */
  KITS(true),
  /** Combos of game rules, time and weather: a world ready to build or record in. */
  WORLD(true),
  /** Combos that end a boss fight or clear things away. */
  BOSSES(true),
  /** Combos of effects for the player: invincible, mining, exploring, recording. */
  PLAYER(true);

  private final boolean combos;

  CommandGroup(boolean combos) {
    this.combos = combos;
  }

  /** Whether its entries are popular combos (listed in their own section). */
  public boolean combos() {
    return combos;
  }

  /** Lower-case id, for lang keys. */
  public String id() {
    return name().toLowerCase(Locale.ROOT);
  }
}
