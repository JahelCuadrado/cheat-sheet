package com.einsteinmods.cheatsheet.core.trades;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The shaped crafting recipe of a villager workstation, as vanilla writes it ({@code
 * data/minecraft/recipe/<workstation>.json}): rows of key characters, a space for an empty slot.
 *
 * @param workstationId the crafted block's item id ({@code minecraft:} namespace implied)
 * @param pattern rows, top to bottom, at most {@link #SIZE} by {@link #SIZE}
 * @param key what each character stands for
 */
public record WorkstationRecipe(
    String workstationId, List<String> pattern, Map<Character, RecipeIngredient> key) {
  /** A crafting table's side. */
  public static final int SIZE = 3;

  private static final char EMPTY = ' ';

  public WorkstationRecipe {
    pattern = List.copyOf(pattern);
    key = Map.copyOf(key);
    if (pattern.isEmpty() || pattern.size() > SIZE) {
      throw new IllegalArgumentException(workstationId + ": " + pattern.size() + " rows");
    }
    for (String row : pattern) {
      if (row.isEmpty() || row.length() > SIZE) {
        throw new IllegalArgumentException(workstationId + ": bad row '" + row + "'");
      }
      for (char symbol : row.toCharArray()) {
        if (symbol != EMPTY && !key.containsKey(symbol)) {
          throw new IllegalArgumentException(workstationId + ": no key for '" + symbol + "'");
        }
      }
    }
  }

  /**
   * The {@link #SIZE}x{@link #SIZE} crafting grid read row by row, the pattern in its top-left
   * corner (where the recipe book puts it); empty slots are empty.
   */
  public List<Optional<RecipeIngredient>> grid() {
    List<Optional<RecipeIngredient>> slots = new ArrayList<>(SIZE * SIZE);
    for (int row = 0; row < SIZE; row++) {
      String line = row < pattern.size() ? pattern.get(row) : "";
      for (int column = 0; column < SIZE; column++) {
        char symbol = column < line.length() ? line.charAt(column) : EMPTY;
        slots.add(symbol == EMPTY ? Optional.empty() : Optional.of(key.get(symbol)));
      }
    }
    return List.copyOf(slots);
  }
}
