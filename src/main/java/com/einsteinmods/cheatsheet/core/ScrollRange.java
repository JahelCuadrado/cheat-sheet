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
}
