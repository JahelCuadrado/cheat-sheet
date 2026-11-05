package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.sheet.ElementRow;
import com.einsteinmods.cheatsheet.client.sheet.SheetContext;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import com.einsteinmods.cheatsheet.client.sheet.SheetPalette;
import com.einsteinmods.cheatsheet.client.sheet.TextLabel;
import com.einsteinmods.cheatsheet.core.redstone.CrafterRules;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * What a comparator reads from a crafter, for every mix of disabled slots and slots holding an item
 * ({@link CrafterRules#comparatorSignal}); both the comparator's and the crafter's zoom show it.
 */
final class CrafterSignalTable {
  private static final int ROW_GAP = 2;
  private static final int CELL_GAP = 3;

  /** The note's example: this many disabled slots, and the filled ones that complete the nine. */
  private static final int EXAMPLE_DISABLED = 5;

  private CrafterSignalTable() {}

  /** The table's title, the table and how to read it, wrapped to {@code width}. */
  static SheetElement build(SheetContext context, int width) {
    Font font = context.font();
    Component disabled = RedstoneSheet.text(context, "crafter_signals.disabled");
    int firstWidth = font.width(disabled);
    int cellWidth = font.width(Integer.toString(CrafterRules.SLOTS));
    List<SheetElement> rows = new ArrayList<>();
    rows.add(
        RedstoneDetails.subheading(context, RedstoneSheet.text(context, "crafter_signals.title")));
    rows.add(
        new ElementRow(
            List.of(
                new BoxedElement(label(font, Component.empty(), SheetPalette.TEXT), firstWidth, 0),
                label(
                    font,
                    RedstoneSheet.text(context, "crafter_signals.filled"),
                    SheetPalette.MUTED_TEXT)),
            CELL_GAP));
    List<SheetElement> header = new ArrayList<>();
    header.add(new BoxedElement(label(font, disabled, SheetPalette.MUTED_TEXT), firstWidth, 0));
    for (int filled = 0; filled <= CrafterRules.SLOTS; filled++) {
      header.add(new BoxedElement(number(font, filled, SheetPalette.HIGHLIGHT_TEXT), cellWidth, 0));
    }
    rows.add(new ElementRow(header, CELL_GAP));
    for (int locked = 0; locked < CrafterRules.SLOTS; locked++) {
      List<SheetElement> cells = new ArrayList<>();
      cells.add(new BoxedElement(number(font, locked, SheetPalette.HIGHLIGHT_TEXT), firstWidth, 0));
      for (int filled = 0; filled <= CrafterRules.SLOTS - locked; filled++) {
        int signal = CrafterRules.comparatorSignal(filled, locked);
        int color = signal == CrafterRules.SLOTS ? SheetPalette.HIGHLIGHT_TEXT : SheetPalette.TEXT;
        cells.add(new BoxedElement(number(font, signal, color), cellWidth, 0));
      }
      rows.add(new ElementRow(cells, CELL_GAP));
    }
    int exampleFilled = CrafterRules.SLOTS - EXAMPLE_DISABLED;
    rows.add(
        context.paragraph(
            RedstoneSheet.text(
                context,
                "crafter_signals.note",
                EXAMPLE_DISABLED,
                exampleFilled,
                CrafterRules.comparatorSignal(exampleFilled, EXAMPLE_DISABLED)),
            width,
            SheetPalette.MUTED_TEXT));
    return new LeftColumn(rows, ROW_GAP);
  }

  private static SheetElement number(Font font, int value, int color) {
    return label(font, Component.literal(Integer.toString(value)), color);
  }

  private static SheetElement label(Font font, Component text, int color) {
    return new TextLabel(font, text.getVisualOrderText(), color, 1, null);
  }
}
