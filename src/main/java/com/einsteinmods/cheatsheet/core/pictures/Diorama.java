package com.einsteinmods.cheatsheet.core.pictures;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small hand-built block scene (a place in miniature) inside a box of {@code width} (x, east) ×
 * {@code height} (y, up) × {@code depth} (z, south) cells. Blocks are listed in the order they were
 * placed; each cell holds at most one block.
 */
public record Diorama(int width, int height, int depth, List<DioramaBlock> blocks) {
  public Diorama {
    blocks = List.copyOf(blocks);
  }

  public static Builder builder(int width, int height, int depth) {
    return new Builder(width, height, depth);
  }

  /** Places blocks cell by cell; a later block replaces an earlier one in the same cell. */
  public static final class Builder {
    private final int width;
    private final int height;
    private final int depth;
    private final Map<Cell, DioramaBlock> cells = new LinkedHashMap<>();

    private Builder(int width, int height, int depth) {
      this.width = width;
      this.height = height;
      this.depth = depth;
    }

    /** One block, {@code state} written as in commands ({@code oak_stairs[facing=east]}). */
    public Builder block(String state, int x, int y, int z) {
      if (x < 0 || y < 0 || z < 0 || x >= width || y >= height || z >= depth) {
        throw new IllegalArgumentException(
            "cell " + x + "," + y + "," + z + " outside " + width + "x" + height + "x" + depth);
      }
      cells.put(new Cell(x, y, z), new DioramaBlock(BlockSpec.parse(state), x, y, z));
      return this;
    }

    /** Every cell of the box between two corners (inclusive) filled with {@code state}. */
    public Builder box(String state, int x0, int y0, int z0, int x1, int y1, int z1) {
      for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
        for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
          for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
            block(state, x, y, z);
          }
        }
      }
      return this;
    }

    /** Empties a cell (carves a door, a window). */
    public Builder clear(int x, int y, int z) {
      cells.remove(new Cell(x, y, z));
      return this;
    }

    public Diorama build() {
      return new Diorama(width, height, depth, new ArrayList<>(cells.values()));
    }
  }

  private record Cell(int x, int y, int z) {}
}
