package com.einsteinmods.cheatsheet.client.sheet;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Panel drawing shared by cards and tabs. */
public final class Panels {
  private static final int BORDER = 1;

  private Panels() {}

  /**
   * A filled panel with a one-pixel border whose corner pixels are left out, which reads as rounded
   * at GUI scale. Five fills.
   */
  public static void rounded(
      GuiGraphicsExtractor graphics, int x, int y, int width, int height, int fill, int border) {
    int right = x + width;
    int bottom = y + height;
    graphics.fill(x + BORDER, y + BORDER, right - BORDER, bottom - BORDER, fill);
    graphics.fill(x + BORDER, y, right - BORDER, y + BORDER, border);
    graphics.fill(x + BORDER, bottom - BORDER, right - BORDER, bottom, border);
    graphics.fill(x, y + BORDER, x + BORDER, bottom - BORDER, border);
    graphics.fill(right - BORDER, y + BORDER, right, bottom - BORDER, border);
  }
}
