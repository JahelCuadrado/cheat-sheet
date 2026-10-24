package com.einsteinmods.cheatsheet.client.picture;

import com.einsteinmods.cheatsheet.client.CheatSheetClient;
import com.einsteinmods.cheatsheet.core.pictures.PictureSize;
import com.einsteinmods.cheatsheet.core.pictures.ShelfPacker;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

/**
 * Item icons drawn at another size than 1x (2x ores, half-size badges), rendered by the game at
 * their final size in window pixels. Vanilla renders every GUI item into its atlas at 16 x GUI
 * scale pixels ({@code GuiRenderer#prepareItemElements}) and stretches that bitmap to whatever pose
 * the item is drawn under, so a 2x block icon showed half its texture's pixels. Here each such icon
 * is rendered once into a texture of its own size (the way vanilla renders into its atlas) and
 * drawn pixel for pixel.
 *
 * <p>Cost: an icon drawn for the first time at a size is drawn the vanilla way for one frame and
 * asks for a bake (one {@link PictureBakeRequest}); the bake renders every icon drawn that frame
 * into a new texture, and later frames draw one blit per icon after an identity-map lookup, with no
 * allocation. A new texture replaces the old one under a second id, freed the frame after, so an
 * icon drawn from the old texture during the bake's frame never samples a freed texture. Released
 * when the cheat sheet closes; nothing exists until it first draws a scaled icon.
 */
public final class ScaledItemIcons implements PictureBakeTarget {
  private static final String TEXTURE_PREFIX = "pictures/scaled_items_";
  private static final int ITEM_SIZE = 16;
  private static final int GAP = 1;

  /** How far from a whole number of window pixels an icon's size may be and still be baked. */
  private static final float WHOLE_PIXEL_TOLERANCE = 1.0E-3F;

  private static final RenderPipeline PIPELINE = RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA;

  private final Identifier[] textureIds = {
    CheatSheetClient.id(TEXTURE_PREFIX + 0), CheatSheetClient.id(TEXTURE_PREFIX + 1)
  };
  private final PictureBakeRequest request = new PictureBakeRequest(this);

  /** Icons by stack (the sheets keep their stacks), one entry per size drawn. */
  private final Map<ItemStack, Entry> entries = new IdentityHashMap<>();

  private @Nullable BakedTexture texture;
  private ShelfPacker.@Nullable Packing packing;
  private int current;
  private boolean retiredPending;
  private long frame;
  private boolean stale;
  private boolean requested;

  /** One icon at one size in window pixels; {@code slot} is its rect in the texture, or -1. */
  private static final class Entry {
    private final ItemStack stack;
    private final int pixels;
    private int slot = -1;
    private long lastFrame;
    private @Nullable Entry next;

    Entry(ItemStack stack, int pixels) {
      this.stack = stack;
      this.pixels = pixels;
    }
  }

  /** Called once per frame before any icon is drawn: frees the texture a bake replaced. */
  public void beginFrame() {
    frame++;
    requested = false;
    if (retiredPending) {
      retiredPending = false;
      Minecraft.getInstance().getTextureManager().release(textureIds[1 - current]);
    }
  }

  /**
   * Draws {@code stack} at 1x at ({@code x}, {@code y}): the vanilla way under an unscaled pose
   * (the sheet), baked at its final size under a scaled one (a zoomed card), so it is never
   * stretched. One pose check per icon when unscaled.
   */
  public void drawItem(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
    Matrix3x2f pose = graphics.pose();
    if (pose.m00() == 1.0F && pose.m11() == 1.0F) {
      graphics.item(stack, x, y);
      return;
    }
    draw(graphics, stack, x, y, 1.0F);
  }

