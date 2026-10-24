package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.sheet.Scissor;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import com.einsteinmods.cheatsheet.core.farming.FarmPlan;
import com.einsteinmods.cheatsheet.core.farming.PlanCell;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * A farm seen from above as a diagram: one flat-coloured square per block on a grid of dark lines,
 * small enough to set several layouts side by side. Flat colours stay sharp at any scale, where
 * textures shrunk this much would not. Each square's tooltip says what it is. Colours are resolved
 * when built; drawing is one fill for the grid and one per block, a GUI stratum per row, rows
 * outside the scissor skipped, and allocates nothing.
 */
final class PlanDiagram implements SheetElement {
  /** How one kind of block looks: its colour and its tooltip. */
  record Swatch(int color, List<FormattedCharSequence> tooltip) {
    Swatch {
      tooltip = List.copyOf(tooltip);
    }
  }

  /** The grid lines between blocks. */
  private static final int LINE = 1;

  private final Swatch[] swatches;
  private final int cell;
  private final int columns;
  private final int rows;
  private final int lineColor;

  /**
   * {@code swatches} has one for every kind of block in {@code plan}; each block is {@code cell}
   * pixels apart, its line included.
   */
  PlanDiagram(FarmPlan plan, Map<PlanCell, Swatch> swatches, int cell, int lineColor) {
    if (cell <= LINE) {
      throw new IllegalArgumentException("cells of " + cell + " pixels leave no room");
    }
    this.cell = cell;
    this.lineColor = lineColor;
    this.columns = plan.width();
    this.rows = plan.height();
    this.swatches = new Swatch[columns * rows];
    for (int y = 0; y < rows; y++) {
      for (int x = 0; x < columns; x++) {
        Swatch swatch = swatches.get(plan.cell(x, y));
        if (swatch == null) {
          throw new IllegalArgumentException("no swatch for " + plan.cell(x, y));
        }
        this.swatches[y * columns + x] = swatch;
      }
    }
  }

  @Override
  public int minWidth() {
    return columns * cell + LINE;
  }

  @Override
  public int height() {
    return rows * cell + LINE;
  }

  @Override
  public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
    graphics.fill(x, y, x + minWidth(), y + height(), lineColor);
    for (int row = 0; row < rows; row++) {
      int top = y + LINE + row * cell;
      if (!Scissor.touches(graphics, x, top, cell)) {
        continue;
      }
      // A stratum per row: vanilla's placement scan stays within one row of blocks.
      graphics.nextStratum();
      for (int column = 0; column < columns; column++) {
        int left = x + LINE + column * cell;
        graphics.fill(
            left,
            top,
            left + cell - LINE,
            top + cell - LINE,
            swatches[row * columns + column].color());
      }
    }
  }

  @Override
  public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
    int column = (localX - LINE) / cell;
    int row = (localY - LINE) / cell;
    if (localX < LINE || localY < LINE || column >= columns || row >= rows) {
      return null;
    }
    return swatches[row * columns + column].tooltip();
  }
}
