package com.einsteinmods.cheatsheet.core;

/** Where a card goes, relative to the top-left corner of the sheet. */
public record CardBounds(int x, int y, int width, int height) {
  public int right() {
    return x + width;
  }

  public int bottom() {
    return y + height;
  }

  public boolean overlaps(CardBounds other) {
    return x < other.right() && other.x < right() && y < other.bottom() && other.y < bottom();
  }
}
