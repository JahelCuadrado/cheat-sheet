package com.einsteinmods.cheatsheet.core.redstone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CrafterRulesTest {
  @Test
  void signalCountsFilledAndDisabledSlots() {
    assertEquals(0, CrafterRules.comparatorSignal(0, 0));
    assertEquals(9, CrafterRules.comparatorSignal(1, 8));
    assertEquals(5, CrafterRules.comparatorSignal(5, 0));
    assertThrows(IllegalArgumentException.class, () -> CrafterRules.comparatorSignal(5, 5));
  }

  @Test
  void hoppersFillTheFirstSlotWithTheFewest() {
    int[] counts = new int[CrafterRules.SLOTS];
    boolean[] disabled = new boolean[CrafterRules.SLOTS];
    disabled[CrafterRules.EXAMPLE_DISABLED_SLOT] = true;
    int[] order = {0, 1, 2, 3, 5, 6, 7, 8, 0, 1};
    for (int expected : order) {
      int slot = CrafterRules.nextSlot(counts, disabled, 64);
      assertEquals(expected, slot);
      counts[slot]++;
    }
    int[] full = {64, 64, 64, 64, 0, 64, 64, 64, 64};
    assertEquals(-1, CrafterRules.nextSlot(full, disabled, 64));
  }

  @Test
  void transparentHalvesComeFromTheirState() {
    for (TransparentBlock block : TransparentBlock.ALL) {
      if (block.state().contains("top")) {
        assertEquals(TransparentBlock.Half.TOP, block.half(), block.state());
      } else if (block.state().contains("bottom")) {
        assertEquals(TransparentBlock.Half.BOTTOM, block.half(), block.state());
      } else {
        assertEquals(TransparentBlock.Half.WHOLE, block.half(), block.state());
      }
    }
  }

  @Test
  void aSecondHopperDoublesTheSpeed() {
    assertEquals(72, CrafterRules.ticksPerCraft(1));
    assertEquals(36, CrafterRules.ticksPerCraft(2));
    assertThrows(IllegalArgumentException.class, () -> CrafterRules.ticksPerCraft(0));
  }
}
