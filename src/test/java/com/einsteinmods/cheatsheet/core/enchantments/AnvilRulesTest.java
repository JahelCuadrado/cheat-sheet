package com.einsteinmods.cheatsheet.core.enchantments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class AnvilRulesTest {
  @Test
  void thePenaltyDoublesPlusOneWithEveryUse() {
    int[] expected = {0, 1, 3, 7, 15, 31, 63};
    for (int uses = 0; uses < expected.length; uses++) {
      assertEquals(expected[uses], AnvilRules.penaltyAfterUses(uses));
    }
    assertEquals(7, AnvilRules.mergedPenalty(3, 1));
    assertEquals(Integer.MAX_VALUE, AnvilRules.nextPenalty(Integer.MAX_VALUE));
  }

  @Test
  void booksPayHalfTheAnvilCostButAtLeastOne() {
    assertEquals(4, AnvilRules.feePerLevel(8, true));
    assertEquals(1, AnvilRules.feePerLevel(1, true));
    assertEquals(8, AnvilRules.feePerLevel(8, false));
  }

  @Test
  void eightBooksInAPyramidPayFarLessThanOneByOne() {
    List<AnvilRules.Step> steps = AnvilRules.pyramid(8);
    assertEquals(
        List.of(
            new AnvilRules.Step(4, 0, 0),
            new AnvilRules.Step(2, 1, 1),
            new AnvilRules.Step(1, 3, 3),
            new AnvilRules.Step(1, 0, 7)),
        steps);
    assertEquals(17, AnvilRules.penaltyCost(steps));
    assertEquals(15, steps.getLast().resultPenalty());
    assertEquals(247, AnvilRules.oneByOnePenaltyCost(8));
    for (AnvilRules.Step step : steps) {
      assertTrue(step.penaltyCost() < AnvilRules.TOO_EXPENSIVE);
    }
  }

  @Test
  void fourBooksInAPyramid() {
    List<AnvilRules.Step> steps = AnvilRules.pyramid(4);
    assertEquals(3, steps.size());
    assertEquals(5, AnvilRules.penaltyCost(steps));
    assertEquals(11, AnvilRules.oneByOnePenaltyCost(4));
  }

  @Test
  void oneByOneTheSeventhBookIsAlwaysTooExpensive() {
    assertEquals(7, AnvilRules.firstTooExpensiveBookOneByOne());
    assertTrue(AnvilRules.penaltyAfterUses(6) >= AnvilRules.TOO_EXPENSIVE);
    assertTrue(AnvilRules.penaltyAfterUses(5) < AnvilRules.TOO_EXPENSIVE);
  }

  @Test
  void aPyramidNeedsAPowerOfTwoBooks() {
    assertThrows(IllegalArgumentException.class, () -> AnvilRules.pyramid(6));
    assertThrows(IllegalArgumentException.class, () -> AnvilRules.pyramid(0));
  }
}
