package com.einsteinmods.cheatsheet.client.sheet;

import com.einsteinmods.cheatsheet.core.ZoomSize;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * One card of a sheet as the zoom sees it, whatever the layout: a masonry {@link Card}, one tile of
 * a {@link TileGrid}, one row of a {@link Table} (with its header), or a view a tab lays out for
 * the zoom instead ({@link SheetElement#zoomView()}, {@link Table#withRowZooms}). Created when the
 * player clicks the card, from objects the sheet already holds; drawn in card pixels at its natural
 * size, the zoom scales it. Its details are its tooltips, shown under the mouse in the zoom too.
 */
public interface ZoomCard {
  /** Width in sheet pixels. */
  int width();

  /** Height in sheet pixels. */
  int height();

  /** Adds the card's draw calls with its top-left corner at ({@code x}, {@code y}). */
  void extract(GuiGraphicsExtractor graphics, int x, int y);

  /** Tooltip at ({@code localX}, {@code localY}) from the card's corner, or null for none. */
  @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY);

  /** How big the zoom gets, or null (the default) for its tab's size. */
  default @Nullable ZoomSize size() {
    return null;
  }
}
