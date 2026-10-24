package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.sheet.Card;
import com.einsteinmods.cheatsheet.client.sheet.CardSpot;
import com.einsteinmods.cheatsheet.client.sheet.CardTint;
import com.einsteinmods.cheatsheet.client.sheet.ElementRow;
import com.einsteinmods.cheatsheet.client.sheet.Panels;
import com.einsteinmods.cheatsheet.client.sheet.Paragraph;
import com.einsteinmods.cheatsheet.client.sheet.SheetBody;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import com.einsteinmods.cheatsheet.client.sheet.SheetPalette;
import com.einsteinmods.cheatsheet.client.sheet.ZoomCard;
import com.einsteinmods.cheatsheet.core.sections.BodyShape;
import com.einsteinmods.cheatsheet.core.sections.SectionBody;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * The two height charts of the ores tab (Overworld, Nether) as one section body, each on a dark
 * card panel under its dimension's name, and the legend on a panel of its own below the second
 * chart (its last line a striped sample: a biome bonus): side by side when they fit (the legend in
 * the room the short Nether chart leaves), else one below the other. Each chart's ore columns are
 * as narrow as its ores at 2x and its best heights on one line allow: the charts keep their natural
 * size and never stretch to the sheet width.
 *
 * <p>Drawing is arithmetic on the shape plus the panels' and charts' own draw calls, one GUI
 * stratum per chart; it allocates nothing.
 */
final class OreChartsBody implements SheetBody {
  /** Space between the panels, as between the bonus tiles beside them. */
  private static final int PANEL_GAP = SlotTiles.GAP;

  private static final int PADDING = Card.PADDING;

  /**
   * A chart panel's side padding, a pixel under a card's: the axis labels and the columns already
   * leave room inside, and both charts and the bonus tiles fit a 640-pixel-wide sheet.
   */
  private static final int CHART_SIDE_PADDING = PADDING - 1;

  private static final int TITLE_GAP = 2;
  private static final int LEGEND_LINE_GAP = 2;

  /** The striped sample before the bonus legend: as tall as a line of text, neutral grey. */
  private static final int SAMPLE_COLOR = SheetPalette.MUTED_TEXT;

  private static final int SIDE_BY_SIDE = 2;
  private static final int STACKED = 1;
  private static final int FIRST = 0;
  private static final int SECOND = 1;

  private final ChartPanel first;
  private final ChartPanel second;

  /** The legend under the second chart when side by side: as wide as that chart's panel. */
  private final LegendPanel besideLegend;

  /** The legend under both charts when stacked: as wide as the wider panel. */
  private final LegendPanel stackedLegend;

  private final Geometry geometry;

  /**
   * @param first the chart drawn first (left, or on top)
   * @param legend what the bars and numbers mean
   * @param bonusLegend what the striped strip means, after a striped sample
   */
  OreChartsBody(
      Font font,
      OreHeightChart first,
      Component firstTitle,
      CardTint firstTint,
      OreHeightChart second,
      Component secondTitle,
      CardTint secondTint,
      Component legend,
      Component bonusLegend) {
    // One label size for both charts: 2x only when every label of both fits.
    boolean bigLabels =
        first.fitsBigLabels(first.columnWidth()) && second.fitsBigLabels(second.columnWidth());
    this.first = new ChartPanel(font, first, firstTitle, firstTint, bigLabels);
    this.second = new ChartPanel(font, second, secondTitle, secondTint, bigLabels);
    this.besideLegend = new LegendPanel(font, legend, bonusLegend, this.second.width(), firstTint);
    this.stackedLegend =
        new LegendPanel(
            font,
            legend,
            bonusLegend,
            Math.max(this.first.width(), this.second.width()),
            firstTint);
    this.geometry = new Geometry();
  }

  @Override
  public SectionBody geometry() {
    return geometry;
  }

  @Override
  public void extract(GuiGraphicsExtractor graphics, int x, int y, BodyShape shape) {
    graphics.nextStratum();
    first.extract(graphics, x, y);
    graphics.nextStratum();
    int secondX = x + secondX(shape);
    int secondY = y + secondY(shape);
    second.extract(graphics, secondX, secondY);
    legendFor(shape).extract(graphics, secondX, secondY + second.height() + PANEL_GAP);
  }

