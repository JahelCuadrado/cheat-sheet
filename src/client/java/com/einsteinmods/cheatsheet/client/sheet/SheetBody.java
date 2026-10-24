package com.einsteinmods.cheatsheet.client.sheet;

import com.einsteinmods.cheatsheet.core.sections.BodyShape;
import com.einsteinmods.cheatsheet.core.sections.SectionBody;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * The body of a {@link Section}: a {@link Table} or a {@link TileGrid}. Its geometry is fixed when
 * built; the layout picks one of its shapes for the sheet width, and drawing follows that shape.
 */
public interface SheetBody {
  SectionBody geometry();

  /** Adds the draw calls of the body laid out as {@code shape}, its corner at (x, y). */
  void extract(GuiGraphicsExtractor graphics, int x, int y, BodyShape shape);

  /** Tooltip at ({@code localX}, {@code localY}) from the body's corner, or null for none. */
  @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, BodyShape shape);

  /**
   * Finds the card at ({@code localX}, {@code localY}) from the body's corner and writes its index
   * and bounds into {@code spot}; whether there is one. Called every frame for the hover highlight,
   * so it allocates nothing. By default the whole body is one card.
   */
  default boolean locateCard(int localX, int localY, BodyShape shape, CardSpot spot) {
    if (localX < 0 || localY < 0 || localX >= shape.width() || localY >= shape.height()) {
      return false;
    }
    spot.set(0, 0, 0, shape.width(), shape.height());
    return true;
  }

  /** Card {@code card} (an index {@link #locateCard} found) to zoom. Called once per click. */
  default ZoomCard zoomCard(int card, BodyShape shape) {
    return new WholeBody(this, shape);
  }

  /** A whole body laid out as {@code shape}, zoomed as one card. */
  record WholeBody(SheetBody body, BodyShape shape) implements ZoomCard {
    @Override
    public int width() {
      return shape.width();
    }

    @Override
    public int height() {
      return shape.height();
    }

    @Override
    public void extract(GuiGraphicsExtractor graphics, int x, int y) {
      body.extract(graphics, x, y, shape);
    }

    @Override
    public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY) {
      return body.tooltipAt(localX, localY, shape);
    }
  }
}
