package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.CheatSheetClient;
import com.einsteinmods.cheatsheet.client.sheet.ItemIcon;
import com.einsteinmods.cheatsheet.client.sheet.SheetContext;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import com.einsteinmods.cheatsheet.client.sheet.SheetPalette;
import com.einsteinmods.cheatsheet.core.ores.Ore;
import com.einsteinmods.cheatsheet.core.ores.OreDimension;
import com.einsteinmods.cheatsheet.core.ores.OreProfile;
import com.einsteinmods.cheatsheet.core.ores.OreRow;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Height chart of one dimension: a column per ore (its block on top, the best height in big numbers
 * below it) over a Y axis, with a bar spanning the heights where the ore generates, brighter where
 * it is more common, a white tick at the best height (grey: the best height in mountains), a
 * striped strip beside the bar where one biome has extra ore (that biome's block in miniature at
 * its top; its name and heights on hover), and a yellow line at the player's height.
 *
 * <p>Everything is resolved when built: bar segments (runs of equal brightness, so a column is a
 * handful of fills), labels and tooltips. Drawing only does arithmetic and draw calls.
 */
final class OreHeightChart implements SheetElement {
  private static final int AXIS_LABEL_GAP = 3;
  private static final int MIN_COLUMN_WIDTH = 22;
  private static final int PREFERRED_COLUMN_WIDTH = 42;

  /**
   * Space kept between the best-height labels of neighbouring columns, so "10–117 10–117" reads as
   * two numbers.
   */
  private static final int BEST_LABEL_GAP = 4;

  /** Columns at least this wide draw the ore at 2x (32 px) with a pixel each side. */
  private static final int BIG_COLUMN_WIDTH = 34;

  private static final int HEADER_GAP = 2;
  private static final int BLOCKS_PER_PIXEL = 2;
  private static final int BAR_WIDTH = 10;
  private static final int MARKER_OVERHANG = 3;
  private static final int BONUS_STRIP_GAP = 2;
  private static final int BONUS_STRIP_WIDTH = 4;

  /**
   * The bonus strip's stripes: a white sprite (solid bands on a faint ground) tiled down the strip
   * and tinted with the ore's colour, so a strip is one or two blits whatever its height.
   */
  private static final Identifier STRIPES =
      Identifier.fromNamespaceAndPath("cheatsheet", "ore_bonus_stripes");

  /** The biome's block at the strip's top: half an item, in sheet pixels. */
  private static final int BONUS_ICON_SIZE = ItemIcon.ITEM_SIZE / 2;

  private static final float BONUS_ICON_SCALE = 0.5F;

  /** Hovering this close to the strip shows its tooltip. */
  private static final int BONUS_HOVER_SLACK = 1;

  private static final int PLAYER_HOVER_SLACK = 2;

  /** Bar brightness steps; more steps only add fills. */
  private static final int DENSITY_LEVELS = 8;

  /** Brightness of the faintest step (rarest heights). */
  private static final float MIN_BRIGHTNESS = 0.3F;

  private static final int BAR_BASE = 0xFF202028;
  private static final int BEST_MARKER = 0xFFFFFFFF;
  private static final int MOUNTAIN_MARKER = 0xFF9A9AA6;
  private static final int PLAYER_LABEL_BACKGROUND = 0xD0000000;

  /** Below Y 0: deepslate. */
  private static final int DEEPSLATE_SHADE = 0x40000000;

  /** At and below Y 31 the Nether is a lava sea. */
  private static final int NETHER_LAVA_SEA_Y = 31;

  private static final int LAVA_TINT = 0x30FF6A00;
  private static final int OVERWORLD_GRID_STEP = 64;
  private static final int NETHER_GRID_STEP = 32;

  private final Font font;
  private final List<Column> columns;
  private final int minColumnWidth;
  private final int widestLabel;
  private final int axisWidth;
  private final int headerHeight;
  private final int iconAreaHeight;
  private final int chartHeight;
  private final int[] gridRows;
  private final FormattedCharSequence[] gridLabels;
  private final int[] gridLabelWidths;
  private final int bandTop;
  private final int bandBottom;
  private final int bandColor;
  private final int playerRow;
  private final @Nullable FormattedCharSequence playerLabel;
  private final int playerLabelWidth;
  private final @Nullable List<FormattedCharSequence> playerTooltip;

