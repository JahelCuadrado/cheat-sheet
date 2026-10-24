package com.einsteinmods.cheatsheet.client.picture;

import com.einsteinmods.cheatsheet.core.pictures.BlockSpec;
import com.einsteinmods.cheatsheet.core.pictures.Diorama;
import com.einsteinmods.cheatsheet.core.pictures.DioramaBlock;
import com.einsteinmods.cheatsheet.core.pictures.ShelfPacker;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * Renders scenes (or item icons) into a new atlas texture, one cell per picture, the way vanilla
 * renders items into its GUI item atlas: an orthographic projection over the whole texture, each
 * cell clipped by a scissor and drawn by the feature dispatcher. Blocks are drawn with item
 * lighting (they look like block icons) and mobs with the lighting of mobs in screens. Only called
 * during the GUI render.
 */
final class PictureBaker implements AutoCloseable {
  private static final String LABEL = "Cheat sheet pictures";
  private static final int COLOR_USAGE =
      GpuTexture.USAGE_COPY_DST
          | GpuTexture.USAGE_TEXTURE_BINDING
          | GpuTexture.USAGE_RENDER_ATTACHMENT;
  private static final int DEPTH_USAGE =
      GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_RENDER_ATTACHMENT;
  private static final float NEAR = -1000.0F;
  private static final float FAR = 1000.0F;

  /** Depth the scene may use on each side of the cell's plane, inside the projection's range. */
  private static final float MAX_HALF_DEPTH = 900.0F;

  /** Share of the cell the scene fills: a pixel of air around it. */
  private static final float FILL = 0.94F;

  private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();
  private static final int CORNERS = 8;

  private final PoseStack poseStack = new PoseStack();
  private final SubmitNodeStorage storage = new SubmitNodeStorage();
  private final Projection projection = new Projection();
  private final BlockModelRenderState blockState = new BlockModelRenderState();
  private final ItemStackRenderState itemState = new ItemStackRenderState();
  private final CameraRenderState camera = new CameraRenderState();
  private @Nullable ProjectionMatrixBuffer projectionBuffer;

