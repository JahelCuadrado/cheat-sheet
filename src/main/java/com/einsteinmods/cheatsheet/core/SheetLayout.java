package com.einsteinmods.cheatsheet.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Packs cards into columns, masonry style: the sheet width is split into equal columns; each card
 * spans as many columns as its minimum width needs (at most all of them) and goes, in order, where
 * its top is highest or where it leaves the least empty space above it ({@link Placement}). Every
 * column count from one up to what {@code minColumnWidth} allows is tried with both placements and
 * the shortest sheet wins (fewer, wider columns on ties), so cards fill the width instead of
 * leaving holes. Deterministic, and cards never overlap: a card's top is below everything already
 * placed in the columns it spans.
 *
 * <p>Cost: one pass over the cards per column count and placement tried; it runs on resize and tab
 * change only.
 *
 * @param cards where each card goes, in input order
 * @param columns how many columns the width was split into
 * @param contentHeight bottom of the lowest card
 */
public record SheetLayout(List<CardBounds> cards, int columns, int contentHeight) {
  public SheetLayout {
    cards = List.copyOf(cards);
  }

  /**
   * @param sizes cards in reading order
   * @param availableWidth sheet width; cards wider than it overflow (spans are clamped)
   * @param minColumnWidth narrowest column; caps how many columns are tried
   * @param gap space between cards, horizontally and vertically
   */
  public static SheetLayout pack(
      List<CardSize> sizes, int availableWidth, int minColumnWidth, int gap) {
    if (minColumnWidth <= 0 || gap < 0) {
      throw new IllegalArgumentException("bad column width or gap: " + minColumnWidth + ", " + gap);
    }
    int width = Math.max(availableWidth, minColumnWidth);
    int maxColumns = Math.max(1, (width + gap) / (minColumnWidth + gap));
    SheetLayout best = packInto(sizes, width, 1, gap, Placement.HIGHEST);
    for (int columns = 1; columns <= maxColumns; columns++) {
      for (Placement placement : Placement.values()) {
        SheetLayout candidate = packInto(sizes, width, columns, gap, placement);
        if (candidate.contentHeight < best.contentHeight) {
          best = candidate;
        }
      }
    }
    return best;
  }

  /** Where a card goes among the positions its span allows. */
  enum Placement {
    /** Where its top is highest (leftmost on ties). */
    HIGHEST,
    /**
     * Where it leaves the least empty space above it in the columns it spans (then highest): a wide
     * card waits for level columns instead of leaving holes beside a short one.
     */
    LEVEL
  }

  /**
   * Masonry into exactly {@code columns} equal columns, cards placed the {@link Placement#HIGHEST}
   * way.
   */
  static SheetLayout packInto(List<CardSize> sizes, int width, int columns, int gap) {
    return packInto(sizes, width, columns, gap, Placement.HIGHEST);
  }

  /** Masonry into exactly {@code columns} equal columns. */
  static SheetLayout packInto(
      List<CardSize> sizes, int width, int columns, int gap, Placement placement) {
    int columnWidth = (width - gap * (columns - 1)) / columns;
    // Leftover pixels of the integer division: the sheet is centred in the available width.
    int left = (width - (columnWidth * columns + gap * (columns - 1))) / 2;
    int[] columnBottoms = new int[columns];
    List<CardBounds> placed = new ArrayList<>(sizes.size());
    int contentHeight = 0;
    for (CardSize size : sizes) {
      int span = Math.min(columns, spanFor(size.minWidth(), columnWidth, gap));
      int bestColumn = 0;
      int bestTop = Integer.MAX_VALUE;
      long bestWaste = Long.MAX_VALUE;
      for (int column = 0; column + span <= columns; column++) {
        int top = maxOf(columnBottoms, column, span);
        long waste =
            placement == Placement.LEVEL ? wasteAbove(columnBottoms, column, span, top) : 0;
        if (waste < bestWaste || (waste == bestWaste && top < bestTop)) {
          bestWaste = waste;
          bestTop = top;
          bestColumn = column;
        }
      }
      int x = left + bestColumn * (columnWidth + gap);
      int cardWidth = columnWidth * span + gap * (span - 1);
      int cardHeight = size.heightAt(cardWidth);
      placed.add(new CardBounds(x, bestTop, cardWidth, cardHeight));
      Arrays.fill(columnBottoms, bestColumn, bestColumn + span, bestTop + cardHeight + gap);
      contentHeight = Math.max(contentHeight, bestTop + cardHeight);
    }
    return new SheetLayout(placed, columns, contentHeight);
  }

  /** Columns a card of this width needs. */
  static int spanFor(int width, int columnWidth, int gap) {
    int span = 1;
    while (columnWidth * span + gap * (span - 1) < width) {
      span++;
    }
    return span;
  }

  /** Empty pixels left between the spanned columns' bottoms and a card placed at {@code top}. */
  private static long wasteAbove(int[] bottoms, int from, int count, int top) {
    long waste = 0;
    for (int index = from; index < from + count; index++) {
      waste += top - bottoms[index];
    }
    return waste;
  }

  private static int maxOf(int[] values, int from, int count) {
    int max = 0;
    for (int index = from; index < from + count; index++) {
      max = Math.max(max, values[index]);
    }
    return max;
  }
}
