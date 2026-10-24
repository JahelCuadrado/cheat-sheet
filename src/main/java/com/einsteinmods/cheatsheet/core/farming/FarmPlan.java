package com.einsteinmods.cheatsheet.core.farming;

import java.util.ArrayList;
import java.util.List;

/**
 * A farm seen from above, as a grid of {@link PlanCell}s: row 0 is the north edge, column 0 the
 * west edge.
 *
 * @param width blocks from west to east
 * @param height blocks from north to south
 * @param cells row by row, {@code width * height} of them
 */
public record FarmPlan(int width, int height, List<PlanCell> cells) {
  public FarmPlan {
    cells = List.copyOf(cells);
    if (width < 1 || height < 1 || cells.size() != width * height) {
      throw new IllegalArgumentException(width + "x" + height + " plan with " + cells.size());
    }
  }

  /** A plan written as rows of {@link PlanCell} symbols, all of the same length. */
  public static FarmPlan of(String... rows) {
    if (rows.length == 0) {
      throw new IllegalArgumentException("a plan needs rows");
    }
    int width = rows[0].length();
    List<PlanCell> cells = new ArrayList<>(width * rows.length);
    for (String row : rows) {
      if (row.length() != width) {
        throw new IllegalArgumentException("row '" + row + "' is not " + width + " wide");
      }
      for (char symbol : row.toCharArray()) {
        cells.add(PlanCell.of(symbol));
      }
    }
    return new FarmPlan(width, rows.length, cells);
  }

  /** The cell at column {@code x}, row {@code y}. */
  public PlanCell cell(int x, int y) {
    if (!contains(x, y)) {
      throw new IndexOutOfBoundsException(x + "," + y + " outside " + width + "x" + height);
    }
    return cells.get(y * width + x);
  }

  /** How many cells of the plan are {@code wanted}. */
  public int count(PlanCell wanted) {
    int count = 0;
    for (PlanCell cell : cells) {
      if (cell == wanted) {
        count++;
      }
    }
    return count;
  }

  /** Whether column {@code x}, row {@code y} is inside the plan. */
  public boolean contains(int x, int y) {
    return x >= 0 && x < width && y >= 0 && y < height;
  }
}
