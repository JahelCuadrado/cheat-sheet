package com.einsteinmods.cheatsheet.core;

/**
 * How big a zoomed card gets, chosen per tab or per card by what there is to see bigger: a card
 * that gives little new information zoomed (a dye) stays {@link #SMALL}, a dense one (a villager's
 * trades) gets {@link #LARGE}. Never the whole screen: each size has a largest factor over the
 * card's sheet size and a largest share of the window, and the sheet stays visible around it.
 *
 * <p>The zoom draws the card at a whole number of window pixels per card pixel ({@link #scale(int,
 * int, int, int, int)}), so every icon, text and picture lands on whole pixels. Pure arithmetic.
 */
public enum ZoomSize {
  /** About 1.5 times the card, at most 60% of the window. */
  SMALL(3, 60),
  /** About twice the card, at most three quarters of the window. */
  MEDIUM(4, 75),
  /** About 2.5 times the card, at most 90% of the window. */
  LARGE(5, 90);

  /** Factors are written in halves: 3 is 1.5 times. */
  private static final int HALVES = 2;

  private static final int PERCENT = 100;

  private final int maxHalves;
  private final int maxPercent;

  ZoomSize(int maxHalves, int maxPercent) {
    this.maxHalves = maxHalves;
    this.maxPercent = maxPercent;
  }

  /**
   * Window pixels per card pixel for a card of {@code cardWidth} x {@code cardHeight} card pixels
   * on a window of {@code windowWidth} x {@code windowHeight} pixels whose sheet is drawn at {@code
   * sheetScale}: the largest whole scale within this size's factor and share of the window, at
   * least one step above the sheet when that still fits the share, and never below the sheet scale
   * (a zoom never shrinks a card).
   */
  public int scale(
      int sheetScale, int windowWidth, int windowHeight, int cardWidth, int cardHeight) {
    if (sheetScale <= 0 || cardWidth <= 0 || cardHeight <= 0) {
      throw new IllegalArgumentException(
          "bad zoom: sheet " + sheetScale + ", card " + cardWidth + "x" + cardHeight);
    }
    int byFactor = Math.max(sheetScale + 1, sheetScale * maxHalves / HALVES);
    int byWidth = windowWidth * maxPercent / PERCENT / cardWidth;
    int byHeight = windowHeight * maxPercent / PERCENT / cardHeight;
    return Math.max(sheetScale, Math.min(byFactor, Math.min(byWidth, byHeight)));
  }
}
