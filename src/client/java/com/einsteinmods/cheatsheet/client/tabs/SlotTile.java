package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.sheet.CardTint;
import com.einsteinmods.cheatsheet.client.sheet.Panels;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import com.einsteinmods.cheatsheet.client.sheet.SheetPalette;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * A tinted tile of fixed slots stacked top to bottom, like a printed card: every tile of a set has
 * the same width, the same slot heights (each slot's tallest content across the set) and so the
 * same piece of information in the same place; a slot a tile does not use stays blank. A slot may
 * have a thin divider above it.
 *
 * <p>Positions are resolved when built; drawing is one panel, the dividers and the contents' own
 * draw calls, with no allocation.
 */
final class SlotTile implements SheetElement {
  /** Space between the panel's edge and the slots. */
  private static final int PADDING = 4;

  /** Space between slots; a divider sits in the middle of the gap above its slot. */
  private static final int SLOT_GAP = 2;

  private static final int DIVIDER_GAP = 5;

  /** How a slot is laid out in every tile of a set. */
  record Slot(boolean centred, boolean dividerAbove) {
    static final Slot START = new Slot(false, false);
    static final Slot CENTRED = new Slot(true, false);
    static final Slot START_DIVIDED = new Slot(false, true);
  }

  private final @Nullable SheetElement[] contents;
  private final int[] contentXs;
  private final int[] contentYs;
  private final int[] dividerYs;
  private final int width;
  private final int height;
  private final int fill;
  private final int border;

  private SlotTile(
      List<@Nullable SheetElement> contents,
      List<Slot> slots,
      int[] slotHeights,
      int width,
      CardTint tint) {
    this.contents = contents.toArray(new SheetElement[0]);
    this.contentXs = new int[slots.size()];
    this.contentYs = new int[slots.size()];
    List<Integer> dividers = new ArrayList<>();
    int room = width - PADDING * 2;
    int top = PADDING;
    for (int index = 0; index < slots.size(); index++) {
      Slot slot = slots.get(index);
      if (index > 0) {
        int gap = slot.dividerAbove() ? DIVIDER_GAP : SLOT_GAP;
        if (slot.dividerAbove()) {
          dividers.add(top + gap / 2);
        }
        top += gap;
      }
      SheetElement content = this.contents[index];
      if (content != null) {
        contentXs[index] =
            PADDING + (slot.centred() ? Math.max(0, (room - content.minWidth()) / 2) : 0);
        contentYs[index] = top + (slotHeights[index] - content.height()) / 2;
      }
      top += slotHeights[index];
    }
    this.dividerYs = new int[dividers.size()];
    for (int index = 0; index < dividers.size(); index++) {
      dividerYs[index] = dividers.get(index);
    }
    this.width = width;
    this.height = top + PADDING;
    this.fill = tint.fill();
    this.border = tint.border();
  }

  /**
   * One tile per entry of {@code contents} (one element or null per slot, in {@code slots} order),
   * all the same size: at least {@code minWidth} wide, or as wide as the widest content.
   */
  static List<SlotTile> uniform(
      List<Slot> slots,
      List<? extends List<? extends @Nullable SheetElement>> contents,
      List<CardTint> tints,
      int minWidth) {
    if (contents.size() != tints.size()) {
      throw new IllegalArgumentException("one tint per tile");
    }
    int[] slotHeights = new int[slots.size()];
    int widest = minWidth - PADDING * 2;
    for (List<? extends @Nullable SheetElement> tile : contents) {
      if (tile.size() != slots.size()) {
        throw new IllegalArgumentException("a tile needs one entry per slot");
      }
      for (int index = 0; index < slots.size(); index++) {
        SheetElement content = tile.get(index);
        if (content != null) {
          slotHeights[index] = Math.max(slotHeights[index], content.height());
          widest = Math.max(widest, content.minWidth());
        }
      }
    }
    int width = widest + PADDING * 2;
    List<SlotTile> tiles = new ArrayList<>(contents.size());
    for (int index = 0; index < contents.size(); index++) {
      tiles.add(
          new SlotTile(
              new ArrayList<>(contents.get(index)), slots, slotHeights, width, tints.get(index)));
    }
    return List.copyOf(tiles);
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
    Panels.rounded(graphics, x, y, this.width, height, fill, border);
    for (int dividerY : dividerYs) {
      graphics.horizontalLine(
          x + PADDING, x + this.width - PADDING - 1, y + dividerY, SheetPalette.GRID_LINE);
    }
    for (int index = 0; index < contents.length; index++) {
      SheetElement content = contents[index];
      if (content != null) {
        content.extract(graphics, x + contentXs[index], y + contentYs[index], content.minWidth());
      }
    }
  }

  @Override
  public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
    for (int index = 0; index < contents.length; index++) {
      SheetElement content = contents[index];
      if (content == null) {
        continue;
      }
      int contentX = localX - contentXs[index];
      int contentY = localY - contentYs[index];
      if (contentX >= 0
          && contentX < content.minWidth()
          && contentY >= 0
          && contentY < content.height()) {
        return content.tooltipAt(contentX, contentY, content.minWidth());
      }
    }
    return null;
  }
}
