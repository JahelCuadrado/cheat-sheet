package com.einsteinmods.cheatsheet.core.trades;

import java.util.Optional;

/**
 * One key of a crafting recipe: an item, or an item tag shown as one of its items.
 *
 * @param itemId the item, or the representative item shown for the tag ({@code minecraft:}
 *     namespace implied)
 * @param tagId the item tag the recipe accepts ({@code planks}), empty for a single item
 */
public record RecipeIngredient(String itemId, Optional<String> tagId) {
  public static RecipeIngredient item(String itemId) {
    return new RecipeIngredient(itemId, Optional.empty());
  }

  /** Any item of {@code tagId}, drawn as {@code shownItemId}. */
  public static RecipeIngredient tag(String tagId, String shownItemId) {
    return new RecipeIngredient(shownItemId, Optional.of(tagId));
  }
}
