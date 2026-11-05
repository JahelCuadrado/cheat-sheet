package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.sheet.Scissor;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Where the mouse is while the Commands tab is drawn, for its buttons' hover light: elements are
 * not given the mouse, but the sheet is drawn in GUI pixels with no pose (the GUI scale is the
 * sheet scale while it is shown), so the mouse in GUI pixels is in the same space. Reads two
 * doubles per call; allocates nothing.
 */
final class CommandPointer {
  private CommandPointer() {}

  /** Mouse x in GUI pixels. */
  static int x() {
    Minecraft minecraft = Minecraft.getInstance();
    Window window = minecraft.getWindow();
    return (int) Math.floor(minecraft.mouseHandler.getScaledXPos(window));
  }

  /** Mouse y in GUI pixels. */
  static int y() {
    Minecraft minecraft = Minecraft.getInstance();
    Window window = minecraft.getWindow();
    return (int) Math.floor(minecraft.mouseHandler.getScaledYPos(window));
  }

  /**
   * Whether ({@code mouseX}, {@code mouseY}) is inside the rectangle and drawn (inside the sheet's
   * view, not under the tab bar).
   */
  static boolean inside(
      GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, int width, int height) {
    return mouseX >= x
        && mouseX < x + width
        && mouseY >= y
        && mouseY < y + height
        && Scissor.contains(graphics, mouseX, mouseY);
  }
}
