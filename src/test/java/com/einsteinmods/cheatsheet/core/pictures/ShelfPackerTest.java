package com.einsteinmods.cheatsheet.core.pictures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShelfPackerTest {
  private static final int GAP = 1;

  @Test
  void packsWithoutOverlapInsideTheImageInInputOrder() {
    List<PictureSize> sizes = new ArrayList<>();
    for (int index = 0; index < 30; index++) {
      sizes.add(new PictureSize(20 + index % 7 * 4, 24 + index % 5 * 5));
    }
    ShelfPacker.Packing packing = ShelfPacker.pack(sizes, GAP);
    assertEquals(sizes.size(), packing.rects().size());
    for (int index = 0; index < sizes.size(); index++) {
      ShelfPacker.Rect rect = packing.rects().get(index);
      assertEquals(sizes.get(index).width(), rect.width());
      assertEquals(sizes.get(index).height(), rect.height());
      assertTrue(rect.x() >= GAP && rect.y() >= GAP);
      assertTrue(rect.x() + rect.width() + GAP <= packing.width());
      assertTrue(rect.y() + rect.height() + GAP <= packing.height());
      for (int other = 0; other < index; other++) {
        assertFalse(overlap(rect, packing.rects().get(other)), index + " overlaps " + other);
      }
    }
  }

  @Test
  void staysCloseToSquare() {
    List<PictureSize> sizes = new ArrayList<>();
    for (int index = 0; index < 32; index++) {
      sizes.add(new PictureSize(22, 40));
    }
    ShelfPacker.Packing packing = ShelfPacker.pack(sizes, GAP);
    double ratio = (double) packing.width() / packing.height();
    assertTrue(ratio > 0.5 && ratio < 2, "ratio " + ratio);
  }

  @Test
  void emptyListPacksToNothing() {
    ShelfPacker.Packing packing = ShelfPacker.pack(List.of(), GAP);
    assertEquals(0, packing.width());
    assertTrue(packing.rects().isEmpty());
  }

  private static boolean overlap(ShelfPacker.Rect a, ShelfPacker.Rect b) {
    return a.x() < b.x() + b.width()
        && b.x() < a.x() + a.width()
        && a.y() < b.y() + b.height()
        && b.y() < a.y() + a.height();
  }
}
