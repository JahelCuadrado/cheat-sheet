package com.einsteinmods.cheatsheet.core;

/**
 * Where a zoomed card goes on the window: on a panel centred where the card was clicked, kept
 * inside the window, the sheet around it left where it was. The card is drawn at {@link
 * Input#scale()} window pixels per card pixel ({@link ZoomSize#scale}); the panel is in sheet
 * pixels, so it lines up with the sheet's own pixels, and the card is centred in it on whole window
 * pixels. Computed once per click; pure arithmetic.
 *
 * @param cardX the card's left edge in window pixels
 * @param cardY the card's top edge in window pixels
 * @param panel the panel behind the card, in sheet pixels
 */
public record ZoomLayout(int cardX, int cardY, Rect panel) {
  /** Space kept between the panel and the window's edges, in sheet pixels. */
  public static final int MARGIN = 6;

  /** Space between the panel's edge and the card, in sheet pixels. */
  public static final int PADDING = 4;

  /** A rectangle. */
  public record Rect(int x, int y, int width, int height) {
    public int right() {
      return x + width;
    }

    public int bottom() {
      return y + height;
    }

    public boolean contains(double pointX, double pointY) {
      return pointX >= x && pointX < right() && pointY >= y && pointY < bottom();
    }
  }

  /**
   * What to lay out.
   *
   * @param windowWidth the window in window pixels
   * @param windowHeight the window in window pixels
   * @param sheetScale window pixels per sheet pixel
   * @param scale window pixels per card pixel
   * @param cardWidth the card in card pixels
   * @param cardHeight the card in card pixels
   * @param anchorX where the zoom is centred (the clicked card's centre), in window pixels
   * @param anchorY where the zoom is centred, in window pixels
   */
  public record Input(
      int windowWidth,
      int windowHeight,
      int sheetScale,
      int scale,
      int cardWidth,
      int cardHeight,
      int anchorX,
      int anchorY) {
    public Input {
      if (sheetScale <= 0 || scale <= 0) {
        throw new IllegalArgumentException("bad scales: sheet " + sheetScale + ", card " + scale);
      }
      if (cardWidth <= 0 || cardHeight <= 0) {
        throw new IllegalArgumentException("bad card: " + cardWidth + "x" + cardHeight);
      }
    }
  }

  public static ZoomLayout of(Input input) {
    int sheet = input.sheetScale();
    int cardWidth = input.cardWidth() * input.scale();
    int cardHeight = input.cardHeight() * input.scale();
    int panelWidth = Math.ceilDiv(cardWidth + PADDING * 2 * sheet, sheet);
    int panelHeight = Math.ceilDiv(cardHeight + PADDING * 2 * sheet, sheet);
    int panelX =
        place(input.anchorX() / sheet - panelWidth / 2, panelWidth, input.windowWidth() / sheet);
    int panelY =
        place(input.anchorY() / sheet - panelHeight / 2, panelHeight, input.windowHeight() / sheet);
    int cardX = panelX * sheet + (panelWidth * sheet - cardWidth) / 2;
    int cardY = panelY * sheet + (panelHeight * sheet - cardHeight) / 2;
    return new ZoomLayout(cardX, cardY, new Rect(panelX, panelY, panelWidth, panelHeight));
  }

  /**
   * {@code start} moved, if needed, so that {@code size} fits between the margins of {@code
   * available}; centred when it is wider than the room between them.
   */
  private static int place(int start, int size, int available) {
    int last = available - MARGIN - size;
    if (last < MARGIN) {
      return (available - size) / 2;
    }
    return Math.clamp(start, MARGIN, last);
  }
}
