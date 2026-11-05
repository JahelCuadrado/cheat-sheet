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
    int byHeight = share(windowHeight) / cardHeight;
    return Math.max(
        sheetScale,
        Math.min(byFactor(sheetScale), Math.min(share(windowWidth) / cardWidth, byHeight)));
  }

  /**
   * {@link #scale} for a card that scrolls inside the zoom: its height does not count, so a tall
   * card keeps the scale of its width instead of shrinking to fit.
   */
  public int scrollingScale(int sheetScale, int windowWidth, int cardWidth) {
    if (sheetScale <= 0 || cardWidth <= 0) {
      throw new IllegalArgumentException("bad zoom: sheet " + sheetScale + ", card " + cardWidth);
    }
    return Math.max(sheetScale, Math.min(byFactor(sheetScale), share(windowWidth) / cardWidth));
  }

  /**
   * Card pixels of height a scrolling card shows at {@code scale}: all of it, or what fits the
   * {@link #LARGE} share of a window of {@code windowHeight} pixels, at least one pixel. Whatever
   * its size, a card that is read top to bottom shows as much as the window allows.
   */
  public int scrollingViewport(int windowHeight, int scale, int cardHeight) {
    return Math.max(1, Math.min(cardHeight, LARGE.share(windowHeight) / scale));
  }

  /** This size's largest scale by its factor, at least one step above the sheet. */
  private int byFactor(int sheetScale) {
    return Math.max(sheetScale + 1, sheetScale * maxHalves / HALVES);
  }

  /** This size's share of {@code windowSide} pixels. */
  private int share(int windowSide) {
    return windowSide * maxPercent / PERCENT;
  }
}
