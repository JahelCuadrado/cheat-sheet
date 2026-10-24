package com.einsteinmods.cheatsheet.core.sections;

import java.util.ArrayList;
import java.util.List;

/**
 * A table: columns as wide as their widest cell, rows all the same height, an optional header row
 * (height 0 for none). A long table may be split into side-by-side blocks of consecutive rows, each
 * with its own header; it reads down the first block, then down the next. Blocks hold the same
 * number of rows except the last, which may hold fewer.
 *
 * @param columnWidths each column's width, padding included
 * @param rowHeight height of every row
 * @param headerHeight height of the header row, 0 for none
 * @param rowCount rows, header not included
 * @param blockGap space between side-by-side blocks
 */
public record TableGeometry(
    List<Integer> columnWidths, int rowHeight, int headerHeight, int rowCount, int blockGap)
    implements SectionBody {
  public TableGeometry {
    columnWidths = List.copyOf(columnWidths);
    if (columnWidths.isEmpty() || rowHeight <= 0 || headerHeight < 0 || rowCount <= 0) {
      throw new IllegalArgumentException(
          "bad table: "
              + columnWidths.size()
              + " columns, "
              + rowCount
              + " rows of "
              + rowHeight
              + ", header "
              + headerHeight);
    }
    for (int width : columnWidths) {
      if (width <= 0) {
        throw new IllegalArgumentException("bad column width: " + width);
      }
    }
    if (blockGap < 0) {
      throw new IllegalArgumentException("bad block gap: " + blockGap);
    }
  }

  /** Width of one block: every column side by side. */
  public int blockWidth() {
    int width = 0;
    for (int column : columnWidths) {
      width += column;
    }
    return width;
  }

  /** Left edge of {@code column} inside its block. */
  public int columnLeft(int column) {
    int left = 0;
    for (int index = 0; index < column; index++) {
      left += columnWidths.get(index);
    }
    return left;
  }

  /** Rows in each block (the last may hold fewer) when split into {@code blocks}. */
  public int rowsPerBlock(int blocks) {
    return Math.ceilDiv(rowCount, blocks);
  }

  /** Left edge of {@code block}. */
  public int blockLeft(int block) {
    return block * (blockWidth() + blockGap);
  }

  /** Block that holds {@code row} when split into {@code blocks}. */
  public int blockOf(int row, int blocks) {
    return row / rowsPerBlock(blocks);
  }

  /** Top of {@code row} (below its block's header) when split into {@code blocks}. */
  public int rowTop(int row, int blocks) {
    return headerHeight + (row % rowsPerBlock(blocks)) * rowHeight;
  }

  /** One shape per block count that shortens the table, from one block up to one row each. */
  @Override
  public List<BodyShape> shapes() {
    List<BodyShape> shapes = new ArrayList<>();
    for (int blocks = 1; blocks <= rowCount; blocks++) {
      int rows = rowsPerBlock(blocks);
      // Fewer blocks already give this many rows per block: more would only leave one empty.
      if (Math.ceilDiv(rowCount, rows) != blocks) {
        continue;
      }
      shapes.add(
          new BodyShape(
              blocks,
              blocks * blockWidth() + (blocks - 1) * blockGap,
              headerHeight + rows * rowHeight));
    }
    return List.copyOf(shapes);
  }
}
