package com.einsteinmods.cheatsheet.core.sections;

import java.util.ArrayList;
import java.util.List;

/**
 * Tiles all the same size (the largest tile's), as many per row as the width allows, left to right
 * then top to bottom, aligned in columns.
 *
 * @param tileWidth width of every tile
 * @param tileHeight height of every tile
 * @param count tiles
 * @param gap space between tiles, both ways
 * @param fullRows fill each row before starting the next (as many per row as fit, the last row
 *     shorter) instead of balancing rows; a grid with fewer tiles than fit keeps the empty columns.
 *     Grids of equal tiles in one sheet then share their columns, like a printed table
 */
public record GridGeometry(int tileWidth, int tileHeight, int count, int gap, boolean fullRows)
    implements SectionBody {
  /** Balanced rows: as short as possible, then as narrow as possible. */
  public GridGeometry(int tileWidth, int tileHeight, int count, int gap) {
    this(tileWidth, tileHeight, count, gap, false);
  }

  public GridGeometry {
    if (tileWidth <= 0 || tileHeight <= 0 || count <= 0 || gap < 0) {
      throw new IllegalArgumentException(
          "bad grid: " + count + " tiles of " + tileWidth + "x" + tileHeight + ", gap " + gap);
    }
  }

  /** Left edge of tile {@code index} with {@code columns} tiles per row. */
  public int tileLeft(int index, int columns) {
    return (index % columns) * (tileWidth + gap);
  }

  /** Top edge of tile {@code index} with {@code columns} tiles per row. */
  public int tileTop(int index, int columns) {
    return (index / columns) * (tileHeight + gap);
  }

  /** One shape per row count, from one column up to every tile in one row. */
  @Override
  public List<BodyShape> shapes() {
    List<BodyShape> shapes = new ArrayList<>();
    for (int rows = count; rows >= 1; rows--) {
      int columns = Math.ceilDiv(count, rows);
      // The fewest columns that give this many rows; skip row counts no column count gives.
      if (Math.ceilDiv(count, columns) != rows) {
        continue;
      }
      shapes.add(
          new BodyShape(
              columns,
              columns * tileWidth + (columns - 1) * gap,
              rows * tileHeight + (rows - 1) * gap));
    }
    return List.copyOf(shapes);
  }

  /**
   * With {@link #fullRows()}, as many columns as fit (as short, and the widest on ties), even past
   * the tile count.
   */
  @Override
  public BodyShape fit(int availableWidth) {
    if (!fullRows) {
      return SectionBody.super.fit(availableWidth);
    }
    int columns = Math.max(1, (availableWidth + gap) / (tileWidth + gap));
    int rows = Math.ceilDiv(count, columns);
    return new BodyShape(
        columns, columns * tileWidth + (columns - 1) * gap, rows * tileHeight + (rows - 1) * gap);
  }
}
