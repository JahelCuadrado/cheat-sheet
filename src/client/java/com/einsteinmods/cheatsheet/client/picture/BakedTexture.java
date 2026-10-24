package com.einsteinmods.cheatsheet.client.picture;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.renderer.texture.AbstractTexture;

/**
 * An atlas's baked image, registered with the texture manager so blits can draw it with the
 * premultiplied-alpha GUI pipeline. Baked at the sheet scale, so it is sampled pixel for pixel.
 */
final class BakedTexture extends AbstractTexture {
  private final int scale;

  BakedTexture(GpuTexture texture, GpuTextureView textureView, int scale) {
    this.texture = texture;
    this.textureView = textureView;
    this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
    this.scale = scale;
  }

  /** Texture pixels per sheet pixel. */
  int scale() {
    return scale;
  }

  int width() {
    return getTexture().getWidth(0);
  }

  int height() {
    return getTexture().getHeight(0);
  }
}
