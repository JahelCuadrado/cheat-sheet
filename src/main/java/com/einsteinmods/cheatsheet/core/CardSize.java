package com.einsteinmods.cheatsheet.core;

/**
 * What a card needs. Its title may wrap: narrower than {@code oneLineWidth}, the title takes more
 * lines and the card is {@code wrappedHeight} tall; heights depend on nothing else, so a sheet lays
 * out in one pass per column count.
 *
 * @param minWidth narrowest width its content (and its wrapped title) fits in
 * @param height its height with the title on one line
 * @param oneLineWidth narrowest width that keeps the title on one line (at least {@code minWidth})
 * @param wrappedHeight its height with the title wrapped (at least {@code height})
 */
public record CardSize(int minWidth, int height, int oneLineWidth, int wrappedHeight) {
  public CardSize {
    if (minWidth <= 0 || height <= 0 || oneLineWidth < minWidth || wrappedHeight < height) {
      throw new IllegalArgumentException(
          "bad card size: "
              + minWidth
              + "x"
              + height
              + ", one line from "
              + oneLineWidth
              + ", wrapped "
              + wrappedHeight);
    }
  }

  /** A card whose title always fits on one line. */
  public static CardSize fixed(int minWidth, int height) {
    return new CardSize(minWidth, height, minWidth, height);
  }

  /** Height when drawn {@code width} pixels wide. */
  public int heightAt(int width) {
    return width >= oneLineWidth ? height : wrappedHeight;
  }
}
