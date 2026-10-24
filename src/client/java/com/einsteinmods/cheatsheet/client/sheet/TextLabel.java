package com.einsteinmods.cheatsheet.client.sheet;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * One line of text, at 1x or bigger (big numbers, recipe signs). The text is laid out once; an
 * optional tooltip explains it.
 */
public final class TextLabel implements SheetElement {
  private final Font font;
  private final FormattedCharSequence text;
  private final int color;
  private final int scale;
  private final int width;
  private final @Nullable List<FormattedCharSequence> tooltip;

  public TextLabel(
      Font font,
      FormattedCharSequence text,
      int color,
      int scale,
      @Nullable List<FormattedCharSequence> tooltip) {
    if (scale < 1) {
      throw new IllegalArgumentException("scale must be at least 1: " + scale);
    }
    this.font = font;
    this.text = text;
    this.color = color;
    this.scale = scale;
    this.width = Math.max(1, font.width(text) * scale);
    this.tooltip = tooltip == null ? null : List.copyOf(tooltip);
  }

  @Override
  public int minWidth() {
    return width;
  }

  @Override
  public int height() {
    return font.lineHeight * scale;
  }

  @Override
  public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
    if (scale == 1) {
      graphics.text(font, text, x, y, color, true);
      return;
    }
    graphics.pose().pushMatrix();
    graphics.pose().translate(x, y);
    graphics.pose().scale(scale, scale);
    graphics.text(font, text, 0, 0, color, true);
    graphics.pose().popMatrix();
  }

  @Override
  public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
    return tooltip;
  }
}
