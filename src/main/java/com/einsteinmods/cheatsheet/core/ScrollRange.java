package com.einsteinmods.cheatsheet.core;

/** Vertical scrolling of a sheet taller than its viewport. */
public final class ScrollRange {
  private ScrollRange() {}

  /** Largest scroll offset: 0 when the content fits. */
  public static int maxOffset(int contentHeight, int viewportHeight) {
    return Math.max(0, contentHeight - viewportHeight);
  }

  /** {@code offset} kept between 0 and {@link #maxOffset}. */
  public static int clamp(int offset, int contentHeight, int viewportHeight) {
    return Math.clamp(offset, 0, maxOffset(contentHeight, viewportHeight));
  }

  /**
   * Height of a scroll bar's thumb on a track of {@code trackHeight} pixels, the share of {@code
   * contentHeight} in view, at least {@code minHeight}.
   */
  public static int thumbHeight(
      int contentHeight, int viewportHeight, int trackHeight, int minHeight) {
    return Math.max(minHeight, trackHeight * viewportHeight / contentHeight);
  }

  /** Where the thumb starts on its track at {@code offset}, of {@code maxOffset} (not 0). */
  public static int thumbOffset(int offset, int maxOffset, int trackHeight, int thumbHeight) {
    return (trackHeight - thumbHeight) * offset / maxOffset;
  }
}
