package com.einsteinmods.cheatsheet.client.picture;

import com.einsteinmods.cheatsheet.client.CheatSheetClient;
import com.einsteinmods.cheatsheet.core.pictures.PictureSize;
import com.einsteinmods.cheatsheet.core.pictures.ShelfPacker;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * A sheet's pictures, packed into one texture that is baked once (mobs, armour and block models
 * rendered by the game, so resource packs apply) and then drawn as plain textured quads.
 *
 * <p>Pictures are sized in sheet pixels and baked at the sheet scale (window pixels per sheet
 * pixel, see {@code CheatSheetScreen}), so each texture pixel is one window pixel. A zoomed card's
 * pictures are baked again, into a second texture of their own, at the zoom's scale: drawn between
 * {@link #beginZoom} and {@link #endZoom}, they are never a stretched copy of the sheet's. A view
 * laid out for the zoom may add bigger pictures of its own ({@link #addZoomOnly}): they are only
 * ever baked into the zoom's texture.
 *
 * <p>Cost: building only records scenes and sizes. The first frame the sheet is shown at a sheet
 * scale asks for a bake (one {@link PictureBakeRequest}); the bake runs once during that frame's
 * GUI render, and every later frame draws one blit per visible picture. A zoom's first frame finds
 * which pictures its card draws and asks for their bake the same way. {@link #release} frees both
 * textures (the screen calls it when it closes), {@link #releaseZoom} the zoom's; a later frame
 * bakes again.
 */
public final class PictureAtlas implements PictureBakeTarget {
  private static final String TEXTURE_PREFIX = "pictures/";
  private static final String ZOOM_SUFFIX = "_zoom";
  private static final int GAP = 1;
  private static final int BYTES_PER_PIXEL = 4;
  private static final RenderPipeline PIPELINE = RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA;

  private final List<Scene> scenes = new ArrayList<>();
  private final List<PictureSize> sizes = new ArrayList<>();

  /** Pictures only a zoom view draws: never baked into the sheet's texture. */
  private final BitSet zoomOnly = new BitSet();

  private final PictureBakeRequest request = new PictureBakeRequest(this);
  private final Layer sheet;
  private final Layer zoom;
  private int requestedScale = 1;
  private boolean drawable;
  private boolean sheetBakeWanted;
  private long bakeNanos;

  /** Between {@link #beginZoom} and {@link #endZoom}: blits draw from the zoom's texture. */
  private boolean zooming;

  private int zoomScale = 1;

  /** Pictures the zoomed card draws, by index; null until the first zoom. */
  private boolean @Nullable [] zoomPictures;

  /** A picture not yet in the zoom's texture was drawn, or the scale changed: bake again. */
  private boolean zoomStale;

  private boolean zoomBakeWanted;

  /** {@code name}: unique among sheets (the tab id), names the texture. */
  public PictureAtlas(String name) {
    this.sheet = new Layer(CheatSheetClient.id(TEXTURE_PREFIX + name));
    this.zoom = new Layer(CheatSheetClient.id(TEXTURE_PREFIX + name + ZOOM_SUFFIX));
  }

  /**
   * Adds a picture of {@code scene}, {@code width} × {@code height} sheet pixels. Build time only.
   */
  public Picture add(Scene scene, int width, int height, List<FormattedCharSequence> tooltip) {
    if (sheet.packing != null) {
      throw new IllegalStateException("pictures are added while the sheet is built");
    }
    PictureSize size = new PictureSize(width, height);
    scenes.add(scene);
    sizes.add(size);
    return new Picture(this, scenes.size() - 1, size, tooltip);
  }

  /**
   * Adds a picture of {@code scene} that only a zoom view draws, {@code width} × {@code height}
   * card pixels: baked at the zoom's scale when a zoom first draws it, never with the sheet. May be
   * called after the sheet is built (a view built on its first click); the caller keeps the picture
   * for later clicks.
   */
  public Picture addZoomOnly(
      Scene scene, int width, int height, List<FormattedCharSequence> tooltip) {
    PictureSize size = new PictureSize(width, height);
    scenes.add(scene);
    sizes.add(size);
    zoomOnly.set(scenes.size() - 1);
    return new Picture(this, scenes.size() - 1, size, tooltip);
  }

  /**
   * Called once per frame before the sheet is drawn: pictures draw when the texture matches {@code
   * sheetScale} (window pixels per sheet pixel); otherwise this frame asks for a bake and pictures
   * stay empty until the next one.
   */
  public void beginFrame(GuiGraphicsExtractor graphics, int sheetScale) {
    requestedScale = sheetScale;
    drawable = sheet.texture != null && sheet.bakedScale == sheetScale;
    if (!drawable && zoomOnly.nextClearBit(0) < scenes.size()) {
      sheetBakeWanted = true;
      graphics.guiRenderState.addPicturesInPictureState(request);
    }
  }

  /**
   * Called before a zoomed card is drawn at {@code scale} window pixels per card pixel: until
   * {@link #endZoom}, pictures draw from the zoom's texture, baked at that scale.
   */
  public void beginZoom(int scale) {
    zooming = true;
    if (zoomScale != scale) {
      zoomScale = scale;
      zoomStale = true;
    }
    boolean[] wanted = zoomPictures;
    if (wanted == null || wanted.length < scenes.size()) {
      zoomPictures =
          wanted == null ? new boolean[scenes.size()] : Arrays.copyOf(wanted, scenes.size());
    }
  }

  /**
   * Called after the zoomed card is drawn: asks for a bake, every frame until it runs, if the card
   * drew a picture not baked yet.
   */
  public void endZoom(GuiGraphicsExtractor graphics) {
    zooming = false;
    if (zoomStale) {
      zoomStale = false;
      zoomBakeWanted = true;
    }
    if (zoomBakeWanted) {
      graphics.guiRenderState.addPicturesInPictureState(request);
    }
  }

  void blit(GuiGraphicsExtractor graphics, int index, int x, int y) {
    if (zooming) {
      boolean[] wanted = zoomPictures;
      if (wanted != null && index < wanted.length && !wanted[index]) {
        wanted[index] = true;
        zoomStale = true;
      }
      zoom.blit(graphics, index, x, y);
      return;
    }
    if (drawable) {
      sheet.blit(graphics, index, x, y);
    }
  }

  /**
   * Bakes what the last frame asked for: every picture at the sheet scale, the zoomed card's at the
   * zoom's scale, replacing older textures. GUI render only.
   */
  @Override
  public void bake(PictureBaker baker, FeatureRenderDispatcher dispatcher) {
    if (sheetBakeWanted) {
      sheetBakeWanted = false;
      if (sheet.texture == null || sheet.bakedScale != requestedScale) {
        long start = System.nanoTime();
        if (sheet.packing == null) {
          List<PictureSize> sheetSizes = new ArrayList<>(sizes.size());
          int[] slots = new int[scenes.size()];
          for (int index = 0; index < slots.length; index++) {
            slots[index] = zoomOnly.get(index) ? -1 : sheetSizes.size();
            if (!zoomOnly.get(index)) {
              sheetSizes.add(sizes.get(index));
            }
          }
          sheet.pack(ShelfPacker.pack(sheetSizes, GAP), slots);
        }
        sheet.bake(baker, sheetScenes(), dispatcher, requestedScale);
        bakeNanos = System.nanoTime() - start;
      }
    }
    boolean[] wanted = zoomPictures;
    if (zoomBakeWanted && wanted != null) {
      zoomBakeWanted = false;
      List<Scene> zoomScenes = new ArrayList<>();
      List<PictureSize> zoomSizes = new ArrayList<>();
      int[] slots = new int[wanted.length];
      for (int index = 0; index < slots.length; index++) {
        slots[index] = wanted[index] ? zoomScenes.size() : -1;
        if (wanted[index]) {
          zoomScenes.add(scenes.get(index));
          zoomSizes.add(sizes.get(index));
        }
      }
      zoom.pack(ShelfPacker.pack(zoomSizes, GAP), slots);
      zoom.bake(baker, zoomScenes, dispatcher, zoomScale);
    }
  }

  /** The scenes of the sheet's own pictures, in their slot order. */
  private List<Scene> sheetScenes() {
    List<Scene> sheetScenes = new ArrayList<>(scenes.size());
    for (int index = 0; index < scenes.size(); index++) {
      if (!zoomOnly.get(index)) {
        sheetScenes.add(scenes.get(index));
      }
    }
    return sheetScenes;
  }

  /** Frees the zoom's texture and forgets its pictures; the next zoom bakes its own. */
  public void releaseZoom() {
    zoom.release();
    zoomBakeWanted = false;
    zoomStale = false;
    boolean[] wanted = zoomPictures;
    if (wanted != null) {
      Arrays.fill(wanted, false);
    }
  }

  /** Frees both textures; the next frame that shows the sheet bakes it again. */
  public void release() {
    sheet.release();
    releaseZoom();
    drawable = false;
  }

  /** CPU time of the last bake of the whole sheet, for development measurements. */
  public long bakeNanos() {
    return bakeNanos;
  }

  /** GPU memory held by the baked textures, 0 when released. */
  public long textureBytes() {
    return sheet.bytes() + zoom.bytes();
  }

  /** One baked texture holding some of the atlas's pictures. */
  private static final class Layer {
    private final Identifier textureId;
    private ShelfPacker.@Nullable Packing packing;

    /** Rect index in {@link #packing} of each picture of the atlas, -1 for one not in it. */
    private int[] slots = new int[0];

    private @Nullable BakedTexture texture;
    private int bakedScale;

    Layer(Identifier textureId) {
      this.textureId = textureId;
    }

    void pack(ShelfPacker.Packing packed, int[] pictureSlots) {
      packing = packed;
      slots = pictureSlots;
    }

    void bake(
        PictureBaker baker,
        List<Scene> layerScenes,
        FeatureRenderDispatcher dispatcher,
        int scale) {
      ShelfPacker.Packing packed = packing;
      if (packed == null) {
        return;
      }
      release();
      // A zoomed card without pictures packs nothing: the GPU refuses a 0x0 texture.
      if (layerScenes.isEmpty() || packed.width() == 0 || packed.height() == 0) {
        return;
      }
      BakedTexture baked = baker.bake(layerScenes, packed, dispatcher, scale);
      Minecraft.getInstance().getTextureManager().register(textureId, baked);
      texture = baked;
      bakedScale = scale;
    }

    void blit(GuiGraphicsExtractor graphics, int index, int x, int y) {
      BakedTexture baked = texture;
      ShelfPacker.Packing packed = packing;
      if (baked == null || packed == null || index >= slots.length || slots[index] < 0) {
        return;
      }
      ShelfPacker.Rect rect = packed.rects().get(slots[index]);
      int scale = baked.scale();
      // The texture is stored bottom row first: v runs from the rect's top down.
      graphics.blit(
          PIPELINE,
          textureId,
          x,
          y,
          rect.x() * scale,
          baked.height() - rect.y() * scale,
          rect.width(),
          rect.height(),
          rect.width() * scale,
          -rect.height() * scale,
          baked.width(),
          baked.height());
    }

    void release() {
      if (texture != null) {
        Minecraft.getInstance().getTextureManager().release(textureId);
        texture = null;
      }
    }

    long bytes() {
      BakedTexture baked = texture;
      return baked == null ? 0 : (long) baked.width() * baked.height() * BYTES_PER_PIXEL;
    }
  }
}
