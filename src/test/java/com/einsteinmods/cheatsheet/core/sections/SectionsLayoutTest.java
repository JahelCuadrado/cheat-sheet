package com.einsteinmods.cheatsheet.core.sections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.einsteinmods.cheatsheet.core.CardBounds;
import com.einsteinmods.cheatsheet.core.sections.SectionsLayout.PlacedSection;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SectionsLayoutTest {
  private static final SectionsLayout.Spacing SPACING = new SectionsLayout.Spacing(10, 8, 6);
  private static final long SEED = 7L;
  private static final int RANDOM_SHEETS = 500;

  /** 19 rows of 40 in blocks 200 wide, header 12 (like the trims by place). */
  private static final TableGeometry BY_PLACE =
      new TableGeometry(List.of(80, 100, 20), 40, 12, 19, 6);

  /** 18 rows of 20 in blocks 280 wide (like the trims colours). */
  private static final TableGeometry COLOURS =
      new TableGeometry(List.of(60, 200, 20), 20, 18, 18, 6);

  @Test
  void pairGoesBesideWhenBothFit() {
    SectionsLayout layout = SectionsLayout.of(List.of(List.of(BY_PLACE, COLOURS)), 500, SPACING);
    PlacedSection left = layout.sections().get(0);
    PlacedSection right = layout.sections().get(1);
    assertEquals(left.y(), right.y());
    assertEquals(left.x() + left.shape().width() + SPACING.pairGap(), right.x());
  }

  @Test
  void pairGoesBelowWhenItDoesNotFit() {
    SectionsLayout layout = SectionsLayout.of(List.of(List.of(BY_PLACE, COLOURS)), 480, SPACING);
    PlacedSection top = layout.sections().get(0);
    PlacedSection below = layout.sections().get(1);
    assertEquals(
        top.y() + SPACING.titleHeight() + top.shape().height() + SPACING.rowGap(), below.y());
    // Alone, each takes the width it can: two blocks of the first.
    assertEquals(2, top.shape().split());
  }

  @Test
  void besideSplitsToKeepTheTallerShort() {
    // 700 wide: two blocks of the first (10 rows: 412) beside one of the second (378) beats one
    // block of the first (772) beside one; below would take 292 + 8 + 10 + 198 = 508.
    SectionsLayout layout = SectionsLayout.of(List.of(List.of(BY_PLACE, COLOURS)), 700, SPACING);
    assertEquals(layout.sections().get(0).y(), layout.sections().get(1).y());
    assertEquals(2, layout.sections().get(0).shape().split());
    assertEquals(1, layout.sections().get(1).shape().split());
    assertEquals(SPACING.titleHeight() + 12 + 10 * 40, layout.contentHeight());
  }

  @Test
  void besideWinsSmallDifferences() {
    // 960 wide: three blocks of the first (292) beside one of the second (378); below would be
    // four blocks (212) + 8 + 10 + three blocks (138) = 368, under a tenth shorter.
    SectionsLayout layout = SectionsLayout.of(List.of(List.of(BY_PLACE, COLOURS)), 960, SPACING);
    assertEquals(layout.sections().get(0).y(), layout.sections().get(1).y());
    assertEquals(3, layout.sections().get(0).shape().split());
    assertEquals(1, layout.sections().get(1).shape().split());
  }

  @Test
  void pairGoesBelowWhenThatIsMuchShorter() {
    // 640 wide: beside, the first fits only one block (772 tall); below, it takes three blocks
    // (292) and the second two (198): 292 + 8 + 10 + 198 = 508.
    SectionsLayout layout = SectionsLayout.of(List.of(List.of(BY_PLACE, COLOURS)), 640, SPACING);
    PlacedSection top = layout.sections().get(0);
    PlacedSection below = layout.sections().get(1);
    assertTrue(below.y() > top.y());
    assertEquals(3, top.shape().split());
    assertEquals(2, below.shape().split());
  }

  @Test
  void rowsCentredAndStacked() {
    SectionsLayout layout = SectionsLayout.of(List.of(List.of(COLOURS)), 400, SPACING);
    PlacedSection only = layout.sections().getFirst();
    assertEquals((400 - 280) / 2, only.x());
    assertEquals(0, only.y());
    assertEquals(SPACING.titleHeight() + only.shape().height(), layout.contentHeight());
  }

  @Test
  void randomSheetsNeverOverlapAndKeepReadingOrder() {
    Random random = new Random(SEED);
    for (int sheet = 0; sheet < RANDOM_SHEETS; sheet++) {
      List<List<SectionBody>> rows =
          List.of(
              List.of(randomTable(random), randomTable(random)),
              List.of(randomGrid(random)),
              List.of(randomGrid(random), randomTable(random)));
      int width = 150 + random.nextInt(900);
      SectionsLayout layout = SectionsLayout.of(rows, width, SPACING);
      List<PlacedSection> placed = layout.sections();
      assertEquals(5, placed.size());
      for (int index = 0; index < placed.size(); index++) {
        CardBounds bounds = bounds(placed.get(index));
        assertTrue(bounds.bottom() <= layout.contentHeight());
        for (int other = index + 1; other < placed.size(); other++) {
          CardBounds next = bounds(placed.get(other));
          assertFalse(bounds.overlaps(next), "sheet " + sheet + ": " + index + ", " + other);
          // Reading order: later sections are below, or beside on the right.
          assertTrue(
              next.y() >= bounds.bottom() || (next.y() == bounds.y() && next.x() >= bounds.right()),
              "sheet " + sheet + ": " + other + " comes after " + index);
        }
        int fitted = placed.get(index).shape().width();
        assertTrue(
            fitted <= width || placed.get(index).shape().split() == 1,
            "only a section narrower than nothing overflows");
      }
    }
  }

  private static CardBounds bounds(PlacedSection section) {
    return new CardBounds(
        section.x(),
        section.y(),
        section.shape().width(),
        SPACING.titleHeight() + section.shape().height());
  }

  private static TableGeometry randomTable(Random random) {
    return new TableGeometry(
        List.of(10 + random.nextInt(60), 10 + random.nextInt(120)),
        10 + random.nextInt(30),
        random.nextInt(20),
        1 + random.nextInt(25),
        6);
  }

  private static GridGeometry randomGrid(Random random) {
    return new GridGeometry(
        10 + random.nextInt(50), 10 + random.nextInt(50), 1 + random.nextInt(20), 2);
  }
}
