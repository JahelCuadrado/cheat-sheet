package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * Elements side by side in columns of one width, top-aligned (each centred across its column).
 * Drawing allocates nothing.
 */
final class TopRow implements SheetElement {
  private final SheetElement[] children;
  private final int columnWidth;
  private final int gap;
  private final int height;

  TopRow(List<SheetElement> children, int columnWidth, int gap) {
    this.children = children.toArray(new SheetElement[0]);
    this.columnWidth = columnWidth;
    this.gap = gap;
    int tallest = 0;
    for (SheetElement child : this.children) {
      tallest = Math.max(tallest, child.height());
    }
    this.height = tallest;
  }

  @Override
  public int minWidth() {
    return children.length * (columnWidth + gap) - gap;
  }

  @Override
  public int height() {
    return height;
  }

  @Override
  public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
    for (int index = 0; index < children.length; index++) {
      SheetElement child = children[index];
      child.extract(graphics, x + childX(index, child), y, child.minWidth());
    }
  }

  @Override
  public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
    int index = localX / (columnWidth + gap);
    if (index >= children.length) {
      return null;
    }
    SheetElement child = children[index];
    int childX = localX - childX(index, child);
    if (childX < 0 || childX >= child.minWidth() || localY >= child.height()) {
      return null;
    }
    return child.tooltipAt(childX, localY, child.minWidth());
  }

  private int childX(int index, SheetElement child) {
    return index * (columnWidth + gap) + Math.max(0, (columnWidth - child.minWidth()) / 2);
  }
}
