package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.CheatSheetClient;
import com.einsteinmods.cheatsheet.client.sheet.ItemIcon;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * A cell of a picture grid: icons stacked and centred (a template over its trimmed armour, a disc),
 * an optional 1x badge over the bottom-right corner of the last icon (a trial key: which vault),
 * then a few short centred text lines (length, signal). One tooltip for the whole cell.
 *
 * <p>Positions are resolved when built; drawing is a loop of draw calls.
 */
final class IconCell implements SheetElement {
  private static final int ICON_GAP = 2;
  private static final int TEXT_GAP = 1;

  /** One stacked icon at its scale. */
  record Icon(ItemStack stack, int scale) {}

  /** One text line under the icons. */
  record Line(Component text, int color) {}

  private final Font font;
  private final ItemStack[] icons;
  private final int[] scales;
  private final int[] iconXs;
  private final int[] iconYs;
  private final @Nullable ItemStack badge;
  private final int badgeX;
  private final int badgeY;
  private final FormattedCharSequence[] lines;
  private final int[] lineColors;
  private final int[] lineXs;
  private final int[] lineYs;
  private final int width;
  private final int height;
  private final List<FormattedCharSequence> tooltip;

  IconCell(
      Font font,
      List<Icon> icons,
      @Nullable ItemStack badge,
      List<Line> lines,
      List<FormattedCharSequence> tooltip) {
    if (icons.isEmpty()) {
      throw new IllegalArgumentException("a cell needs an icon");
    }
    this.font = font;
    this.tooltip = List.copyOf(tooltip);
    this.badge = badge;
    this.lines = new FormattedCharSequence[lines.size()];
    this.lineColors = new int[lines.size()];
    int[] lineWidths = new int[lines.size()];
    int widest = 0;
    for (Icon icon : icons) {
      widest = Math.max(widest, ItemIcon.ITEM_SIZE * icon.scale());
    }
    for (int index = 0; index < lines.size(); index++) {
      this.lines[index] = lines.get(index).text().getVisualOrderText();
      this.lineColors[index] = lines.get(index).color();
      lineWidths[index] = font.width(this.lines[index]);
      widest = Math.max(widest, lineWidths[index]);
    }
    this.width = widest;

    this.icons = new ItemStack[icons.size()];
    this.scales = new int[icons.size()];
    this.iconXs = new int[icons.size()];
    this.iconYs = new int[icons.size()];
    int cursorY = 0;
    int lastSize = 0;
    for (int index = 0; index < icons.size(); index++) {
      Icon icon = icons.get(index);
      lastSize = ItemIcon.ITEM_SIZE * icon.scale();
      if (index > 0) {
        cursorY += ICON_GAP;
      }
      this.icons[index] = icon.stack();
      this.scales[index] = icon.scale();
      this.iconXs[index] = (widest - lastSize) / 2;
      this.iconYs[index] = cursorY;
      cursorY += lastSize;
    }
    int lastIcon = icons.size() - 1;
    this.badgeX = iconXs[lastIcon] + lastSize - ItemIcon.ITEM_SIZE;
    this.badgeY = iconYs[lastIcon] + lastSize - ItemIcon.ITEM_SIZE;

    this.lineXs = new int[lines.size()];
    this.lineYs = new int[lines.size()];
    for (int index = 0; index < lines.size(); index++) {
      cursorY += TEXT_GAP;
      lineXs[index] = (widest - lineWidths[index]) / 2;
      lineYs[index] = cursorY;
      cursorY += font.lineHeight;
    }
    this.height = cursorY;
  }

  @Override
  public int minWidth() {
    return width;
  }

  @Override
  public int height() {
    return height;
  }

  @Override
  public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
    for (int index = 0; index < icons.length; index++) {
      int scale = scales[index];
      if (scale == 1) {
        CheatSheetClient.scaledItemIcons()
            .drawItem(graphics, icons[index], x + iconXs[index], y + iconYs[index]);
      } else {
        CheatSheetClient.scaledItemIcons()
            .draw(graphics, icons[index], x + iconXs[index], y + iconYs[index], scale);
      }
    }
    if (badge != null) {
      CheatSheetClient.scaledItemIcons().drawItem(graphics, badge, x + badgeX, y + badgeY);
    }
    for (int index = 0; index < lines.length; index++) {
      graphics.text(
          font, lines[index], x + lineXs[index], y + lineYs[index], lineColors[index], true);
    }
  }

  @Override
  public List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
    return tooltip;
  }
}
