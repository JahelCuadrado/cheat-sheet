package com.einsteinmods.cheatsheet.core;

import java.util.OptionalInt;

/**
 * The player's sheet size setting: {@link #AUTO} or a fixed number of window pixels per sheet
 * pixel, independent of the game's GUI scale. Saved by {@link #id()}.
 */
public enum SheetScale {
  AUTO("auto", OptionalInt.empty()),
  ONE("1", OptionalInt.of(1)),
  TWO("2", OptionalInt.of(2)),
  THREE("3", OptionalInt.of(3)),
  FOUR("4", OptionalInt.of(4));

  private final String id;
  private final OptionalInt fixed;

  SheetScale(String id, OptionalInt fixed) {
    this.id = id;
    this.fixed = fixed;
  }

  /** Value saved in the settings file. */
  public String id() {
    return id;
  }

  /** Window pixels per sheet pixel for a fixed setting; empty for {@link #AUTO}. */
  public OptionalInt fixed() {
    return fixed;
  }

  /** The setting saved as {@code id}; {@link #AUTO} for an unknown or missing value. */
  public static SheetScale fromId(String id) {
    for (SheetScale scale : values()) {
      if (scale.id.equals(id)) {
        return scale;
      }
    }
    return AUTO;
  }
}
