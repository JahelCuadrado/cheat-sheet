package com.einsteinmods.cheatsheet.core.redstone;

/**
 * The crafter as Minecraft 26.3 makes it; {@code RedstoneMechanicsGameTest} fills one through a
 * hopper, crafts with it in a world and checks each number.
 */
public final class CrafterRules {
  /** {@code CrafterBlockEntity}: a 3×3 grid. */
  public static final int SLOTS = 9;

  /**
   * {@code CrafterBlock#neighborChanged}: a signal turning on schedules one craft this many game
   * ticks later; it must turn off before the next.
   */
  public static final int CRAFT_DELAY_TICKS = 4;

  /** The example recipe, a chest: the grid's centre slot is disabled, planks fill the rest. */
  public static final int EXAMPLE_DISABLED_SLOT = 4;

  private CrafterRules() {}

  /**
   * {@code CrafterBlockEntity#getRedstoneSignal}: a comparator reads one per slot that holds an
   * item or is disabled, 0 to 9.
   */
  public static int comparatorSignal(int filledSlots, int disabledSlots) {
    if (filledSlots < 0 || disabledSlots < 0 || filledSlots + disabledSlots > SLOTS) {
      throw new IllegalArgumentException(filledSlots + " filled, " + disabledSlots + " disabled");
    }
    return filledSlots + disabledSlots;
  }

  /**
   * Game ticks between crafts of a full nine-slot recipe filled by {@code hoppers} hoppers: each
   * passes one item every {@link RedstoneRules#HOPPER_TICKS_PER_ITEM}.
   */
  public static int ticksPerCraft(int hoppers) {
    if (hoppers <= 0) {
      throw new IllegalArgumentException(hoppers + " hoppers");
    }
    return SLOTS * RedstoneRules.HOPPER_TICKS_PER_ITEM / hoppers;
  }

  /**
   * The slot a hopper puts one more item of a kind into ({@code CrafterBlockEntity#canPlaceItem},
   * tried slot by slot in reading order): the first enabled slot with the fewest of that item, an
   * empty slot counting as none. {@code counts}: the item's count per slot; {@code disabled}: which
   * slots are disabled. -1 when every enabled slot is full ({@code maxStack}).
   */
  public static int nextSlot(int[] counts, boolean[] disabled, int maxStack) {
    if (counts.length != SLOTS || disabled.length != SLOTS) {
      throw new IllegalArgumentException("a crafter has " + SLOTS + " slots");
    }
    int best = -1;
    for (int slot = 0; slot < SLOTS; slot++) {
      if (!disabled[slot] && counts[slot] < maxStack && (best < 0 || counts[slot] < counts[best])) {
        best = slot;
      }
    }
    return best;
  }
}
