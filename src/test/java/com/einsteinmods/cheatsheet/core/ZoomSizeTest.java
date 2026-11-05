package com.einsteinmods.cheatsheet.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ZoomSizeTest {
  private static final int WINDOW_WIDTH = 1920;
  private static final int WINDOW_HEIGHT = 1080;
  private static final int SHEET_SCALE = 3;

  @Test
  void aTallCardShrinksToTheSheetScaleWithoutScrolling() {
    assertEquals(
        SHEET_SCALE, ZoomSize.LARGE.scale(SHEET_SCALE, WINDOW_WIDTH, WINDOW_HEIGHT, 406, 1000));
  }

  @Test
  void aScrollingCardKeepsTheScaleOfItsWidth() {
    assertEquals(4, ZoomSize.LARGE.scrollingScale(SHEET_SCALE, WINDOW_WIDTH, 406));
  }

  @Test
  void scrollingScaleKeepsTheSizesFactorAndTheSheetScale() {
    assertEquals(7, ZoomSize.LARGE.scrollingScale(SHEET_SCALE, WINDOW_WIDTH, 100));
    assertEquals(SHEET_SCALE, ZoomSize.LARGE.scrollingScale(SHEET_SCALE, WINDOW_WIDTH, 1000));
    assertThrows(IllegalArgumentException.class, () -> ZoomSize.LARGE.scrollingScale(0, 100, 100));
  }

  @Test
  void viewportIsTheWindowsShareOrTheWholeCard() {
    assertEquals(243, ZoomSize.LARGE.scrollingViewport(WINDOW_HEIGHT, 4, 1000));
    assertEquals(243, ZoomSize.SMALL.scrollingViewport(WINDOW_HEIGHT, 4, 1000));
    assertEquals(200, ZoomSize.LARGE.scrollingViewport(WINDOW_HEIGHT, 4, 200));
    assertEquals(1, ZoomSize.LARGE.scrollingViewport(1, 4, 200));
  }
}
