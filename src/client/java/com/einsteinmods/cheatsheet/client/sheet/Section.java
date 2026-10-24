package com.einsteinmods.cheatsheet.client.sheet;

import com.einsteinmods.cheatsheet.core.sections.SectionBody;
import com.einsteinmods.cheatsheet.core.sections.SectionsLayout;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * A titled part of a row-based sheet: a bold title over one {@link Table} or {@link TileGrid}.
 * Sections stack top to bottom in reading order; one {@link #besidePrevious()} goes beside the
 * section before it when both fit the width, otherwise below it.
 *
 * @param title the title, laid out once
 * @param titleColor its colour (a tint's title colour)
 * @param body the table or grid
 * @param pairedWithPrevious goes beside the section before when both fit
 */
public record Section(
    FormattedCharSequence title, int titleColor, SheetBody body, boolean pairedWithPrevious) {
  /** Space between a section's title and its body. */
  private static final int TITLE_GAP = 2;

  /** Space between stacked sections. */
  private static final int ROW_GAP = 8;

  /** Space between the two sections of a pair. */
  private static final int PAIR_GAP = 8;

  public static Section of(Component title, CardTint tint, SheetBody body) {
    return new Section(
        title.copy().withStyle(ChatFormatting.BOLD).getVisualOrderText(),
        tint.title(),
        body,
        false);
  }

  /** The same section, placed beside the one before it when both fit. */
  public Section besidePrevious() {
    return new Section(title, titleColor, body, true);
  }

  /** Lays out {@code sections} in {@code width}. Resize and tab change only. */
  public static SectionsLayout layout(List<Section> sections, Font font, int width) {
    List<List<SectionBody>> rows = new ArrayList<>();
    for (Section section : sections) {
      if (section.pairedWithPrevious && !rows.isEmpty() && rows.getLast().size() == 1) {
        rows.getLast().add(section.body.geometry());
      } else {
        rows.add(new ArrayList<>(List.of(section.body.geometry())));
      }
    }
    return SectionsLayout.of(rows, width, spacing(font));
  }

  /** Draws the sections inside the view (between {@code viewTop} and {@code viewBottom}). */
  public static void extract(
      GuiGraphicsExtractor graphics,
      Font font,
      List<Section> sections,
      SectionsLayout layout,
      int left,
      int top,
      int viewTop,
      int viewBottom) {
    int titleHeight = titleHeight(font);
    List<SectionsLayout.PlacedSection> placed = layout.sections();
    for (int index = 0; index < sections.size(); index++) {
      SectionsLayout.PlacedSection where = placed.get(index);
      int y = top + where.y();
      if (y >= viewBottom || y + titleHeight + where.shape().height() <= viewTop) {
        continue;
      }
      Section section = sections.get(index);
      graphics.text(font, section.title, left + where.x(), y, section.titleColor, true);
      section.body.extract(graphics, left + where.x(), y + titleHeight, where.shape());
    }
  }

  /** Tooltip at ({@code x}, {@code y}) from the sheet's corner, or null. */
  public static @Nullable List<FormattedCharSequence> tooltipAt(
      Font font, List<Section> sections, SectionsLayout layout, int x, int y) {
    int titleHeight = titleHeight(font);
    List<SectionsLayout.PlacedSection> placed = layout.sections();
    for (int index = 0; index < sections.size(); index++) {
      SectionsLayout.PlacedSection where = placed.get(index);
      int localX = x - where.x();
      int localY = y - where.y() - titleHeight;
      if (localX >= 0
          && localX < where.shape().width()
          && localY >= 0
          && localY < where.shape().height()) {
        return sections.get(index).body.tooltipAt(localX, localY, where.shape());
      }
    }
    return null;
  }

  /**
   * Finds the card at ({@code x}, {@code y}) from the sheet's corner and writes it into {@code
   * spot}, bounds from the sheet's corner; whether there is one. Allocates nothing.
   */
  public static boolean locateCard(
      Font font, List<Section> sections, SectionsLayout layout, int x, int y, CardSpot spot) {
    int titleHeight = titleHeight(font);
    List<SectionsLayout.PlacedSection> placed = layout.sections();
    for (int index = 0; index < sections.size(); index++) {
      SectionsLayout.PlacedSection where = placed.get(index);
      int localX = x - where.x();
      int localY = y - where.y() - titleHeight;
      if (localX >= 0
          && localX < where.shape().width()
          && localY >= 0
          && localY < where.shape().height()) {
        if (!sections.get(index).body.locateCard(localX, localY, where.shape(), spot)) {
          return false;
        }
        spot.moveInto(index, where.x(), where.y() + titleHeight);
        return true;
      }
    }
    return false;
  }

  /** The card {@link #locateCard} found, to zoom. Once per click. */
  public static ZoomCard zoomCard(List<Section> sections, SectionsLayout layout, CardSpot spot) {
    SectionsLayout.PlacedSection where = layout.sections().get(spot.group());
    return sections.get(spot.group()).body.zoomCard(spot.card(), where.shape());
  }

  private static SectionsLayout.Spacing spacing(Font font) {
    return new SectionsLayout.Spacing(titleHeight(font), ROW_GAP, PAIR_GAP);
  }

  private static int titleHeight(Font font) {
    return font.lineHeight + TITLE_GAP;
  }
}
