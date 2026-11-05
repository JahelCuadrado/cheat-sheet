package com.einsteinmods.cheatsheet.core.redstone;

import java.util.OptionalInt;

/**
 * What a comparator reads from a container, as {@code
 * AbstractContainerMenu#getRedstoneSignalFromContainer} computes it in Minecraft 26.3: each slot's
 * fullness (items over the item's stack size) summed in floats, divided by the slot count, then
 * {@code Mth.lerpDiscrete(fullness, 0, 15)}: 0 when empty, else {@code 1 + floor(fullness * 14)}.
 * Containers here are filled slot by slot with one kind of item, as players fill them.
 */
public final class ComparatorSignal {
  /** Most items in a stack: blocks, ingots. */
  public static final int STACK_64 = 64;

  /** Ender pearls, eggs, snowballs, signs, buckets of snow... */
  public static final int STACK_16 = 16;

  /** Tools, armour, potions: one per slot. */
  public static final int UNSTACKABLE = 1;

  /** The zoom's worked examples: this signal comes in at the input end. */
  public static final int EXAMPLE_INPUT = 10;

  /** The compare example's side signal that turns the output off: just above the input. */
  public static final int EXAMPLE_STRONGER_SIDE = EXAMPLE_INPUT + 1;

  /** The side signal of the subtract example. */
  public static final int EXAMPLE_SUBTRACTED_SIDE = 4;

  private ComparatorSignal() {}

  /**
   * {@code ComparatorBlock#calculateOutputSignal}: what comes out of the front for {@code input} at
   * the input end and {@code side} as the strongest side signal; 0 when the side is stronger.
   */
  public static int output(int input, int side, boolean subtract) {
    if (side > input) {
      return 0;
    }
    return subtract ? input - side : input;
  }

  /**
   * The signal of a container of {@code slots} slots holding {@code items} items that stack to
   * {@code stackSize}, filled slot by slot.
   */
  public static int containerSignal(int slots, int stackSize, int items) {
    if (slots <= 0 || stackSize <= 0 || items < 0 || items > (long) slots * stackSize) {
      throw new IllegalArgumentException(items + " items of " + stackSize + " in " + slots);
    }
    int fullSlots = items / stackSize;
    int rest = items % stackSize;
    // Vanilla adds 1.0F per full slot, then the partial slot: the same single rounding as this.
    float total = (float) fullSlots;
    if (rest > 0) {
      total += (float) rest / stackSize;
    }
    total /= slots;
    return lerpDiscrete(total, RedstoneRules.MAX_SIGNAL);
  }

  /**
   * The fewest items (stacking to {@code stackSize}) that make a container of {@code slots} slots
   * give exactly {@code signal}; empty when no count does (one unstackable item in a 3-slot furnace
   * already gives 5, skipping 1 to 4).
   */
  public static OptionalInt itemsForSignal(int slots, int stackSize, int signal) {
    if (signal < 1 || signal > RedstoneRules.MAX_SIGNAL) {
      throw new IllegalArgumentException("signal " + signal);
    }
    int capacity = slots * stackSize;
    for (int items = 1; items <= capacity; items++) {
      int reached = containerSignal(slots, stackSize, items);
      if (reached == signal) {
        return OptionalInt.of(items);
      }
      if (reached > signal) {
        return OptionalInt.empty();
      }
    }
    return OptionalInt.empty();
  }

  /** {@code Mth.lerpDiscrete(alpha, 0, max)}: 0 at 0, else {@code 1 + floor(alpha * (max - 1))}. */
  private static int lerpDiscrete(float alpha, int max) {
    return (int) Math.floor(alpha * (max - 1)) + (alpha > 0.0F ? 1 : 0);
  }
}
