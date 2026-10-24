package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * Elements in reading order, wrapped into as many rows as a fixed width needs, each row centred and
 * its elements centred vertically on it: a dye's sources, big, in a narrow tile. Positions are
 * resolved when built; drawing allocates nothing.
 */
final class CentredRows implements SheetElement {
  private final SheetElement[] children;
  private final int[] childXs;
  private final int[] childYs;
  private final int width;
  private final int height;

  /** Children {@code first} (inclusive) to {@code end} (exclusive), {@code width} wide in all. */
  private record Row(int first, int end, int width) {}

  /**
   * @param wrapWidth rows break before an element that would make them wider than this
   */
  CentredRows(List<SheetElement> children, int wrapWidth, int gap) {
    if (children.isEmpty()) {
      throw new IllegalArgumentException("rows need elements");
    }
    this.children = children.toArray(new SheetElement[0]);
    this.childXs = new int[this.children.length];
    this.childYs = new int[this.children.length];
    List<Row> rows = new ArrayList<>();
    int first = 0;
    int rowWidth = 0;
    for (int index = 0; index < this.children.length; index++) {
      int childWidth = this.children[index].minWidth();
      if (index > first && rowWidth + gap + childWidth > wrapWidth) {
        rows.add(new Row(first, index, rowWidth));
        first = index;
        rowWidth = childWidth;
      } else {
        rowWidth += (index > first ? gap : 0) + childWidth;
      }
    }
    rows.add(new Row(first, this.children.length, rowWidth));
    int widest = 1;
    for (Row row : rows) {
      widest = Math.max(widest, row.width());
    }
    int top = 0;
    for (Row row : rows) {
      int tallest = 0;
      for (int index = row.first(); index < row.end(); index++) {
        tallest = Math.max(tallest, this.children[index].height());
      }
      int x = (widest - row.width()) / 2;
      for (int index = row.first(); index < row.end(); index++) {
        childXs[index] = x;
        childYs[index] = top + (tallest - this.children[index].height()) / 2;
        x += this.children[index].minWidth() + gap;
      }
      top += tallest + gap;
    }
    this.width = widest;
    this.height = top - gap;
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
      child.extract(graphics, x + childXs[index], y + childYs[index], child.minWidth());
    }
  }

  @Override
  public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
    for (int index = 0; index < children.length; index++) {
      SheetElement child = children[index];
      int childX = localX - childXs[index];
      int childY = localY - childYs[index];
      if (childX >= 0 && childX < child.minWidth() && childY >= 0 && childY < child.height()) {
        return child.tooltipAt(childX, childY, child.minWidth());
      }
    }
    return null;
  }
}
