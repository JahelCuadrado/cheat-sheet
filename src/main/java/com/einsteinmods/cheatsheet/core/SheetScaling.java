package com.einsteinmods.cheatsheet.core;

import java.util.function.IntPredicate;

/**
 * Picks the sheet scale (window pixels per sheet pixel) and converts between the game's GUI pixels
 * and sheet pixels. Scales are whole numbers: every sheet pixel then covers the same square of
 * window pixels, so item icons, text and baked pictures stay crisp (a fractional scale draws some
 * pixel columns one window pixel wider than others).
 *
 * <p>Auto takes the largest scale at which the whole tab fits the window without scrolling, but
 * never less than {@code windowHeight / }{@link #REFERENCE_HEIGHT}, rounded to the nearest whole
 * scale (2 on a 1080p window: a canvas of 960x540; 1 at 720p, 3 at 1440p), so a long tab scrolls
 * instead of shrinking to unreadable text. No scale makes the canvas smaller than {@link
 * #MIN_CANVAS_WIDTH}x{@link #MIN_CANVAS_HEIGHT}, the smallest GUI vanilla allows.
 */
public final class SheetScaling {
  /** Auto never draws the sheet smaller than a canvas about this many sheet pixels tall. */
  public static final int REFERENCE_HEIGHT = 540;

  public static final int MIN_CANVAS_WIDTH = 320;
  public static final int MIN_CANVAS_HEIGHT = 240;

  private SheetScaling() {}

  /** Largest scale that leaves a canvas of at least {@code 320x240} (at least 1). */
  public static int maxScale(int windowWidth, int windowHeight) {
    return Math.max(1, Math.min(windowWidth / MIN_CANVAS_WIDTH, windowHeight / MIN_CANVAS_HEIGHT));
  }

  /**
   * Smallest scale auto picks: {@code windowHeight / 540} rounded to the nearest, between 1 and
   * {@link #maxScale}.
   */
  public static int minAutoScale(int windowWidth, int windowHeight) {
    int nearest = (windowHeight + REFERENCE_HEIGHT / 2) / REFERENCE_HEIGHT;
    return Math.clamp(nearest, 1, maxScale(windowWidth, windowHeight));
  }

  /**
   * The scale to draw at. A fixed setting is capped at {@link #maxScale}; auto tries every scale
   * from {@link #maxScale} down to {@link #minAutoScale} and takes the first at which {@code
   * fitsWithoutScrolling} holds, else {@link #minAutoScale} (the sheet scrolls).
   *
   * @param fitsWithoutScrolling whether the whole tab fits the window at a scale; called at most
   *     {@code maxScale - minAutoScale + 1} times
   */
  public static int choose(
      SheetScale setting, int windowWidth, int windowHeight, IntPredicate fitsWithoutScrolling) {
    int max = maxScale(windowWidth, windowHeight);
    if (setting.fixed().isPresent()) {
      return Math.min(setting.fixed().getAsInt(), max);
    }
    int min = minAutoScale(windowWidth, windowHeight);
    for (int scale = max; scale > min; scale--) {
      if (fitsWithoutScrolling.test(scale)) {
        return scale;
      }
    }
    return min;
  }

  /** Sheet pixels across {@code windowPixels} window pixels at {@code sheetScale}. */
  public static int toSheet(int windowPixels, int sheetScale) {
    return windowPixels / sheetScale;
  }
}