  @Override
  public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, BodyShape shape) {
    if (first.contains(localX, localY)) {
      return first.tooltipAt(localX, localY);
    }
    int secondLocalX = localX - secondX(shape);
    int secondLocalY = localY - secondY(shape);
    if (second.contains(secondLocalX, secondLocalY)) {
      return second.tooltipAt(secondLocalX, secondLocalY);
    }
    return null;
  }

  /** Each chart's panel is a card; the legend is not. */
  @Override
  public boolean locateCard(int localX, int localY, BodyShape shape, CardSpot spot) {
    if (first.contains(localX, localY)) {
      spot.set(FIRST, 0, 0, first.width(), first.height());
      return true;
    }
    int secondX = secondX(shape);
    int secondY = secondY(shape);
    if (second.contains(localX - secondX, localY - secondY)) {
      spot.set(SECOND, secondX, secondY, second.width(), second.height());
      return true;
    }
    return false;
  }

  @Override
  public ZoomCard zoomCard(int card, BodyShape shape) {
    return card == FIRST ? first : second;
  }

  private int secondX(BodyShape shape) {
    return shape.split() == SIDE_BY_SIDE ? first.width() + PANEL_GAP : 0;
  }

  private int secondY(BodyShape shape) {
    return shape.split() == SIDE_BY_SIDE ? 0 : first.height() + PANEL_GAP;
  }

  private LegendPanel legendFor(BodyShape shape) {
    return shape.split() == SIDE_BY_SIDE ? besideLegend : stackedLegend;
  }

  /** One chart on a tinted panel, under its bold title; also its zoom. */
  private static final class ChartPanel implements ZoomCard {
    private final Font font;
    private final OreHeightChart chart;
    private final FormattedCharSequence title;
    private final int titleWidth;
    private final CardTint tint;
    private final boolean bigLabels;
    private final int chartWidth;
    private final int titleHeight;

    ChartPanel(Font font, OreHeightChart chart, Component title, CardTint tint, boolean bigLabels) {
      this.font = font;
      this.chart = chart;
      this.title = title.copy().withStyle(ChatFormatting.BOLD).getVisualOrderText();
      this.titleWidth = font.width(this.title);
      this.tint = tint;
      this.bigLabels = bigLabels;
      this.chartWidth = chart.widthFor(chart.columnWidth());
      this.titleHeight = font.lineHeight + TITLE_GAP;
    }

    @Override
    public int width() {
      return Math.max(chartWidth, titleWidth) + CHART_SIDE_PADDING * 2;
    }

    @Override
    public int height() {
      return PADDING * 2 + titleHeight + chart.height();
    }

    boolean contains(int localX, int localY) {
      return localX >= 0 && localX < width() && localY >= 0 && localY < height();
    }

    @Override
    public void extract(GuiGraphicsExtractor graphics, int x, int y) {
      int width = width();
      Panels.rounded(graphics, x, y, width, height(), tint.fill(), tint.border());
      graphics.text(font, title, x + (width - titleWidth) / 2, y + PADDING, tint.title(), true);
      chart.extract(
          graphics, x + CHART_SIDE_PADDING, y + PADDING + titleHeight, chartWidth, bigLabels);
    }

    @Override
    public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY) {
      int chartX = localX - CHART_SIDE_PADDING;
      int chartY = localY - PADDING - titleHeight;
      if (chartX < 0 || chartX >= chartWidth || chartY < 0 || chartY >= chart.height()) {
        return null;
      }
      return chart.tooltipAt(chartX, chartY, chartWidth);
    }
  }

  /** The legend on a tinted panel {@code width} wide, its text wrapped inside. */
  private static final class LegendPanel {
    private final Paragraph legend;
    private final ElementRow bonusLegend;
    private final int width;
    private final int height;
    private final CardTint tint;

    LegendPanel(Font font, Component legend, Component bonusLegend, int width, CardTint tint) {
      int innerWidth = width - PADDING * 2;
      this.legend = new Paragraph(font, legend, innerWidth, SheetPalette.TEXT);
      SheetElement sample = new StripSample(OreHeightChart.stripWidth(), font.lineHeight - 1);
      this.bonusLegend =
          new ElementRow(
              List.of(
                  sample,
                  new Paragraph(
                      font,
                      bonusLegend,
                      innerWidth - sample.minWidth() - ElementRow.GAP,
                      SheetPalette.TEXT)),
              ElementRow.GAP);
      this.width = width;
      this.height =
          PADDING * 2 + this.legend.height() + LEGEND_LINE_GAP + this.bonusLegend.height();
      this.tint = tint;
    }

    int height() {
      return height;
    }

    void extract(GuiGraphicsExtractor graphics, int x, int y) {
      Panels.rounded(graphics, x, y, width, height, tint.fill(), tint.border());
      legend.extract(graphics, x + PADDING, y + PADDING, legend.minWidth());
      bonusLegend.extract(
          graphics,
          x + PADDING,
          y + PADDING + legend.height() + LEGEND_LINE_GAP,
          bonusLegend.minWidth());
    }
  }

  /** A short piece of the striped strip, the legend's sample. */
  private record StripSample(int width, int height) implements SheetElement {
    @Override
    public int minWidth() {
      return width;
    }

    @Override
    public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
      OreHeightChart.extractStrip(graphics, x, y, y + height, SAMPLE_COLOR);
    }
  }

  /** Two shapes: stacked (narrow, tall) and side by side (wide, short). */
  private final class Geometry implements SectionBody {
    @Override
    public List<BodyShape> shapes() {
      return List.of(stacked(), sideBySide());
    }

    private BodyShape stacked() {
      int width = Math.max(first.width(), second.width());
      int height =
          first.height() + PANEL_GAP + second.height() + PANEL_GAP + stackedLegend.height();
      return new BodyShape(STACKED, width, height);
    }

    private BodyShape sideBySide() {
      int width = first.width() + PANEL_GAP + second.width();
      int height = Math.max(first.height(), second.height() + PANEL_GAP + besideLegend.height());
      return new BodyShape(SIDE_BY_SIDE, width, height);
    }
  }
}
