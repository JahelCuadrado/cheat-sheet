package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * Cells in rows of at most {@code columns}, each centred in a column as wide as the widest cell, so
 * pictures line up; a shorter last row is centred. Positions are resolved when built.
 */
final class CellGrid implements SheetElement {
  private final SheetElement[] cells;
  private final int[] cellXs;
  private final int[] cellYs;
  private final int width;
  private final int height;

  CellGrid(List<? extends SheetElement> cells, int columns, int gap) {
    if (cells.isEmpty() || columns < 1) {
      throw new IllegalArgumentException("a grid needs cells and columns");
    }
    this.cells = cells.toArray(new SheetElement[0]);
    int columnCount = Math.min(columns, this.cells.length);
    int cellWidth = 0;
    for (SheetElement cell : this.cells) {
      cellWidth = Math.max(cellWidth, cell.minWidth());
    }
    this.width = columnCount * cellWidth + (columnCount - 1) * gap;
    this.cellXs = new int[this.cells.length];
    this.cellYs = new int[this.cells.length];
    int rowTop = 0;
    for (int rowStart = 0; rowStart < this.cells.length; rowStart += columnCount) {
      int rowEnd = Math.min(this.cells.length, rowStart + columnCount);
      int rowWidth = (rowEnd - rowStart) * cellWidth + (rowEnd - rowStart - 1) * gap;
      int rowLeft = (width - rowWidth) / 2;
      int rowHeight = 0;
      for (int index = rowStart; index < rowEnd; index++) {
        SheetElement cell = this.cells[index];
        cellXs[index] =
            rowLeft + (index - rowStart) * (cellWidth + gap) + (cellWidth - cell.minWidth()) / 2;
        cellYs[index] = rowTop;
        rowHeight = Math.max(rowHeight, cell.height());
      }
      rowTop += rowHeight + gap;
    }
    this.height = rowTop - gap;
  }

  /** Columns for {@code count} cells with at most {@code maxColumns} per row, rows kept even. */
  static int balancedColumns(int count, int maxColumns) {
    int rows = Math.ceilDiv(count, maxColumns);
    return Math.ceilDiv(count, rows);
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
    int left = x + (width - this.width) / 2;
    for (int index = 0; index < cells.length; index++) {
      SheetElement cell = cells[index];
      cell.extract(graphics, left + cellXs[index], y + cellYs[index], cell.minWidth());
    }
  }

  @Override
  public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
    int offsetX = localX - (width - this.width) / 2;
    for (int index = 0; index < cells.length; index++) {
      SheetElement cell = cells[index];
      int cellX = cellXs[index];
      int cellY = cellYs[index];
      if (offsetX >= cellX
          && offsetX < cellX + cell.minWidth()
          && localY >= cellY
          && localY < cellY + cell.height()) {
        return cell.tooltipAt(offsetX - cellX, localY - cellY, cell.minWidth());
      }
    }
    return null;
  }
}
