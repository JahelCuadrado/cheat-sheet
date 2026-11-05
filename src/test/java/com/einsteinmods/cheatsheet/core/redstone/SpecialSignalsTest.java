package com.einsteinmods.cheatsheet.core.redstone;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class SpecialSignalsTest {
  @Test
  void respawnAnchorGivesItsFiveLevels() {
    assertEquals(List.of(0, 3, 7, 11, 15), SpecialSignals.respawnAnchorSignals());
  }

  @Test
  void cakeGivesTwoPerSliceLeft() {
    assertEquals(14, SpecialSignals.cake(0));
    assertEquals(2, SpecialSignals.cake(SpecialSignals.CAKE_SLICES - 1));
  }

  @Test
  void lecternRunsFromOneToFifteen() {
    assertEquals(1, SpecialSignals.lectern(0, 10));
    assertEquals(15, SpecialSignals.lectern(9, 10));
    assertEquals(15, SpecialSignals.lectern(0, 1));
    assertEquals(8, SpecialSignals.lectern(1, 3));
  }

  @Test
  void shelfAddsItsSlotsBits() {
    assertEquals(0, SpecialSignals.shelf(false, false, false));
    assertEquals(1, SpecialSignals.shelf(true, false, false));
    assertEquals(5, SpecialSignals.shelf(true, false, true));
    assertEquals(7, SpecialSignals.shelf(true, true, true));
  }

  @Test
  void chiseledBookshelfCountsSlotsFromOne() {
    assertEquals(1, SpecialSignals.chiseledBookshelf(0));
    assertEquals(
        SpecialSignals.CHISELED_BOOKSHELF_SLOTS,
        SpecialSignals.chiseledBookshelf(SpecialSignals.CHISELED_BOOKSHELF_SLOTS - 1));
  }

  @Test
  void creakingHeartFadesToZeroAtItsRange() {
    assertEquals(15, SpecialSignals.creakingHeart(0));
    assertEquals(13, SpecialSignals.creakingHeart(6));
    assertEquals(1, SpecialSignals.creakingHeart(31));
    assertEquals(0, SpecialSignals.creakingHeart(SpecialSignals.CREAKING_HEART_RANGE));
    assertEquals(0, SpecialSignals.creakingHeart(40));
  }
}
