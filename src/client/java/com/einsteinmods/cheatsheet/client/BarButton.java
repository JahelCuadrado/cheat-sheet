package com.einsteinmods.cheatsheet.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

/**
 * A square button of the cheat sheet's top bar, the same size on screen whatever scale the shown
 * tab is drawn at: it is laid out in bar pixels ({@code barScale} window pixels each, the player's
 * GUI scale) and drawn under a pose of {@code barScale / sheetScale}, so every bar pixel covers the
 * same whole number of window pixels at every sheet scale (fills, sprites and fonts stay crisp; an
 * item goes through {@link com.einsteinmods.cheatsheet.client.picture.ScaledItemIcons}, baked at
 * its size). Its GUI bounds, for the mouse, are the bar rect in GUI pixels, rounded outwards.
 *
 * <p>Cost: one pose push and pop per button and frame when the scales differ; no allocation.
 */
abstract class BarButton extends Button {
  /** Side of the square, in bar pixels. */
  static final int SIZE = 20;

  private int barX;
  private int barY;
  private float poseScale = 1.0F;

  BarButton(Component name, OnPress onPress, CreateNarration narration) {
    super(0, 0, SIZE, SIZE, name, onPress, narration);
  }

  /**
   * Puts the button at ({@code x}, {@code y}) bar pixels; the bar is drawn at {@code barScale} and
   * the GUI at {@code sheetScale} window pixels per pixel.
   */
  final void place(int x, int y, int barScale, int sheetScale) {
    barX = x;
    barY = y;
    poseScale = (float) barScale / sheetScale;
    int left = Math.floorDiv(x * barScale, sheetScale);
    int top = Math.floorDiv(y * barScale, sheetScale);
    int right = Math.ceilDiv((x + SIZE) * barScale, sheetScale);
    int bottom = Math.ceilDiv((y + SIZE) * barScale, sheetScale);
    setPosition(left, top);
    setSize(right - left, bottom - top);
  }

  @Override
  protected final void extractContents(
      GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    if (poseScale == 1.0F) {
      extractSquare(graphics, barX, barY);
      return;
    }
    Matrix3x2fStack pose = graphics.pose();
    pose.pushMatrix();
    pose.scale(poseScale, poseScale);
    extractSquare(graphics, barX, barY);
    pose.popMatrix();
  }

  /** Draws the {@link #SIZE}-pixel square at ({@code x}, {@code y}) bar pixels. */
  protected abstract void extractSquare(GuiGraphicsExtractor graphics, int x, int y);
}
