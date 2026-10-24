package com.einsteinmods.cheatsheet.client.sheet;

import com.einsteinmods.cheatsheet.core.CardSize;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * A tinted rounded panel with a bold title and its elements stacked below, each centred. A title
 * wider than the content wraps (both forms are laid out when built) instead of widening the card.
 */
public final class Card {
  /** Narrowest card, and so the sheet's narrowest column: titles never wrap narrower than this. */
  public static final int MIN_WIDTH = 72;

  /** Space between the border and the content. */
  public static final int PADDING = 4;

  /** Space between stacked elements, and between the title and the first element. */
  public static final int ELEMENT_GAP = 3;

  private static final String WORD_SEPARATOR = " ";

  private final Font font;
  private final FormattedCharSequence title;
  private final int titleWidth;
  private final FormattedCharSequence[] wrappedTitle;
  private final int[] wrappedTitleWidths;
  private final CardTint tint;
  private final List<SheetElement> elements;
  private final CardSize size;

  public Card(Font font, Component title, CardTint tint, List<SheetElement> elements) {
    this.font = font;
    Component boldTitle = title.copy().withStyle(ChatFormatting.BOLD);
    this.title = boldTitle.getVisualOrderText();
    this.titleWidth = font.width(this.title);
    this.tint = tint;
    this.elements = List.copyOf(elements);
    int minContent = 1;
    int bodyHeight = 0;
    for (SheetElement element : this.elements) {
      minContent = Math.max(minContent, element.minWidth());
      bodyHeight += ELEMENT_GAP + element.height();
    }
    int wrapWidth =
        Math.max(Math.max(minContent, MIN_WIDTH - PADDING * 2), longestWord(font, title));
    List<FormattedCharSequence> lines = font.split(boldTitle, wrapWidth);
    this.wrappedTitle = lines.toArray(new FormattedCharSequence[0]);
    this.wrappedTitleWidths = new int[wrappedTitle.length];
    int wrappedWidth = 0;
    for (int index = 0; index < wrappedTitle.length; index++) {
      wrappedTitleWidths[index] = font.width(wrappedTitle[index]);
      wrappedWidth = Math.max(wrappedWidth, wrappedTitleWidths[index]);
    }
    int height = PADDING * 2 + font.lineHeight + bodyHeight;
    this.size =
        new CardSize(
            Math.max(minContent, wrappedWidth) + PADDING * 2,
            height,
            Math.max(minContent, titleWidth) + PADDING * 2,
            height + (Math.max(1, wrappedTitle.length) - 1) * font.lineHeight);
  }

  /** Widest word of the title in bold: wrapping never splits a word. Build time only. */
  private static int longestWord(Font font, Component title) {
    int widest = 0;
    for (String word : title.getString().split(WORD_SEPARATOR)) {
      widest = Math.max(widest, font.width(Component.literal(word).withStyle(ChatFormatting.BOLD)));
    }
    return widest;
  }

  public CardSize size() {
    return size;
  }

  public void extract(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
    Panels.rounded(graphics, x, y, width, height, tint.fill(), tint.border());
    int innerWidth = width - PADDING * 2;
    int cursorY = y + PADDING;
    if (width >= size.oneLineWidth()) {
      graphics.text(font, title, x + (width - titleWidth) / 2, cursorY, tint.title(), true);
      cursorY += font.lineHeight;
    } else {
      for (int index = 0; index < wrappedTitle.length; index++) {
        graphics.text(
            font,
            wrappedTitle[index],
            x + (width - wrappedTitleWidths[index]) / 2,
            cursorY,
            tint.title(),
            true);
        cursorY += font.lineHeight;
      }
    }
    for (int index = 0; index < elements.size(); index++) {
      SheetElement element = elements.get(index);
      cursorY += ELEMENT_GAP;
      int elementWidth = Math.min(innerWidth, element.width(innerWidth));
      element.extract(
          graphics, x + PADDING + (innerWidth - elementWidth) / 2, cursorY, elementWidth);
      cursorY += element.height();
    }
  }

  /** Tooltip of the element under ({@code localX}, {@code localY}) from the card's corner. */
  public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
    int innerWidth = width - PADDING * 2;
    int titleLines = width >= size.oneLineWidth() ? 1 : wrappedTitle.length;
    int cursorY = PADDING + font.lineHeight * titleLines;
    for (int index = 0; index < elements.size(); index++) {
      SheetElement element = elements.get(index);
      cursorY += ELEMENT_GAP;
      int elementWidth = Math.min(innerWidth, element.width(innerWidth));
      int elementX = PADDING + (innerWidth - elementWidth) / 2;
      if (localY >= cursorY
          && localY < cursorY + element.height()
          && localX >= elementX
          && localX < elementX + elementWidth) {
        return element.tooltipAt(localX - elementX, localY - cursorY, elementWidth);
      }
      cursorY += element.height();
    }
    return null;
  }

  /** This card as laid out ({@code width} x {@code height}), to zoom. */
  public ZoomCard zoomCard(int width, int height) {
    Card card = this;
    return new ZoomCard() {
      @Override
      public int width() {
        return width;
      }

      @Override
      public int height() {
        return height;
      }

      @Override
      public void extract(GuiGraphicsExtractor graphics, int x, int y) {
        card.extract(graphics, x, y, width, height);
      }

      @Override
      public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY) {
        return card.tooltipAt(localX, localY, width);
      }
    };
  }
}
