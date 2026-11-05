package com.einsteinmods.cheatsheet.core.redstone;

/**
 * The item sorter's filter hopper (the wiki's basic overflow-safe sorter): {@link #ITEMS} of the
 * sorted item in its first slot and one junk item in each of the other {@link #JUNK_SLOTS}. Its
 * comparator then reads {@link #lockedSignal()}, which the circuit's dust fades out before the
 * torch; one more item reads {@link #openSignal()}, which reaches it and unlocks the hopper below.
 */
public final class SorterFilter {
  /** Items of the sorted kind (stacking to 64) kept in the filter. */
  public static final int ITEMS = 41;

  /** One junk item in each other slot of the hopper. */
  public static final int JUNK_SLOTS = 4;

  private static final int HOPPER_SLOTS = 5;

  private SorterFilter() {}

  /** The filter's comparator signal holding {@code items} sorted items and the junk. */
  public static int signal(int items) {
    // Each slot's fullness is a multiple of 1/64, so summing slot by slot equals one total.
    return ComparatorSignal.containerSignal(
        HOPPER_SLOTS, ComparatorSignal.STACK_64, items + JUNK_SLOTS);
  }

  /** With {@link #ITEMS}: the bottom hopper stays locked. */
  public static int lockedSignal() {
    return signal(ITEMS);
  }

  /** With one more item: the bottom hopper takes it. */
  public static int openSignal() {
    return signal(ITEMS + 1);
  }
}
