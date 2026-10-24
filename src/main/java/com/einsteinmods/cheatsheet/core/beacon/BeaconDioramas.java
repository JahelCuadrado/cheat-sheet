package com.einsteinmods.cheatsheet.core.beacon;

import com.einsteinmods.cheatsheet.core.pictures.Diorama;
import java.util.List;

/**
 * Beacon pyramids and conduit frames in miniature. Every pyramid sits in the box of the biggest one
 * and every frame in the box of the full frame, so pictures framed on that box keep one scale: a
 * smaller pyramid or frame looks smaller.
 */
public final class BeaconDioramas {
  /** Side of the biggest pyramid's base, in blocks. */
  public static final int PYRAMID_SIDE = BeaconPyramid.layerSide(BeaconPyramid.MAX_LEVELS);

  /** Height of the biggest pyramid with its beacon, in blocks. */
  public static final int PYRAMID_HEIGHT = BeaconPyramid.MAX_LEVELS + 1;

  /** Side of the cube holding a conduit's frame, in blocks. */
  public static final int FRAME_SIDE = ConduitFrame.RING_SIDE;

  /** Layers from the top, in the base blocks players use most. */
  private static final List<String> LAYER_BLOCKS =
      List.of("diamond_block", "emerald_block", "gold_block", "iron_block");

  private static final String BEACON = "beacon";

  /**
   * Each ring in its own frame block (the flat one dark, so it stands out from the upright ones);
   * the six tips, shared by two rings, are sea lanterns.
   */
  private static final String X_RING = "prismarine";

  private static final String Y_RING = "dark_prismarine";
  private static final String Z_RING = "prismarine_bricks";
  private static final String TIP = "sea_lantern";

  /** Stands in for the water around the conduit in pictures. */
  private static final String WATER = "light_blue_stained_glass";

  /** The water reaches this far from the conduit each way. */
  private static final int WATER_RADIUS = 1;

  private BeaconDioramas() {}

  /** A pyramid of {@code levels} levels with the beacon on top, centred in the biggest's box. */
  public static Diorama pyramid(int levels) {
    int centre = PYRAMID_SIDE / 2;
    Diorama.Builder pyramid = Diorama.builder(PYRAMID_SIDE, PYRAMID_HEIGHT, PYRAMID_SIDE);
    for (int layer = 1; layer <= levels; layer++) {
      int half = BeaconPyramid.layerSide(layer) / 2;
      int y = levels - layer;
      pyramid.box(
          LAYER_BLOCKS.get((layer - 1) % LAYER_BLOCKS.size()),
          centre - half,
          y,
          centre - half,
          centre + half,
          y,
          centre + half);
    }
    return pyramid.block(BEACON, centre, levels, centre).build();
  }

  /**
   * The first {@code blocks} frame positions ({@link ConduitFrame#positions()}), around an empty
   * middle where the conduit goes (the conduit and its water have no block model to draw).
   */
  public static Diorama conduitFrame(int blocks) {
    List<ConduitFrame.FramePosition> positions = ConduitFrame.positions();
    if (blocks < 0 || blocks > positions.size()) {
      throw new IllegalArgumentException("frame of " + blocks + " blocks");
    }
    int centre = FRAME_SIDE / 2;
    Diorama.Builder frame = Diorama.builder(FRAME_SIDE, FRAME_SIDE, FRAME_SIDE);
    for (ConduitFrame.FramePosition position : positions.subList(0, blocks)) {
      frame.block(
          frameBlock(position),
          centre + position.x(),
          centre + position.y(),
          centre + position.z());
    }
    return frame.build();
  }

  /**
   * A conduit's build after {@code rings} rings ({@link ConduitFrame#onRings}): the 3x3x3 of water
   * (drawn as {@link #WATER}: water has no block model to draw) with an empty middle where the
   * conduit goes, and the frame blocks of those rings.
   */
  public static Diorama conduitRings(int rings) {
    int centre = FRAME_SIDE / 2;
    Diorama.Builder build = Diorama.builder(FRAME_SIDE, FRAME_SIDE, FRAME_SIDE);
    build.box(
        WATER,
        centre - WATER_RADIUS,
        centre - WATER_RADIUS,
        centre - WATER_RADIUS,
        centre + WATER_RADIUS,
        centre + WATER_RADIUS,
        centre + WATER_RADIUS);
    build.clear(centre, centre, centre);
    for (ConduitFrame.FramePosition position : ConduitFrame.positions()) {
      if (ConduitFrame.onRings(rings, position.x(), position.y(), position.z())) {
        build.block(
            frameBlock(position),
            centre + position.x(),
            centre + position.y(),
            centre + position.z());
      }
    }
    return build.build();
  }

  private static String frameBlock(ConduitFrame.FramePosition position) {
    int zeros =
        (position.x() == 0 ? 1 : 0) + (position.y() == 0 ? 1 : 0) + (position.z() == 0 ? 1 : 0);
    if (zeros > 1) {
      return TIP;
    }
    if (position.x() == 0) {
      return X_RING;
    }
    return position.y() == 0 ? Y_RING : Z_RING;
  }
}
