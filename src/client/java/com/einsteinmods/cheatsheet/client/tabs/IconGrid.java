package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.CheatSheetClient;
import com.einsteinmods.cheatsheet.client.sheet.ItemIcon;
import com.einsteinmods.cheatsheet.client.sheet.Panels;
import com.einsteinmods.cheatsheet.client.sheet.Scissor;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import com.einsteinmods.cheatsheet.client.sheet.SheetPalette;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * A grid of small pictures (cells made of icons, numbers and sprites), each with its tooltip, in a
 * fixed number of columns. Cells can be split into bands, each marked on the left by a coloured
 * badge with its own tooltip: villager levels, the wandering trader's offers.
 *
 * <p>Everything is placed when built. Drawing skips cells outside the scissor (tall cards scroll
 * partly out of view), so a long table costs what its visible part costs; it allocates nothing.
 */
final class IconGrid implements SheetElement {
  /** Side of an item sprite at 1x. */
  static final int ITEM = ItemIcon.ITEM_SIZE;

  private static final int BADGE_SIZE = 11;
  private static final int BADGE_GAP = 4;
  private static final int BADGE_TRAIL_WIDTH = 1;
  private static final int BADGE_TRAIL_ALPHA = 0x80;
  private static final int BADGE_TEXT = 0xFF202020;
  private static final int BAND_GAP = 4;

  /** Count text sits on the item's bottom-right corner, as in inventories. */
  private static final int COUNT_RIGHT = 17;

  private static final int COUNT_TOP = 9;

  private static final int NO_ROW = Integer.MIN_VALUE;

  private final Font font;
  private final List<Cell> cells;
  private final int[] cellX;
  private final int[] cellY;
  private final List<Band> bands;
  private final int[] bandTops;
  private final int[] bandBottoms;
  private final int cellWidth;
  private final int cellHeight;
  private final int left;
  private final int width;
  private final int height;

  private IconGrid(
      Font font,
      List<Cell> cells,
      int[] cellX,
      int[] cellY,
      List<Band> bands,
      int[] bandTops,
      int[] bandBottoms,
      int cellWidth,
      int cellHeight,
      int left,
      int width,
      int height) {
    this.font = font;
    this.cells = List.copyOf(cells);
    this.cellX = cellX;
    this.cellY = cellY;
    this.bands = List.copyOf(bands);
    this.bandTops = bandTops;
    this.bandBottoms = bandBottoms;
    this.cellWidth = cellWidth;
    this.cellHeight = cellHeight;
    this.left = left;
    this.width = width;
    this.height = height;
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
    for (int index = 0; index < bands.size(); index++) {
      extractBand(
          graphics, bands.get(index), x, y + bandTops[index], y + bandBottoms[index], index);
    }
    int row = NO_ROW;
    for (int index = 0; index < cells.size(); index++) {
      int top = y + cellY[index];
      if (!Scissor.contains(graphics, x, top)
          && !Scissor.contains(graphics, x, top + cellHeight - 1)) {
        continue;
      }
      if (cellY[index] != row) {
        // Vanilla stacks each new element above the highest one it overlaps, searching every
        // element of the current stratum: a stratum per row keeps that search to one row.
        graphics.nextStratum();
        row = cellY[index];
      }
      cells.get(index).extract(graphics, font, x + left + cellX[index], top);
    }
  }

  private void extractBand(
      GuiGraphicsExtractor graphics, Band band, int x, int top, int bottom, int index) {
    if (index > 0) {
      graphics.horizontalLine(x, x + width - 1, top - BAND_GAP / 2 - 1, SheetPalette.GRID_LINE);
    }
    if (band.color() == 0) {
      return;
    }
    int trailX = x + BADGE_SIZE / 2;
    graphics.fill(
        trailX,
        top + BADGE_SIZE,
        trailX + BADGE_TRAIL_WIDTH,
        bottom,
        (BADGE_TRAIL_ALPHA << 24) | (band.color() & 0xFFFFFF));
    Panels.rounded(graphics, x, top, BADGE_SIZE, BADGE_SIZE, band.color(), band.color());
    if (band.label() != null) {
      graphics.text(
          font,
          band.label(),
          x + (BADGE_SIZE - band.labelWidth()) / 2 + 1,
          top + (BADGE_SIZE - font.lineHeight) / 2 + 1,
          BADGE_TEXT,
          false);
    }
  }

