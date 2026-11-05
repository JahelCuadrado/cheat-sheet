package com.einsteinmods.cheatsheet.core.redstone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SculkSensorsTest {
  @Test
  void everyFrequencyFromOneToFifteenOnceInOrder() {
    assertEquals(RedstoneRules.MAX_SIGNAL, SculkSensors.FREQUENCIES.size());
    Set<String> events = new HashSet<>();
    for (int index = 0; index < SculkSensors.FREQUENCIES.size(); index++) {
      SculkSensors.Frequency frequency = SculkSensors.FREQUENCIES.get(index);
      assertEquals(index + 1, frequency.frequency());
      assertFalse(frequency.gameEvents().isEmpty(), "frequency " + frequency.frequency());
      for (String event : frequency.gameEvents()) {
        assertTrue(events.add(event), event + " twice");
      }
    }
  }

  @Test
  void signalFadesWithDistanceButNeverBelowOne() {
    assertEquals(15, SculkSensors.signalAtDistance(0.0F, SculkSensors.RANGE));
    assertEquals(12, SculkSensors.signalAtDistance(2.0F, SculkSensors.RANGE));
    assertEquals(1, SculkSensors.signalAtDistance(8.0F, SculkSensors.RANGE));
    assertEquals(14, SculkSensors.signalAtDistance(2.0F, SculkSensors.CALIBRATED_RANGE));
    assertEquals(1, SculkSensors.signalAtDistance(16.0F, SculkSensors.CALIBRATED_RANGE));
  }
}
