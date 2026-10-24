package com.einsteinmods.cheatsheet.core.discs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SongLengthTest {
  @Test
  void formatsMinutesAndPaddedSeconds() {
    assertEquals("2:58", SongLength.format(178.0F));
    assertEquals("1:11", SongLength.format(71.0F));
    assertEquals("5:45", SongLength.format(345.0F));
    assertEquals("0:05", SongLength.format(5.0F));
    assertEquals("0:00", SongLength.format(0.0F));
  }

  @Test
  void roundsToTheNearestSecond() {
    assertEquals("1:00", SongLength.format(59.6F));
    assertEquals("0:59", SongLength.format(59.4F));
  }

  @Test
  void rejectsNegativeOrNaN() {
    assertThrows(IllegalArgumentException.class, () -> SongLength.format(-1.0F));
    assertThrows(IllegalArgumentException.class, () -> SongLength.format(Float.NaN));
  }
}
