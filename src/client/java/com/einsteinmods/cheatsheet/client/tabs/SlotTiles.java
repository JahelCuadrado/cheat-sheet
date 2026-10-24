package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.sheet.CardTint;
import com.einsteinmods.cheatsheet.client.sheet.Panels;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import com.einsteinmods.cheatsheet.client.sheet.Table;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * The tiles of one tab, printed-infographic style: every tile of the tab the same width and every
 * tile of a section the same height (the largest content's, at least a minimum width), and, among
 * tiles of one {@link Layout}, every piece of information in the same place. A layout is a stack of
 * slots; each slot is as tall as its tallest content in the layout, so the third line of one
 * pyramid sits where the third line of the next one does. Each piece starts at its slot's top.
 *
 * <p>Collect every tile first ({@link Layout#add}), then {@link #build}: sizes are known only once
 * all tiles are in. Tiles go into {@link com.einsteinmods.cheatsheet.client.sheet.TileGrid}s.
 * Positions are resolved when built; drawing is a panel and one call per slot, no allocation.
 */
final class SlotTiles {
  /** Space between a tile's border and its content. */
  static final int PADDING = 5;

  /** Space between slots. */
  static final int SLOT_GAP = 3;

  /** Space between tiles in a grid. */
  static final int GAP = 4;

  /** A slot a tile leaves empty (a line only some tiles have). */
  static final SheetElement EMPTY =
      new SheetElement() {
        @Override
        public int minWidth() {
          return 0;
        }

        @Override
        public int height() {
          return 0;
        }

        @Override
        public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {}
      };

  private final int minWidth;
  private final List<Layout> layouts = new ArrayList<>();
  private int section;
  private boolean built;

  /** {@code minWidth}: tiles are at least this wide, so a row of them fills the sheet. */
  SlotTiles(int minWidth) {
    this.minWidth = minWidth;
  }

  /** Layouts added from now on go in the next section: their tiles get a height of their own. */
  void nextSection() {
    section++;
  }

  /** A new layout: its tiles' panel colours and how each slot places its content. */
  Layout layout(CardTint tint, List<Table.Align> slots) {
    if (built) {
      throw new IllegalStateException("layouts are added before the tiles are built");
    }
    Layout layout = new Layout(tint, slots, section);
    layouts.add(layout);
    return layout;
  }

  /** Sizes every tile; after this each layout's {@link Layout#tiles()} are ready. */
  void build() {
    int width = minWidth;
    int[] heights = new int[section + 1];
    for (Layout layout : layouts) {
      layout.measure();
      width = Math.max(width, layout.contentWidth + PADDING * 2);
      heights[layout.section] =
          Math.max(heights[layout.section], layout.contentHeight + PADDING * 2);
    }
    for (Layout layout : layouts) {
      layout.place(width, heights[layout.section]);
    }
    built = true;
  }

  /** Tiles that share slots; each tile gives one element per slot (or {@link #EMPTY}). */
  static final class Layout {
    private final CardTint tint;
    private final List<Table.Align> align;
    private final int section;
    private final List<List<SheetElement>> contents = new ArrayList<>();
    private final List<SheetElement> tiles = new ArrayList<>();
    private int[] slotHeights = new int[0];
    private int contentWidth;
    private int contentHeight;

    private Layout(CardTint tint, List<Table.Align> align, int section) {
      if (align.isEmpty()) {
        throw new IllegalArgumentException("a layout needs slots");
      }
      this.tint = tint;
      this.align = List.copyOf(align);
      this.section = section;
    }

    /** Adds a tile; it is drawn in the order added. */
    Layout add(List<? extends SheetElement> slots) {
      if (slots.size() != align.size()) {
        throw new IllegalArgumentException(
            "a tile needs " + align.size() + " slots, got " + slots.size());
      }
      contents.add(List.copyOf(slots));
      return this;
    }

    /** The tiles, sized; only after {@link SlotTiles#build}. */
    List<SheetElement> tiles() {
      if (tiles.size() != contents.size()) {
        throw new IllegalStateException("tiles are read after they are built");
      }
      return List.copyOf(tiles);
    }

    private void measure() {
      slotHeights = new int[align.size()];
      contentWidth = 0;
      for (List<SheetElement> slots : contents) {
        for (int slot = 0; slot < slots.size(); slot++) {
          slotHeights[slot] = Math.max(slotHeights[slot], slots.get(slot).height());
          contentWidth = Math.max(contentWidth, slots.get(slot).minWidth());
        }
      }
      contentHeight = 0;
      int used = 0;
      for (int slotHeight : slotHeights) {
        if (slotHeight > 0) {
          contentHeight += (used > 0 ? SLOT_GAP : 0) + slotHeight;
          used++;
        }
      }
    }

    private void place(int width, int height) {
      tiles.clear();
      int inner = width - PADDING * 2;
      for (List<SheetElement> slots : contents) {
        int[] xs = new int[slots.size()];
        int[] ys = new int[slots.size()];
        int top = PADDING;
        for (int slot = 0; slot < slots.size(); slot++) {
          SheetElement element = slots.get(slot);
          int offset = align.get(slot) == Table.Align.CENTER ? (inner - element.minWidth()) / 2 : 0;
          xs[slot] = PADDING + offset;
          ys[slot] = top;
          if (slotHeights[slot] > 0) {
            top += slotHeights[slot] + SLOT_GAP;
          }
        }
        tiles.add(new Tile(slots.toArray(new SheetElement[0]), xs, ys, width, height, tint));
      }
    }
  }

  /** One tile: a panel with its slots' contents at fixed places. */
  private static final class Tile implements SheetElement {
    private final SheetElement[] slots;
    private final int[] xs;
    private final int[] ys;
    private final int width;
    private final int height;
    private final int fill;
    private final int border;

    private Tile(SheetElement[] slots, int[] xs, int[] ys, int width, int height, CardTint tint) {
      this.slots = slots;
      this.xs = xs;
      this.ys = ys;
      this.width = width;
      this.height = height;
      this.fill = tint.fill();
      this.border = tint.border();
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
      for (int slot = 0; slot < slots.length; slot++) {
        SheetElement element = slots[slot];
        element.extract(graphics, x + xs[slot], y + ys[slot], element.minWidth());
      }
    }

    @Override
    public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
      for (int slot = 0; slot < slots.length; slot++) {
        SheetElement element = slots[slot];
        int slotX = localX - xs[slot];
        int slotY = localY - ys[slot];
        if (slotX >= 0 && slotX < element.minWidth() && slotY >= 0 && slotY < element.height()) {
          return element.tooltipAt(slotX, slotY, element.minWidth());
        }
      }
      return null;
    }
  }
}
