package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Short text wrapped once, at build time, with every line centred: notes under a centred picture.
 */
final class CentredText implements SheetElement {
  private final Font font;
  private final FormattedCharSequence[] lines;
  private final int[] offsets;
  private final int color;
  private final int width;

  CentredText(Font font, Component text, int wrapWidth, int color) {
    this.font = font;
    this.lines = font.split(text, wrapWidth).toArray(new FormattedCharSequence[0]);
    this.color = color;
    int widest = 1;
    for (FormattedCharSequence line : lines) {
      widest = Math.max(widest, font.width(line));
    }
    this.width = widest;
    this.offsets = new int[lines.length];
    for (int index = 0; index < lines.length; index++) {
      offsets[index] = (widest - font.width(lines[index])) / 2;
    }
  }

  @Override
  public int minWidth() {
    return width;
  }

  @Override
  public int height() {
    return Math.max(1, lines.length) * font.lineHeight;
  }

  @Override
  public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
    for (int index = 0; index < lines.length; index++) {
      graphics.text(
          font, lines[index], x + offsets[index], y + index * font.lineHeight, color, true);
    }
  }
}
