package com.einsteinmods.cheatsheet.core.sections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TableGeometryTest {
  private static final int ROW = 20;
  private static final int HEADER = 12;
  private static final int GAP = 6;

  /** Three columns of 10, 30 and 20: blocks 60 wide. */
  private static TableGeometry table(int rows) {
    return new TableGeometry(List.of(10, 30, 20), ROW, HEADER, rows, GAP);
  }

  @Test
  void columnsSitSideBySide() {
    TableGeometry table = table(5);
    assertEquals(60, table.blockWidth());
    assertEquals(0, table.columnLeft(0));
    assertEquals(10, table.columnLeft(1));
    assertEquals(40, table.columnLeft(2));
  }

  @Test
  void oneBlockHoldsEveryRowUnderTheHeader() {
    BodyShape one = table(5).narrowest();
    assertEquals(new BodyShape(1, 60, HEADER + 5 * ROW), one);
  }

  @Test
  void splitsOnlyWhenItShortensTheTable() {
    // 19 rows: 1, 2, 3, 4, 5 blocks give 19, 10, 7, 5, 4 rows; 6 blocks would also give 4.
    List<Integer> blocks = table(19).shapes().stream().map(BodyShape::split).toList();
    assertEquals(List.of(1, 2, 3, 4, 5, 7, 10, 19), blocks);
  }

  @Test
  void shapesGetWiderAndShorter() {
    List<BodyShape> shapes = table(19).shapes();
    for (int index = 1; index < shapes.size(); index++) {
      assertTrue(shapes.get(index).width() > shapes.get(index - 1).width());
      assertTrue(shapes.get(index).height() < shapes.get(index - 1).height());
    }
  }

  @Test
  void fitTakesTheMostBlocksTheWidthAllows() {
    TableGeometry table = table(19);
    // Two blocks: 60 + 6 + 60 = 126; three need 192.
    assertEquals(2, table.fit(191).split());
    assertEquals(3, table.fit(192).split());
    // Too narrow even for one block: one block, overflowing.
    assertEquals(1, table.fit(10).split());
  }

  @Test
  void readsDownEachBlockThenTheNext() {
    TableGeometry table = table(19);
    int blocks = 3;
    int previousBlock = 0;
    int previousTop = Integer.MIN_VALUE;
    for (int row = 0; row < 19; row++) {
      int block = table.blockOf(row, blocks);
      int top = table.rowTop(row, blocks);
      if (block == previousBlock) {
        assertTrue(top > previousTop, "row " + row + " is below the one before");
      } else {
        assertEquals(previousBlock + 1, block, "row " + row + " starts the next block");
        assertEquals(HEADER, top, "row " + row + " is the first of its block");
      }
      previousBlock = block;
      previousTop = top;
    }
    assertEquals(blocks - 1, previousBlock);
  }

  @Test
  void blocksDoNotOverlap() {
    TableGeometry table = table(19);
    for (int block = 1; block < 4; block++) {
      assertEquals(table.blockLeft(block - 1) + table.blockWidth() + GAP, table.blockLeft(block));
    }
  }

  @Test
  void rejectsEmptyTables() {
    assertThrows(
        IllegalArgumentException.class, () -> new TableGeometry(List.of(), ROW, HEADER, 3, GAP));
    assertThrows(
        IllegalArgumentException.class, () -> new TableGeometry(List.of(10), ROW, HEADER, 0, GAP));
    assertThrows(
        IllegalArgumentException.class, () -> new TableGeometry(List.of(0), ROW, HEADER, 3, GAP));
  }
}
