package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.sheet.ElementRow;
import com.einsteinmods.cheatsheet.client.sheet.ItemIcon;
import com.einsteinmods.cheatsheet.client.sheet.SheetContext;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import com.einsteinmods.cheatsheet.client.sheet.SheetPalette;
import com.einsteinmods.cheatsheet.client.sheet.TextLabel;
import com.einsteinmods.cheatsheet.client.sheet.Tooltips;
import com.einsteinmods.cheatsheet.core.redstone.InstantMining;
import com.einsteinmods.cheatsheet.core.redstone.NetherRoof;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The tables of the Techniques section: the Nether ceiling's bedrock by height ({@link NetherRoof})
 * and which pickaxe and Haste mine a piston at once ({@link InstantMining}).
 */
final class TechniqueTables {
  private static final int ROW_GAP = 2;
  private static final int CELL_GAP = 8;

  private TechniqueTables() {}

  /** Each ceiling layer and its chance of bedrock, the walk layer and the never-bedrock line. */
  static SheetElement roof(SheetContext context, int width) {
    Font font = context.font();
    List<Component[]> table = new ArrayList<>();
    table.add(new Component[] {bold(context, "roof.y"), bold(context, "roof.chance")});
    for (int y = NetherRoof.ALWAYS_Y; y > NetherRoof.NEVER_Y; y--) {
      table.add(
          new Component[] {
            Component.literal(Integer.toString(y)),
            RedstoneSheet.text(context, "roof.percent", NetherRoof.bedrockPercent(y))
          });
    }
    List<SheetElement> rows = new ArrayList<>();
    rows.add(heading(context, "roof.title"));
    rows.addAll(cells(font, table, List.of()));
    rows.add(
        note(
            context,
            RedstoneSheet.text(context, "roof.note", NetherRoof.WALK_Y, NetherRoof.NEVER_Y + 1),
            width));
    return new LeftColumn(rows, ROW_GAP);
  }

  /** Each pickaxe, its Efficiency and Haste, and whether it mines a piston in one tick. */
  static SheetElement instantMining(SheetContext context, int width) {
    Font font = context.font();
    List<Component[]> table = new ArrayList<>();
    List<ItemStack> icons = new ArrayList<>();
    table.add(
        new Component[] {
          bold(context, "mining.pickaxe"),
          bold(context, "mining.haste"),
          bold(context, "mining.instant")
        });
    for (InstantMining row : InstantMining.ROWS) {
      ItemStack pickaxe = RedstoneSheet.stack(context, row.pickaxe());
      icons.add(pickaxe);
      table.add(
          new Component[] {
            RedstoneSheet.text(context, "mining.efficiency", row.efficiency()),
            row.haste() == 0
                ? RedstoneSheet.text(context, "mining.no_haste")
                : RedstoneSheet.text(context, "mining.haste_level", row.haste()),
            RedstoneSheet.text(context, row.instant() ? "sticky.yes" : "mining.no")
                .copy()
                .withStyle(row.instant() ? ChatFormatting.GREEN : ChatFormatting.RED)
          });
    }
    List<SheetElement> rows = new ArrayList<>();
    rows.add(heading(context, "mining.title"));
    rows.addAll(cells(font, table, icons));
    rows.add(note(context, RedstoneSheet.text(context, "mining.note"), width));
    return new LeftColumn(rows, ROW_GAP);
  }

  /** Rows of text in aligned columns; {@code icons}, when given, lead each row after the first. */
  private static List<SheetElement> cells(
      Font font, List<Component[]> table, List<ItemStack> icons) {
    int[] widths = new int[table.getFirst().length];
    for (Component[] row : table) {
      for (int column = 0; column < row.length; column++) {
        widths[column] = Math.max(widths[column], font.width(row[column]));
      }
    }
    int iconWidth = icons.isEmpty() ? 0 : ItemIcon.ITEM_SIZE + CELL_GAP;
    List<SheetElement> rows = new ArrayList<>(table.size());
    for (int index = 0; index < table.size(); index++) {
      Component[] row = table.get(index);
      List<SheetElement> cells = new ArrayList<>(row.length + 1);
      if (iconWidth > 0) {
        cells.add(
            index == 0
                ? new BoxedElement(label(font, Component.empty()), ItemIcon.ITEM_SIZE, 0)
                : new ItemIcon(
                    icons.get(index - 1),
                    1,
                    Tooltips.ofItem(font, icons.get(index - 1), List.of())));
      }
      for (int column = 0; column < row.length; column++) {
        cells.add(new LeftAligned(label(font, row[column]), widths[column]));
      }
      rows.add(new ElementRow(cells, CELL_GAP));
    }
    return rows;
  }

  private static SheetElement heading(SheetContext context, String key) {
    return RedstoneDetails.subheading(context, RedstoneSheet.text(context, key));
  }

  private static SheetElement note(SheetContext context, Component text, int width) {
    return context.paragraph(text, width, SheetPalette.MUTED_TEXT);
  }

  private static Component bold(SheetContext context, String key) {
    return RedstoneSheet.text(context, key).copy().withStyle(ChatFormatting.BOLD);
  }

  private static SheetElement label(Font font, Component text) {
    return new TextLabel(font, text.getVisualOrderText(), SheetPalette.TEXT, 1, null);
  }
}
