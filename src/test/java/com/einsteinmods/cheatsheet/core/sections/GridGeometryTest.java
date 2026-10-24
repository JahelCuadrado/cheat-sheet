package com.einsteinmods.cheatsheet.core.sections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GridGeometryTest {
  private static final int TILE = 30;
  private static final int GAP = 2;

  @Test
  void fitPutsAsManyTilesPerRowAsTheWidthAllows() {
    GridGeometry grid = new GridGeometry(TILE, TILE, 16, GAP);
    // 8 per row: 8 * 30 + 7 * 2 = 254.
    assertEquals(new BodyShape(8, 254, 2 * TILE + GAP), grid.fit(254));
    // 253 fits 7, but 7 per row still needs 3 rows: 6 per row is as short and narrower.
    assertEquals(6, grid.fit(253).split());
    assertEquals(16, grid.fit(10_000).split());
  }

  @Test
  void fullRowsPutAsManyTilesPerRowAsFit() {
    GridGeometry grid = new GridGeometry(TILE, TILE, 16, GAP, true);
    // 253 fits 7 per row: rows of 7, 7 and 2 instead of balanced rows of 6.
    assertEquals(new BodyShape(7, 7 * TILE + 6 * GAP, 3 * TILE + 2 * GAP), grid.fit(253));
    assertEquals(1, grid.fit(1).split());
    // Fewer tiles than fit: the empty columns stay, so the grid lines up with fuller ones.
    assertEquals(new BodyShape(8, 254, TILE), new GridGeometry(TILE, TILE, 3, GAP, true).fit(254));
  }

  @Test
  void tilesAreAlignedInColumnsAndRows() {
    GridGeometry grid = new GridGeometry(TILE, TILE + 4, 16, GAP);
    int columns = 8;
    Set<Integer> lefts = new HashSet<>();
    Set<Integer> tops = new HashSet<>();
    for (int index = 0; index < 16; index++) {
      lefts.add(grid.tileLeft(index, columns));
      tops.add(grid.tileTop(index, columns));
    }
    assertEquals(columns, lefts.size());
    assertEquals(2, tops.size());
    assertEquals(TILE + GAP, grid.tileLeft(1, columns));
    assertEquals(TILE + 4 + GAP, grid.tileTop(columns, columns));
  }

  @Test
  void tilesNeverOverlapAndKeepReadingOrder() {
    GridGeometry grid = new GridGeometry(TILE, TILE, 11, GAP);
    for (BodyShape shape : grid.shapes()) {
      int columns = shape.split();
      for (int index = 1; index < 11; index++) {
        int left = grid.tileLeft(index, columns);
        int top = grid.tileTop(index, columns);
        int previousLeft = grid.tileLeft(index - 1, columns);
        int previousTop = grid.tileTop(index - 1, columns);
        boolean nextInRow = top == previousTop && left >= previousLeft + TILE + GAP;
        boolean nextRow = top >= previousTop + TILE + GAP && left == 0;
        assertTrue(nextInRow || nextRow, "tile " + index + " at " + columns + " per row");
        assertTrue(left + TILE <= shape.width() && top + TILE <= shape.height());
      }
    }
  }

  @Test
  void everyShapeShortensTheGrid() {
    List<BodyShape> shapes = new GridGeometry(TILE, TILE, 11, GAP).shapes();
    // 11 tiles: 11, 6, 4, 3, 2 and 1 rows; 5 rows would need 3 per row, which gives 4 rows.
    assertEquals(List.of(1, 2, 3, 4, 6, 11), shapes.stream().map(BodyShape::split).toList());
    for (int index = 1; index < shapes.size(); index++) {
      assertTrue(shapes.get(index).height() < shapes.get(index - 1).height());
    }
  }
}
