package com.einsteinmods.cheatsheet.core.beacon;

import java.util.List;
import java.util.OptionalInt;

/**
 * Where a conduit material comes from in survival.
 *
 * @param key lang key suffix ({@code source.<key>})
 * @param iconId item shown beside it (id without namespace)
 * @param kind how the game gives it, which says what {@code references} name
 * @param references loot tables ({@link Kind#LOOT}) or a wandering trader trade ({@link
 *     Kind#TRADE}), ids without namespace; empty for {@link Kind#HELD_ITEM}
 * @param amount the number its text shows (a chance in percent, a price), if any
 */
public record MaterialSource(
    String key, String iconId, Kind kind, List<String> references, OptionalInt amount) {
  public enum Kind {
    /** A loot table: a chest, a mob's drops, fishing, a broken block. */
    LOOT,
    /** A trade, read from {@code villager_trade/<reference>}. */
    TRADE,
    /** Mobs that spawn holding it drop it ({@code Drowned.NAUTILUS_SHELL_CHANCE}). */
    HELD_ITEM
  }

  public MaterialSource {
    references = List.copyOf(references);
    if ((kind == Kind.HELD_ITEM) != references.isEmpty()) {
      throw new IllegalArgumentException(key + ": " + kind + " with " + references);
    }
  }

  static MaterialSource loot(String key, String iconId, String... tables) {
    return new MaterialSource(key, iconId, Kind.LOOT, List.of(tables), OptionalInt.empty());
  }
}
