package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * Two elements side by side, both from the top: {@code left}, then {@code right} starting {@code
 * rightX} from the left edge (a picture column beside a column of lines). Drawing allocates
 * nothing.
 */
record TopAligned(SheetElement left, int rightX, SheetElement right) implements SheetElement {
  @Override
  public int minWidth() {
    return Math.max(rightX, left.minWidth()) + right.minWidth();
  }

  @Override
  public int height() {
    return Math.max(left.height(), right.height());
  }

  @Override
  public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
    left.extract(graphics, x, y, left.minWidth());
    right.extract(graphics, x + rightStart(), y, right.minWidth());
  }

  @Override
  public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
    if (localX >= rightStart()) {
      return localY < right.height()
          ? right.tooltipAt(localX - rightStart(), localY, right.minWidth())
          : null;
    }
    return localX < left.minWidth() && localY < left.height()
        ? left.tooltipAt(localX, localY, left.minWidth())
        : null;
  }

  private int rightStart() {
    return Math.max(rightX, left.minWidth());
  }
}
