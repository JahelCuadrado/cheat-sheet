package com.einsteinmods.cheatsheet.client.sheet;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * What a {@link SheetSource} builds from, plus shortcuts for the common elements so a tab reads as
 * a description of its cards. Only used while building, never while drawing.
 *
 * @param minecraft the client: player, level and its registries (null outside a world)
 * @param font the GUI font
 */
public record SheetContext(Minecraft minecraft, Font font) {
  /** Scale of big icons and numbers. */
  public static final int BIG = 2;

  private static final int NORMAL = 1;
  private static final String TAB_KEY_PREFIX = "cheatsheet.tab.";

  /** Stack of the item with this id ({@code minecraft:diamond}); air if unknown. */
  public ItemStack stack(String itemId) {
    return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(itemId)));
  }

  /** Text of a tab's lang key: {@code cheatsheet.tab.<tabId>.<key>}. */
  public Component tabText(String tabId, String key, Object... args) {
    return Component.translatable(TAB_KEY_PREFIX + tabId + "." + key, args);
  }

  /** An item icon; its tooltip is the item's name followed by {@code details}. */
  public ItemIcon item(ItemStack stack, int scale, Component... details) {
    return new ItemIcon(stack, scale, Tooltips.ofItem(font, stack, List.of(details)));
  }

  public TextLabel label(Component text, int color) {
    return new TextLabel(font, text.getVisualOrderText(), color, NORMAL, null);
  }

  /** A 2x label: big numbers, recipe signs. */
  public TextLabel bigLabel(Component text, int color) {
    return new TextLabel(font, text.getVisualOrderText(), color, BIG, null);
  }

  public Paragraph paragraph(Component text, int wrapWidth, int color) {
    return new Paragraph(font, text, wrapWidth, color);
  }

  public ElementRow row(SheetElement... elements) {
    return new ElementRow(List.of(elements), ElementRow.GAP);
  }

  public Card card(Component title, CardTint tint, SheetElement... elements) {
    return new Card(font, title, tint, List.of(elements));
  }
}
