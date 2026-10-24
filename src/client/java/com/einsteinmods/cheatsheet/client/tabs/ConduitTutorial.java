package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.picture.PictureAtlas;
import com.einsteinmods.cheatsheet.client.picture.Scene;
import com.einsteinmods.cheatsheet.client.sheet.CardTint;
import com.einsteinmods.cheatsheet.client.sheet.ElementRow;
import com.einsteinmods.cheatsheet.client.sheet.ItemIcon;
import com.einsteinmods.cheatsheet.client.sheet.SheetContext;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import com.einsteinmods.cheatsheet.client.sheet.SheetPalette;
import com.einsteinmods.cheatsheet.client.sheet.Tooltips;
import com.einsteinmods.cheatsheet.client.sheet.ZoomCard;
import com.einsteinmods.cheatsheet.core.ZoomSize;
import com.einsteinmods.cheatsheet.core.beacon.BeaconDioramas;
import com.einsteinmods.cheatsheet.core.beacon.ConduitFrame;
import com.einsteinmods.cheatsheet.core.beacon.ConduitMaterials;
import com.einsteinmods.cheatsheet.core.beacon.MaterialSource;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The conduit's zoom: a step-by-step card. Four numbered steps, each a picture of the build so far
 * (the water, then one ring more each step) with what to do and the range it reaches; the range at
 * every frame size; then where each material comes from, every source with its icon ({@link
 * ConduitMaterials}). Built on the first click on a conduit tile; its pictures are zoom-only, baked
 * at the zoom's scale.
 */
final class ConduitTutorial {
  /** Steps and materials share these columns. */
  private static final int COLUMNS = 4;

  private static final int COLUMN_WIDTH = 134;
  private static final int COLUMN_GAP = 6;
  private static final int CONTENT_WIDTH = COLUMNS * COLUMN_WIDTH + (COLUMNS - 1) * COLUMN_GAP;

  private static final int PICTURE_SIZE = 52;
  private static final int PART_GAP = 3;
  private static final int SECTION_GAP = 3;
  private static final int LINE_GAP = 2;
  private static final int BADGE_GAP = 3;
  private static final int RANGE_GAP = 8;
  private static final int BADGE_COLOR = SheetPalette.HIGHLIGHT_TEXT;
  private static final CardTint TINT = CardTint.BLUE;
  private static final String RANGE_SIGN = "↔ ";
  private static final String NAMESPACE = "minecraft:";

  /** Every build framed on the full frame's box: one scale, the frame grows step by step. */
  private static final AABB FRAME_BOX =
      new AABB(
          0, 0, 0, BeaconDioramas.FRAME_SIDE, BeaconDioramas.FRAME_SIDE, BeaconDioramas.FRAME_SIDE);

  /** The materials, one column each, in the order the recipes use them. */
  private static final List<String> MATERIALS =
      List.of(
          ConduitMaterials.HEART_OF_THE_SEA,
          ConduitMaterials.NAUTILUS_SHELL,
          ConduitMaterials.PRISMARINE_SHARD,
          ConduitMaterials.PRISMARINE_CRYSTALS);

  private ConduitTutorial() {}

  /** The card; adds its pictures to {@code atlas} as zoom-only. */
  static ZoomCard build(SheetContext context, PictureAtlas atlas) {
    Font font = context.font();
    ItemStack frameBlock = stack(context, ConduitMaterials.FRAME_RECIPES.getFirst().resultId());
    List<SheetElement> steps = new ArrayList<>(ConduitFrame.RINGS + 1);
    for (int rings = 0; rings <= ConduitFrame.RINGS; rings++) {
      steps.add(step(context, atlas, frameBlock, rings));
    }
    List<SheetElement> materials = new ArrayList<>(MATERIALS.size());
    for (String material : MATERIALS) {
      materials.add(material(context, material));
    }
    List<SheetElement> parts =
        List.of(
            title(context, text(context, "conduit.guide.title")),
            new TopRow(steps, COLUMN_WIDTH, COLUMN_GAP),
            ranges(context, frameBlock),
            title(context, text(context, "conduit.guide.materials")),
            new TopRow(materials, COLUMN_WIDTH, COLUMN_GAP));
    List<SheetElement> spaced = new ArrayList<>(parts.size());
    for (SheetElement part : parts) {
      spaced.add(new BoxedElement(part, CONTENT_WIDTH, 0));
    }
    return ZoomViews.panel(new LeftColumn(spaced, SECTION_GAP), TINT, ZoomSize.LARGE);
  }

