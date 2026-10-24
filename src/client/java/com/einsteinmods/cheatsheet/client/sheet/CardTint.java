package com.einsteinmods.cheatsheet.client.sheet;

import net.minecraft.util.ARGB;

/**
 * Card colours: a translucent fill, a lighter one-pixel border and the title colour, all derived
 * from one accent so every card of every tab follows the same recipe. Use a named tint, or {@link
 * #of} for a card coloured like what it shows (a dye).
 */
public final class CardTint {
  public static final CardTint STONE = new CardTint(0xFF8A8F9C);
  public static final CardTint GOLD = new CardTint(0xFFE0B43C);
  public static final CardTint RED = new CardTint(0xFFD9493C);
  public static final CardTint ORANGE = new CardTint(0xFFE07A3A);
  public static final CardTint GREEN = new CardTint(0xFF4CC25A);
  public static final CardTint AQUA = new CardTint(0xFF46C8D2);
  public static final CardTint BLUE = new CardTint(0xFF4F7BE0);
  public static final CardTint PURPLE = new CardTint(0xFFA15CE0);
  public static final CardTint PINK = new CardTint(0xFFE068B4);
  public static final CardTint NETHER = new CardTint(0xFFB0342C);

  private static final int DARK_BASE = 0xFF14141A;
  private static final int WHITE = 0xFFFFFFFF;
  private static final float FILL_ALPHA = 0.82F;

  /** How much of the accent mixes into the dark fill, the border and (towards white) the title. */
  private static final float FILL_ACCENT = 0.18F;

  private static final float BORDER_ACCENT = 0.55F;
  private static final float TITLE_LIGHTEN = 0.35F;

  private final int accent;
  private final int fill;
  private final int border;
  private final int title;

  private CardTint(int accent) {
    this.accent = ARGB.opaque(accent);
    this.fill = ARGB.color(FILL_ALPHA, ARGB.srgbLerp(FILL_ACCENT, DARK_BASE, this.accent));
    this.border = ARGB.srgbLerp(BORDER_ACCENT, DARK_BASE, this.accent);
    this.title = ARGB.srgbLerp(TITLE_LIGHTEN, this.accent, WHITE);
  }

  /** A tint from any accent colour (RGB; alpha is ignored). Build-time only: it allocates. */
  public static CardTint of(int accent) {
    return new CardTint(accent);
  }

  /** The pure accent colour, for chart marks matching the card. */
  public int accent() {
    return accent;
  }

  public int fill() {
    return fill;
  }

  public int border() {
    return border;
  }

  public int title() {
    return title;
  }
}
