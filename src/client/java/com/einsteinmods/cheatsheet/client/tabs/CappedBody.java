package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.sheet.CardSpot;
import com.einsteinmods.cheatsheet.client.sheet.SheetBody;
import com.einsteinmods.cheatsheet.client.sheet.ZoomCard;
import com.einsteinmods.cheatsheet.core.sections.BodyShape;
import com.einsteinmods.cheatsheet.core.sections.SectionBody;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * A table or grid that never splits into more than {@code maxSplit} blocks or tiles per row,
 * however wide the sheet: one table that must stay whole (1), two even rows of eight dyes (8).
 * Drawing and tooltips are the wrapped body's.
 */
final class CappedBody implements SheetBody {
  private final SheetBody body;
  private final SectionBody geometry;

  CappedBody(SheetBody body, int maxSplit) {
    if (maxSplit < 1) {
      throw new IllegalArgumentException("max split must be at least 1: " + maxSplit);
    }
    this.body = body;
    List<BodyShape> kept = new ArrayList<>();
    for (BodyShape shape : body.geometry().shapes()) {
      if (shape.split() <= maxSplit) {
        kept.add(shape);
      }
    }
    // The narrowest shape always has the smallest split, so at least one is kept.
    List<BodyShape> shapes = List.copyOf(kept);
    this.geometry = () -> shapes;
  }

  @Override
  public SectionBody geometry() {
    return geometry;
  }

  @Override
  public void extract(GuiGraphicsExtractor graphics, int x, int y, BodyShape shape) {
    body.extract(graphics, x, y, shape);
  }

  @Override
  public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, BodyShape shape) {
    return body.tooltipAt(localX, localY, shape);
  }

  @Override
  public boolean locateCard(int localX, int localY, BodyShape shape, CardSpot spot) {
    return body.locateCard(localX, localY, shape, spot);
  }

  @Override
  public ZoomCard zoomCard(int card, BodyShape shape) {
    return body.zoomCard(card, shape);
  }
}
