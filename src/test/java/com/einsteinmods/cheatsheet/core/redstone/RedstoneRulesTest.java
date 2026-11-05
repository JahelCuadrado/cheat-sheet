package com.einsteinmods.cheatsheet.core.redstone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RedstoneRulesTest {
  @Test
  void secondsPrintWithTheDecimalsTheyNeed() {
    assertEquals("0.05", RedstoneRules.seconds(1));
    assertEquals("0.1", RedstoneRules.seconds(2));
    assertEquals("0.4", RedstoneRules.seconds(8));
    assertEquals("1", RedstoneRules.seconds(20));
    assertEquals("1.5", RedstoneRules.seconds(30));
    assertEquals("8", RedstoneRules.seconds(160));
  }

  @Test
  void repeaterDelaysAreTwoGameTicksEach() {
    assertEquals(2, RedstoneRules.repeaterTicks(RedstoneRules.REPEATER_MIN_DELAY));
    assertEquals(8, RedstoneRules.repeaterTicks(RedstoneRules.REPEATER_MAX_DELAY));
    assertThrows(IllegalArgumentException.class, () -> RedstoneRules.repeaterTicks(0));
  }

  @Test
  void repeaterClockRunsOnceRoundItsLoop() {
    assertEquals(32, RedstoneRules.repeaterClockPeriodTicks());
    assertEquals(12, RedstoneRules.repeaterClockOffTicks());
  }

  @Test
  void eachHopperClockItemAddsTwoHopperMoves() {
    assertEquals(52, RedstoneRules.hopperClockCycleTicks(4));
    assertEquals(116, RedstoneRules.hopperClockCycleTicks(8));
    assertEquals("7.4", RedstoneRules.seconds(RedstoneRules.hopperClockCycleTicks(10)));
  }

  @Test
  void lightPlateCountsOnePerEntityAndHeavyOnePerTen() {
    for (int entities = 1; entities <= RedstoneRules.LIGHT_PLATE_MAX_ENTITIES; entities++) {
      assertEquals(
          entities,
          RedstoneRules.weightedPlateSignal(entities, RedstoneRules.LIGHT_PLATE_MAX_ENTITIES));
    }
    assertEquals(15, RedstoneRules.weightedPlateSignal(40, RedstoneRules.LIGHT_PLATE_MAX_ENTITIES));
    assertEquals(1, RedstoneRules.weightedPlateSignal(10, RedstoneRules.HEAVY_PLATE_MAX_ENTITIES));
    assertEquals(2, RedstoneRules.weightedPlateSignal(11, RedstoneRules.HEAVY_PLATE_MAX_ENTITIES));
    assertEquals(
        15, RedstoneRules.weightedPlateSignal(150, RedstoneRules.HEAVY_PLATE_MAX_ENTITIES));
    assertEquals(
        141, RedstoneRules.entitiesForPlateSignal(15, RedstoneRules.HEAVY_PLATE_MAX_ENTITIES));
  }
}
