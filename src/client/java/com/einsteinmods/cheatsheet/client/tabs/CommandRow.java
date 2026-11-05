package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

/**
 * One command of the Commands tab, on one line: its name, the command in a dark strip (cut with "…"
 * when longer; a combo shows how many commands it sends, then its first), and its buttons at the
 * right end. The row's tooltip has the whole command and what it does; a button's tooltip says what
 * the button does. Texts are cut and laid out when built; drawing reads the mouse once (hover
 * light) and allocates nothing.
 */
final class CommandRow implements SheetElement {
  static final int HEIGHT = 11;

  /** Colour of the command text: a pale green, like a terminal. */
  static final int CODE_TEXT = 0xFFB8E6A8;

  private static final int CODE_BACKGROUND = 0x70000000;
  private static final int TEXT_Y = 2;
  private static final int STRIP_TOP = 1;
  private static final int STRIP_PADDING = 2;
  private static final int NAME_GAP = 3;
  private static final int BUTTONS_GAP = 2;
  private static final int BUTTON_Y = 1;
  private static final String ELLIPSIS = "…";

  /** After a combo's command count, in the strip: "16× give @s …". */
  private static final String COUNT_SUFFIX = "× ";

  private static final int COUNT_TEXT = 0xFFE0C060;

  /** How long a copied or sent command shows its green tick. */
  private static final long CONFIRM_MILLIS = 1200L;

  private final Font font;
  private final int width;
  private final FormattedCharSequence name;
  private final int nameColor;
  private final FormattedCharSequence code;
  private final int stripX;
  private final int stripWidth;
  private final CommandButton[] buttons;
  private final int buttonsX;
  private final List<FormattedCharSequence> tooltip;
  private int confirmedButton = -1;
  private long confirmedUntil;

  /**
   * @param width the row's width
   * @param nameWidth room for the name (cut to fit); the command strip takes what is left
   */
  CommandRow(
      Font font,
      int width,
      String name,
      int nameColor,
      int nameWidth,
      List<String> lines,
      List<CommandButton> buttons,
      List<FormattedCharSequence> tooltip) {
    this.font = font;
    this.width = width;
    this.name = Component.literal(cut(font, name, nameWidth)).getVisualOrderText();
    this.nameColor = nameColor;
    this.buttons = buttons.toArray(new CommandButton[0]);
    this.buttonsX = width - CommandButton.rowWidth(this.buttons.length);
    this.stripX = nameWidth + NAME_GAP;
    this.stripWidth = Math.max(1, buttonsX - BUTTONS_GAP - stripX);
    int codeWidth = stripWidth - STRIP_PADDING * 2;
    if (lines.size() == 1) {
      this.code = Component.literal(cut(font, lines.getFirst(), codeWidth)).getVisualOrderText();
    } else {
      String count = lines.size() + COUNT_SUFFIX;
      this.code =
          Component.empty()
              .append(Component.literal(count).withColor(COUNT_TEXT))
              .append(Component.literal(cut(font, lines.getFirst(), codeWidth - font.width(count))))
              .getVisualOrderText();
    }
    this.tooltip = List.copyOf(tooltip);
  }

  /** {@code text} cut to {@code width} pixels with "…" at the end when it is wider. */
  static String cut(Font font, String text, int width) {
    if (font.width(text) <= width) {
      return text;
    }
    return font.plainSubstrByWidth(text, Math.max(0, width - font.width(ELLIPSIS))).stripTrailing()
        + ELLIPSIS;
  }

  @Override
  public int minWidth() {
    return width;
  }

  @Override
  public int height() {
    return HEIGHT;
  }

  @Override
  public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
    graphics.text(font, name, x, y + TEXT_Y, nameColor, true);
    int stripLeft = x + stripX;
    graphics.fill(
        stripLeft, y + STRIP_TOP, stripLeft + stripWidth, y + HEIGHT - STRIP_TOP, CODE_BACKGROUND);
    graphics.text(font, code, stripLeft + STRIP_PADDING, y + TEXT_Y, CODE_TEXT, false);
    int mouseX = CommandPointer.x();
    int mouseY = CommandPointer.y();
    boolean confirming = confirmedButton >= 0 && Util.getMillis() < confirmedUntil;
    int buttonX = x + buttonsX;
    for (int index = 0; index < buttons.length; index++) {
      boolean hovered =
          CommandPointer.inside(
              graphics,
              mouseX,
              mouseY,
              buttonX,
              y + BUTTON_Y,
              CommandButton.SIZE,
              CommandButton.SIZE);
      buttons[index].extract(
          graphics, buttonX, y + BUTTON_Y, hovered, confirming && index == confirmedButton);
      buttonX += CommandButton.SIZE + CommandButton.GAP;
    }
  }

  @Override
  public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
    int button = buttonAt(localX, localY);
    return button >= 0 ? buttons[button].tooltip() : tooltip;
  }

  @Override
  public boolean mouseClicked(int localX, int localY, int width) {
    int button = buttonAt(localX, localY);
    if (button < 0) {
      return false;
    }
    CommandButton.Press press = buttons[button].press();
    if (press == CommandButton.Press.CONFIRMED) {
      confirm(button);
    }
    return press != CommandButton.Press.IGNORED;
  }

  /** Shows the green tick on button {@code button} for a moment. */
  void confirm(int button) {
    confirmedButton = button;
    confirmedUntil = Util.getMillis() + CONFIRM_MILLIS;
  }

  /** Index of the button at the point from the row's corner, or -1. */
  private int buttonAt(int localX, int localY) {
    int offset = localX - buttonsX;
    if (offset < 0 || localY < BUTTON_Y || localY >= BUTTON_Y + CommandButton.SIZE) {
      return -1;
    }
    int pitch = CommandButton.SIZE + CommandButton.GAP;
    int index = offset / pitch;
    return index < buttons.length && offset % pitch < CommandButton.SIZE ? index : -1;
  }
}
