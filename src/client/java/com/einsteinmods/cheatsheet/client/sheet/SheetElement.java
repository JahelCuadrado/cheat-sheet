package com.einsteinmods.cheatsheet.client.sheet;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * One piece of a card: an icon, a label, a chart. Built once with everything it shows already
 * resolved (items, wrapped text, chart segments), so drawing does no lookups and allocates nothing
 * of its own. Sizes are fixed at build time; only the width may grow when the element stretches.
 *
 * <p>Tabs may add their own elements (a chart of their own) by implementing this interface.
 */
public interface SheetElement {
  /** Narrowest width the element can be drawn at. */
  int minWidth();

  /** Height; never depends on the width, so a sheet lays out in one pass. */
  int height();

  /**
   * Width taken when {@code available} pixels are offered (at least {@link #minWidth()}): the
   * natural width, or more for elements that stretch. The card centres the element in what is left.
   */
  default int width(int available) {
    return minWidth();
  }

  /** Adds the element's draw calls with its top-left corner at ({@code x}, {@code y}). */
  void extract(GuiGraphicsExtractor graphics, int x, int y, int width);

  /**
   * Tooltip for the mouse at ({@code localX}, {@code localY}) from the element's top-left corner
   * (always inside its bounds), or null for none. Returns lines built at build time.
   */
  default @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
    return null;
  }

  /**
   * What the zoom shows when the player clicks a card made of this element (a tile): a view laid
   * out for the zoom, or null (the default) for the element itself, bigger. Called once per click;
   * a view that builds anything keeps it for the next click.
   */
  default @Nullable ZoomCard zoomView() {
    return null;
  }
}
