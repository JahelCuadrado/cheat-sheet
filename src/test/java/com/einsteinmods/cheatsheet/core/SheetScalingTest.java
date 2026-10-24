package com.einsteinmods.cheatsheet.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SheetScalingTest {
  private static final int FULL_HD_WIDTH = 1920;
  private static final int FULL_HD_HEIGHT = 1080;

  @Test
  void autoOnFullHdIsTwoToFour() {
    assertEquals(2, SheetScaling.minAutoScale(FULL_HD_WIDTH, FULL_HD_HEIGHT));
    assertEquals(4, SheetScaling.maxScale(FULL_HD_WIDTH, FULL_HD_HEIGHT));
  }

  @Test
  void autoTakesTheLargestScaleThatFits() {
    // A tab that fits a canvas up to 640x360 (scale 3 on 1080p).
    int scale =
        SheetScaling.choose(
            SheetScale.AUTO, FULL_HD_WIDTH, FULL_HD_HEIGHT, candidate -> candidate <= 3);
    assertEquals(3, scale);
  }

  @Test
  void autoTriesFromLargestDownAndNeverBelowTheReference() {
    List<Integer> tried = new ArrayList<>();
    int scale =
        SheetScaling.choose(
            SheetScale.AUTO,
            FULL_HD_WIDTH,
            FULL_HD_HEIGHT,
            candidate -> {
              tried.add(candidate);
              return false;
            });
    assertEquals(2, scale, "nothing fits: the 960x540 canvas, scrolled");
    assertEquals(List.of(4, 3), tried, "the floor needs no test");
  }

  @Test
  void smallWindowsDrawAtOne() {
    assertEquals(1, SheetScaling.minAutoScale(960, 540));
    assertEquals(1, SheetScaling.minAutoScale(1280, 720), "1.33 rounds to 1");
    assertEquals(2, SheetScaling.minAutoScale(1600, 900), "1.67 rounds to 2");
    assertEquals(3, SheetScaling.minAutoScale(2560, 1440), "2.67 rounds to 3");
    assertEquals(1, SheetScaling.choose(SheetScale.AUTO, 640, 480, candidate -> false));
    assertEquals(2, SheetScaling.choose(SheetScale.AUTO, 640, 480, candidate -> true));
    assertEquals(1, SheetScaling.maxScale(300, 200), "never below one");
  }

  @Test
  void fixedScalesAreCappedToTheWindow() {
    assertEquals(
        4, SheetScaling.choose(SheetScale.FOUR, FULL_HD_WIDTH, FULL_HD_HEIGHT, s -> false));
    assertEquals(2, SheetScaling.choose(SheetScale.FOUR, 960, 540, candidate -> true));
    assertEquals(1, SheetScaling.choose(SheetScale.ONE, FULL_HD_WIDTH, FULL_HD_HEIGHT, s -> true));
  }

  @Test
  void windowPixelsConvertToWholeSheetPixels() {
    assertEquals(640, SheetScaling.toSheet(FULL_HD_WIDTH, 3));
  }

  @Test
  void savedIdsRoundTripAndUnknownIsAuto() {
    for (SheetScale scale : SheetScale.values()) {
      assertEquals(scale, SheetScale.fromId(scale.id()));
    }
    assertEquals(SheetScale.AUTO, SheetScale.fromId("7"));
  }
}