  /**
   * Renders every scene into its packed cell, at {@code pixelScale} texture pixels per sheet pixel.
   */
  BakedTexture bake(
      List<Scene> scenes,
      ShelfPacker.Packing packing,
      FeatureRenderDispatcher dispatcher,
      int pixelScale) {
    GpuDevice device = RenderSystem.getDevice();
    int maxSize = device.getDeviceInfo().limits().maxTextureSizeForFormat(GpuFormat.RGBA8_UNORM);
    int scale = Math.max(1, pixelScale);
    while (scale > 1 && Math.max(packing.width(), packing.height()) * scale > maxSize) {
      scale--;
    }
    int width = packing.width() * scale;
    int height = packing.height() * scale;
    GpuTexture color =
        device.createTexture(LABEL, COLOR_USAGE, GpuFormat.RGBA8_UNORM, width, height, 1, 1);
    GpuTextureView colorView = device.createTextureView(color);
    GpuTexture depth =
        device.createTexture(LABEL, DEPTH_USAGE, GpuFormat.D32_FLOAT, width, height, 1, 1);
    GpuTextureView depthView = device.createTextureView(depth);
    begin(color, depth, width, height);

    Minecraft minecraft = Minecraft.getInstance();
    BlockModelResolver blocks = new BlockModelResolver(minecraft.getModelManager());
    ClientLevel level = minecraft.level;
    Stage stage = level == null ? null : new Stage(level);
    for (int index = 0; index < scenes.size(); index++) {
      ShelfPacker.Rect rect = packing.rects().get(index);
      Scene scene = scenes.get(index);
      List<Entity> mobs = stage == null ? List.of() : scene.cast().create(stage);
      int left = rect.x() * scale;
      int top = rect.y() * scale;
      int cellWidth = rect.width() * scale;
      int cellHeight = rect.height() * scale;
      RenderSystem.enableScissorForRenderTypeDraws(
          left, height - top - cellHeight, cellWidth, cellHeight);
      poseStack.pushPose();
      if (frame(scene, mobs, left, top, cellWidth, cellHeight)) {
        if (scene.diorama().isPresent()) {
          submitBlocks(blocks, scene.diorama().get());
          minecraft.gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_3D);
          flush(dispatcher, colorView, depthView);
        }
        if (!mobs.isEmpty()) {
          submitMobs(minecraft.getEntityRenderDispatcher(), mobs);
          minecraft.gameRenderer.lighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
          flush(dispatcher, colorView, depthView);
        }
      }
      poseStack.popPose();
    }
    RenderSystem.disableScissorForRenderTypeDraws();
    depthView.close();
    depth.close();
    return new BakedTexture(color, colorView, scale);
  }

  /**
   * Renders each item icon into its packed cell, its size in texture pixels, the way vanilla
   * renders an item into its GUI item atlas ({@code GuiItemAtlas#drawToSlot}): the item's GUI model
   * scaled to the cell, with item lighting. Only called during the GUI render.
   */
  BakedTexture bakeItems(
      List<ItemStack> stacks, ShelfPacker.Packing packing, FeatureRenderDispatcher dispatcher) {
    GpuDevice device = RenderSystem.getDevice();
    int width = packing.width();
    int height = packing.height();
    GpuTexture color =
        device.createTexture(LABEL, COLOR_USAGE, GpuFormat.RGBA8_UNORM, width, height, 1, 1);
    GpuTextureView colorView = device.createTextureView(color);
    GpuTexture depth =
        device.createTexture(LABEL, DEPTH_USAGE, GpuFormat.D32_FLOAT, width, height, 1, 1);
    GpuTextureView depthView = device.createTextureView(depth);
    begin(color, depth, width, height);

    Minecraft minecraft = Minecraft.getInstance();
    for (int index = 0; index < stacks.size(); index++) {
      ShelfPacker.Rect rect = packing.rects().get(index);
      minecraft
          .getItemModelResolver()
          .updateForTopItem(
              itemState, stacks.get(index), ItemDisplayContext.GUI, minecraft.level, null, 0);
      RenderSystem.enableScissorForRenderTypeDraws(
          rect.x(), height - rect.y() - rect.height(), rect.width(), rect.height());
      poseStack.pushPose();
      poseStack.translate(rect.x() + rect.width() / 2.0F, rect.y() + rect.height() / 2.0F, 0.0F);
      poseStack.scale(rect.width(), -rect.height(), rect.width());
      minecraft
          .gameRenderer
          .lighting()
          .setupFor(
              itemState.usesBlockLight() ? Lighting.Entry.ITEMS_3D : Lighting.Entry.ITEMS_FLAT);
      itemState.submit(
          poseStack, storage, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
      flush(dispatcher, colorView, depthView);
      poseStack.popPose();
    }
    RenderSystem.disableScissorForRenderTypeDraws();
    depthView.close();
    depth.close();
    return new BakedTexture(color, colorView, 1);
  }

  /** Clears a new target and sets an orthographic projection over all of it. */
  private void begin(GpuTexture color, GpuTexture depth, int width, int height) {
    RenderSystem.getDevice()
        .createCommandEncoder()
        .clearColorAndDepthTextures(color, GuiRenderer.CLEAR_COLOR, depth, 0.0);
    ProjectionMatrixBuffer buffer = projectionBuffer;
    if (buffer == null) {
      buffer = new ProjectionMatrixBuffer(LABEL);
      projectionBuffer = buffer;
    }
    projection.setupOrtho(NEAR, FAR, width, height, true);
    RenderSystem.setProjectionMatrix(buffer.getBuffer(projection), ProjectionType.ORTHOGRAPHIC);
  }

  /**
   * Sets the pose so the scene's bounds (or its focus box), seen at its angle, fill the cell; false
   * for an empty scene.
   */
  private boolean frame(
      Scene scene, List<Entity> mobs, int left, int top, int cellWidth, int cellHeight) {
    if (mobs.isEmpty() && scene.diorama().isEmpty()) {
      return false;
    }
    AABB bounds = scene.focus().orElse(null);
    if (bounds != null) {
      return frame(scene, bounds, left, top, cellWidth, cellHeight);
    }
    if (scene.diorama().isPresent()) {
      bounds = blockBounds(scene.diorama().get());
    }
    for (Entity mob : mobs) {
      bounds = bounds == null ? mob.getBoundingBox() : bounds.minmax(mob.getBoundingBox());
    }
    if (bounds == null) {
      return false;
    }
    return frame(
        scene, bounds.expandTowards(0, -scene.extraBelow(), 0), left, top, cellWidth, cellHeight);
  }

  /** Sets the pose so {@code bounds}, seen at the scene's angle, fill the cell. */
  private boolean frame(
      Scene scene, AABB bounds, int left, int top, int cellWidth, int cellHeight) {
    Quaternionf rotation =
        new Quaternionf()
            .rotationX((float) Math.toRadians(scene.pitchDegrees()))
            .rotateY((float) Math.toRadians(scene.yawDegrees()));
    Matrix4f turn = new Matrix4f().rotation(rotation);
    Vector3f min = new Vector3f(Float.POSITIVE_INFINITY);
    Vector3f max = new Vector3f(Float.NEGATIVE_INFINITY);
    Vector3f corner = new Vector3f();
    for (int index = 0; index < CORNERS; index++) {
      corner.set(
          (index & 1) == 0 ? bounds.minX : bounds.maxX,
          (index & 2) == 0 ? bounds.minY : bounds.maxY,
          (index & 4) == 0 ? bounds.minZ : bounds.maxZ);
      turn.transformPosition(corner);
      min.min(corner);
      max.max(corner);
    }
    float spanX = max.x - min.x;
    float spanY = max.y - min.y;
    float spanZ = max.z - min.z;
    float pixelsPerBlock =
        Math.min(
            Math.min(cellWidth * FILL / spanX, cellHeight * FILL / spanY),
            MAX_HALF_DEPTH * 2 / spanZ);
    poseStack.translate(left + cellWidth / 2.0F, top + cellHeight / 2.0F, 0.0F);
    poseStack.scale(pixelsPerBlock, -pixelsPerBlock, pixelsPerBlock);
    poseStack.translate(-(min.x + max.x) / 2, -(min.y + max.y) / 2, -(min.z + max.z) / 2);
    poseStack.mulPose(turn);
    return true;
  }

  private static @Nullable AABB blockBounds(Diorama diorama) {
    AABB bounds = null;
    for (DioramaBlock block : diorama.blocks()) {
      AABB cell =
          new AABB(block.x(), block.y(), block.z(), block.x() + 1, block.y() + 1, block.z() + 1);
      bounds = bounds == null ? cell : bounds.minmax(cell);
    }
    return bounds;
  }

  private void submitBlocks(BlockModelResolver blocks, Diorama diorama) {
    for (DioramaBlock block : diorama.blocks()) {
      BlockState state = state(block.block());
      if (state.isAir()) {
        continue;
      }
      blocks.update(blockState, state, DISPLAY_CONTEXT);
      poseStack.pushPose();
      poseStack.translate(block.x(), block.y(), block.z());
      blockState.submit(
          poseStack, storage, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
      poseStack.popPose();
    }
  }

  private void submitMobs(EntityRenderDispatcher dispatcher, List<Entity> mobs) {
    for (Entity mob : mobs) {
      EntityRenderState state = renderState(dispatcher, mob);
      state.shadowPieces.clear();
      state.outlineColor = 0;
      state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
      dispatcher.submit(state, camera, mob.getX(), mob.getY(), mob.getZ(), poseStack, storage);
    }
  }

  private static <T extends Entity> EntityRenderState renderState(
      EntityRenderDispatcher dispatcher, T mob) {
    EntityRenderer<? super T, ?> renderer = dispatcher.getRenderer(mob);
    return renderer.createRenderState(mob, 1.0F);
  }

  private void flush(
      FeatureRenderDispatcher dispatcher, GpuTextureView colorView, GpuTextureView depthView) {
    try (FeatureRenderDispatcher.PreparedFrame frame = dispatcher.prepareFrame(storage);
        RenderPass pass =
            RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(
                    () -> LABEL, colorView, Optional.empty(), depthView, OptionalDouble.empty())) {
      RenderSystem.bindDefaultUniforms(pass);
      FeatureRenderDispatcher.renderAllFeatures(pass, frame);
    }
  }

  /**
   * The block state a spec names; air when the block, a property or a value is unknown ({@code
   * DioramaBlocksGameTest} checks every spec against the game).
   */
  private static BlockState state(BlockSpec spec) {
    Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(spec.blockId()));
    BlockState state = block.defaultBlockState();
    for (Map.Entry<String, String> property : spec.properties().entrySet()) {
      Property<?> known = block.getStateDefinition().getProperty(property.getKey());
      Optional<BlockState> changed =
          known == null ? Optional.empty() : withValue(state, known, property.getValue());
      if (changed.isEmpty()) {
        return Blocks.AIR.defaultBlockState();
      }
      state = changed.get();
    }
    return state;
  }

  private static <T extends Comparable<T>> Optional<BlockState> withValue(
      BlockState state, Property<T> property, String value) {
    return property.getValue(value).map(parsed -> state.setValue(property, parsed));
  }

  @Override
  public void close() {
    if (projectionBuffer != null) {
      projectionBuffer.close();
      projectionBuffer = null;
    }
  }
}