  /**
   * Draws {@code stack} at ({@code x}, {@code y}), {@code scale} times its 16 pixels; drawn the
   * vanilla way (a stretched icon) only until its bake, or when the pose does not land it on whole
   * window pixels.
   */
  public void draw(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, float scale) {
    float size = ITEM_SIZE * scale;
    int guiSize = Math.round(size);
    Matrix3x2f pose = graphics.pose();
    float exact = size * pose.m00() * Minecraft.getInstance().getWindow().getGuiScale();
    int pixels = Math.round(exact);
    if (stack.isEmpty()
        || pixels <= 0
        || Math.abs(exact - pixels) > WHOLE_PIXEL_TOLERANCE
        || Math.abs(size - guiSize) > WHOLE_PIXEL_TOLERANCE
        || pose.m00() != pose.m11()
        || pose.m01() != 0.0F
        || pose.m10() != 0.0F) {
      drawStretched(graphics, stack, x, y, scale);
      return;
    }
    Entry entry = find(stack, pixels);
    if (entry == null) {
      entry = new Entry(stack, pixels);
      entry.next = entries.get(stack);
      entries.put(stack, entry);
      stale = true;
    }
    entry.lastFrame = frame;
    BakedTexture baked = texture;
    ShelfPacker.Packing packed = packing;
    if (baked != null && packed != null && entry.slot >= 0) {
      ShelfPacker.Rect rect = packed.rects().get(entry.slot);
      // The texture is stored bottom row first: v runs from the rect's top down.
      graphics.blit(
          PIPELINE,
          textureIds[current],
          x,
          y,
          rect.x(),
          baked.height() - rect.y(),
          guiSize,
          guiSize,
          rect.width(),
          -rect.height(),
          baked.width(),
          baked.height());
    } else {
      drawStretched(graphics, stack, x, y, scale);
    }
    if (stale && !requested) {
      requested = true;
      graphics.guiRenderState.addPicturesInPictureState(request);
    }
  }

  private @Nullable Entry find(ItemStack stack, int pixels) {
    for (Entry entry = entries.get(stack); entry != null; entry = entry.next) {
      if (entry.pixels == pixels) {
        return entry;
      }
    }
    return null;
  }

  private static void drawStretched(
      GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, float scale) {
    graphics.pose().pushMatrix();
    graphics.pose().translate(x, y);
    graphics.pose().scale(scale, scale);
    graphics.item(stack, 0, 0);
    graphics.pose().popMatrix();
  }

  /** Renders every icon drawn this frame into a new texture; icons not drawn are forgotten. */
  @Override
  public void bake(PictureBaker baker, FeatureRenderDispatcher dispatcher) {
    if (!stale) {
      return;
    }
    stale = false;
    List<Entry> live = new ArrayList<>();
    for (Entry first : entries.values()) {
      for (Entry entry = first; entry != null; entry = entry.next) {
        if (entry.lastFrame == frame) {
          live.add(entry);
        }
      }
    }
    entries.clear();
    List<ItemStack> stacks = new ArrayList<>(live.size());
    List<PictureSize> sizes = new ArrayList<>(live.size());
    for (int index = 0; index < live.size(); index++) {
      Entry entry = live.get(index);
      entry.slot = index;
      entry.next = entries.get(entry.stack);
      entries.put(entry.stack, entry);
      stacks.add(entry.stack);
      sizes.add(new PictureSize(entry.pixels, entry.pixels));
    }
    if (live.isEmpty()) {
      return;
    }
    ShelfPacker.Packing packed = ShelfPacker.pack(sizes, GAP);
    BakedTexture baked = baker.bakeItems(stacks, packed, dispatcher);
    int next = 1 - current;
    Minecraft.getInstance().getTextureManager().register(textureIds[next], baked);
    retiredPending = texture != null;
    texture = baked;
    packing = packed;
    current = next;
  }

  /** Frees the textures and forgets every icon; the cheat sheet calls it when it closes. */
  public void release() {
    if (texture != null || retiredPending) {
      Minecraft.getInstance().getTextureManager().release(textureIds[0]);
      Minecraft.getInstance().getTextureManager().release(textureIds[1]);
    }
    texture = null;
    packing = null;
    retiredPending = false;
    stale = false;
    entries.clear();
  }
}
