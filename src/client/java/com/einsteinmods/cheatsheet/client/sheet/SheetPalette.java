package com.einsteinmods.cheatsheet.client.sheet;

/** Colours shared by every sheet (ARGB), so all tabs look like one cheat sheet. */
public final class SheetPalette {
  /** Plain text on cards. */
  public static final int TEXT = 0xFFFFFFFF;

  /** Secondary text: captions, axis labels, notes. */
  public static final int MUTED_TEXT = 0xFFB4B4BE;

  /** Big numbers and other values the eye should land on. */
  public static final int HIGHLIGHT_TEXT = 0xFFFFE066;

  /** "You are here" markers (the player's height). */
  public static final int PLAYER_MARKER = 0xFFFFD23C;

  /** Chart backgrounds inside a card. */
  public static final int CHART_BACKGROUND = 0x66000000;

  /** Faint grid lines over charts. */
  public static final int GRID_LINE = 0x30FFFFFF;

  /** Tabs: idle, hovered and selected fill and border. */
  public static final int TAB_FILL = 0xA0101016;

  public static final int TAB_BORDER = 0xFF3C3C48;
  public static final int TAB_HOVER_FILL = 0xC0202030;
  public static final int TAB_HOVER_BORDER = 0xFF8A8A9A;
  public static final int TAB_SELECTED_FILL = 0xD0303044;
  public static final int TAB_SELECTED_BORDER = 0xFFFFFFFF;

  /** Scroll bar track and thumb. */
  public static final int SCROLL_BAR_TRACK = 0x60000000;

  public static final int SCROLL_BAR_THUMB = 0xC0C0C0CC;

  /** The card under the mouse: a faint wash and a light outline (cards can be zoomed). */
  public static final int CARD_HOVER_FILL = 0x20FFFFFF;

  public static final int CARD_HOVER_BORDER = 0x90FFFFFF;

  /** Behind a zoomed card: a veil over the (blurred) sheet, and the zoom's panel. */
  public static final int ZOOM_VEIL = 0x70000000;

  public static final int ZOOM_PANEL_FILL = 0xF0101016;
  public static final int ZOOM_PANEL_BORDER = 0xFF8A8A9A;

  private SheetPalette() {}
}
