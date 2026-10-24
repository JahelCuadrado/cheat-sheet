package com.einsteinmods.cheatsheet.core.farming;

import java.util.ArrayList;
import java.util.List;

/**
 * The farming tab's plans: the most compact way to lay one farm out, nine blocks from north to
 * south, and four ways to lay sugar cane out on a 16x16 field. {@code FarmPlansTest} checks each
 * against {@link FarmRules}.
 */
public final class FarmPlans {
  /** Side of the square field the sugar cane layouts are drawn on. */
  public static final int CANE_FIELD = 16;

  /** Sugar cane layout 1: strips of water and cane, one block wide each. Half the field is cane. */
  public static final FarmPlan CANE_ROWS = repeated(CANE_FIELD, "SWSWSWSWSWSWSWSW");

  /** Sugar cane layout 2: a strip of water between every two strips of cane. */
  public static final FarmPlan CANE_PAIRS = repeated(CANE_FIELD, "WSSWSSWSSWSSWSSW");

  /**
   * Sugar cane layout 3: water a knight's move apart, each with the four canes around it, crosses
   * fitted together; the blocks at the edges that no cross reaches stay free (easier to walk), and
   * the middle of the north and south edges is a two-block gate (two canes fewer: 190).
   */
  public static final FarmPlan CANE_CROSSES =
      FarmPlan.of(
          "WSSDSWSDDSWSSDSW",
          "SSWSSSSWSSSSWSSS",
          "DSSSWSSSSWSSSSWS",
          "SWSSSSWSSSSWSSSD",
          "SSSWSSSSWSSSSWSS",
          "WSSSSWSSSSWSSSSW",
          "SSWSSSSWSSSSWSSS",
          "DSSSWSSSSWSSSSWS",
          "SWSSSSWSSSSWSSSD",
          "SSSWSSSSWSSSSWSS",
          "WSSSSWSSSSWSSSSW",
          "SSWSSSSWSSSSWSSS",
          "DSSSWSSSSWSSSSWS",
          "SWSSSSWSSSSWSSSD",
          "SSSWSSSSWSSSSWSS",
          "WSDSSWSDDSWSDSSW");

  /**
   * Sugar cane layout 4, the one that fills the field: one water block in four, shifted along each
   * row so that every block of the field is cane or water, cane everywhere water is not.
   */
  public static final FarmPlan CANE_DENSEST =
      repeated(
          CANE_FIELD / 4,
          "SWSSSWSSSWSSSWSS",
          "SSSWSSSWSSSWSSSW",
          "WSSSWSSSWSSSWSSS",
          "SSWSSSWSSSWSSSWS");

  /** The four sugar cane layouts, in the order they are numbered. */
  public static final List<FarmPlan> CANE_LAYOUTS =
      List.of(CANE_ROWS, CANE_PAIRS, CANE_CROSSES, CANE_DENSEST);

  /**
   * A 9x9 field around one water block (all hydrated), wheat and carrots in alternating rows so no
   * crop is slowed by its neighbours.
   */
  public static final FarmPlan FIELD =
      FarmPlan.of(
          "HHHHHHHHH",
          "RRRRRRRRR",
          "HHHHHHHHH",
          "RRRRRRRRR",
          "HHHHWHHHH",
          "RRRRRRRRR",
          "HHHHHHHHH",
          "RRRRRRRRR",
          "HHHHHHHHH");

  /**
   * Stems on farmland along a water row, a free block beside each for its fruit: pumpkins to the
   * west, melons to the east, some already grown.
   */
  public static final FarmPlan STEMS =
      FarmPlan.of("PpWmD", "DpWmM", "PpWmM", "DpWmD", "PpWmM", "DpWmM", "PpWmD", "DpWmM", "PpWmM");

  /** Cactus in a checkerboard on sand: nothing touches a cactus' sides. */
  public static final FarmPlan CACTUS =
      FarmPlan.of(
          "XAXAXAXAX",
          "AXAXAXAXA",
          "XAXAXAXAX",
          "AXAXAXAXA",
          "XAXAXAXAX",
          "AXAXAXAXA",
          "XAXAXAXAX",
          "AXAXAXAXA",
          "XAXAXAXAX");

  private FarmPlans() {}

  /** {@code rows} repeated {@code times} times, top to bottom. */
  private static FarmPlan repeated(int times, String... rows) {
    List<String> all = new ArrayList<>(rows.length * times);
    for (int copy = 0; copy < times; copy++) {
      all.addAll(List.of(rows));
    }
    return FarmPlan.of(all.toArray(new String[0]));
  }
}
