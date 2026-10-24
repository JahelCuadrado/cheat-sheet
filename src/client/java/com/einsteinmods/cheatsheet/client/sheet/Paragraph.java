package com.einsteinmods.cheatsheet.client.sheet;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Short text wrapped once, at build time, to a fixed width: notes and captions. */
public final class Paragraph implements SheetElement {
  private final Font font;
  private final List<FormattedCharSequence> lines;
  private final int color;
  private final int width;

  private static final String WORD_SEPARATOR = " ";

  /**
   * Text wrapped at {@code wrapWidth}, or at its widest word when wider: a word never splits (a
   * long name such as "saqueadores" stays whole).
   */
  public static Paragraph wholeWords(Font font, Component text, int wrapWidth, int color) {
    int widest = wrapWidth;
    for (String word : text.getString().split(WORD_SEPARATOR)) {
      widest = Math.max(widest, font.width(Component.literal(word).withStyle(text.getStyle())));
    }
    return new Paragraph(font, text, widest, color);
  }

  public Paragraph(Font font, Component text, int wrapWidth, int color) {
    this.font = font;
    this.lines = List.copyOf(font.split(text, wrapWidth));
    this.color = color;
    int widest = 1;
    for (FormattedCharSequence line : lines) {
      widest = Math.max(widest, font.width(line));
    }
    this.width = widest;
  }

  @Override
  public int minWidth() {
    return width;
  }

  @Override
  public int height() {
    return Math.max(1, lines.size()) * font.lineHeight;
  }

  @Override
  public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
    for (int index = 0; index < lines.size(); index++) {
      graphics.text(font, lines.get(index), x, y + index * font.lineHeight, color, true);
    }
  }
}
