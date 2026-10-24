package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * Elements stacked top to bottom and aligned on their left edge, so rows of icons and names line up
 * as a list (a card centres each element, which would make such rows ragged).
 */
final class LeftColumn implements SheetElement {
  private final List<SheetElement> children;
  private final int gap;
  private final int width;
  private final int height;

  LeftColumn(List<SheetElement> children, int gap) {
    if (children.isEmpty()) {
      throw new IllegalArgumentException("a column needs elements");
    }
    this.children = List.copyOf(children);
    this.gap = gap;
    int widest = 0;
    int totalHeight = gap * (children.size() - 1);
    for (SheetElement child : children) {
      widest = Math.max(widest, child.minWidth());
      totalHeight += child.height();
    }
    this.width = widest;
    this.height = totalHeight;
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
    int childY = y;
    for (int index = 0; index < children.size(); index++) {
      SheetElement child = children.get(index);
      child.extract(graphics, x, childY, child.minWidth());
      childY += child.height() + gap;
    }
  }

  @Override
  public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
    int childY = 0;
    for (int index = 0; index < children.size(); index++) {
      SheetElement child = children.get(index);
      if (localY >= childY && localY < childY + child.height() && localX < child.minWidth()) {
        return child.tooltipAt(localX, localY - childY, child.minWidth());
      }
      childY += child.height() + gap;
    }
    return null;
  }
}
