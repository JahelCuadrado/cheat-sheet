package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * Elements one below the other, left-aligned: a name over its caption. Positions are resolved when
 * built; drawing allocates nothing.
 */
final class ElementColumn implements SheetElement {
  private final SheetElement[] children;
  private final int[] childYs;
  private final int width;
  private final int height;

  ElementColumn(List<SheetElement> children, int gap) {
    if (children.isEmpty()) {
      throw new IllegalArgumentException("a column needs elements");
    }
    this.children = children.toArray(new SheetElement[0]);
    this.childYs = new int[this.children.length];
    int top = 0;
    int widest = 0;
    for (int index = 0; index < this.children.length; index++) {
      if (index > 0) {
        top += gap;
      }
      childYs[index] = top;
      top += this.children[index].height();
      widest = Math.max(widest, this.children[index].minWidth());
    }
    this.width = widest;
    this.height = top;
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
    for (int index = 0; index < children.length; index++) {
      SheetElement child = children[index];
      child.extract(graphics, x, y + childYs[index], child.minWidth());
    }
  }

  @Override
  public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
    for (int index = 0; index < children.length; index++) {
      SheetElement child = children[index];
      int childY = localY - childYs[index];
      if (localX < child.minWidth() && childY >= 0 && childY < child.height()) {
        return child.tooltipAt(localX, childY, child.minWidth());
      }
    }
    return null;
  }
}
