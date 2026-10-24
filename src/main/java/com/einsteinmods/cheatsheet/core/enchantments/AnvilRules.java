package com.einsteinmods.cheatsheet.core.enchantments;

import java.util.ArrayList;
import java.util.List;

/**
 * What combining enchantments on an anvil costs, as {@code AnvilMenu#createResult} works it out:
 * the prior work penalty of both items (each anvil use makes it {@code 2p + 1}, so after {@code n}
 * uses it is {@code 2^n - 1}), plus each enchantment of the right item at its new level times its
 * anvil cost (halved, at least 1, from a book). A cost of {@link #TOO_EXPENSIVE} or more is refused
 * in survival. The left item's own enchantments are free.
 */
public final class AnvilRules {
  /** {@code AnvilMenu#createResult} / {@code AnvilScreen}: "Too Expensive!" from this cost. */
  public static final int TOO_EXPENSIVE = 40;

  private AnvilRules() {}

  /** The penalty an item has after one more anvil use ({@code calculateIncreasedRepairCost}). */
  public static int nextPenalty(int penalty) {
    if (penalty < 0) {
      throw new IllegalArgumentException("penalty " + penalty);
    }
    return (int) Math.min(penalty * 2L + 1L, Integer.MAX_VALUE);
  }

  /** The penalty of an item used {@code uses} times on an anvil: {@code 2^uses - 1}. */
  public static int penaltyAfterUses(int uses) {
    if (uses < 0) {
      throw new IllegalArgumentException("uses " + uses);
    }
    int penalty = 0;
    for (int use = 0; use < uses; use++) {
      penalty = nextPenalty(penalty);
    }
    return penalty;
  }

  /** The result's penalty: one more use than the more used of the two items. */
  public static int mergedPenalty(int leftPenalty, int rightPenalty) {
    return nextPenalty(Math.max(leftPenalty, rightPenalty));
  }

  /** Levels an enchantment of the right item costs per level, from a book or an item. */
  public static int feePerLevel(int anvilCost, boolean fromBook) {
    return fromBook ? Math.max(1, anvilCost / 2) : anvilCost;
  }

  /**
   * One level of the pyramid: {@code merges} pairs of items, both of a pair with {@code
   * inputPenalty}; or, for the last step, the gear (penalty 0) on the left and the combined book.
   *
   * @param merges anvil uses at this level
   * @param leftPenalty penalty of each left item
   * @param rightPenalty penalty of each right item
   */
  public record Step(int merges, int leftPenalty, int rightPenalty) {
    /** Penalty levels each merge of this step pays (enchantments not counted). */
    public int penaltyCost() {
      return leftPenalty + rightPenalty;
    }

    /** The penalty of each result. */
    public int resultPenalty() {
      return mergedPenalty(leftPenalty, rightPenalty);
    }
  }

  /**
   * Books merged in pairs, then pairs of pairs, down to one book that goes onto the gear last:
   * every item takes part in as few uses as possible. {@code books} is a power of two.
   */
  public static List<Step> pyramid(int books) {
    if (books < 1 || Integer.bitCount(books) != 1) {
      throw new IllegalArgumentException("a pyramid needs a power of two books: " + books);
    }
    List<Step> steps = new ArrayList<>();
    int penalty = 0;
    for (int items = books; items > 1; items /= 2) {
      Step step = new Step(items / 2, penalty, penalty);
      steps.add(step);
      penalty = step.resultPenalty();
    }
    steps.add(new Step(1, 0, penalty));
    return List.copyOf(steps);
  }

  /** Penalty levels a whole plan pays, enchantments not counted. */
  public static int penaltyCost(List<Step> steps) {
    int total = 0;
    for (Step step : steps) {
      total += step.merges() * step.penaltyCost();
    }
    return total;
  }

  /** Penalty levels of putting {@code books} books onto the gear one after another. */
  public static int oneByOnePenaltyCost(int books) {
    int total = 0;
    for (int book = 0; book < books; book++) {
      total += penaltyAfterUses(book);
    }
    return total;
  }

  /**
   * The first book that is always too expensive when books go onto the gear one by one: its penalty
   * alone reaches {@link #TOO_EXPENSIVE} (counting from 1).
   */
  public static int firstTooExpensiveBookOneByOne() {
    int book = 1;
    while (penaltyAfterUses(book - 1) < TOO_EXPENSIVE) {
      book++;
    }
    return book;
  }
}
