package com.einsteinmods.cheatsheet.client.sheet;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2fc;

/**
 * Culling against the current scissor, which vanilla only exposes as a point test in screen
 * coordinates: the sheet is drawn under a pose scale (its own scale), so points are transformed by
 * the current pose first. Use these instead of {@code GuiGraphicsExtractor#containsPointInScissor}.
 */
public final class Scissor {
  /**
   * Points tested down a span are at most this far apart, so a span taller than the view (a tall
   * card scrolled through) still counts as visible; no sheet view is this short.
   */
  private static final int PROBE_STEP = 32;

  private Scissor() {}

  /**
   * Whether the vertical span from {@code top}, {@code height} pixels tall at column {@code x},
   * touches the scissor. Its top and bottom, plus a point every {@link #PROBE_STEP} pixels between
   * them; allocates nothing.
   */
  public static boolean touches(GuiGraphicsExtractor graphics, int x, int top, int height) {
    int bottom = top + height - 1;
    for (int y = top; y < bottom; y += PROBE_STEP) {
      if (contains(graphics, x, y)) {
        return true;
      }
    }
    return contains(graphics, x, bottom);
  }

  /** Whether the point ({@code x}, {@code y}), in the current pose's coordinates, is drawn. */
  public static boolean contains(GuiGraphicsExtractor graphics, int x, int y) {
    Matrix3x2fc pose = graphics.pose();
    float screenX = pose.m00() * x + pose.m10() * y + pose.m20();
    float screenY = pose.m01() * x + pose.m11() * y + pose.m21();
    return graphics.containsPointInScissor(Mth.floor(screenX), Mth.floor(screenY));
  }
}
