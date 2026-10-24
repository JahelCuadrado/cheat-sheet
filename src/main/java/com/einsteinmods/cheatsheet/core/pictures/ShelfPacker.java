package com.einsteinmods.cheatsheet.core.pictures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Packs pictures into one image: tallest first, left to right in shelves, so a sheet's pictures
 * share one texture (one draw batch) and the image stays close to square.
 */
public final class ShelfPacker {
  private ShelfPacker() {}

  /** A picture's place in the packed image. */
  public record Rect(int x, int y, int width, int height) {}

  /** Rects in the order of the sizes given, and the size of the whole image. */
  public record Packing(List<Rect> rects, int width, int height) {
    public Packing {
      rects = List.copyOf(rects);
    }
  }

  /** Packs {@code sizes} with {@code gap} pixels between pictures and around the image's edges. */
  public static Packing pack(List<PictureSize> sizes, int gap) {
    if (sizes.isEmpty()) {
      return new Packing(List.of(), 0, 0);
    }
    long area = 0;
    int widest = 0;
    for (PictureSize size : sizes) {
      area += (long) (size.width() + gap) * (size.height() + gap);
      widest = Math.max(widest, size.width());
    }
    int shelfWidth = Math.max(widest + gap * 2, (int) Math.ceil(Math.sqrt(area)) + gap);
    Integer[] order = new Integer[sizes.size()];
    Arrays.setAll(order, index -> index);
    Arrays.sort(
        order, Comparator.comparingInt((Integer index) -> sizes.get(index).height()).reversed());
    Rect[] rects = new Rect[sizes.size()];
    int cursorX = gap;
    int shelfTop = gap;
    int shelfHeight = 0;
    int usedWidth = 0;
    for (int index : order) {
      PictureSize size = sizes.get(index);
      if (cursorX + size.width() + gap > shelfWidth && cursorX > gap) {
        shelfTop += shelfHeight + gap;
        cursorX = gap;
        shelfHeight = 0;
      }
      rects[index] = new Rect(cursorX, shelfTop, size.width(), size.height());
      cursorX += size.width() + gap;
      usedWidth = Math.max(usedWidth, cursorX);
      shelfHeight = Math.max(shelfHeight, size.height());
    }
    return new Packing(
        new ArrayList<>(Arrays.asList(rects)), usedWidth, shelfTop + shelfHeight + gap);
  }
}