  /**
   * Step {@code rings + 1}: its number and name, the build after {@code rings} rings (the conduit
   * drawn over the middle) with the frame blocks and range it reaches beside it, and what to do.
   */
  private static SheetElement step(
      SheetContext context, PictureAtlas atlas, ItemStack frameBlock, int rings) {
    Font font = context.font();
    int number = rings + 1;
    Component name = text(context, "conduit.guide.step." + number + ".title");
    int blocks = ConduitFrame.blocksOnRings(rings);
    int range = ConduitFrame.range(blocks);
    Component what =
        switch (rings) {
          case 0 -> text(context, "conduit.guide.step.1");
          case 1 ->
              text(
                  context,
                  "conduit.guide.step.2",
                  ConduitFrame.RING_SIDE,
                  ConduitFrame.RING_SIDE,
                  blocks,
                  ConduitFrame.MIN_ACTIVE_BLOCKS);
          case 2 -> text(context, "conduit.guide.step.3", blocks);
          default ->
              text(context, "conduit.guide.step.4", blocks, range, ConduitFrame.ATTACK_RANGE);
        };
    List<FormattedCharSequence> tooltip = Tooltips.of(font, name, List.of(what));
    SheetElement heading =
        line(
            font,
            new IconGrid.Flow(font)
                .badge(BADGE_COLOR, Component.literal(Integer.toString(number)))
                .gap(BADGE_GAP)
                .text(name.copy().withStyle(ChatFormatting.BOLD), TINT.title())
                .build(tooltip));
    SheetElement picture =
        new ItemOverPicture(
            atlas.addZoomOnly(
                Scene.of(BeaconDioramas.conduitRings(rings)).focusedOn(FRAME_BOX),
                PICTURE_SIZE,
                PICTURE_SIZE,
                tooltip),
            new ItemStack(Items.CONDUIT));
    // Beside the picture: the frame blocks and the range they reach, or "not active yet".
    int besideWidth = COLUMN_WIDTH - PICTURE_SIZE - ElementRow.GAP;
    SheetElement reached =
        range == 0
            ? new CentredText(
                font, text(context, "conduit.guide.inactive"), besideWidth, SheetPalette.MUTED_TEXT)
            : line(font, rangeCell(context, frameBlock, blocks, range));
    return new ZoomViews.CentredColumn(
        List.of(
            new BoxedElement(heading, COLUMN_WIDTH, 0),
            new ElementRow(List.of(picture, reached), ElementRow.GAP),
            new CentredText(font, what, COLUMN_WIDTH, SheetPalette.TEXT)),
        PART_GAP);
  }

  /** "Range at each frame size:" and every step, the frame block with its count and the range. */
  private static SheetElement ranges(SheetContext context, ItemStack frameBlock) {
    Font font = context.font();
    List<SheetElement> cells = new ArrayList<>();
    cells.add(context.label(text(context, "conduit.guide.ranges"), SheetPalette.MUTED_TEXT));
    for (int blocks : ConduitFrame.rangeSteps()) {
      cells.add(line(font, rangeCell(context, frameBlock, blocks, ConduitFrame.range(blocks))));
    }
    return new CentredRows(cells, CONTENT_WIDTH, RANGE_GAP);
  }