  /** One ore's column, resolved at build time. Pixel rows are relative to the chart's top. */
  record Column(
      ItemStack icon,
      FormattedCharSequence bestText,
      int bestTextWidth,
      int[] segmentTops,
      int[] segmentBottoms,
      int[] segmentColors,
      int bonusTop,
      int bonusBottom,
      int bonusColor,
      ItemStack bonusIcon,
      @Nullable List<FormattedCharSequence> bonusTooltip,
      int bestRow,
      int mountainRow,
      List<FormattedCharSequence> tooltip) {}

  /** No row: the column has no such marker or strip. */
  private static final int NONE = -1;

  private OreHeightChart(
      SheetContext context,
      OreDimension dimension,
      List<Column> columns,
      OptionalInt playerY,
      Component playerText) {
    this.font = context.font();
    this.columns = List.copyOf(columns);
    // A column never gets narrower than its 1x best-height label ("10–117"), or labels overlap.
    int widestLabel = 0;
    for (Column column : this.columns) {
      widestLabel = Math.max(widestLabel, column.bestTextWidth());
    }
    this.widestLabel = widestLabel;
    this.minColumnWidth = Math.max(MIN_COLUMN_WIDTH, widestLabel + BEST_LABEL_GAP);
    this.iconAreaHeight = ItemIcon.ITEM_SIZE * SheetContext.BIG;
    this.headerHeight =
        iconAreaHeight + HEADER_GAP + font.lineHeight * SheetContext.BIG + HEADER_GAP * 2;
    this.chartHeight = dimension.heightLayers() / BLOCKS_PER_PIXEL;

    int step = dimension == OreDimension.OVERWORLD ? OVERWORLD_GRID_STEP : NETHER_GRID_STEP;
    List<Integer> gridHeights = new ArrayList<>();
    for (int y = Math.floorDiv(dimension.minY(), step) * step;
        y <= dimension.maxY() + 1;
        y += step) {
      if (y >= dimension.minY()) {
        gridHeights.add(y);
      }
    }
    this.gridRows = new int[gridHeights.size()];
    this.gridLabels = new FormattedCharSequence[gridHeights.size()];
    this.gridLabelWidths = new int[gridHeights.size()];
    int widestGridLabel = 0;
    for (int index = 0; index < gridHeights.size(); index++) {
      int y = gridHeights.get(index);
      gridRows[index] = Math.min(chartHeight - 1, boundaryRow(dimension, y));
      gridLabels[index] = Component.literal(Integer.toString(y)).getVisualOrderText();
      gridLabelWidths[index] = font.width(gridLabels[index]);
      widestGridLabel = Math.max(widestGridLabel, gridLabelWidths[index]);
    }
    this.axisWidth = widestGridLabel + AXIS_LABEL_GAP;

    if (dimension == OreDimension.OVERWORLD) {
      this.bandTop = boundaryRow(dimension, Ore.DEEPSLATE_TOP_Y);
      this.bandBottom = chartHeight;
      this.bandColor = DEEPSLATE_SHADE;
    } else {
      this.bandTop = boundaryRow(dimension, NETHER_LAVA_SEA_Y + 1);
      this.bandBottom = chartHeight;
      this.bandColor = LAVA_TINT;
    }

    if (playerY.isPresent()) {
      int y = Math.clamp(playerY.getAsInt(), dimension.minY(), dimension.maxY());
      this.playerRow = layerRow(dimension, y);
      this.playerLabel =
          Component.literal(Integer.toString(playerY.getAsInt())).getVisualOrderText();
      this.playerLabelWidth = font.width(playerLabel);
      this.playerTooltip = List.of(playerText.getVisualOrderText());
    } else {
      this.playerRow = NONE;
      this.playerLabel = null;
      this.playerLabelWidth = 0;
      this.playerTooltip = null;
    }
  }

  /**
   * @param columns one per ore, from {@link #column}
   * @param playerY the player's height when they are in this dimension
   * @param playerText tooltip of the player's line
   */
  static OreHeightChart of(
      SheetContext context,
      OreDimension dimension,
      List<Column> columns,
      OptionalInt playerY,
      Component playerText) {
    return new OreHeightChart(context, dimension, columns, playerY, playerText);
  }

