package com.einsteinmods.cheatsheet.client.sheet;

import com.einsteinmods.cheatsheet.core.ZoomSize;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * One element zoomed as a card at its natural width: a tile of a grid, or a view a tab lays out for
 * the zoom; {@code size} is how big it gets (null for its tab's size).
 */
public record ElementCard(SheetElement element, @Nullable ZoomSize size) implements ZoomCard {
  public ElementCard(SheetElement element) {
    this(element, null);
  }

  @Override
  public int width() {
    return element.minWidth();
  }

  @Override
  public int height() {
    return element.height();
  }

  @Override
  public void extract(GuiGraphicsExtractor graphics, int x, int y) {
    element.extract(graphics, x, y, element.minWidth());
  }

  @Override
  public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY) {
    return element.tooltipAt(localX, localY, element.minWidth());
  }
}
