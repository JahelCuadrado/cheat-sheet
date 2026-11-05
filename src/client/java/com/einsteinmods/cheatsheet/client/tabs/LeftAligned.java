package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/** {@code content} at the left of a box {@code width} wide, so the next column lines up. */
record LeftAligned(SheetElement content, int width) implements SheetElement {
  @Override
  public int minWidth() {
    return Math.max(width, content.minWidth());
  }

  @Override
  public int height() {
    return content.height();
  }

  @Override
  public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
    content.extract(graphics, x, y, content.minWidth());
  }

  @Override
  public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
    return localX < content.minWidth() ? content.tooltipAt(localX, localY, width) : null;
  }
}
