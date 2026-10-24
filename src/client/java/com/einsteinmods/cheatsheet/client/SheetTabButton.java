package com.einsteinmods.cheatsheet.client;

import com.einsteinmods.cheatsheet.client.sheet.ItemIcon;
import com.einsteinmods.cheatsheet.client.sheet.Panels;
import com.einsteinmods.cheatsheet.client.sheet.SheetPalette;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * A cheat sheet tab: a square with its item, the same size on screen at every sheet scale ({@link
 * BarButton}); its name is the tooltip (and the narration).
 */
final class SheetTabButton extends BarButton {
  /** Space around the icon. */
  private static final int PADDING = (SIZE - ItemIcon.ITEM_SIZE) / 2;

  private final ItemStack icon;
  private boolean selected;

  private SheetTabButton(ItemStack icon, Component name, OnPress onPress) {
    super(name, onPress, DEFAULT_NARRATION);
    this.icon = icon;
    setTooltip(Tooltip.create(name));
  }

  static SheetTabButton create(ItemStack icon, Component name, OnPress onPress) {
    return new SheetTabButton(icon, name, onPress);
  }

  void setSelected(boolean selected) {
    this.selected = selected;
  }

  @Override
  protected void extractSquare(GuiGraphicsExtractor graphics, int x, int y) {
    int fill;
    int border;
    if (selected) {
      fill = SheetPalette.TAB_SELECTED_FILL;
      border = SheetPalette.TAB_SELECTED_BORDER;
    } else if (isHoveredOrFocused()) {
      fill = SheetPalette.TAB_HOVER_FILL;
      border = SheetPalette.TAB_HOVER_BORDER;
    } else {
      fill = SheetPalette.TAB_FILL;
      border = SheetPalette.TAB_BORDER;
    }
    Panels.rounded(graphics, x, y, SIZE, SIZE, fill, border);
    CheatSheetClient.scaledItemIcons().drawItem(graphics, icon, x + PADDING, y + PADDING);
  }
}
