package com.einsteinmods.cheatsheet.core.pictures;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlaceTest {
  /** A diorama small enough to read at 40 pixels, big enough to be recognised. */
  private static final int MIN_BLOCKS = 10;

  private static final int MAX_BLOCKS = 30;
  private static final int MAX_SIDE = 6;

  @Test
  void everyDioramaKeepsOneBlockPerCellInsideItsBox() {
    for (Place place : Place.values()) {
      Diorama diorama = place.diorama();
      Set<List<Integer>> cells = new HashSet<>();
      for (DioramaBlock block : diorama.blocks()) {
        assertTrue(
            cells.add(List.of(block.x(), block.y(), block.z())),
            place + " repeats a cell " + block);
        assertTrue(
            block.x() >= 0
                && block.y() >= 0
                && block.z() >= 0
                && block.x() < diorama.width()
                && block.y() < diorama.height()
                && block.z() < diorama.depth(),
            place + " has a block outside its box: " + block);
      }
    }
  }

  @Test
  void everyDioramaHasAFewBlocksInASmallBox() {
    for (Place place : Place.values()) {
      Diorama diorama = place.diorama();
      int count = diorama.blocks().size();
      assertTrue(count >= MIN_BLOCKS && count <= MAX_BLOCKS, place + " has " + count + " blocks");
      assertTrue(
          Math.max(diorama.width(), Math.max(diorama.height(), diorama.depth())) <= MAX_SIDE,
          place + " is too big");
    }
  }

  @Test
  void everyDioramaUsesItsWholeBox() {
    for (Place place : Place.values()) {
      Diorama diorama = place.diorama();
      int[] max = new int[3];
      for (DioramaBlock block : diorama.blocks()) {
        max[0] = Math.max(max[0], block.x() + 1);
        max[1] = Math.max(max[1], block.y() + 1);
        max[2] = Math.max(max[2], block.z() + 1);
      }
      assertTrue(
          max[0] == diorama.width() && max[1] == diorama.height() && max[2] == diorama.depth(),
          place + " declares a box bigger than its blocks");
    }
  }
}
