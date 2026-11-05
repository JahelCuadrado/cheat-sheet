package com.einsteinmods.cheatsheet.core.redstone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class ComparatorSignalTest {
  private static final int FULL_STACKS_PER_SIGNAL_DIVISOR = 14;

  @Test
  void emptyIsZeroAndFullIsFifteen() {
    for (SignalContainer container : SignalContainer.values()) {
      int slots = container.slots();
      assertEquals(0, ComparatorSignal.containerSignal(slots, ComparatorSignal.STACK_64, 0));
      assertEquals(1, ComparatorSignal.containerSignal(slots, ComparatorSignal.STACK_64, 1));
      assertEquals(
          RedstoneRules.MAX_SIGNAL,
          ComparatorSignal.containerSignal(
              slots, ComparatorSignal.STACK_64, slots * ComparatorSignal.STACK_64));
    }
  }

  @Test
  void workedExamplesMatchTheZoom() {
    assertEquals(10, ComparatorSignal.output(10, 0, false));
    assertEquals(10, ComparatorSignal.output(10, 10, false));
    assertEquals(0, ComparatorSignal.output(10, 11, false));
    assertEquals(6, ComparatorSignal.output(10, 4, true));
    assertEquals(0, ComparatorSignal.output(10, 11, true));
  }

  /** The wiki's worked examples (minecraft.wiki/w/Redstone_Comparator). */
  @Test
  void matchesTheWikiExamples() {
    assertEquals(8, ComparatorSignal.containerSignal(9, ComparatorSignal.STACK_64, 300));
    assertEquals(OptionalInt.of(110), ComparatorSignal.itemsForSignal(3, 64, 9));
    // Hopper, strength 10: at least three stacks and 14 (206).
    assertEquals(OptionalInt.of(206), ComparatorSignal.itemsForSignal(5, 64, 10));
  }

  /** The wiki's closed form: max(signal, ceil(slots * 64 / 14 * (signal - 1))). */
  @Test
  void everyTableCellMatchesTheWikiFormula() {
    for (SignalContainer container : SignalContainer.values()) {
      int[] items = container.itemsPerSignal();
      for (int signal = 1; signal <= RedstoneRules.MAX_SIGNAL; signal++) {
        long scaled = (long) container.slots() * ComparatorSignal.STACK_64 * (signal - 1);
        long ceil = (scaled + FULL_STACKS_PER_SIGNAL_DIVISOR - 1) / FULL_STACKS_PER_SIGNAL_DIVISOR;
        assertEquals(Math.max(signal, ceil), items[signal - 1], container + " signal " + signal);
      }
    }
  }

  @Test
  void eachCountIsTheFewestForItsSignal() {
    for (SignalContainer container : SignalContainer.values()) {
      int[] items = container.itemsPerSignal();
      for (int signal = 1; signal <= RedstoneRules.MAX_SIGNAL; signal++) {
        int count = items[signal - 1];
        assertEquals(
            signal,
            ComparatorSignal.containerSignal(container.slots(), ComparatorSignal.STACK_64, count));
        assertTrue(
            ComparatorSignal.containerSignal(
                    container.slots(), ComparatorSignal.STACK_64, count - 1)
                < signal);
      }
    }
  }

  @Test
  void unstackablesSkipLevelsInSmallContainers() {
    // One sword in a furnace: 1/3 full gives 1 + floor(14/3) = 5.
    assertEquals(5, ComparatorSignal.containerSignal(3, ComparatorSignal.UNSTACKABLE, 1));
    assertTrue(ComparatorSignal.itemsForSignal(3, ComparatorSignal.UNSTACKABLE, 2).isEmpty());
    // A brewing stand's three potions: 3/5 full.
    assertEquals(9, ComparatorSignal.containerSignal(5, ComparatorSignal.UNSTACKABLE, 3));
  }

  @Test
  void sorterFilterSitsJustBelowTheSignalThatOpens() {
    assertEquals(2, SorterFilter.lockedSignal());
    assertEquals(3, SorterFilter.openSignal());
  }
}
