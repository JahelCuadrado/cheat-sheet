package com.einsteinmods.cheatsheet.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * A square vanilla button showing an item instead of a label, like the pause menu's icon buttons
 * (drawn from the item's model, so resource packs apply). The name is its tooltip and narration.
 */
public final class ItemIconButton extends Button {
  public static final int SIZE = 20;

  /** Item sprites are 16 px: centred in the 20 px button. */
  private static final int ICON_INSET = 2;

  private final ItemStack icon;

  public ItemIconButton(int x, int y, ItemStack icon, Component name, OnPress onPress) {
    super(x, y, SIZE, SIZE, name, onPress, DEFAULT_NARRATION);
    this.icon = icon;
    setTooltip(Tooltip.create(name));
  }

  @Override
  protected void extractContents(
      GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    extractDefaultSprite(graphics);
    graphics.item(icon, getX() + ICON_INSET, getY() + ICON_INSET);
  }
}
