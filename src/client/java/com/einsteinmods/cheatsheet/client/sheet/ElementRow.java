package com.einsteinmods.cheatsheet.client.sheet;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * Elements side by side, centred vertically: a recipe (icon + icon → icon), an icon with its note.
 * Children keep their own width.
 */
public final class ElementRow implements SheetElement {
  /** Default space between children. */
  public static final int GAP = 4;

  private final List<SheetElement> children;
  private final int gap;
  private final int width;
  private final int height;

  public ElementRow(List<SheetElement> children, int gap) {
    if (children.isEmpty()) {
      throw new IllegalArgumentException("a row needs elements");
    }
    this.children = List.copyOf(children);
    this.gap = gap;
    int totalWidth = gap * (children.size() - 1);
    int tallest = 0;
    for (SheetElement child : children) {
      totalWidth += child.minWidth();
      tallest = Math.max(tallest, child.height());
    }
    this.width = totalWidth;
    this.height = tallest;
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
    int childX = x;
    for (int index = 0; index < children.size(); index++) {
      SheetElement child = children.get(index);
      int childWidth = child.minWidth();
      child.extract(graphics, childX, y + (height - child.height()) / 2, childWidth);
      childX += childWidth + gap;
    }
  }

  @Override
  public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
    int childX = 0;
    for (int index = 0; index < children.size(); index++) {
      SheetElement child = children.get(index);
      int childWidth = child.minWidth();
      int childY = (height - child.height()) / 2;
      if (localX >= childX
          && localX < childX + childWidth
          && localY >= childY
          && localY < childY + child.height()) {
        return child.tooltipAt(localX - childX, localY - childY, childWidth);
      }
      childX += childWidth + gap;
    }
    return null;
  }
}
