package com.einsteinmods.cheatsheet.client.picture;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.jspecify.annotations.Nullable;

/**
 * Asks {@link PictureBakeRenderer} to bake a target (an atlas of pictures or of item icons) during
 * the GUI render, where the game draws its own mobs in screens (same lights). Draws nothing itself:
 * what it bakes is drawn from the next frame on.
 */
record PictureBakeRequest(PictureBakeTarget target) implements PictureInPictureRenderState {
  /** Any area on screen will do: the request draws nothing there. */
  private static final ScreenRectangle AREA = new ScreenRectangle(0, 0, 1, 1);

  @Override
  public int x0() {
    return AREA.left();
  }

  @Override
  public int x1() {
    return AREA.right();
  }

  @Override
  public int y0() {
    return AREA.top();
  }

  @Override
  public int y1() {
    return AREA.bottom();
  }

  @Override
  public float scale() {
    return 1.0F;
  }

  @Override
  public @Nullable ScreenRectangle scissorArea() {
    return null;
  }

  @Override
  public ScreenRectangle bounds() {
    return AREA;
  }
}
