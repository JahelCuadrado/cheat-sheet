package com.einsteinmods.cheatsheet.client.picture;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.jspecify.annotations.Nullable;

/**
 * Bakes a sheet's pictures when asked by a {@link PictureBakeRequest}. Registered once with
 * Fabric's picture-in-picture registry; vanilla calls it during the GUI render, after the world,
 * with the UI lightmap bound (as for the player in the inventory). It never uses the base class's
 * own texture: the atlas owns its texture and is drawn with plain blits.
 */
public final class PictureBakeRenderer extends PictureInPictureRenderer<PictureBakeRequest> {
  private static final String LABEL = "cheat sheet pictures";

  /** Created on the first bake: nothing is allocated until the cheat sheet shows a picture. */
  private @Nullable PictureBaker baker;

  @Override
  public void prepare(
      PictureBakeRequest request,
      GuiRenderState guiRenderState,
      FeatureRenderDispatcher featureRenderDispatcher,
      int guiScale) {
    PictureBaker current = baker;
    if (current == null) {
      current = new PictureBaker();
      baker = current;
    }
    // The target bakes at the scale it was drawn at.
    request.target().bake(current, featureRenderDispatcher);
  }

  /** Never called: {@link #prepare} bakes into the atlas instead. */
  @Override
  protected void renderToTexture(
      PictureBakeRequest request, PoseStack poseStack, SubmitNodeCollector collector) {
    throw new UnsupportedOperationException("pictures are baked into their atlas");
  }

  @Override
  public Class<PictureBakeRequest> getRenderStateClass() {
    return PictureBakeRequest.class;
  }

  @Override
  protected String getTextureLabel() {
    return LABEL;
  }

  @Override
  public void close() {
    super.close();
    if (baker != null) {
      baker.close();
    }
  }
}
