package com.einsteinmods.cheatsheet.core.redstone;

import java.util.List;
import java.util.Locale;

/**
 * Containers a comparator reads by fullness, grouped by slot count (their sizes in Minecraft 26.3:
 * {@code ChestBlockEntity}, {@code BarrelBlockEntity}, {@code ShulkerBoxBlockEntity} 27; a double
 * chest 54; {@code DispenserBlockEntity} 9; {@code HopperBlockEntity} and {@code
 * BrewingStandBlockEntity} 5; {@code AbstractFurnaceBlockEntity} 3; {@code DecoratedPotBlockEntity}
 * 1). The first item id is the row's icon; {@code ComparatorSignalGameTest} fills real ones and
 * reads a comparator. The crafter is not here: it counts slots, not fullness ({@link
 * CrafterRules#comparatorSignal}).
 */
public enum SignalContainer {
  CHEST(27, List.of("chest", "barrel", "shulker_box")),
  DOUBLE_CHEST(54, List.of("chest")),
  DISPENSER(9, List.of("dispenser", "dropper")),
  HOPPER(5, List.of("hopper", "brewing_stand")),
  FURNACE(3, List.of("furnace", "smoker", "blast_furnace")),
  DECORATED_POT(1, List.of("decorated_pot"));

  private final int slots;
  private final List<String> itemIds;

  SignalContainer(int slots, List<String> itemIds) {
    this.slots = slots;
    this.itemIds = List.copyOf(itemIds);
  }

  public int slots() {
    return slots;
  }

  /** Item ids (without namespace) of the containers of this size. */
  public List<String> itemIds() {
    return itemIds;
  }

  /** Fewest items stacking to 64 for each signal 1 to 15 (index 0 is signal 1). */
  public int[] itemsPerSignal() {
    int[] items = new int[RedstoneRules.MAX_SIGNAL];
    for (int signal = 1; signal <= RedstoneRules.MAX_SIGNAL; signal++) {
      items[signal - 1] =
          ComparatorSignal.itemsForSignal(slots, ComparatorSignal.STACK_64, signal).orElseThrow();
    }
    return items;
  }

  /** Lower-case id for lang keys. */
  public String key() {
    return name().toLowerCase(Locale.ROOT);
  }
}
