package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import com.einsteinmods.cheatsheet.client.sheet.SheetPalette;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * The top line of a Commands card: its bold title, an optional muted note after it (how many
 * commands are saved) and an optional button at the right end (add a command).
 */
final class CommandHeader implements SheetElement {
  private static final int NOTE_GAP = 4;
  private static final int TEXT_Y = 2;

  private final Font font;
  private final int width;
  private final FormattedCharSequence title;
  private final int titleColor;
  private final @Nullable FormattedCharSequence note;
  private final int noteX;
  private final @Nullable CommandButton button;

  CommandHeader(
      Font font,
      int width,
      Component title,
      int titleColor,
      @Nullable Component note,
      @Nullable CommandButton button) {
    this.font = font;
    this.width = width;
    Component bold = title.copy().withStyle(ChatFormatting.BOLD);
    this.title = bold.getVisualOrderText();
    this.titleColor = titleColor;
    this.note = note == null ? null : note.getVisualOrderText();
    this.noteX = font.width(bold) + NOTE_GAP;
    this.button = button;
  }

  @Override
  public int minWidth() {
    return width;
  }

  @Override
  public int height() {
    return CommandRow.HEIGHT;
  }

  @Override
  public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
    graphics.text(font, title, x, y + TEXT_Y, titleColor, true);
    FormattedCharSequence shownNote = note;
    if (shownNote != null) {
      graphics.text(font, shownNote, x + noteX, y + TEXT_Y, SheetPalette.MUTED_TEXT, true);
    }
    CommandButton shownButton = button;
    if (shownButton != null) {
      int buttonX = x + this.width - CommandButton.SIZE;
      boolean hovered =
          CommandPointer.inside(
              graphics,
              CommandPointer.x(),
              CommandPointer.y(),
              buttonX,
              y + 1,
              CommandButton.SIZE,
              CommandButton.SIZE);
      shownButton.extract(graphics, buttonX, y + 1, hovered, false);
    }
  }

  @Override
  public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
    CommandButton shownButton = button;
    return shownButton != null && overButton(localX, localY) ? shownButton.tooltip() : null;
  }

  @Override
  public boolean mouseClicked(int localX, int localY, int width) {
    CommandButton shownButton = button;
    return shownButton != null
        && overButton(localX, localY)
        && shownButton.press() != CommandButton.Press.IGNORED;
  }

  private boolean overButton(int localX, int localY) {
    return localX >= width - CommandButton.SIZE && localY >= 1 && localY < 1 + CommandButton.SIZE;
  }
}
