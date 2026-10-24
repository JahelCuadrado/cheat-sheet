package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.sheet.CardTint;
import com.einsteinmods.cheatsheet.client.sheet.Panels;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import com.einsteinmods.cheatsheet.client.sheet.TextLabel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * A tile of a fixed size, like a printed card: a tinted panel with each piece of information at a
 * fixed spot (the tab gives every tile of a grid the same size and the same spots, so tiles read
 * alike). Spots left empty stay blank. Drawing allocates nothing.
 */
final class SpotTile implements SheetElement {
  /** An element and its top-left corner inside the tile. */
  record Slot(SheetElement element, int x, int y) {}

  private static final String WORD_SEPARATOR = " ";

  private final int width;
  private final int height;
  private final int fill;
  private final int border;
  private final SheetElement[] elements;
  private final int[] xs;
  private final int[] ys;
  private final @Nullable List<FormattedCharSequence> tooltip;

  /**
   * @param tooltip shown anywhere on the tile where no slot has its own; null for none
   */
  SpotTile(
      int width,
      int height,
      CardTint tint,
      List<Slot> slots,
      @Nullable List<FormattedCharSequence> tooltip) {
    this.width = width;
    this.height = height;
    this.fill = tint.fill();
    this.border = tint.border();
    this.elements = new SheetElement[slots.size()];
    this.xs = new int[slots.size()];
    this.ys = new int[slots.size()];
    for (int index = 0; index < slots.size(); index++) {
      Slot slot = slots.get(index);
      elements[index] = slot.element();
      xs[index] = slot.x();
      ys[index] = slot.y();
    }
    this.tooltip = tooltip == null ? null : List.copyOf(tooltip);
  }

  @Override
  public int minWidth() {
    return width;
  }

  @Override
  public int height() {
    return height;
  }

  @Override
  public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
    // A stratum per tile: vanilla's placement scan then covers one tile's elements, not a row's.
    graphics.nextStratum();
    Panels.rounded(graphics, x, y, this.width, height, fill, border);
    for (int index = 0; index < elements.length; index++) {
      SheetElement element = elements[index];
      element.extract(graphics, x + xs[index], y + ys[index], element.minWidth());
    }
  }

  /** The topmost slot's tooltip under the mouse, else the tile's own. */
  @Override
  public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
    for (int index = elements.length - 1; index >= 0; index--) {
      SheetElement element = elements[index];
      int slotX = localX - xs[index];
      int slotY = localY - ys[index];
      if (slotX >= 0 && slotX < element.minWidth() && slotY >= 0 && slotY < element.height()) {
        List<FormattedCharSequence> found = element.tooltipAt(slotX, slotY, element.minWidth());
        if (found != null) {
          return found;
        }
      }
    }
    return tooltip;
  }

  /** Width of the widest word of {@code text}: wrapping narrower would split it. Build only. */
  static int widestWord(Font font, Component text) {
    int widest = 1;
    for (String word : text.getString().split(WORD_SEPARATOR)) {
      widest = Math.max(widest, font.width(Component.literal(word).withStyle(text.getStyle())));
    }
    return widest;
  }

  /**
   * {@code text} wrapped at {@code wrapWidth} (at least its widest word), one label per line, to
   * centre each line in its slot. Build only.
   */
  static List<TextLabel> lines(Font font, Component text, int wrapWidth, int color) {
    List<TextLabel> lines = new ArrayList<>();
    for (FormattedCharSequence line :
        font.split(text, Math.max(wrapWidth, widestWord(font, text)))) {
      lines.add(new TextLabel(font, line, color, 1, null));
    }
    return List.copyOf(lines);
  }

  /** A GUI sprite (a hunger drumstick, a mob effect icon), with an optional tooltip. */
  record Sprite(
      Identifier sprite, int width, int height, @Nullable List<FormattedCharSequence> tooltip)
      implements SheetElement {
    @Override
    public int minWidth() {
      return width;
    }

    @Override
    public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, this.width, height);
    }

    @Override
    public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
      return tooltip;
    }
  }

  /** A plain rectangle: a divider, a bar. */
  record Fill(int width, int height, int color) implements SheetElement {
    @Override
    public int minWidth() {
      return width;
    }

    @Override
    public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
      graphics.fill(x, y, x + this.width, y + height, color);
    }
  }
}
