package com.einsteinmods.cheatsheet.client.sheet;

import com.einsteinmods.cheatsheet.client.picture.PictureAtlas;
import com.einsteinmods.cheatsheet.core.ZoomLayout;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * A clicked card drawn again, bigger, on a panel over the sheet where it was clicked ({@link
 * ZoomLayout}); the sheet stays drawn behind it. Its details are its tooltips, shown under the
 * mouse as on the sheet.
 *
 * <p>The GUI scale stays the sheet's: the card is drawn under a pose of zoom scale / sheet scale
 * whose corner sits on a whole window pixel, so each card pixel is exactly {@link #scale()} window
 * pixels. Text glyphs and fills land on whole pixels; item icons are baked at their final size
 * ({@code ScaledItemIcons}) and the card's pictures again at the zoom's scale ({@link
 * PictureAtlas#beginZoom}). Nothing is a stretched bitmap, and the sheet behind is untouched.
 *
 * <p>Built once per click. A frame is the panel and the card's own draw calls; it allocates nothing
 * of its own. The first frame also asks for the bake of the card's pictures and scaled icons (shown
 * from the next frame on).
 */
public final class CardZoom {
  private final ZoomCard card;
  private final @Nullable PictureAtlas pictures;
  private final ZoomLayout layout;
  private final int sheetScale;
  private final int scale;

  /** Sheet pixels per card pixel: the pose the card is drawn under. */
  private final float cardPose;

  private CardZoom(
      ZoomCard card,
      @Nullable PictureAtlas pictures,
      ZoomLayout layout,
      int sheetScale,
      int scale) {
    this.card = card;
    this.pictures = pictures;
    this.layout = layout;
    this.sheetScale = sheetScale;
    this.scale = scale;
    this.cardPose = (float) scale / sheetScale;
  }

  /**
   * Zooms {@code card} at {@code scale} window pixels per card pixel on a window of {@code
   * windowWidth} x {@code windowHeight} pixels, the sheet being drawn at {@code sheetScale},
   * centred on ({@code anchorX}, {@code anchorY}) in window pixels; {@code pictures} is the sheet's
   * atlas (null for none).
   */
  public static CardZoom open(
      ZoomCard card,
      @Nullable PictureAtlas pictures,
      int windowWidth,
      int windowHeight,
      int sheetScale,
      int scale,
      int anchorX,
      int anchorY) {
    ZoomLayout layout =
        ZoomLayout.of(
            new ZoomLayout.Input(
                windowWidth,
                windowHeight,
                sheetScale,
                scale,
                card.width(),
                card.height(),
                anchorX,
                anchorY));
    return new CardZoom(card, pictures, layout, sheetScale, scale);
  }

  /** Window pixels per card pixel. */
  public int scale() {
    return scale;
  }

  /** Whether the point (sheet pixels) is on the zoom's panel; a click elsewhere closes it. */
  public boolean contains(double x, double y) {
    return layout.panel().contains(x, y);
  }

  /** Tooltip under the mouse ({@code mouseX}, {@code mouseY} in sheet pixels), or null. */
  public @Nullable List<FormattedCharSequence> tooltipAt(int mouseX, int mouseY) {
    int localX = Math.floorDiv(mouseX * sheetScale - layout.cardX(), scale);
    int localY = Math.floorDiv(mouseY * sheetScale - layout.cardY(), scale);
    if (localX < 0 || localY < 0 || localX >= card.width() || localY >= card.height()) {
      return null;
    }
    return card.tooltipAt(localX, localY);
  }

  /** Adds the panel and the card. */
  public void extract(GuiGraphicsExtractor graphics) {
    ZoomLayout.Rect panel = layout.panel();
    Panels.rounded(
        graphics,
        panel.x(),
        panel.y(),
        panel.width(),
        panel.height(),
        SheetPalette.ZOOM_PANEL_FILL,
        SheetPalette.ZOOM_PANEL_BORDER);
    graphics.nextStratum();
    PictureAtlas atlas = pictures;
    if (atlas != null) {
      atlas.beginZoom(scale);
    }
    graphics.pose().pushMatrix();
    graphics
        .pose()
        .translate((float) layout.cardX() / sheetScale, (float) layout.cardY() / sheetScale);
    graphics.pose().scale(cardPose);
    card.extract(graphics, 0, 0);
    graphics.pose().popMatrix();
    if (atlas != null) {
      atlas.endZoom(graphics);
    }
  }

  /** Frees what the zoom baked: its pictures' texture. */
  public void close() {
    if (pictures != null) {
      pictures.releaseZoom();
    }
  }
}
