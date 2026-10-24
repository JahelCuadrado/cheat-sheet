package com.einsteinmods.cheatsheet.core.dyes;

import java.util.ArrayList;
import java.util.List;

/**
 * Every vanilla recipe that makes a dye. Recipes are server data, never sent to the client, so this
 * is a copy; {@code DyeRecipeGameTest} checks it against the game's recipe manager.
 */
public final class DyeTable {
  private static final String WHITE = "white";
  private static final String ORANGE = "orange";
  private static final String MAGENTA = "magenta";
  private static final String LIGHT_BLUE = "light_blue";
  private static final String YELLOW = "yellow";
  private static final String LIME = "lime";
  private static final String PINK = "pink";
  private static final String GRAY = "gray";
  private static final String LIGHT_GRAY = "light_gray";
  private static final String CYAN = "cyan";
  private static final String PURPLE = "purple";
  private static final String BLUE = "blue";
  private static final String BROWN = "brown";
  private static final String GREEN = "green";
  private static final String RED = "red";
  private static final String BLACK = "black";

  private static final String WHITE_DYE = DyeRecipe.dyeItemId(WHITE);
  private static final String BLACK_DYE = DyeRecipe.dyeItemId(BLACK);
  private static final String GRAY_DYE = DyeRecipe.dyeItemId(GRAY);
  private static final String BLUE_DYE = DyeRecipe.dyeItemId(BLUE);
  private static final String RED_DYE = DyeRecipe.dyeItemId(RED);
  private static final String PINK_DYE = DyeRecipe.dyeItemId(PINK);
  private static final String GREEN_DYE = DyeRecipe.dyeItemId(GREEN);

  /** The sixteen colours in vanilla order (the creative inventory's). */
  public static final List<String> COLORS =
      List.of(
          WHITE,
          ORANGE,
          MAGENTA,
          LIGHT_BLUE,
          YELLOW,
          LIME,
          PINK,
          GRAY,
          LIGHT_GRAY,
          CYAN,
          PURPLE,
          BLUE,
          BROWN,
          GREEN,
          RED,
          BLACK);

  /** Plants and items first, then mixes, as the sheet shows them. */
  public static final List<DyeRecipe> RECIPES =
      List.of(
          craft(WHITE, 1, "bone_meal"),
          craft(WHITE, 1, "lily_of_the_valley"),
          craft(ORANGE, 1, "orange_tulip"),
          craft(ORANGE, 1, "torchflower"),
          craft(ORANGE, 1, "open_eyeblossom"),
          craft(ORANGE, 2, RED_DYE, DyeRecipe.dyeItemId(YELLOW)),
          craft(MAGENTA, 1, "allium"),
          craft(MAGENTA, 2, "lilac"),
          craft(MAGENTA, 2, DyeRecipe.dyeItemId(PURPLE), PINK_DYE),
          craft(MAGENTA, 3, BLUE_DYE, RED_DYE, PINK_DYE),
          craft(MAGENTA, 4, BLUE_DYE, RED_DYE, RED_DYE, WHITE_DYE),
          craft(LIGHT_BLUE, 1, "blue_orchid"),
          craft(LIGHT_BLUE, 2, BLUE_DYE, WHITE_DYE),
          craft(YELLOW, 1, "dandelion"),
          craft(YELLOW, 1, "golden_dandelion"),
          craft(YELLOW, 2, "sunflower"),
          craft(YELLOW, 1, "wildflowers"),
          smelt(LIME, "sea_pickle"),
          craft(LIME, 2, GREEN_DYE, WHITE_DYE),
          craft(PINK, 1, "pink_tulip"),
          craft(PINK, 2, "peony"),
          craft(PINK, 1, "pink_petals"),
          craft(PINK, 1, "cactus_flower"),
          craft(PINK, 2, RED_DYE, WHITE_DYE),
          craft(GRAY, 1, "closed_eyeblossom"),
          craft(GRAY, 2, BLACK_DYE, WHITE_DYE),
          craft(LIGHT_GRAY, 1, "azure_bluet"),
          craft(LIGHT_GRAY, 1, "oxeye_daisy"),
          craft(LIGHT_GRAY, 1, "white_tulip"),
          craft(LIGHT_GRAY, 2, GRAY_DYE, WHITE_DYE),
          craft(LIGHT_GRAY, 3, BLACK_DYE, WHITE_DYE, WHITE_DYE),
          craft(CYAN, 2, "pitcher_plant"),
          craft(CYAN, 2, BLUE_DYE, GREEN_DYE),
          craft(PURPLE, 2, BLUE_DYE, RED_DYE),
          craft(BLUE, 1, "lapis_lazuli"),
          craft(BLUE, 1, "cornflower"),
          craft(BROWN, 1, "cocoa_beans"),
          smelt(GREEN, "cactus"),
          craft(RED, 1, "poppy"),
          craft(RED, 1, "red_tulip"),
          craft(RED, 2, "rose_bush"),
          craft(RED, 1, "beetroot"),
          craft(BLACK, 1, "ink_sac"),
          craft(BLACK, 1, "wither_rose"));

  private DyeTable() {}

  /** Recipes making the {@code color} dye, in table order. */
  public static List<DyeRecipe> recipesFor(String color) {
    List<DyeRecipe> found = new ArrayList<>();
    for (DyeRecipe recipe : RECIPES) {
      if (recipe.color().equals(color)) {
        found.add(recipe);
      }
    }
    return List.copyOf(found);
  }

  private static DyeRecipe craft(String color, int count, String... ingredients) {
    return new DyeRecipe(color, DyeRecipeKind.CRAFTING, List.of(ingredients), count);
  }

  private static DyeRecipe smelt(String color, String ingredient) {
    return new DyeRecipe(color, DyeRecipeKind.SMELTING, List.of(ingredient), 1);
  }
}