  /**
   * Resolves one ore's column.
   *
   * @param color the ore's colour (opaque ARGB)
   * @param bestText printed under the icon, on one line
   * @param tooltip shown over the whole column
   * @param bonusIcon the bonus biome's block, drawn at the top of the striped strip (empty: none)
   * @param bonusTooltip shown over the striped strip (ignored without biome bonus)
   */
  static Column column(
      Font font,
      OreProfile profile,
      ItemStack icon,
      int color,
      Component bestText,
      List<FormattedCharSequence> tooltip,
      ItemStack bonusIcon,
      List<FormattedCharSequence> bonusTooltip) {
    OreDimension dimension = profile.ore().dimension();
    int rows = dimension.heightLayers() / BLOCKS_PER_PIXEL;
    List<int[]> segments = new ArrayList<>();
    int runLevel = 0;
    int runTop = 0;
    for (int row = 0; row <= rows; row++) {
      int level =
          row == rows ? 0 : densityLevel(profile, dimension.maxY() - row * BLOCKS_PER_PIXEL);
      if (level != runLevel) {
        if (runLevel > 0) {
          segments.add(new int[] {runTop, row, levelColor(color, runLevel)});
        }
        runLevel = level;
        runTop = row;
      }
    }
    int[] tops = new int[segments.size()];
    int[] bottoms = new int[segments.size()];
    int[] colors = new int[segments.size()];
    for (int index = 0; index < segments.size(); index++) {
      tops[index] = segments.get(index)[0];
      bottoms[index] = segments.get(index)[1];
      colors[index] = segments.get(index)[2];
    }

    int bonusTop = NONE;
    int bonusBottom = NONE;
    for (OreRow bonus : profile.bonusRows()) {
      int top = layerRow(dimension, Math.min(bonus.maxY(), dimension.maxY()));
      int bottom = layerRow(dimension, Math.max(bonus.minY(), dimension.minY())) + 1;
      bonusTop = bonusTop == NONE ? top : Math.min(bonusTop, top);
      bonusBottom = Math.max(bonusBottom, bottom);
    }
    int bestRow = profile.best().isFlat() ? NONE : layerRow(dimension, profile.best().y());
    int mountainRow =
        profile.mountainBestY().isPresent()
            ? layerRow(dimension, profile.mountainBestY().getAsInt())
            : NONE;
    FormattedCharSequence best = bestText.getVisualOrderText();
    return new Column(
        icon,
        best,
        font.width(best),
        tops,
        bottoms,
        colors,
        bonusTop,
        bonusBottom,
        color,
        bonusIcon,
        bonusTop == NONE ? null : List.copyOf(bonusTooltip),
        bestRow,
        mountainRow,
        List.copyOf(tooltip));
  }

  /** Width with every ore column {@code column} pixels wide. */
  int widthFor(int column) {
    return axisWidth + column * columns.size();
  }

  /** The narrowest column that draws the ore at 2x and every best height on one line ("10–117"). */
  int columnWidth() {
    return Math.max(BIG_COLUMN_WIDTH, widestLabel + BEST_LABEL_GAP);
  }

  /** Ore columns. */
  int columns() {
    return columns.size();
  }

  @Override
  public int minWidth() {
    return axisWidth + minColumnWidth * columns.size();
  }

  @Override
  public int width(int available) {
    int preferred = axisWidth + Math.max(PREFERRED_COLUMN_WIDTH, minColumnWidth) * columns.size();
    return Math.clamp(available, minWidth(), preferred);
  }

  @Override
  public int height() {
    // Axis labels are centred on their line: the bottom one hangs half a line below the chart.
    return headerHeight + chartHeight + font.lineHeight / 2;
  }

