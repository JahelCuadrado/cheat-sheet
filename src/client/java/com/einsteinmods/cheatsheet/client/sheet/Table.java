package com.einsteinmods.cheatsheet.client.sheet;

import com.einsteinmods.cheatsheet.core.sections.BodyShape;
import com.einsteinmods.cheatsheet.core.sections.TableGeometry;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * A printed-style table: each record one row, read left to right; columns as wide as their widest
 * cell, every row as tall as the tallest cell, an optional header row. Rows alternate a soft shade,
 * thin lines separate rows and columns. When the sheet is wide, the table splits into side-by-side
 * blocks (each with its header) that read down the first block, then the next.
 *
 * <p>Positions inside a cell are resolved when built. Drawing skips rows outside the scissor and
 * puts each row in its own GUI stratum (vanilla's placement scan stays per row); it allocates
 * nothing.
 */
public final class Table implements SheetBody {
  /** Space between a cell's content and its column's edges. */
  public static final int PADDING_X = 2;

  /** Narrow padding for dense grids of pictures (a matrix of colours). */
  public static final int TIGHT_PADDING_X = 1;

  /** Space between a cell's content and its row's edges. */
  public static final int PADDING_Y = 1;

  /** Space between side-by-side blocks. */
  public static final int BLOCK_GAP = 6;

  private static final int LINE = 1;
  private static final float HEADER_SHADE = 0.35F;
  private static final int ALTERNATE_ROW = 0x10FFFFFF;

  /** How a column places its cells: from the start (text) or centred (pictures). */
  public enum Align {
    START,
    CENTER
  }

  private final SheetElement @Nullable [] header;
  private final SheetElement[][] rows;
  private final int paddingX;
  private final int[] columnLefts;
  private final int[] columnWidths;
  private final int[][] cellXs;
  private final int[][] cellYs;
  private final int @Nullable [] headerXs;
  private final int @Nullable [] headerYs;
  private final int blockWidth;
  private final int rowHeight;
  private final int headerHeight;
  private final int fill;
  private final int border;
  private final int headerFill;
  private final TableGeometry geometry;

  /** The zoom view of each row by index, or null to zoom the row itself. */
  private @Nullable IntFunction<? extends ZoomCard> rowZooms;

  /**
   * @param align each column's alignment; its size is the column count
   * @param header one cell per column, or null for no header row
   * @param rows one list of cells per row, one cell per column
   * @param tint colours of the blocks' panels
   */
  public Table(
      List<Align> align,
      @Nullable List<? extends SheetElement> header,
      List<? extends List<? extends SheetElement>> rows,
      CardTint tint) {
    this(align, header, rows, tint, PADDING_X);
  }

  /** A table whose cells keep {@code paddingX} pixels from their column's edges. */
  public Table(
      List<Align> align,
      @Nullable List<? extends SheetElement> header,
      List<? extends List<? extends SheetElement>> rows,
      CardTint tint,
      int paddingX) {
    this.paddingX = paddingX;
    int columns = align.size();
    if (columns == 0 || rows.isEmpty()) {
      throw new IllegalArgumentException("a table needs columns and rows");
    }
    if (header != null && header.size() != columns) {
      throw new IllegalArgumentException("the header needs one cell per column");
    }
    this.rows = new SheetElement[rows.size()][];
    for (int row = 0; row < rows.size(); row++) {
      if (rows.get(row).size() != columns) {
        throw new IllegalArgumentException("row " + row + " needs one cell per column");
      }
      this.rows[row] = rows.get(row).toArray(new SheetElement[0]);
    }
    this.header = header == null ? null : header.toArray(new SheetElement[0]);

    this.columnWidths = new int[columns];
    int tallest = 1;
    for (SheetElement[] row : this.rows) {
      for (int column = 0; column < columns; column++) {
        columnWidths[column] = Math.max(columnWidths[column], row[column].minWidth());
        tallest = Math.max(tallest, row[column].height());
      }
    }
    int tallestHeader = 0;
    if (this.header != null) {
      for (int column = 0; column < columns; column++) {
        columnWidths[column] = Math.max(columnWidths[column], this.header[column].minWidth());
        tallestHeader = Math.max(tallestHeader, this.header[column].height());
      }
    }
    this.columnLefts = new int[columns];
    List<Integer> widths = new ArrayList<>(columns);
    int left = 0;
    for (int column = 0; column < columns; column++) {
      columnWidths[column] += paddingX * 2;
      columnLefts[column] = left;
      left += columnWidths[column];
      widths.add(columnWidths[column]);
    }
    this.blockWidth = left;
    this.rowHeight = tallest + PADDING_Y * 2;
    this.headerHeight = this.header == null ? 0 : tallestHeader + PADDING_Y * 2;

    this.cellXs = new int[this.rows.length][];
    this.cellYs = new int[this.rows.length][];
    for (int row = 0; row < this.rows.length; row++) {
      cellXs[row] = new int[columns];
      cellYs[row] = new int[columns];
      place(this.rows[row], align, rowHeight, cellXs[row], cellYs[row]);
    }
    if (this.header == null) {
      this.headerXs = null;
      this.headerYs = null;
    } else {
      this.headerXs = new int[columns];
      this.headerYs = new int[columns];
      place(this.header, align, headerHeight, headerXs, headerYs);
    }
    this.fill = tint.fill();
    this.border = tint.border();
    this.headerFill = ARGB.srgbLerp(HEADER_SHADE, tint.fill(), tint.border());
    this.geometry = new TableGeometry(widths, rowHeight, headerHeight, this.rows.length, BLOCK_GAP);
  }

  /** Where each cell's content goes inside its row: aligned in its column, centred vertically. */
  private void place(SheetElement[] cells, List<Align> align, int height, int[] xs, int[] ys) {
    for (int column = 0; column < cells.length; column++) {
      SheetElement cell = cells[column];
      int room = columnWidths[column] - paddingX * 2;
      int offset = align.get(column) == Align.CENTER ? (room - cell.minWidth()) / 2 : 0;
      xs[column] = columnLefts[column] + paddingX + offset;
      ys[column] = (height - cell.height()) / 2;
    }
  }

  @Override
  public TableGeometry geometry() {
    return geometry;
  }

  @Override
  public void extract(GuiGraphicsExtractor graphics, int x, int y, BodyShape shape) {
    int blocks = shape.split();
    int rowsPerBlock = geometry.rowsPerBlock(blocks);
    for (int block = 0; block < blocks; block++) {
      int first = block * rowsPerBlock;
      int end = Math.min(rows.length, first + rowsPerBlock);
      if (first >= end) {
        break;
      }
      int left = x + block * (blockWidth + BLOCK_GAP);
      int bottom = y + headerHeight + (end - first) * rowHeight;
      Panels.rounded(graphics, left, y, blockWidth, bottom - y, fill, border);
      if (header != null && headerXs != null && headerYs != null) {
        graphics.fill(
            left + LINE, y + LINE, left + blockWidth - LINE, y + headerHeight, headerFill);
        extractCells(graphics, header, headerXs, headerYs, left, y);
        graphics.horizontalLine(left, left + blockWidth - 1, y + headerHeight - 1, border);
      }
      for (int row = first; row < end; row++) {
        int top = y + headerHeight + (row - first) * rowHeight;
        if (!Scissor.contains(graphics, left, top)
            && !Scissor.contains(graphics, left, top + rowHeight - 1)) {
          continue;
        }
        // Vanilla stacks each new element above the highest one it overlaps, searching every
        // element of the current stratum: a stratum per row keeps that search to one row.
        graphics.nextStratum();
        if ((row - first) % 2 == 1) {
          graphics.fill(left + LINE, top, left + blockWidth - LINE, top + rowHeight, ALTERNATE_ROW);
        }
        if (row > first) {
          graphics.horizontalLine(
              left + LINE, left + blockWidth - LINE - 1, top, SheetPalette.GRID_LINE);
        }
        for (int column = 1; column < columnLefts.length; column++) {
          graphics.verticalLine(
              left + columnLefts[column], top, top + rowHeight, SheetPalette.GRID_LINE);
        }
        extractCells(graphics, rows[row], cellXs[row], cellYs[row], left, top);
      }
    }
  }

  private static void extractCells(
      GuiGraphicsExtractor graphics, SheetElement[] cells, int[] xs, int[] ys, int left, int top) {
    for (int column = 0; column < cells.length; column++) {
      SheetElement cell = cells[column];
      cell.extract(graphics, left + xs[column], top + ys[column], cell.minWidth());
    }
  }

  @Override
  public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, BodyShape shape) {
    int pitch = blockWidth + BLOCK_GAP;
    int block = localX / pitch;
    int inBlockX = localX - block * pitch;
    if (localX < 0 || localY < 0 || block >= shape.split() || inBlockX >= blockWidth) {
      return null;
    }
    if (localY < headerHeight) {
      return header == null || headerXs == null || headerYs == null
          ? null
          : cellTooltip(header, headerXs, headerYs, inBlockX, localY);
    }
    int rowsPerBlock = geometry.rowsPerBlock(shape.split());
    int rowInBlock = (localY - headerHeight) / rowHeight;
    int row = block * rowsPerBlock + rowInBlock;
    if (rowInBlock >= rowsPerBlock || row >= rows.length) {
      return null;
    }
    int top = headerHeight + rowInBlock * rowHeight;
    return cellTooltip(rows[row], cellXs[row], cellYs[row], inBlockX, localY - top);
  }

  private static @Nullable List<FormattedCharSequence> cellTooltip(
      SheetElement[] cells, int[] xs, int[] ys, int x, int y) {
    for (int column = 0; column < cells.length; column++) {
      SheetElement cell = cells[column];
      int cellX = x - xs[column];
      int cellY = y - ys[column];
      if (cellX >= 0 && cellX < cell.minWidth() && cellY >= 0 && cellY < cell.height()) {
        return cell.tooltipAt(cellX, cellY, cell.minWidth());
      }
    }
    return null;
  }

  /** A row is a card: the one under the point (the header is not one), across its block. */
  @Override
  public boolean locateCard(int localX, int localY, BodyShape shape, CardSpot spot) {
    int pitch = blockWidth + BLOCK_GAP;
    if (localX < 0 || localY < headerHeight) {
      return false;
    }
    int block = localX / pitch;
    if (block >= shape.split() || localX - block * pitch >= blockWidth) {
      return false;
    }
    int rowsPerBlock = geometry.rowsPerBlock(shape.split());
    int rowInBlock = (localY - headerHeight) / rowHeight;
    int row = block * rowsPerBlock + rowInBlock;
    if (rowInBlock >= rowsPerBlock || row >= rows.length) {
      return false;
    }
    spot.set(row, block * pitch, headerHeight + rowInBlock * rowHeight, blockWidth, rowHeight);
    return true;
  }

  /**
   * Rows zoom as the view {@code zooms} makes for their index (called once per click) instead of as
   * themselves. Build time only; returns this table.
   */
  public Table withRowZooms(IntFunction<? extends ZoomCard> zooms) {
    rowZooms = zooms;
    return this;
  }

  /** Row {@code card}'s zoom view, or the row under the header as a one-row table. */
  @Override
  public ZoomCard zoomCard(int card, BodyShape shape) {
    IntFunction<? extends ZoomCard> zooms = rowZooms;
    return zooms != null ? zooms.apply(card) : new RowCard(card);
  }

  /** One row of the table under its header, drawn like a block of one row. */
  private final class RowCard implements ZoomCard {
    private final int row;

    RowCard(int row) {
      this.row = row;
    }

    @Override
    public int width() {
      return blockWidth;
    }

    @Override
    public int height() {
      return headerHeight + rowHeight;
    }

    @Override
    public void extract(GuiGraphicsExtractor graphics, int x, int y) {
      Panels.rounded(graphics, x, y, blockWidth, height(), fill, border);
      if (header != null && headerXs != null && headerYs != null) {
        graphics.fill(x + LINE, y + LINE, x + blockWidth - LINE, y + headerHeight, headerFill);
        extractCells(graphics, header, headerXs, headerYs, x, y);
        graphics.horizontalLine(x, x + blockWidth - 1, y + headerHeight - 1, border);
      }
      int top = y + headerHeight;
      graphics.nextStratum();
      for (int column = 1; column < columnLefts.length; column++) {
        graphics.verticalLine(
            x + columnLefts[column], top, top + rowHeight, SheetPalette.GRID_LINE);
      }
      extractCells(graphics, rows[row], cellXs[row], cellYs[row], x, top);
    }

    @Override
    public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY) {
      if (localY < headerHeight) {
        return header == null || headerXs == null || headerYs == null
            ? null
            : cellTooltip(header, headerXs, headerYs, localX, localY);
      }
      return cellTooltip(rows[row], cellXs[row], cellYs[row], localX, localY - headerHeight);
    }
  }
}