  private static IconGrid.Cell rangeCell(
      SheetContext context, ItemStack frameBlock, int blocks, int range) {
    Font font = context.font();
    return new IconGrid.Flow(font)
        .item(frameBlock, Integer.toString(blocks))
        .text(Component.literal(" " + RANGE_SIGN), SheetPalette.MUTED_TEXT)
        .text(Component.literal(Integer.toString(range)), SheetPalette.HIGHLIGHT_TEXT)
        .build(
            Tooltips.of(
                font,
                text(context, "conduit.blocks", blocks),
                List.of(text(context, "conduit.range", range))));
  }

  /** A material's icon and name, then each place it comes from, its icon beside the text. */
  private static SheetElement material(SheetContext context, String itemId) {
    Font font = context.font();
    ItemStack stack = stack(context, itemId);
    List<SheetElement> lines = new ArrayList<>();
    lines.add(
        iconText(
            context,
            new ItemIcon(stack, 1, Tooltips.ofItem(font, stack, List.of())),
            stack.getHoverName().copy().withStyle(ChatFormatting.BOLD),
            TINT.title()));
    for (MaterialSource source : ConduitMaterials.sourcesOf(itemId)) {
      ItemStack icon = stack(context, source.iconId());
      Component where = BeaconSheet.sourceText(context, source);
      lines.add(
          iconText(
              context,
              new ItemIcon(icon, 1, Tooltips.of(font, where, List.of())),
              where,
              SheetPalette.TEXT));
    }
    return new LeftColumn(lines, LINE_GAP);
  }

  /** An icon with text wrapped beside it, within a column. */
  private static SheetElement iconText(
      SheetContext context, SheetElement icon, Component text, int color) {
    return new ElementRow(
        List.of(
            icon, context.paragraph(text, COLUMN_WIDTH - icon.minWidth() - ElementRow.GAP, color)),
        ElementRow.GAP);
  }

  private static SheetElement title(SheetContext context, Component title) {
    return context.paragraph(
        title.copy().withStyle(ChatFormatting.BOLD), CONTENT_WIDTH, TINT.title());
  }

  /** One drawn line (icons, sprites and text side by side) as an element. */
  private static SheetElement line(Font font, IconGrid.Cell cell) {
    return new IconGrid.Builder(font, 1, 0, 0).cell(cell).build();
  }

  private static ItemStack stack(SheetContext context, String itemId) {
    return context.stack(NAMESPACE + itemId);
  }

  private static Component text(SheetContext context, String key, Object... args) {
    return context.tabText(BeaconSheet.ID, key, args);
  }

  /**
   * Elements side by side in columns of one width, top-aligned (each centred across its column).
   * Drawing allocates nothing.
   */
  private static final class TopRow implements SheetElement {
    private final SheetElement[] children;
    private final int columnWidth;
    private final int gap;
    private final int height;

    TopRow(List<SheetElement> children, int columnWidth, int gap) {
      this.children = children.toArray(new SheetElement[0]);
      this.columnWidth = columnWidth;
      this.gap = gap;
      int tallest = 0;
      for (SheetElement child : this.children) {
        tallest = Math.max(tallest, child.height());
      }
      this.height = tallest;
    }

    @Override
    public int minWidth() {
      return children.length * (columnWidth + gap) - gap;
    }

    @Override
    public int height() {
      return height;
    }

    @Override
    public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
      for (int index = 0; index < children.length; index++) {
        SheetElement child = children[index];
        child.extract(graphics, x + childX(index, child), y, child.minWidth());
      }
    }

    @Override
    public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
      int index = localX / (columnWidth + gap);
      if (index >= children.length) {
        return null;
      }
      SheetElement child = children[index];
      int childX = localX - childX(index, child);
      if (childX < 0 || childX >= child.minWidth() || localY >= child.height()) {
        return null;
      }
      return child.tooltipAt(childX, localY, child.minWidth());
    }

    private int childX(int index, SheetElement child) {
      return index * (columnWidth + gap) + Math.max(0, (columnWidth - child.minWidth()) / 2);
    }
  }
}
