package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.sheet.Panels;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * A button centred across {@code width}, under a zoom's quick look: «Show explanation» or «Hide
 * explanation». A click on it tells the card to switch ({@link ZoomViews#explained}). Laid out
 * once; drawing allocates nothing.
 */
final class ExplanationButton implements SheetElement {
  private static final int PADDING_X = 8;
  private static final int PADDING_Y = 3;
  private static final int FILL = 0x40FFFFFF;
  private static final int BORDER = 0xFFB0B0C0;
  private static final int LABEL_COLOR = 0xFFFFFFFF;

  private final Font font;
  private final FormattedCharSequence label;
  private final int width;
  private final int buttonWidth;
  private final int buttonHeight;

  ExplanationButton(Font font, Component label, int width) {
    this.font = font;
    this.label = label.getVisualOrderText();
    this.buttonWidth = font.width(this.label) + PADDING_X * 2;
    this.buttonHeight = font.lineHeight + PADDING_Y * 2;
    this.width = Math.max(width, buttonWidth);
  }

  @Override
  public int minWidth() {
    return width;
  }

  @Override
  public int height() {
    return buttonHeight;
  }

  private int left() {
    return (width - buttonWidth) / 2;
  }

  @Override
  public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
    int left = x + left();
    Panels.rounded(graphics, left, y, buttonWidth, buttonHeight, FILL, BORDER);
    graphics.text(font, label, left + PADDING_X, y + PADDING_Y + 1, LABEL_COLOR, true);
  }

  @Override
  public boolean mouseClicked(int localX, int localY, int width) {
    return localX >= left()
        && localX < left() + buttonWidth
        && localY >= 0
        && localY < buttonHeight;
  }
}
