package com.einsteinmods.cheatsheet.core.farming;

/** One block of a farm seen from above: what stands there, written as one symbol in a plan. */
public enum PlanCell {
  WATER('W'),
  /** Bare ground: dirt, or grass. */
  DIRT('D'),
  SUGAR_CANE('S'),
  WHEAT('H'),
  CARROTS('R'),
  PUMPKIN_STEM('p'),
  MELON_STEM('m'),
  PUMPKIN('P'),
  MELON('M'),
  SAND('A'),
  CACTUS('X');

  private final char symbol;

  PlanCell(char symbol) {
    this.symbol = symbol;
  }

  public char symbol() {
    return symbol;
  }

  /** The cell written as {@code symbol}. */
  public static PlanCell of(char symbol) {
    for (PlanCell cell : values()) {
      if (cell.symbol == symbol) {
        return cell;
      }
    }
    throw new IllegalArgumentException("no plan cell '" + symbol + "'");
  }

  /** A crop on farmland (wheat, carrots, stems): grows faster or slower with its neighbours. */
  public boolean isCrop() {
    return this == WHEAT || this == CARROTS || this == PUMPKIN_STEM || this == MELON_STEM;
  }

  /** A stem's fruit; only stems have one. */
  public PlanCell fruit() {
    return switch (this) {
      case PUMPKIN_STEM -> PUMPKIN;
      case MELON_STEM -> MELON;
      default -> throw new IllegalStateException(this + " is not a stem");
    };
  }
}
