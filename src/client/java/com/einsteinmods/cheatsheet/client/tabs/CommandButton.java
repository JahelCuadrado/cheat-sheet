package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.CheatSheetClient;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

/**
 * A small square icon button of the Commands tab (chat, copy, run, favourite, edit, delete, add),
 * drawn by the element that holds it: a faint square that lights up under the mouse, and a white
 * icon (tinted for a dangerous command, dimmed when disabled). Its tooltip and action are fixed
 * when the sheet is built.
 *
 * @param icon the 7x7 sprite
 * @param iconColor the icon's tint
 * @param tooltip lines under the mouse, built once
 * @param action what a click does; returns whether it shows a green tick for a moment (copied,
 *     sent, saved) in place of the icon
 * @param enabled whether it reacts to the mouse
 */
record CommandButton(
    Identifier icon,
    int iconColor,
    List<FormattedCharSequence> tooltip,
    BooleanSupplier action,
    boolean enabled) {
  /** Side of the square. */
  static final int SIZE = 9;

  /** Space between buttons of a row. */
  static final int GAP = 1;

  static final Identifier CHAT = sprite("chat");
  static final Identifier COPY = sprite("copy");
  static final Identifier RUN = sprite("run");
  static final Identifier EDIT = sprite("edit");
  static final Identifier DELETE = sprite("delete");
  static final Identifier ADD = sprite("add");
  static final Identifier FAVOURITE = sprite("favourite");
  static final Identifier DONE = sprite("done");

  static final int ICON = 0xFFE8E8F0;
  static final int DANGER_ICON = 0xFFFF7A6E;
  static final int FAVOURITE_ICON = 0xFFFFD84A;
  static final int DONE_ICON = 0xFF7CE07C;
  private static final int DISABLED_ICON = 0x60E8E8F0;
  private static final int FILL = 0x30FFFFFF;
  private static final int HOVER_FILL = 0xFF5C5C72;
  private static final int ICON_SIZE = 7;
  private static final int ICON_INSET = (SIZE - ICON_SIZE) / 2;

  /** Width of {@code count} buttons side by side. */
  static int rowWidth(int count) {
    return count * SIZE + Math.max(0, count - 1) * GAP;
  }

  /** Draws the button at ({@code x}, {@code y}); {@code done} shows a green tick instead. */
  void extract(GuiGraphicsExtractor graphics, int x, int y, boolean hovered, boolean done) {
    boolean lit = enabled && hovered;
    graphics.fill(x, y, x + SIZE, y + SIZE, lit ? HOVER_FILL : FILL);
    Identifier sprite = done ? DONE : icon;
    int color = done ? DONE_ICON : enabled ? iconColor : DISABLED_ICON;
    graphics.blitSprite(
        RenderPipelines.GUI_TEXTURED,
        sprite,
        x + ICON_INSET,
        y + ICON_INSET,
        ICON_SIZE,
        ICON_SIZE,
        color);
  }

  /** Whether a press did anything, and whether it shows the green tick. */
  enum Press {
    IGNORED,
    DONE,
    CONFIRMED
  }

  /** Runs the action with vanilla's click sound; ignored when disabled. */
  Press press() {
    if (!enabled) {
      return Press.IGNORED;
    }
    AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
    return action.getAsBoolean() ? Press.CONFIRMED : Press.DONE;
  }

  private static Identifier sprite(String name) {
    return CheatSheetClient.id("commands/" + name);
  }
}
