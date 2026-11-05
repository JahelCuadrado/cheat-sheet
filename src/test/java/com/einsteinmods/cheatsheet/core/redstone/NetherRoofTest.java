package com.einsteinmods.cheatsheet.core.redstone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NetherRoofTest {
  @Test
  void ceilingFadesFiveLayersDown() {
    assertEquals(100, NetherRoof.bedrockPercent(127));
    assertEquals(80, NetherRoof.bedrockPercent(126));
    assertEquals(20, NetherRoof.bedrockPercent(123));
    assertEquals(0, NetherRoof.bedrockPercent(122));
    assertEquals(125, NetherRoof.ladderFeetY(127));
  }

  @Test
  void hasteTwoAndEfficiencyFiveMineAPistonAtOnce() {
    assertTrue(InstantMining.ROWS.get(0).instant());
    assertTrue(InstantMining.ROWS.get(2).instant());
    assertFalse(InstantMining.ROWS.get(3).instant());
    assertFalse(InstantMining.ROWS.get(4).instant());
  }
}
