package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * An element centred in a fixed box, so it takes the room of a picture: an icon standing in for a
 * place in miniature. The box is at least as big as the element.
 */
final class BoxedElement implements SheetElement {
  private final SheetElement content;
  private final int width;
  private final int height;
  private final int offsetX;
  private final int offsetY;

  BoxedElement(SheetElement content, int width, int height) {
    this.content = content;
    this.width = Math.max(width, content.minWidth());
    this.height = Math.max(height, content.height());
    this.offsetX = (this.width - content.minWidth()) / 2;
    this.offsetY = (this.height - content.height()) / 2;
  }

  @Override
  public int minWidth() {
    return width;
  }

  @Override
  public int height() {
    return height;
  }

  @Override
  public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
    content.extract(graphics, x + offsetX, y + offsetY, content.minWidth());
  }

  @Override
  public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
    int x = localX - offsetX;
    int y = localY - offsetY;
    if (x < 0 || y < 0 || x >= content.minWidth() || y >= content.height()) {
      return null;
    }
    return content.tooltipAt(x, y, content.minWidth());
  }
}
