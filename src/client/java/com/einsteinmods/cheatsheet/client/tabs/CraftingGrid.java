package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.CheatSheetClient;
import com.einsteinmods.cheatsheet.client.sheet.ItemIcon;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import com.einsteinmods.cheatsheet.client.sheet.SheetPalette;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * A crafting table recipe as the game shows it: the 3x3 grid of slots with the items in place, an
 * arrow and the result at 2x. Every item has its own tooltip. Drawing is a few fills and one item
 * per filled slot; it allocates nothing.
 */
final class CraftingGrid implements SheetElement {
  /** Side of the crafting grid. */
  static final int SIDE = 3;

  private static final int SLOT = ItemIcon.ITEM_SIZE + 2;
  private static final int SLOT_GAP = 1;
  private static final int GRID = SIDE * SLOT + (SIDE - 1) * SLOT_GAP;
  private static final int ARROW_GAP = 4;
  private static final int RESULT_SCALE = 2;
  private static final int RESULT = ItemIcon.ITEM_SIZE * RESULT_SCALE;
  private static final int SLOT_FILL = 0x66000000;
  private static final int SLOT_EDGE = 0x30FFFFFF;
  private static final String ARROW = "→";

  /** One slot of the grid: its item ({@link ItemStack#EMPTY} for none) and that item's tooltip. */
  record Slot(ItemStack stack, List<FormattedCharSequence> tooltip) {
    static final Slot EMPTY = new Slot(ItemStack.EMPTY, List.of());

    Slot {
      tooltip = List.copyOf(tooltip);
    }
  }

  private final Font font;
  private final Slot[] slots;
  private final ItemStack result;
  private final List<FormattedCharSequence> resultTooltip;
  private final FormattedCharSequence arrow;
  private final int arrowWidth;

  /** {@code slots}: nine, row by row. */
  CraftingGrid(
      Font font, List<Slot> slots, ItemStack result, List<FormattedCharSequence> resultTooltip) {
    if (slots.size() != SIDE * SIDE) {
      throw new IllegalArgumentException("a crafting grid has " + SIDE * SIDE + " slots");
    }
    this.font = font;
    this.slots = slots.toArray(new Slot[0]);
    this.result = result;
    this.resultTooltip = List.copyOf(resultTooltip);
    this.arrow = Component.literal(ARROW).getVisualOrderText();
    this.arrowWidth = font.width(arrow);
  }

  @Override
  public int minWidth() {
    return GRID + ARROW_GAP * 2 + arrowWidth + ARROW_GAP + RESULT;
  }

  @Override
  public int height() {
    return GRID;
  }

  @Override
  public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
    for (int index = 0; index < slots.length; index++) {
      int slotX = x + slotLeft(index);
      int slotY = y + slotTop(index);
      graphics.fill(slotX, slotY, slotX + SLOT, slotY + SLOT, SLOT_FILL);
      graphics.horizontalLine(slotX, slotX + SLOT - 1, slotY + SLOT - 1, SLOT_EDGE);
      ItemStack stack = slots[index].stack();
      if (!stack.isEmpty()) {
        CheatSheetClient.scaledItemIcons().drawItem(graphics, stack, slotX + 1, slotY + 1);
      }
    }
    int arrowX = x + GRID + ARROW_GAP * 2;
    graphics.text(
        font, arrow, arrowX, y + (GRID - font.lineHeight) / 2 + 1, SheetPalette.MUTED_TEXT, true);
    CheatSheetClient.scaledItemIcons()
        .draw(graphics, result, x + resultLeft(), y + (GRID - RESULT) / 2, RESULT_SCALE);
  }

  @Override
  public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
    if (localX >= resultLeft()) {
      return resultTooltip;
    }
    for (int index = 0; index < slots.length; index++) {
      int slotX = localX - slotLeft(index);
      int slotY = localY - slotTop(index);
      if (slotX >= 0 && slotX < SLOT && slotY >= 0 && slotY < SLOT) {
        List<FormattedCharSequence> tooltip = slots[index].tooltip();
        return tooltip.isEmpty() ? null : tooltip;
      }
    }
    return null;
  }

  private int resultLeft() {
    return GRID + ARROW_GAP * 2 + arrowWidth + ARROW_GAP;
  }

  private static int slotLeft(int index) {
    return (index % SIDE) * (SLOT + SLOT_GAP);
  }

  private static int slotTop(int index) {
    return (index / SIDE) * (SLOT + SLOT_GAP);
  }
}
