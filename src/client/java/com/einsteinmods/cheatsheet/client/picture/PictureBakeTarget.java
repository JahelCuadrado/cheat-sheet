package com.einsteinmods.cheatsheet.client.picture;

import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;

/** Something {@link PictureBakeRenderer} bakes when a {@link PictureBakeRequest} asks for it. */
interface PictureBakeTarget {
  /** Bakes what the last frame asked for. GUI render only. */
  void bake(PictureBaker baker, FeatureRenderDispatcher dispatcher);
}
