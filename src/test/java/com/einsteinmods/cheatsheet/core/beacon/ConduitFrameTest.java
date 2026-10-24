package com.einsteinmods.cheatsheet.core.beacon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.einsteinmods.cheatsheet.core.pictures.Diorama;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ConduitFrameTest {
  private static final int REACH = 3;

  @Test
  void fortyTwoPositionsOnThreeRingsCount() {
    int counted = 0;
    for (int x = -REACH; x <= REACH; x++) {
      for (int y = -REACH; y <= REACH; y++) {
        for (int z = -REACH; z <= REACH; z++) {
          if (ConduitFrame.counts(x, y, z)) {
            counted++;
          }
        }
      }
    }
    assertEquals(ConduitFrame.FULL_FRAME_BLOCKS, counted);
    // The water cube and the corners of the 5x5x5 never count.
    assertFalse(ConduitFrame.counts(1, 1, 1));
    assertFalse(ConduitFrame.counts(2, 2, 2));
    assertFalse(ConduitFrame.counts(2, 2, 1));
    assertTrue(ConduitFrame.counts(0, 2, 2));
    assertTrue(ConduitFrame.counts(-2, 0, 1));
  }

  @Test
  void positionsAreEveryCountedCellOnceSpreadOverTheRings() {
    List<ConduitFrame.FramePosition> positions = ConduitFrame.positions();
    assertEquals(ConduitFrame.FULL_FRAME_BLOCKS, positions.size());
    assertEquals(positions.size(), new HashSet<>(positions).size());
    for (ConduitFrame.FramePosition position : positions) {
      assertTrue(ConduitFrame.counts(position.x(), position.y(), position.z()), position::toString);
    }
    Set<String> rings = new HashSet<>();
    for (ConduitFrame.FramePosition position :
        positions.subList(0, ConduitFrame.MIN_ACTIVE_BLOCKS)) {
      if (position.x() == 0) {
        rings.add("x");
      }
      if (position.y() == 0) {
        rings.add("y");
      }
      if (position.z() == 0) {
        rings.add("z");
      }
    }
    assertEquals(Set.of("x", "y", "z"), rings);
  }

  @Test
  void dioramasKeepOneBoxAndGrowWithTheirBlocks() {
    Diorama full = BeaconDioramas.conduitFrame(ConduitFrame.FULL_FRAME_BLOCKS);
    assertEquals(ConduitFrame.FULL_FRAME_BLOCKS, full.blocks().size());
    assertEquals(
        ConduitFrame.MIN_ACTIVE_BLOCKS,
        BeaconDioramas.conduitFrame(ConduitFrame.MIN_ACTIVE_BLOCKS).blocks().size());
    assertEquals(BeaconDioramas.FRAME_SIDE, full.width());
    assertEquals(
        BeaconPyramid.totalBlocks(BeaconPyramid.MAX_LEVELS) + 1,
        BeaconDioramas.pyramid(BeaconPyramid.MAX_LEVELS).blocks().size());
    Diorama smallest = BeaconDioramas.pyramid(1);
    assertEquals(BeaconPyramid.totalBlocks(1) + 1, smallest.blocks().size());
    assertEquals(BeaconDioramas.PYRAMID_SIDE, smallest.width());
  }

  @Test
  void ringsAreBuiltFlatThenUprightAndEndAtTheFullFrame() {
    assertEquals(0, ConduitFrame.blocksOnRings(0));
    assertEquals(16, ConduitFrame.blocksOnRings(1));
    assertEquals(30, ConduitFrame.blocksOnRings(2));
    assertEquals(ConduitFrame.FULL_FRAME_BLOCKS, ConduitFrame.blocksOnRings(ConduitFrame.RINGS));
    // The first ring alone activates the conduit; each ring widens its range.
    assertEquals(32, ConduitFrame.range(ConduitFrame.blocksOnRings(1)));
    assertEquals(64, ConduitFrame.range(ConduitFrame.blocksOnRings(2)));
    assertEquals(96, ConduitFrame.range(ConduitFrame.blocksOnRings(ConduitFrame.RINGS)));
    assertTrue(ConduitFrame.onRings(1, 2, 0, 1));
    assertFalse(ConduitFrame.onRings(1, 0, 2, 1));
    assertTrue(ConduitFrame.onRings(2, 0, 2, 1));
    assertFalse(ConduitFrame.onRings(2, 1, 2, 0));
    assertTrue(ConduitFrame.onRings(ConduitFrame.RINGS, 1, 2, 0));
  }

  @Test
  void ringDioramasHoldTheWaterAndTheirRings() {
    int water = 3 * 3 * 3 - 1;
    for (int rings = 0; rings <= ConduitFrame.RINGS; rings++) {
      Diorama build = BeaconDioramas.conduitRings(rings);
      assertEquals(water + ConduitFrame.blocksOnRings(rings), build.blocks().size());
      assertEquals(BeaconDioramas.FRAME_SIDE, build.width());
    }
  }
}
