package com.einsteinmods.cheatsheet.core.sections;

/**
 * One way to lay out a section's body.
 *
 * @param split how it is split: side-by-side blocks for a table, tiles per row for a grid
 * @param width its width in GUI pixels
 * @param height its height in GUI pixels
 */
public record BodyShape(int split, int width, int height) {
  public BodyShape {
    if (split <= 0 || width <= 0 || height <= 0) {
      throw new IllegalArgumentException(
          "bad body shape: split " + split + ", " + width + "x" + height);
    }
  }
}
