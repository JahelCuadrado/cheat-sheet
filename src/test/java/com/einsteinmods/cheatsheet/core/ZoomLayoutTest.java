package com.einsteinmods.cheatsheet.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ZoomLayoutTest {
  /** A 1080p window, the sheet at 2x (canvas 960x540). */
  private static final int WINDOW_WIDTH = 1920;

  private static final int WINDOW_HEIGHT = 1080;
  private static final int SHEET_SCALE = 2;

  private static ZoomLayout layout(int scale, int cardWidth, int cardHeight, int x, int y) {
    return ZoomLayout.of(
        new ZoomLayout.Input(
            WINDOW_WIDTH, WINDOW_HEIGHT, SHEET_SCALE, scale, cardWidth, cardHeight, x, y));
  }

  @Test
  void dyeTileIsOneAndAHalfTimes() {
    assertEquals(3, ZoomSize.SMALL.scale(SHEET_SCALE, WINDOW_WIDTH, WINDOW_HEIGHT, 114, 195));
  }

  @Test
  void eachSizeCapsTheFactorOverTheSheet() {
    assertEquals(3, ZoomSize.SMALL.scale(SHEET_SCALE, WINDOW_WIDTH, WINDOW_HEIGHT, 40, 40));
    assertEquals(4, ZoomSize.MEDIUM.scale(SHEET_SCALE, WINDOW_WIDTH, WINDOW_HEIGHT, 40, 40));
    assertEquals(5, ZoomSize.LARGE.scale(SHEET_SCALE, WINDOW_WIDTH, WINDOW_HEIGHT, 40, 40));
    assertEquals(4, ZoomSize.SMALL.scale(3, WINDOW_WIDTH, WINDOW_HEIGHT, 40, 40));
    assertEquals(2, ZoomSize.SMALL.scale(1, WINDOW_WIDTH, WINDOW_HEIGHT, 40, 40));
  }

  @Test
  void aCardTooBigForItsShareKeepsTheSheetScale() {
    assertEquals(2, ZoomSize.MEDIUM.scale(SHEET_SCALE, WINDOW_WIDTH, WINDOW_HEIGHT, 900, 500));
  }

  @Test
  void bigCardsGetASmallerScaleToStayInTheirShare() {
    // 300 x 3 = 900 of 1080 is past three quarters: the card stays at the sheet scale.
    assertEquals(2, ZoomSize.MEDIUM.scale(SHEET_SCALE, WINDOW_WIDTH, WINDOW_HEIGHT, 200, 300));
    assertEquals(3, ZoomSize.LARGE.scale(SHEET_SCALE, WINDOW_WIDTH, WINDOW_HEIGHT, 200, 300));
  }

  @Test
  void panelIsCentredOnTheClickedCardAndTheCardInIt() {
    ZoomLayout zoom = layout(4, 100, 50, 960, 540);
    ZoomLayout.Rect panel = zoom.panel();
    assertEquals(100 * 4 / SHEET_SCALE + ZoomLayout.PADDING * 2, panel.width());
    assertEquals(480, panel.x() + panel.width() / 2);
    assertEquals(270, panel.y() + panel.height() / 2);
    assertEquals(panel.x() * SHEET_SCALE + ZoomLayout.PADDING * SHEET_SCALE, zoom.cardX());
    assertEquals(panel.y() * SHEET_SCALE + ZoomLayout.PADDING * SHEET_SCALE, zoom.cardY());
  }

  @Test
  void panelNearAnEdgeIsPushedInsideTheMargins() {
    ZoomLayout zoom = layout(4, 100, 50, 10, 1070);
    ZoomLayout.Rect panel = zoom.panel();
    assertEquals(ZoomLayout.MARGIN, panel.x());
    assertEquals(WINDOW_HEIGHT / SHEET_SCALE - ZoomLayout.MARGIN, panel.bottom());
  }

  @Test
  void oddCardWidthsStayCentredOnWholeWindowPixels() {
    ZoomLayout zoom = layout(3, 101, 33, 960, 540);
    ZoomLayout.Rect panel = zoom.panel();
    int left = zoom.cardX() - panel.x() * SHEET_SCALE;
    int right = panel.right() * SHEET_SCALE - (zoom.cardX() + 101 * 3);
    assertTrue(Math.abs(left - right) <= 1, left + " vs " + right);
  }

  @Test
  void panelWiderThanTheWindowIsCentred() {
    ZoomLayout zoom = layout(2, 1000, 50, 0, 540);
    assertEquals((WINDOW_WIDTH / SHEET_SCALE - zoom.panel().width()) / 2, zoom.panel().x());
  }

  @Test
  void rejectsEmptyCards() {
    assertThrows(IllegalArgumentException.class, () -> layout(2, 0, 10, 0, 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> ZoomSize.SMALL.scale(SHEET_SCALE, WINDOW_WIDTH, WINDOW_HEIGHT, 10, 0));
  }
}
