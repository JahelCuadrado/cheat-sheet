package com.einsteinmods.cheatsheet.client.sheet;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/** Tooltip lines built once, when a sheet is built: a title, then grey details wrapped. */
public final class Tooltips {
  /** Details wrap at this width, so long lists stay readable. */
  public static final int WRAP_WIDTH = 220;

  private Tooltips() {}

  public static List<FormattedCharSequence> of(
      Font font, Component title, List<Component> details) {
    List<FormattedCharSequence> lines = new ArrayList<>(details.size() + 1);
    lines.add(title.getVisualOrderText());
    for (Component detail : details) {
      lines.addAll(font.split(detail.copy().withStyle(ChatFormatting.GRAY), WRAP_WIDTH));
    }
    return List.copyOf(lines);
  }

  /** The item's name (in its rarity colour) followed by the details. */
  public static List<FormattedCharSequence> ofItem(
      Font font, ItemStack stack, List<Component> details) {
    return of(font, stack.getStyledHoverName(), details);
  }
}
