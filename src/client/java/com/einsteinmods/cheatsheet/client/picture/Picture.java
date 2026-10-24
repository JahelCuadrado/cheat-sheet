package com.einsteinmods.cheatsheet.client.picture;

import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import com.einsteinmods.cheatsheet.core.pictures.PictureSize;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;

/**
 * A scene (a place, a mob) drawn from its sheet's {@link PictureAtlas}: one textured quad per
 * frame, nothing while the atlas is being baked.
 */
public final class Picture implements SheetElement {
  private final PictureAtlas atlas;
  private final int index;
  private final PictureSize size;
  private final List<FormattedCharSequence> tooltip;

  Picture(PictureAtlas atlas, int index, PictureSize size, List<FormattedCharSequence> tooltip) {
    this.atlas = atlas;
    this.index = index;
    this.size = size;
    this.tooltip = List.copyOf(tooltip);
  }

  @Override
  public int minWidth() {
    return size.width();
  }

  @Override
  public int height() {
    return size.height();
  }

  @Override
  public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
    atlas.blit(graphics, index, x, y);
  }

  @Override
  public List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
    return tooltip;
  }
}
