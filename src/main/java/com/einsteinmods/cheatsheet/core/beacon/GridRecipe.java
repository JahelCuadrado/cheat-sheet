package com.einsteinmods.cheatsheet.core.beacon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A crafting table recipe written as vanilla's data files write it: rows of symbols and what each
 * symbol stands for. A shapeless recipe is written the same way, its symbols in any order.
 *
 * @param resultId item made, one of it (id without namespace)
 * @param pattern up to three rows of up to three symbols; a space is an empty slot
 * @param key item id (without namespace) of each symbol
 * @param shapeless the items go in any slots
 */
public record GridRecipe(
    String resultId, List<String> pattern, Map<Character, String> key, boolean shapeless) {
  /** Slots per side of the crafting grid. */
  public static final int SIDE = 3;

  private static final char EMPTY = ' ';

  public GridRecipe {
    pattern = List.copyOf(pattern);
    key = Collections.unmodifiableMap(new LinkedHashMap<>(key));
    if (pattern.isEmpty() || pattern.size() > SIDE) {
      throw new IllegalArgumentException(resultId + ": " + pattern.size() + " rows");
    }
    for (String row : pattern) {
      if (row.isEmpty() || row.length() > SIDE) {
        throw new IllegalArgumentException(resultId + ": row '" + row + "'");
      }
      for (char symbol : row.toCharArray()) {
        if (symbol != EMPTY && !key.containsKey(symbol)) {
          throw new IllegalArgumentException(resultId + ": no item for '" + symbol + "'");
        }
      }
    }
  }

  /** The nine slots of the crafting grid, row by row, the pattern in the top-left corner. */
  public List<Optional<String>> slots() {
    List<Optional<String>> slots = new ArrayList<>(SIDE * SIDE);
    for (int row = 0; row < SIDE; row++) {
      for (int column = 0; column < SIDE; column++) {
        char symbol =
            row < pattern.size() && column < pattern.get(row).length()
                ? pattern.get(row).charAt(column)
                : EMPTY;
        slots.add(symbol == EMPTY ? Optional.empty() : Optional.of(key.get(symbol)));
      }
    }
    return List.copyOf(slots);
  }

  /** How many of each item the recipe takes, in the order they first appear in the pattern. */
  public Map<String, Integer> counts() {
    Map<String, Integer> counts = new LinkedHashMap<>();
    for (String row : pattern) {
      for (char symbol : row.toCharArray()) {
        if (symbol != EMPTY) {
          counts.merge(key.get(symbol), 1, Integer::sum);
        }
      }
    }
    return Collections.unmodifiableMap(counts);
  }
}
