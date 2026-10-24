package com.einsteinmods.cheatsheet.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SheetLayoutTest {
  private static final int MIN_COLUMN = 120;
  private static final int GAP = 6;
  private static final long SEED = 42L;
  private static final int RANDOM_SHEETS = 500;

  @Test
  void narrowSheetStacksCardsInOneColumn() {
    SheetLayout layout =
        SheetLayout.pack(
            List.of(CardSize.fixed(100, 50), CardSize.fixed(80, 30)), MIN_COLUMN, MIN_COLUMN, GAP);
    assertEquals(1, layout.columns());
    assertEquals(new CardBounds(0, 0, MIN_COLUMN, 50), layout.cards().get(0));
    assertEquals(new CardBounds(0, 50 + GAP, MIN_COLUMN, 30), layout.cards().get(1));
    assertEquals(50 + GAP + 30, layout.contentHeight());
  }

  @Test
  void cardGoesUnderTheShortestColumn() {
    // Two columns of 120: a tall card left, a short one right; the third goes right.
    SheetLayout layout =
        SheetLayout.packInto(
            List.of(CardSize.fixed(50, 100), CardSize.fixed(50, 20), CardSize.fixed(50, 20)),
            MIN_COLUMN * 2 + GAP,
            2,
            GAP);
    CardBounds third = layout.cards().get(2);
    assertEquals(MIN_COLUMN + GAP, third.x());
    assertEquals(20 + GAP, third.y());
  }

  @Test
  void cardSpansWhatItsMinimumWidthNeeds() {
    SheetLayout layout =
        SheetLayout.packInto(
            List.of(CardSize.fixed(MIN_COLUMN + 1, 40)), MIN_COLUMN * 4 + GAP * 3, 4, GAP);
    assertEquals(MIN_COLUMN * 2 + GAP, layout.cards().getFirst().width());
  }

  @Test
  void narrowWidthClampsTheSpan() {
    SheetLayout narrow = SheetLayout.pack(List.of(CardSize.fixed(500, 40)), 200, MIN_COLUMN, GAP);
    assertEquals(1, narrow.columns());
    assertEquals(200, narrow.cards().getFirst().width());
  }

  @Test
  void shortestColumnCountWins() {
    // Three equal cards fit side by side: three columns beat four (a hole) and two (a second row).
    List<CardSize> sizes = List.of(sized(100, 60), sized(100, 60), sized(100, 60));
    SheetLayout layout = SheetLayout.pack(sizes, 500, 100, GAP);
    assertEquals(60, layout.contentHeight());
    assertEquals(3, layout.columns());
  }

  @Test
  void wideCardWaitsForLevelColumnsInsteadOfLeavingAHole() {
    // Three columns of 100: a short card, two tall ones, then a two-column card and a short one.
    List<CardSize> sizes =
        List.of(sized(100, 10), sized(100, 50), sized(100, 50), sized(200, 20), sized(100, 40));
    int width = 100 * 3 + GAP * 2;
    SheetLayout highest = SheetLayout.packInto(sizes, width, 3, GAP);
    // Leftmost on ties: the wide card sits over the short one, leaving a hole under it.
    assertEquals(0, highest.cards().get(3).x());
    assertEquals(50 + GAP + 40, highest.contentHeight());
    SheetLayout layout = SheetLayout.pack(sizes, width, 100, GAP);
    // Over the two tall cards instead; the last card fills the gap under the short one.
    assertEquals(100 + GAP, layout.cards().get(3).x());
    assertEquals(new CardBounds(0, 10 + GAP, 100, 40), layout.cards().get(4));
    assertEquals(50 + GAP + 20, layout.contentHeight());
  }

  @Test
  void longTitleWrapsInsteadOfSpanningTwoColumns() {
    // Content 60 wide; the title needs 150 on one line, two lines (10 px taller) when wrapped.
    CardSize titled = new CardSize(60, 40, 150, 50);
    SheetLayout narrow = SheetLayout.packInto(List.of(titled), MIN_COLUMN * 2 + GAP, 2, GAP);
    CardBounds wrapped = narrow.cards().getFirst();
    assertEquals(MIN_COLUMN, wrapped.width());
    assertEquals(50, wrapped.height());
    SheetLayout wide = SheetLayout.packInto(List.of(titled), 400, 1, GAP);
    assertEquals(40, wide.cards().getFirst().height());
  }

  @Test
  void heightFollowsTheTitleThreshold() {
    CardSize titled = new CardSize(60, 40, 150, 50);
    assertEquals(50, titled.heightAt(149));
    assertEquals(40, titled.heightAt(150));
    assertEquals(40, CardSize.fixed(60, 40).heightAt(60));
  }

  @Test
  void randomSheetsNeverOverlapStayInsideAndRepeat() {
    Random random = new Random(SEED);
    for (int sheet = 0; sheet < RANDOM_SHEETS; sheet++) {
      int width = 150 + random.nextInt(1200);
      List<CardSize> sizes = new ArrayList<>();
      int count = 1 + random.nextInt(12);
      for (int card = 0; card < count; card++) {
        int minWidth = 40 + random.nextInt(400);
        int height = 20 + random.nextInt(200);
        sizes.add(
            new CardSize(
                minWidth, height, minWidth + random.nextInt(200), height + random.nextInt(3) * 10));
      }
      SheetLayout layout = SheetLayout.pack(sizes, width, MIN_COLUMN, GAP);
      assertEquals(layout, SheetLayout.pack(sizes, width, MIN_COLUMN, GAP), "deterministic");
      List<CardBounds> cards = layout.cards();
      for (int first = 0; first < cards.size(); first++) {
        CardBounds card = cards.get(first);
        CardSize size = sizes.get(first);
        assertTrue(card.x() >= 0 && card.right() <= width, "inside: " + card + " in " + width);
        assertTrue(card.bottom() <= layout.contentHeight(), "below content: " + card);
        assertEquals(size.heightAt(card.width()), card.height(), "height for its width");
        // Equal columns leave fewer pixels than columns unused (the sheet is centred).
        boolean fits = size.minWidth() <= width - layout.columns();
        assertTrue(!fits || card.width() >= size.minWidth(), "wide enough: " + card);
        for (int second = first + 1; second < cards.size(); second++) {
          assertFalse(card.overlaps(cards.get(second)), card + " overlaps " + cards.get(second));
        }
      }
      for (int columns = 1; columns <= layout.columns() + 1; columns++) {
        if (columns * MIN_COLUMN + (columns - 1) * GAP <= Math.max(width, MIN_COLUMN)) {
          SheetLayout other =
              SheetLayout.packInto(sizes, Math.max(width, MIN_COLUMN), columns, GAP);
          assertTrue(other.contentHeight() >= layout.contentHeight(), "shortest wins");
        }
      }
    }
  }

  private static CardSize sized(int width, int height) {
    return CardSize.fixed(width, height);
  }
}
