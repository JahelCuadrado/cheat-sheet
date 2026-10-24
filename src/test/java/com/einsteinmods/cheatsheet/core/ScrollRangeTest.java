package com.einsteinmods.cheatsheet.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ScrollRangeTest {
  @Test
  void contentThatFitsDoesNotScroll() {
    assertEquals(0, ScrollRange.clamp(40, 100, 200));
    assertEquals(0, ScrollRange.maxOffset(100, 200));
  }

  @Test
  void offsetStaysBetweenTopAndBottom() {
    assertEquals(0, ScrollRange.clamp(-10, 300, 200));
    assertEquals(100, ScrollRange.clamp(500, 300, 200));
    assertEquals(60, ScrollRange.clamp(60, 300, 200));
  }
}
