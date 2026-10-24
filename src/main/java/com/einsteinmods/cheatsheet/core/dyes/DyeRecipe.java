package com.einsteinmods.cheatsheet.core.dyes;

import java.util.List;

/**
 * One vanilla recipe that makes a dye.
 *
 * @param color the dye colour's id ({@code light_gray})
 * @param kind crafting or smelting
 * @param ingredients item ids without namespace, one per item used (repeats included)
 * @param count dyes made
 */
public record DyeRecipe(String color, DyeRecipeKind kind, List<String> ingredients, int count) {
  private static final String DYE_SUFFIX = "_dye";

  public DyeRecipe {
    if (ingredients.isEmpty() || count < 1) {
      throw new IllegalArgumentException("bad dye recipe for " + color);
    }
    if (kind == DyeRecipeKind.SMELTING && ingredients.size() != 1) {
      throw new IllegalArgumentException("smelting takes one ingredient: " + color);
    }
    ingredients = List.copyOf(ingredients);
  }

  /** A mix of other dyes (two or more ingredients), rather than a plant or item turned into dye. */
  public boolean isMix() {
    return ingredients.size() > 1;
  }

  /** Item id of the dye made. */
  public String resultItemId() {
    return dyeItemId(color);
  }

  public static String dyeItemId(String color) {
    return color + DYE_SUFFIX;
  }
}
