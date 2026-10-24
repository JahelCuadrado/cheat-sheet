package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.CheatSheetClient;
import com.einsteinmods.cheatsheet.client.sheet.ItemIcon;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/** An item at half size (8 pixels): a marker beside a bigger icon, such as an upgrade it has. */
final class SmallItemIcon implements SheetElement {
  static final int SIZE = ItemIcon.ITEM_SIZE / 2;
  private static final float HALF = 0.5F;

  private final ItemStack stack;
  private final List<FormattedCharSequence> tooltip;

  SmallItemIcon(ItemStack stack, List<FormattedCharSequence> tooltip) {
    this.stack = stack;
    this.tooltip = List.copyOf(tooltip);
  }

  @Override
  public int minWidth() {
    return SIZE;
  }

  @Override
  public int height() {
    return SIZE;
  }

  @Override
  public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
    CheatSheetClient.scaledItemIcons().draw(graphics, stack, x, y, HALF);
  }

  @Override
  public List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
    return tooltip;
  }
}
