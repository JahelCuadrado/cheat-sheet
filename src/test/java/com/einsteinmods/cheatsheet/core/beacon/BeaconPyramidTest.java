package com.einsteinmods.cheatsheet.core.beacon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class BeaconPyramidTest {
  @Test
  void pyramidsNeedTheWellKnownBlockCounts() {
    assertEquals(9, BeaconPyramid.totalBlocks(1));
    assertEquals(34, BeaconPyramid.totalBlocks(2));
    assertEquals(83, BeaconPyramid.totalBlocks(3));
    assertEquals(164, BeaconPyramid.totalBlocks(4));
    assertEquals(81, BeaconPyramid.layerBlocks(4));
  }

  @Test
  void rangeGrowsTenBlocksPerLevel() {
    assertEquals(20, BeaconPyramid.range(1));
    assertEquals(50, BeaconPyramid.range(4));
    assertEquals(11, BeaconPyramid.effectSeconds(1));
    assertEquals(17, BeaconPyramid.effectSeconds(4));
  }

  @Test
  void levelsOutsideTheBeaconAreRejected() {
    assertThrows(IllegalArgumentException.class, () -> BeaconPyramid.range(0));
    assertThrows(IllegalArgumentException.class, () -> BeaconPyramid.layerSide(5));
  }

  @Test
  void conduitRangeGrowsEverySevenFrameBlocks() {
    assertEquals(List.of(16, 21, 28, 35, 42), ConduitFrame.rangeSteps());
    assertEquals(0, ConduitFrame.range(15));
    assertEquals(32, ConduitFrame.range(16));
    assertEquals(32, ConduitFrame.range(20));
    assertEquals(48, ConduitFrame.range(21));
    assertEquals(96, ConduitFrame.range(42));
  }
}
