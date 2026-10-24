package com.einsteinmods.cheatsheet.core.trades;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BarterTableTest {
  private static final double TOLERANCE = 1.0e-12;

  @Test
  void chancesAddUpToOne() {
    double total = 0.0;
    for (BarterRow row : BarterTable.ROWS) {
      total += BarterTable.chance(row);
    }
    assertEquals(1.0, total, TOLERANCE);
    assertEquals(469, BarterTable.totalWeight());
  }

  @Test
  void soulSpeedBookIsTheRarest() {
    assertEquals(5.0 / 469.0, BarterTable.chance(BarterTable.ROWS.getFirst()), TOLERANCE);
  }
}