  @Override
  public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
    if (left > 0 && localX < BADGE_SIZE) {
      for (int index = 0; index < bands.size(); index++) {
        if (localY >= bandTops[index] && localY < bandBottoms[index]) {
          List<FormattedCharSequence> tooltip = bands.get(index).tooltip();
          return tooltip.isEmpty() ? null : tooltip;
        }
      }
      return null;
    }
    for (int index = 0; index < cells.size(); index++) {
      int cellLeft = left + cellX[index];
      if (localX >= cellLeft
          && localX < cellLeft + cellWidth
          && localY >= cellY[index]
          && localY < cellY[index] + cellHeight) {
        List<FormattedCharSequence> tooltip = cells.get(index).tooltip();
        return tooltip.isEmpty() ? null : tooltip;
      }
    }
    return null;
  }

  /** A badge marking a band of cells: its colour (0: none), a short label and a tooltip. */
  record Band(
      int color,
      @Nullable FormattedCharSequence label,
      int labelWidth,
      List<FormattedCharSequence> tooltip) {
    Band {
      tooltip = List.copyOf(tooltip);
    }

    static Band of(
        Font font, int color, @Nullable Component label, List<FormattedCharSequence> tooltip) {
      FormattedCharSequence text = label == null ? null : label.getVisualOrderText();
      return new Band(color, text, text == null ? 0 : font.width(text), tooltip);
    }

    /** No badge: cells start at the left edge. */
    static Band none() {
      return new Band(0, null, 0, List.of());
    }
  }

  /** One picture: parts drawn in order, relative to its top-left corner. */
  record Cell(int width, int height, List<Part> parts, List<FormattedCharSequence> tooltip) {
    Cell {
      parts = List.copyOf(parts);
      tooltip = List.copyOf(tooltip);
    }

    void extract(GuiGraphicsExtractor graphics, Font font, int x, int y) {
      for (int index = 0; index < parts.size(); index++) {
        parts.get(index).extract(graphics, font, x, y);
      }
    }
  }

  /** A piece of a cell. */
  sealed interface Part permits ItemPart, TextPart, ScaledTextPart, SpritePart, FillPart {
    void extract(GuiGraphicsExtractor graphics, Font font, int x, int y);
  }

  /** An item at 1x or bigger. */
  record ItemPart(ItemStack stack, int scale, int x, int y) implements Part {
    @Override
    public void extract(GuiGraphicsExtractor graphics, Font font, int originX, int originY) {
      if (scale == 1) {
        CheatSheetClient.scaledItemIcons().drawItem(graphics, stack, originX + x, originY + y);
        return;
      }
      CheatSheetClient.scaledItemIcons().draw(graphics, stack, originX + x, originY + y, scale);
    }
  }

  /** One line of text, laid out once. */
  record TextPart(FormattedCharSequence text, int color, int x, int y, boolean shadow)
      implements Part {
    @Override
    public void extract(GuiGraphicsExtractor graphics, Font font, int originX, int originY) {
      graphics.text(font, text, originX + x, originY + y, color, shadow);
    }
  }

  /** One line of text drawn bigger: a number the eye should land on. */
  record ScaledTextPart(FormattedCharSequence text, int color, int x, int y, int scale)
      implements Part {
    @Override
    public void extract(GuiGraphicsExtractor graphics, Font font, int originX, int originY) {
      graphics.pose().pushMatrix();
      graphics.pose().translate(originX + x, originY + y);
      graphics.pose().scale(scale, scale);
      graphics.text(font, text, 0, 0, color, true);
      graphics.pose().popMatrix();
    }
  }

  /** A GUI sprite: a mob effect icon, a hunger drumstick. */
  record SpritePart(Identifier sprite, int x, int y, int width, int height) implements Part {
    @Override
    public void extract(GuiGraphicsExtractor graphics, Font font, int originX, int originY) {
      graphics.blitSprite(
          RenderPipelines.GUI_TEXTURED, sprite, originX + x, originY + y, width, height);
    }
  }

  /** A plain rectangle: a highlight behind a cell. */
  record FillPart(int x, int y, int width, int height, int color) implements Part {
    @Override
    public void extract(GuiGraphicsExtractor graphics, Font font, int originX, int originY) {
      graphics.fill(originX + x, originY + y, originX + x + width, originY + y + height, color);
    }
  }

  /**
   * Lays parts out left to right on one line of item height: items (with an inventory-style count),
   * numbers, signs. Text is centred on the items.
   */
  static final class Flow {
    private final Font font;
    private final List<Part> parts = new ArrayList<>();
    private int cursor;

    Flow(Font font) {
      this.font = font;
    }

    /** An item, with {@code count} on its corner when not null (as inventories show counts). */
    Flow item(ItemStack stack, @Nullable String count) {
      parts.add(new ItemPart(stack, 1, cursor, 0));
      if (count != null) {
        FormattedCharSequence text = Component.literal(count).getVisualOrderText();
        parts.add(
            new TextPart(
                text, SheetPalette.TEXT, cursor + COUNT_RIGHT - font.width(text), COUNT_TOP, true));
      }
      cursor += ITEM;
      return this;
    }

    Flow text(Component text, int color) {
      FormattedCharSequence line = text.getVisualOrderText();
      parts.add(new TextPart(line, color, cursor, (ITEM - font.lineHeight) / 2 + 1, true));
      cursor += font.width(line);
      return this;
    }

    /** A square GUI sprite of {@code size} pixels, centred on the items. */
    Flow sprite(Identifier sprite, int size) {
      parts.add(new SpritePart(sprite, cursor, (ITEM - size) / 2, size, size));
      cursor += size;
      return this;
    }

    /** A plain square of {@code size} pixels, centred on the items: a colour swatch. */
    Flow fill(int size, int color) {
      parts.add(new FillPart(cursor, (ITEM - size) / 2, size, size, color));
      cursor += size;
      return this;
    }

    /** A small coloured square with a dark label, like the band badges. */
    Flow badge(int color, Component label) {
      int top = (ITEM - BADGE_SIZE) / 2;
      parts.add(new FillPart(cursor, top, BADGE_SIZE, BADGE_SIZE, color));
      FormattedCharSequence text = label.getVisualOrderText();
      parts.add(
          new TextPart(
              text,
              BADGE_TEXT,
              cursor + (BADGE_SIZE - font.width(text)) / 2 + 1,
              top + (BADGE_SIZE - font.lineHeight) / 2 + 1,
              false));
      cursor += BADGE_SIZE;
      return this;
    }

    Flow gap(int pixels) {
      cursor += pixels;
      return this;
    }

    Cell build(List<FormattedCharSequence> tooltip) {
      return new Cell(cursor, ITEM, parts, tooltip);
    }
  }

  /** Collects cells and bands, then places them in {@code columns} equal columns. */
  static final class Builder {
    private final Font font;
    private final int columns;
    private final int columnGap;
    private final int rowGap;
    private final List<Band> bands = new ArrayList<>();
    private final List<List<Cell>> bandCells = new ArrayList<>();

    Builder(Font font, int columns, int columnGap, int rowGap) {
      if (columns < 1) {
        throw new IllegalArgumentException("columns " + columns);
      }
      this.font = font;
      this.columns = columns;
      this.columnGap = columnGap;
      this.rowGap = rowGap;
    }

    /** Starts a band: following cells begin on a new row next to its badge. */
    Builder band(Band band) {
      bands.add(band);
      bandCells.add(new ArrayList<>());
      return this;
    }

    Builder cell(Cell cell) {
      if (bands.isEmpty()) {
        band(Band.none());
      }
      bandCells.getLast().add(cell);
      return this;
    }

    IconGrid build() {
      int cellWidth = 1;
      int cellHeight = 1;
      boolean badges = false;
      List<Cell> cells = new ArrayList<>();
      for (int band = 0; band < bands.size(); band++) {
        badges |= bands.get(band).color() != 0;
        for (Cell cell : bandCells.get(band)) {
          cellWidth = Math.max(cellWidth, cell.width());
          cellHeight = Math.max(cellHeight, cell.height());
          cells.add(cell);
        }
      }
      int left = badges ? BADGE_SIZE + BADGE_GAP : 0;
      int[] cellX = new int[cells.size()];
      int[] cellY = new int[cells.size()];
      int[] bandTops = new int[bands.size()];
      int[] bandBottoms = new int[bands.size()];
      int top = 0;
      int index = 0;
      for (int band = 0; band < bands.size(); band++) {
        if (band > 0) {
          top += BAND_GAP;
        }
        bandTops[band] = top;
        List<Cell> members = bandCells.get(band);
        int rows = Math.max(1, (members.size() + columns - 1) / columns);
        for (int member = 0; member < members.size(); member++) {
          cellX[index] = (member % columns) * (cellWidth + columnGap);
          cellY[index] = top + (member / columns) * (cellHeight + rowGap);
          index++;
        }
        int bandHeight = rows * cellHeight + (rows - 1) * rowGap;
        top += badges ? Math.max(bandHeight, BADGE_SIZE) : bandHeight;
        bandBottoms[band] = top;
      }
      int usedColumns = Math.min(columns, Math.max(1, maxBandSize()));
      int width = left + usedColumns * cellWidth + (usedColumns - 1) * columnGap;
      return new IconGrid(
          font,
          cells,
          cellX,
          cellY,
          bands,
          bandTops,
          bandBottoms,
          cellWidth,
          cellHeight,
          left,
          width,
          Math.max(1, top));
    }

    private int maxBandSize() {
      int largest = 0;
      for (List<Cell> members : bandCells) {
        largest = Math.max(largest, members.size());
      }
      return largest;
    }
  }
}
