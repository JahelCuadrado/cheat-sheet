package com.einsteinmods.cheatsheet.client.sheet;

import com.einsteinmods.cheatsheet.core.ScrollRange;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** The thin scroll bar of the sheet and of a scrolling zoom. Two fills, no allocation. */
public final class ScrollBar {
  /** Width of the bar. */
  public static final int WIDTH = 3;

  private static final int MIN_THUMB_HEIGHT = 10;

  private ScrollBar() {}

  /**
   * The bar with its left edge at {@code left}, from {@code top} to {@code bottom}, for content of
   * {@code contentHeight} seen through {@code viewportHeight} (any unit, the same for both) and
   * scrolled by {@code offset}; nothing when the content fits.
   */
  public static void extract(
      GuiGraphicsExtractor graphics,
      int left,
      int top,
      int bottom,
      int contentHeight,
      int viewportHeight,
      int offset) {
    int maxOffset = ScrollRange.maxOffset(contentHeight, viewportHeight);
    if (maxOffset == 0) {
      return;
    }
    int right = left + WIDTH;
    int track = bottom - top;
    graphics.fill(left, top, right, bottom, SheetPalette.SCROLL_BAR_TRACK);
    int thumb = ScrollRange.thumbHeight(contentHeight, viewportHeight, track, MIN_THUMB_HEIGHT);
    int thumbTop = top + ScrollRange.thumbOffset(offset, maxOffset, track, thumb);
    graphics.fill(left, thumbTop, right, thumbTop + thumb, SheetPalette.SCROLL_BAR_THUMB);
  }
}
