package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.sheet.CardTint;
import com.einsteinmods.cheatsheet.client.sheet.Panels;
import com.einsteinmods.cheatsheet.client.sheet.Scissor;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * A card of a size chosen by its tab, with every piece at a fixed position: cards built alike line
 * up piece by piece, and a missing piece leaves its place blank. Pieces are grouped in rows (a
 * header, a villager level); drawing skips rows outside the scissor, puts each row in its own GUI
 * stratum (vanilla's placement scan stays within one row) and allocates nothing.
 */
final class FixedCard implements SheetElement {
  private final int width;
  private final int height;
  private final int fill;
  private final int border;
  private final SheetElement[][] pieces;
  private final int[][] pieceXs;
  private final int[][] pieceYs;
  private final int[] rowTops;
  private final int[] rowHeights;

  private FixedCard(Builder builder, int width, int height) {
    this.width = width;
    this.height = height;
    this.fill = builder.tint.fill();
    this.border = builder.tint.border();
    int rows = builder.rows.size();
    this.pieces = new SheetElement[rows][];
    this.pieceXs = new int[rows][];
    this.pieceYs = new int[rows][];
    this.rowTops = new int[rows];
    this.rowHeights = new int[rows];
    for (int row = 0; row < rows; row++) {
      List<Placed> members = builder.rows.get(row);
      pieces[row] = new SheetElement[members.size()];
      pieceXs[row] = new int[members.size()];
      pieceYs[row] = new int[members.size()];
      int top = Integer.MAX_VALUE;
      int bottom = Integer.MIN_VALUE;
      for (int index = 0; index < members.size(); index++) {
        Placed placed = members.get(index);
        pieces[row][index] = placed.element();
        pieceXs[row][index] = placed.x();
        pieceYs[row][index] = placed.y();
        top = Math.min(top, placed.y());
        bottom = Math.max(bottom, placed.y() + placed.element().height());
      }
      rowTops[row] = members.isEmpty() ? 0 : top;
      rowHeights[row] = members.isEmpty() ? 0 : bottom - top;
    }
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
    if (Scissor.touches(graphics, x, y, height)) {
      Panels.rounded(graphics, x, y, this.width, height, fill, border);
    }
    for (int row = 0; row < pieces.length; row++) {
      if (rowHeights[row] == 0
          || !Scissor.touches(graphics, x, y + rowTops[row], rowHeights[row])) {
        continue;
      }
      graphics.nextStratum();
      SheetElement[] members = pieces[row];
      for (int index = 0; index < members.length; index++) {
        SheetElement piece = members[index];
        piece.extract(graphics, x + pieceXs[row][index], y + pieceYs[row][index], piece.minWidth());
      }
    }
  }

  @Override
  public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
    for (int row = 0; row < pieces.length; row++) {
      if (localY < rowTops[row] || localY >= rowTops[row] + rowHeights[row]) {
        continue;
      }
      SheetElement[] members = pieces[row];
      for (int index = 0; index < members.length; index++) {
        SheetElement piece = members[index];
        int pieceX = localX - pieceXs[row][index];
        int pieceY = localY - pieceYs[row][index];
        if (pieceX >= 0 && pieceX < piece.minWidth() && pieceY >= 0 && pieceY < piece.height()) {
          List<FormattedCharSequence> tooltip = piece.tooltipAt(pieceX, pieceY, piece.minWidth());
          if (tooltip != null && !tooltip.isEmpty()) {
            return tooltip;
          }
        }
      }
    }
    return null;
  }

  private record Placed(SheetElement element, int x, int y) {}

  /** A plain rectangle: a slot's background, a divider, a bar. No tooltip. */
  record Fill(int width, int height, int color) implements SheetElement {
    @Override
    public int minWidth() {
      return width;
    }

    @Override
    public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
      graphics.fill(x, y, x + this.width, y + height, color);
    }
  }

  /** A cell of {@link IconGrid} (items, counts, signs laid out once) as a card piece. */
  record CellPiece(Font font, IconGrid.Cell cell) implements SheetElement {
    @Override
    public int minWidth() {
      return cell.width();
    }

    @Override
    public int height() {
      return cell.height();
    }

    @Override
    public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
      cell.extract(graphics, font, x, y);
    }

    @Override
    public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
      return cell.tooltip().isEmpty() ? null : cell.tooltip();
    }
  }

  /** Collects pieces row by row, then fixes the card's size. Build time only. */
  static final class Builder {
    private final CardTint tint;
    private final List<List<Placed>> rows = new ArrayList<>();

    Builder(CardTint tint) {
      this.tint = tint;
      rows.add(new ArrayList<>());
    }

    /** Starts a new row: following pieces are drawn in their own stratum, culled together. */
    Builder row() {
      if (!rows.getLast().isEmpty()) {
        rows.add(new ArrayList<>());
      }
      return this;
    }

    /** Adds {@code element} with its top-left corner at ({@code x}, {@code y}) in the card. */
    Builder add(SheetElement element, int x, int y) {
      rows.getLast().add(new Placed(element, x, y));
      return this;
    }

    /** Bottom of the lowest piece so far, to size cards alike. */
    int contentBottom() {
      int bottom = 0;
      for (List<Placed> row : rows) {
        for (Placed placed : row) {
          bottom = Math.max(bottom, placed.y() + placed.element().height());
        }
      }
      return bottom;
    }

    FixedCard build(int width, int height) {
      return new FixedCard(this, width, height);
    }
  }
}
