package com.einsteinmods.cheatsheet.client.sheet;

import com.einsteinmods.cheatsheet.client.CheatSheetClient;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * An item drawn at 1x or bigger (2x makes objects stand out on the sheet), with its name and the
 * source's detail lines as tooltip. Uses the item's own model, so resource packs apply.
 */
public final class ItemIcon implements SheetElement {
  /** Side of an item sprite at 1x. */
  public static final int ITEM_SIZE = 16;

  private final ItemStack stack;
  private final int scale;
  private final List<FormattedCharSequence> tooltip;

  public ItemIcon(ItemStack stack, int scale, List<FormattedCharSequence> tooltip) {
    if (scale < 1) {
      throw new IllegalArgumentException("scale must be at least 1: " + scale);
    }
    this.stack = stack;
    this.scale = scale;
    this.tooltip = List.copyOf(tooltip);
  }

  @Override
  public int minWidth() {
    return ITEM_SIZE * scale;
  }

  @Override
  public int height() {
    return ITEM_SIZE * scale;
  }

  @Override
  public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
    if (scale == 1) {
      CheatSheetClient.scaledItemIcons().drawItem(graphics, stack, x, y);
      return;
    }
    CheatSheetClient.scaledItemIcons().draw(graphics, stack, x, y, scale);
  }

  @Override
  public List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
    return tooltip;
  }
}
