package com.einsteinmods.cheatsheet.core.beacon;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A conduit's frame ({@code ConduitBlockEntity}): the conduit sits, waterlogged, in a 3x3x3 of
 * water, and frame blocks count only on three perpendicular 5x5 rings through it, 42 positions. Any
 * 16 of them activate it (no ring needs closing); every 7 blocks add 16 blocks of range ({@code
 * activeSize / 7 * 16}); the full frame also attacks hostile mobs close by.
 */
public final class ConduitFrame {
  /** Frame blocks needed to activate ({@code MIN_ACTIVE_SIZE}). */
  public static final int MIN_ACTIVE_BLOCKS = 16;

  /** Frame blocks of a full frame, which also attacks ({@code MIN_KILL_SIZE}). */
  public static final int FULL_FRAME_BLOCKS = 42;

  /** Distance at which a full frame attacks hostile mobs ({@code KILL_RANGE}). */
  public static final int ATTACK_RANGE = 8;

  /** Rings of the frame, built one after another in {@link #onRings}. */
  public static final int RINGS = 3;

  /** Side of each ring around the conduit. */
  public static final int RING_SIDE = 5;

  private static final int BLOCKS_PER_STEP = 7;
  private static final int RANGE_PER_STEP = 16;

  /** Distance from the conduit to its rings. */
  private static final int RING_RADIUS = RING_SIDE / 2;

  /** The conduit sits in the middle of a 3x3x3 of water: this far from it each way. */
  private static final int WATER_RADIUS = 1;

  /** Where a frame block counts, from the conduit (x east, y up, z south). */
  public record FramePosition(int x, int y, int z) {}

  private static final List<FramePosition> POSITIONS = interleavedRings();

  private ConduitFrame() {}

  /**
   * Whether a block at this offset from the conduit counts towards its frame, as {@code
   * ConduitBlockEntity.updateShape} tests it: outside the 3x3x3 of water around the conduit, and on
   * the edge of one of the three 5x5 squares through the conduit (planes x = 0, y = 0, z = 0).
   */
  public static boolean counts(int x, int y, int z) {
    int ax = Math.abs(x);
    int ay = Math.abs(y);
    int az = Math.abs(z);
    if (ax > RING_RADIUS || ay > RING_RADIUS || az > RING_RADIUS) {
      return false;
    }
    boolean outsideWater = ax > WATER_RADIUS || ay > WATER_RADIUS || az > WATER_RADIUS;
    return outsideWater
        && (x == 0 && (ay == RING_RADIUS || az == RING_RADIUS)
            || y == 0 && (ax == RING_RADIUS || az == RING_RADIUS)
            || z == 0 && (ax == RING_RADIUS || ay == RING_RADIUS));
  }

  /**
   * All {@value #FULL_FRAME_BLOCKS} positions, taking turns between the three rings (each walked
   * round its edge), so the first {@code n} show a partial frame spread over every ring: any of
   * them count, no ring needs closing.
   */
  public static List<FramePosition> positions() {
    return POSITIONS;
  }

  private static List<FramePosition> interleavedRings() {
    Set<FramePosition> positions = new LinkedHashSet<>();
    for (int[] point : squareEdge()) {
      positions.add(new FramePosition(0, point[0], point[1]));
      positions.add(new FramePosition(point[0], 0, point[1]));
      positions.add(new FramePosition(point[0], point[1], 0));
    }
    return List.copyOf(positions);
  }

  /** The 16 cells on the edge of a 5x5 square centred on 0, walked round from one corner. */
  private static List<int[]> squareEdge() {
    List<int[]> edge = new ArrayList<>();
    for (int u = -RING_RADIUS; u < RING_RADIUS; u++) {
      edge.add(new int[] {u, -RING_RADIUS});
    }
    for (int v = -RING_RADIUS; v < RING_RADIUS; v++) {
      edge.add(new int[] {RING_RADIUS, v});
    }
    for (int u = RING_RADIUS; u > -RING_RADIUS; u--) {
      edge.add(new int[] {u, RING_RADIUS});
    }
    for (int v = RING_RADIUS; v > -RING_RADIUS; v--) {
      edge.add(new int[] {-RING_RADIUS, v});
    }
    return edge;
  }

  /**
   * Whether a frame block at this offset lies on one of the first {@code rings} rings in building
   * order ({@link #RINGS}): the flat ring (y = 0), then the upright one facing east-west (x = 0),
   * then the one facing north-south (z = 0).
   */
  public static boolean onRings(int rings, int x, int y, int z) {
    if (rings < 0 || rings > RINGS) {
      throw new IllegalArgumentException("rings " + rings);
    }
    if (!counts(x, y, z)) {
      return false;
    }
    return rings >= 1 && y == 0 && onEdge(x, z)
        || rings >= 2 && x == 0 && onEdge(y, z)
        || rings >= RINGS && z == 0 && onEdge(x, y);
  }

  /** Frame blocks of the first {@code rings} rings ({@link #onRings}): 16, 30, 42. */
  public static int blocksOnRings(int rings) {
    int blocks = 0;
    for (int x = -RING_RADIUS; x <= RING_RADIUS; x++) {
      for (int y = -RING_RADIUS; y <= RING_RADIUS; y++) {
        for (int z = -RING_RADIUS; z <= RING_RADIUS; z++) {
          if (onRings(rings, x, y, z)) {
            blocks++;
          }
        }
      }
    }
    return blocks;
  }

  /** Whether ({@code u}, {@code v}) is on the edge of a ring's 5x5 square. */
  private static boolean onEdge(int u, int v) {
    return Math.abs(u) == RING_RADIUS || Math.abs(v) == RING_RADIUS;
  }

  /** Range of Conduit Power with {@code frameBlocks} frame blocks; 0 when inactive. */
  public static int range(int frameBlocks) {
    if (frameBlocks < MIN_ACTIVE_BLOCKS) {
      return 0;
    }
    return Math.min(frameBlocks, FULL_FRAME_BLOCKS) / BLOCKS_PER_STEP * RANGE_PER_STEP;
  }

  /** Frame sizes at which the range grows, from activation to the full frame. */
  public static List<Integer> rangeSteps() {
    List<Integer> steps = new ArrayList<>();
    steps.add(MIN_ACTIVE_BLOCKS);
    int first = (MIN_ACTIVE_BLOCKS / BLOCKS_PER_STEP + 1) * BLOCKS_PER_STEP;
    for (int blocks = first; blocks <= FULL_FRAME_BLOCKS; blocks += BLOCKS_PER_STEP) {
      steps.add(blocks);
    }
    return List.copyOf(steps);
  }
}
