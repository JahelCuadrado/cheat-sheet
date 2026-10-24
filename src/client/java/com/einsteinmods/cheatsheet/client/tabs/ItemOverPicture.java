package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.CheatSheetClient;
import com.einsteinmods.cheatsheet.client.sheet.ItemIcon;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * A picture with an item drawn over its middle: for what has no block model to bake (the conduit,
 * drawn by a block entity renderer). Tooltips are the picture's.
 */
final class ItemOverPicture implements SheetElement {
  private final SheetElement picture;
  private final ItemStack item;
  private final int itemX;
  private final int itemY;

  ItemOverPicture(SheetElement picture, ItemStack item) {
    this.picture = picture;
    this.item = item;
    this.itemX = (picture.minWidth() - ItemIcon.ITEM_SIZE) / 2;
    this.itemY = (picture.height() - ItemIcon.ITEM_SIZE) / 2;
  }

  @Override
  public int minWidth() {
    return picture.minWidth();
  }

  @Override
  public int height() {
    return picture.height();
  }

  @Override
  public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
    picture.extract(graphics, x, y, picture.minWidth());
    CheatSheetClient.scaledItemIcons().drawItem(graphics, item, x + itemX, y + itemY);
  }

  @Override
  public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
    return picture.tooltipAt(localX, localY, picture.minWidth());
  }
}
