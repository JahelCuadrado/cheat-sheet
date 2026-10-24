package com.einsteinmods.cheatsheet.client.sheet;

import com.einsteinmods.cheatsheet.core.sections.BodyShape;
import com.einsteinmods.cheatsheet.core.sections.GridGeometry;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * Tiles all the same size (the largest tile's), each centred in its slot, as many per row as the
 * sheet width allows, read left to right then down: dyes, discs. Drawing skips rows outside the
 * scissor, uses one GUI stratum per row and allocates nothing.
 */
public final class TileGrid implements SheetBody {
  private final SheetElement[] tiles;
  private final int[] offsetXs;
  private final int[] offsetYs;
  private final GridGeometry geometry;

  public TileGrid(List<? extends SheetElement> tiles, int gap) {
    this(tiles, gap, false);
  }

  /**
   * With {@code fullRows}, rows fill up before the next starts (see {@link
   * GridGeometry#fullRows()}): equal tiles of several grids of a sheet line up in columns.
   */
  public TileGrid(List<? extends SheetElement> tiles, int gap, boolean fullRows) {
    if (tiles.isEmpty()) {
      throw new IllegalArgumentException("a grid needs tiles");
    }
    this.tiles = tiles.toArray(new SheetElement[0]);
    int width = 1;
    int height = 1;
    for (SheetElement tile : this.tiles) {
      width = Math.max(width, tile.minWidth());
      height = Math.max(height, tile.height());
    }
    this.offsetXs = new int[this.tiles.length];
    this.offsetYs = new int[this.tiles.length];
    for (int index = 0; index < this.tiles.length; index++) {
      offsetXs[index] = (width - this.tiles[index].minWidth()) / 2;
      offsetYs[index] = (height - this.tiles[index].height()) / 2;
    }
    this.geometry = new GridGeometry(width, height, this.tiles.length, gap, fullRows);
  }

  @Override
  public GridGeometry geometry() {
    return geometry;
  }

  @Override
  public void extract(GuiGraphicsExtractor graphics, int x, int y, BodyShape shape) {
    int columns = shape.split();
    for (int index = 0; index < tiles.length; index++) {
      int top = y + geometry.tileTop(index, columns);
      if (index % columns == 0) {
        if (!Scissor.touches(graphics, x, top, geometry.tileHeight())) {
          index += columns - 1;
          continue;
        }
        // A stratum per row: vanilla's placement scan stays within one row.
        graphics.nextStratum();
      }
      SheetElement tile = tiles[index];
      tile.extract(
          graphics,
          x + geometry.tileLeft(index, columns) + offsetXs[index],
          top + offsetYs[index],
          tile.minWidth());
    }
  }

  @Override
  public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, BodyShape shape) {
    int columns = shape.split();
    int pitchX = geometry.tileWidth() + geometry.gap();
    int pitchY = geometry.tileHeight() + geometry.gap();
    if (localX < 0 || localY < 0) {
      return null;
    }
    int column = localX / pitchX;
    int index = (localY / pitchY) * columns + column;
    if (column >= columns || index >= tiles.length) {
      return null;
    }
    SheetElement tile = tiles[index];
    int tileX = localX - column * pitchX - offsetXs[index];
    int tileY = localY - (localY / pitchY) * pitchY - offsetYs[index];
    if (tileX < 0 || tileX >= tile.minWidth() || tileY < 0 || tileY >= tile.height()) {
      return null;
    }
    return tile.tooltipAt(tileX, tileY, tile.minWidth());
  }

  /** A tile is a card: the one under the point, at its own size inside its slot. */
  @Override
  public boolean locateCard(int localX, int localY, BodyShape shape, CardSpot spot) {
    int columns = shape.split();
    int pitchX = geometry.tileWidth() + geometry.gap();
    int pitchY = geometry.tileHeight() + geometry.gap();
    if (localX < 0 || localY < 0) {
      return false;
    }
    int column = localX / pitchX;
    int index = (localY / pitchY) * columns + column;
    if (column >= columns || index >= tiles.length) {
      return false;
    }
    SheetElement tile = tiles[index];
    int left = geometry.tileLeft(index, columns) + offsetXs[index];
    int top = geometry.tileTop(index, columns) + offsetYs[index];
    if (localX < left
        || localX >= left + tile.minWidth()
        || localY < top
        || localY >= top + tile.height()) {
      return false;
    }
    spot.set(index, left, top, tile.minWidth(), tile.height());
    return true;
  }

  /** The tile's own zoom view, or the tile itself. */
  @Override
  public ZoomCard zoomCard(int card, BodyShape shape) {
    ZoomCard view = tiles[card].zoomView();
    return view != null ? view : new ElementCard(tiles[card]);
  }
}
