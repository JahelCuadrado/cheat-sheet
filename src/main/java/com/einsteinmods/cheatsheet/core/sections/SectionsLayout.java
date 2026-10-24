package com.einsteinmods.cheatsheet.core.sections;

import java.util.ArrayList;
import java.util.List;

/**
 * Lays out a sheet made of sections (a title over one table or grid), stacked top to bottom in
 * reading order, each row centred. A row may hold a pair: the two sections go side by side when
 * both fit the width at their narrowest (splitting so the taller of the two is as short as
 * possible); the second goes below the first instead when that fits in the width and saves more
 * than {@link #STACKING_SAVING_PERCENT} % of the height (a pair side by side reads as one view, so
 * it wins small differences). Each section alone takes the shortest shape that fits the width,
 * stretched when its body can spread out ({@link SectionBody#stretch}).
 *
 * <p>Cost: a few passes over each body's shapes; it runs on resize and tab change only.
 *
 * @param sections where each section goes, in input order (row by row, left before right)
 * @param contentHeight bottom of the lowest section
 */
public record SectionsLayout(List<PlacedSection> sections, int contentHeight) {
  /** Below wins over beside only when it makes the pair this much shorter, in percent. */
  public static final int STACKING_SAVING_PERCENT = 10;

  private static final int PERCENT = 100;

  public SectionsLayout {
    sections = List.copyOf(sections);
  }

  /** Where a section goes: its title's top-left corner, and the body's shape below the title. */
  public record PlacedSection(int x, int y, BodyShape shape) {}

  /** Spacing: the title's height above each body, between rows, and between a pair. */
  public record Spacing(int titleHeight, int rowGap, int pairGap) {
    public Spacing {
      if (titleHeight < 0 || rowGap < 0 || pairGap < 0) {
        throw new IllegalArgumentException("negative spacing");
      }
    }
  }

  /**
   * @param rows sections in reading order; each row is one section or a pair (beside if it fits)
   * @param availableWidth sheet width
   */
  public static SectionsLayout of(
      List<List<SectionBody>> rows, int availableWidth, Spacing spacing) {
    List<PlacedSection> placed = new ArrayList<>();
    int top = 0;
    for (List<SectionBody> row : rows) {
      if (row.isEmpty() || row.size() > 2) {
        throw new IllegalArgumentException("a row holds one section or a pair: " + row.size());
      }
      List<BodyShape> shapes = row.size() == 2 ? beside(row, availableWidth, spacing) : List.of();
      if (!shapes.isEmpty()
          && (long) Math.max(shapes.get(0).height(), shapes.get(1).height()) * PERCENT
              > (long) stackedHeight(row, availableWidth, spacing)
                  * (PERCENT + STACKING_SAVING_PERCENT)) {
        shapes = List.of();
      }
      if (shapes.isEmpty()) {
        for (SectionBody body : row) {
          BodyShape shape = body.stretch(body.fit(availableWidth), availableWidth);
          placed.add(new PlacedSection(centred(shape.width(), availableWidth), top, shape));
          top += spacing.titleHeight() + shape.height() + spacing.rowGap();
        }
        continue;
      }
      BodyShape left = shapes.get(0);
      BodyShape right = shapes.get(1);
      int x = centred(left.width() + spacing.pairGap() + right.width(), availableWidth);
      placed.add(new PlacedSection(x, top, left));
      placed.add(new PlacedSection(x + left.width() + spacing.pairGap(), top, right));
      top += spacing.titleHeight() + Math.max(left.height(), right.height()) + spacing.rowGap();
    }
    return new SectionsLayout(placed, Math.max(0, top - spacing.rowGap()));
  }

  /**
   * Shapes of a pair side by side, the taller as short as possible (narrower on ties); empty when
   * the two do not fit side by side even at their narrowest.
   */
  private static List<BodyShape> beside(
      List<SectionBody> pair, int availableWidth, Spacing spacing) {
    SectionBody first = pair.get(0);
    SectionBody second = pair.get(1);
    int room = availableWidth - spacing.pairGap();
    if (first.narrowest().width() + second.narrowest().width() > room) {
      return List.of();
    }
    BodyShape bestLeft = null;
    BodyShape bestRight = null;
    int bestHeight = Integer.MAX_VALUE;
    int bestWidth = Integer.MAX_VALUE;
    for (BodyShape left : first.shapes()) {
      if (left.width() + second.narrowest().width() > room) {
        break;
      }
      BodyShape right = second.fit(room - left.width());
      int height = Math.max(left.height(), right.height());
      int width = left.width() + right.width();
      if (height < bestHeight || (height == bestHeight && width < bestWidth)) {
        bestLeft = left;
        bestRight = right;
        bestHeight = height;
        bestWidth = width;
      }
    }
    return bestLeft == null ? List.of() : List.of(bestLeft, bestRight);
  }

  /** Height of a pair's bodies one below the other, counting the second title and the gap. */
  private static int stackedHeight(List<SectionBody> pair, int availableWidth, Spacing spacing) {
    return pair.get(0).fit(availableWidth).height()
        + spacing.rowGap()
        + spacing.titleHeight()
        + pair.get(1).fit(availableWidth).height();
  }

  private static int centred(int width, int availableWidth) {
    return Math.max(0, (availableWidth - width) / 2);
  }
}
