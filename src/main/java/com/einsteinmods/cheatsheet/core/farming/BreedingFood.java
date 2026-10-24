package com.einsteinmods.cheatsheet.core.farming;

import java.util.List;

/**
 * What one animal is fed to breed.
 *
 * @param entityIds the animal (and any animal bred the same way), first one shown
 * @param food its breeding food
 * @param tamed it must be tamed first
 */
public record BreedingFood(List<String> entityIds, Food food, boolean tamed) {
  public BreedingFood {
    entityIds = List.copyOf(entityIds);
    if (entityIds.isEmpty()) {
      throw new IllegalArgumentException("a breeding food needs an animal");
    }
  }

  /** Where the food comes from. */
  public sealed interface Food permits TagFood, FixedFood {}

  /**
   * The items of an item tag, read from the game (data packs and mods change it).
   *
   * @param tagId the tag, e.g. {@code minecraft:cow_food}
   */
  public record TagFood(String tagId) implements Food {}

  /**
   * Items written in the animal's code, not in a tag.
   *
   * @param itemIds the items, e.g. {@code minecraft:golden_carrot}
   */
  public record FixedFood(List<String> itemIds) implements Food {
    public FixedFood {
      itemIds = List.copyOf(itemIds);
      if (itemIds.isEmpty()) {
        throw new IllegalArgumentException("a fixed food needs items");
      }
    }
  }
}
