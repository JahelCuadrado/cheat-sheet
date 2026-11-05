package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.sheet.ElementRow;
import com.einsteinmods.cheatsheet.client.sheet.SheetContext;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import com.einsteinmods.cheatsheet.client.sheet.SheetPalette;
import com.einsteinmods.cheatsheet.client.sheet.TextLabel;
import com.einsteinmods.cheatsheet.core.mobfarms.FortressPiece;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * The wither skeleton farm's table of fortress pieces: each piece's floor plan and how far its box
 * reaches under and over the floor level ({@link FortressPiece}), then how to read it.
 */
final class FortressPiecesTable {
  private static final int ROW_GAP = 2;
  private static final int CELL_GAP = 6;
  private static final String RANGE = "–";
  private static final String TIMES = " × ";

  private FortressPiecesTable() {}

  static SheetElement build(SheetContext context, int width) {
    Font font = context.font();
    List<Component[]> table = new ArrayList<>(FortressPiece.ALL.size() + 1);
    table.add(
        new Component[] {
          bold(context, "piece"),
          bold(context, "plan"),
          bold(context, "below"),
          bold(context, "above")
        });
    for (FortressPiece piece : FortressPiece.ALL) {
      table.add(
          new Component[] {
            RedstoneSheet.text(context, "farm.fortress.piece." + piece.key()),
            Component.literal(piece.width() + TIMES + piece.depth()),
            range(piece.lowestFloor(), piece.highestFloor()),
            range(piece.lowestAbove(), piece.highestAbove())
          });
    }
    int[] widths = new int[table.getFirst().length];
    for (Component[] row : table) {
      for (int column = 0; column < row.length; column++) {
        widths[column] = Math.max(widths[column], font.width(row[column]));
      }
    }
    List<SheetElement> rows = new ArrayList<>(table.size() + 2);
    rows.add(
        context.paragraph(
            RedstoneSheet.text(context, "farm.fortress.title")
                .copy()
                .withStyle(ChatFormatting.BOLD),
            width,
            SheetPalette.TEXT));
    for (Component[] row : table) {
      List<SheetElement> cells = new ArrayList<>(row.length);
      for (int column = 0; column < row.length; column++) {
        int color = column == 0 ? SheetPalette.TEXT : SheetPalette.HIGHLIGHT_TEXT;
        cells.add(
            new LeftAligned(
                new TextLabel(font, row[column].getVisualOrderText(), color, 1, null),
                widths[column]));
      }
      rows.add(new ElementRow(cells, CELL_GAP));
    }
    rows.add(
        context.paragraph(
            RedstoneSheet.text(context, "farm.fortress.note"), width, SheetPalette.MUTED_TEXT));
    return new LeftColumn(rows, ROW_GAP);
  }

  private static Component bold(SheetContext context, String column) {
    return RedstoneSheet.text(context, "farm.fortress." + column)
        .copy()
        .withStyle(ChatFormatting.BOLD);
  }

  private static Component range(int lowest, int highest) {
    return Component.literal(
        lowest == highest ? Integer.toString(lowest) : lowest + RANGE + highest);
  }
}
