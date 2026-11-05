package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * A picture of whole blocks seen from above with numbers written on some of them, each on a dark
 * tag to stay readable over the dust: the signal it carries. Tooltips are the picture's. Drawing
 * allocates nothing.
 */
final class NumberedPicture implements SheetElement {
  private static final int NUMBER_COLOR = 0xFFFFFFFF;
  private static final int TAG_COLOR = 0xD0000000;
  private static final int TAG_PADDING = 1;

  private final SheetElement picture;
  private final Font font;
  private final FormattedCharSequence[] numbers;
  private final int[] numberWidth;
  private final int[] numberX;
  private final int[] numberY;

  /** {@code value} written in the middle of the block at {@code column}, {@code row}. */
  record CellNumber(int column, int row, int value) {}

  NumberedPicture(SheetElement picture, Font font, int cell, List<CellNumber> cells) {
    this.picture = picture;
    this.font = font;
    this.numbers = new FormattedCharSequence[cells.size()];
    this.numberWidth = new int[cells.size()];
    this.numberX = new int[cells.size()];
    this.numberY = new int[cells.size()];
    for (int index = 0; index < numbers.length; index++) {
      CellNumber number = cells.get(index);
      numbers[index] = Component.literal(Integer.toString(number.value())).getVisualOrderText();
      numberWidth[index] = font.width(numbers[index]);
      numberX[index] = number.column() * cell + (cell - numberWidth[index]) / 2;
      numberY[index] = number.row() * cell + (cell - font.lineHeight) / 2;
    }
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
    for (int index = 0; index < numbers.length; index++) {
      int left = x + numberX[index];
      int top = y + numberY[index];
      graphics.fill(
          left - TAG_PADDING,
          top - TAG_PADDING,
          left + numberWidth[index] + TAG_PADDING,
          top + font.lineHeight,
          TAG_COLOR);
      graphics.text(font, numbers[index], left, top, NUMBER_COLOR, true);
    }
  }

  @Override
  public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
    return picture.tooltipAt(localX, localY, picture.minWidth());
  }
}