  @Override
  public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
    extract(graphics, x, y, width, true);
  }

  /** Whether every best-height label fits at 2x, with a gap, in columns this wide. */
  boolean fitsBigLabels(int columnWidth) {
    return columnWidth >= BIG_COLUMN_WIDTH
        && widestLabel * SheetContext.BIG + BEST_LABEL_GAP <= columnWidth;
  }

  /**
   * Draws the chart {@code width} wide; labels go 2x only when {@code bigLabels} allows it (charts
   * shown together use one label size).
   */
  void extract(GuiGraphicsExtractor graphics, int x, int y, int width, boolean bigLabels) {
    int columnsWidth = width - axisWidth;
    int columnWidth = columnsWidth / columns.size();
    int columnsLeft = x + axisWidth + (columnsWidth - columnWidth * columns.size()) / 2;
    int right = x + width;
    int chartTop = y + headerHeight;
    int chartBottom = chartTop + chartHeight;
    boolean big = columnWidth >= BIG_COLUMN_WIDTH;
    // One label size for the whole chart: 2x only when the widest label fits with a gap.
    boolean bigText = bigLabels && fitsBigLabels(columnWidth);

    graphics.fill(x + axisWidth, chartTop, right, chartBottom, SheetPalette.CHART_BACKGROUND);
    graphics.fill(x + axisWidth, chartTop + bandTop, right, chartTop + bandBottom, bandColor);
    for (int index = 0; index < gridRows.length; index++) {
      int lineY = chartTop + gridRows[index];
      graphics.horizontalLine(x + axisWidth, right - 1, lineY, SheetPalette.GRID_LINE);
      graphics.text(
          font,
          gridLabels[index],
          x + axisWidth - AXIS_LABEL_GAP - gridLabelWidths[index],
          lineY - font.lineHeight / 2,
          SheetPalette.MUTED_TEXT,
          false);
    }

    for (int index = 0; index < columns.size(); index++) {
      extractColumn(
          graphics,
          columns.get(index),
          columnsLeft + index * columnWidth,
          columnWidth,
          y,
          big,
          bigText);
    }

    if (playerLabel != null) {
      int lineY = chartTop + playerRow;
      graphics.horizontalLine(x + axisWidth, right - 1, lineY, SheetPalette.PLAYER_MARKER);
      int labelTop = lineY - font.lineHeight / 2;
      graphics.fill(
          x, labelTop - 1, x + axisWidth - 1, labelTop + font.lineHeight, PLAYER_LABEL_BACKGROUND);
      graphics.text(
          font,
          playerLabel,
          x + axisWidth - AXIS_LABEL_GAP - playerLabelWidth,
          labelTop,
          SheetPalette.PLAYER_MARKER,
          false);
    }
  }

  private void extractColumn(
      GuiGraphicsExtractor graphics,
      Column column,
      int left,
      int width,
      int top,
      boolean big,
      boolean bigText) {
    int center = left + width / 2;
    int iconScale = big ? SheetContext.BIG : 1;
    int iconSize = ItemIcon.ITEM_SIZE * iconScale;
    int iconX = center - iconSize / 2;
    int iconY = top + (iconAreaHeight - iconSize) / 2;
    if (big) {
      CheatSheetClient.scaledItemIcons().draw(graphics, column.icon(), iconX, iconY, iconScale);
    } else {
      graphics.item(column.icon(), iconX, iconY);
    }

    extractBestText(graphics, column, center, top + iconAreaHeight + HEADER_GAP, bigText);

    int chartTop = top + headerHeight;

    int barLeft = center - BAR_WIDTH / 2;
    int barRight = barLeft + BAR_WIDTH;
    int[] tops = column.segmentTops();
    int[] bottoms = column.segmentBottoms();
    int[] colors = column.segmentColors();
    for (int index = 0; index < tops.length; index++) {
      graphics.fill(
          barLeft, chartTop + tops[index], barRight, chartTop + bottoms[index], colors[index]);
    }
    if (column.bonusTop() != NONE) {
      int stripLeft = barRight + BONUS_STRIP_GAP;
      int stripTop = chartTop + column.bonusTop();
      extractStrip(
          graphics, stripLeft, stripTop, chartTop + column.bonusBottom(), column.bonusColor());
      if (!column.bonusIcon().isEmpty()) {
        int bonusIconY = chartTop + bonusIconTop(column.bonusTop());
        CheatSheetClient.scaledItemIcons()
            .draw(
                graphics,
                column.bonusIcon(),
                bonusIconLeft(stripLeft),
                bonusIconY,
                BONUS_ICON_SCALE);
      }
    }
    if (column.mountainRow() != NONE) {
      graphics.horizontalLine(
          barLeft - MARKER_OVERHANG,
          barRight + MARKER_OVERHANG - 1,
          chartTop + column.mountainRow(),
          MOUNTAIN_MARKER);
    }
    if (column.bestRow() != NONE) {
      graphics.horizontalLine(
          barLeft - MARKER_OVERHANG,
          barRight + MARKER_OVERHANG - 1,
          chartTop + column.bestRow(),
          BEST_MARKER);
    }
  }

  /**
   * A strip striped in {@code color} from {@code top} to {@code bottom}; also the legend's sample.
   */
  static void extractStrip(
      GuiGraphicsExtractor graphics, int left, int top, int bottom, int color) {
    graphics.blitSprite(
        RenderPipelines.GUI_TEXTURED, STRIPES, left, top, BONUS_STRIP_WIDTH, bottom - top, color);
  }

  /** Width of the striped strip, for the legend's sample. */
  static int stripWidth() {
    return BONUS_STRIP_WIDTH;
  }

  /** The biome icon sits just above the strip, or over its top where the chart has no room. */
  private static int bonusIconTop(int bonusTop) {
    return bonusTop > BONUS_ICON_SIZE ? bonusTop - BONUS_ICON_SIZE - 1 : bonusTop;
  }

  /** The biome icon is centred on the strip. */
  private static int bonusIconLeft(int stripLeft) {
    return stripLeft + (BONUS_STRIP_WIDTH - BONUS_ICON_SIZE) / 2;
  }

  /** The best height on one line, at 2x when {@code big}, centred in the label's area. */
  private void extractBestText(
      GuiGraphicsExtractor graphics, Column column, int center, int areaTop, boolean big) {
    int textScale = big ? SheetContext.BIG : 1;
    int textX = center - column.bestTextWidth() * textScale / 2;
    int textY = areaTop + (font.lineHeight * SheetContext.BIG - font.lineHeight * textScale) / 2;
    if (textScale == 1) {
      graphics.text(font, column.bestText(), textX, textY, SheetPalette.HIGHLIGHT_TEXT, true);
      return;
    }
    graphics.pose().pushMatrix();
    graphics.pose().translate(textX, textY);
    graphics.pose().scale(textScale, textScale);
    graphics.text(font, column.bestText(), 0, 0, SheetPalette.HIGHLIGHT_TEXT, true);
    graphics.pose().popMatrix();
  }

  @Override
  public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
    if (playerTooltip != null
        && Math.abs(localY - headerHeight - playerRow) <= PLAYER_HOVER_SLACK) {
      return playerTooltip;
    }
    int columnsWidth = width - axisWidth;
    int columnWidth = columnsWidth / columns.size();
    int offset = localX - axisWidth - (columnsWidth - columnWidth * columns.size()) / 2;
    if (offset < 0 || localY >= headerHeight + chartHeight) {
      return null;
    }
    int index = offset / columnWidth;
    if (index >= columns.size()) {
      return null;
    }
    Column column = columns.get(index);
    List<FormattedCharSequence> bonus = column.bonusTooltip();
    if (bonus != null && overStrip(column, offset - index * columnWidth, columnWidth, localY)) {
      return bonus;
    }
    return column.tooltip();
  }

  /** Whether ({@code columnX}, {@code localY}) is on the column's striped strip or its icon. */
  private boolean overStrip(Column column, int columnX, int columnWidth, int localY) {
    int stripLeft = columnWidth / 2 - BAR_WIDTH / 2 + BAR_WIDTH + BONUS_STRIP_GAP;
    int iconLeft = bonusIconLeft(stripLeft);
    int left = Math.min(stripLeft, iconLeft) - BONUS_HOVER_SLACK;
    int right =
        Math.max(stripLeft + BONUS_STRIP_WIDTH, iconLeft + BONUS_ICON_SIZE) + BONUS_HOVER_SLACK;
    int chartY = localY - headerHeight;
    return columnX >= left
        && columnX < right
        && chartY >= bonusIconTop(column.bonusTop())
        && chartY < column.bonusBottom();
  }

  /** Pixel row (from the chart's top) holding layer {@code y}. */
  private static int layerRow(OreDimension dimension, int y) {
    return (dimension.maxY() - y) / BLOCKS_PER_PIXEL;
  }

  /** Pixel row of the line between layer {@code y} and the one below it. */
  private static int boundaryRow(OreDimension dimension, int y) {
    return (dimension.maxY() + 1 - y) / BLOCKS_PER_PIXEL;
  }

  /** Brightness step of the pixel row whose top layer is {@code topY}: 0 for no ore. */
  private static int densityLevel(OreProfile profile, int topY) {
    double density = 0.0;
    for (int layer = 0; layer < BLOCKS_PER_PIXEL; layer++) {
      density = Math.max(density, profile.relativeDensityAt(topY - layer));
    }
    if (density <= 0.0) {
      return 0;
    }
    return 1 + (int) Math.min(DENSITY_LEVELS - 1, Math.floor(density * DENSITY_LEVELS));
  }

  private static int levelColor(int color, int level) {
    float brightness =
        MIN_BRIGHTNESS + (1.0F - MIN_BRIGHTNESS) * (level - 1) / (DENSITY_LEVELS - 1);
    return ARGB.srgbLerp(brightness, BAR_BASE, color);
  }
}
