package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.picture.PictureAtlas;
import com.einsteinmods.cheatsheet.client.picture.Scene;
import com.einsteinmods.cheatsheet.client.sheet.ElementRow;
import com.einsteinmods.cheatsheet.client.sheet.ItemIcon;
import com.einsteinmods.cheatsheet.client.sheet.SheetContext;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import com.einsteinmods.cheatsheet.client.sheet.SheetPalette;
import com.einsteinmods.cheatsheet.client.sheet.Tooltips;
import com.einsteinmods.cheatsheet.core.redstone.PistonRules;
import com.einsteinmods.cheatsheet.core.redstone.RedstoneBuild;
import com.einsteinmods.cheatsheet.core.redstone.TransparentBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * What a guide adds under its steps: which blocks are transparent to redstone (each pictured with
 * dust on top where dust can stand), and what a piston and its slime or honey do to each kind of
 * block. Built with the guide, on the first click.
 */
final class RedstoneLists {
  private static final int BLOCK_PICTURE = 30;
  private static final int CELL_GAP = 2;
  private static final int LINE_GAP = 2;
  private static final int GROUP_GAP = 4;
  private static final int TABLE_ROW_GAP = 4;

  /** The piston table's pushed, pulled and dragged columns, each this wide. */
  private static final int STICKY_CELL_WIDTH = 140;

  private static final String NAMESPACE = "minecraft:";

  private RedstoneLists() {}

  /** The lists under {@code build}'s steps, {@code width} wide; empty for most builds. */
  static Optional<SheetElement> of(
      SheetContext context, PictureAtlas atlas, RedstoneBuild build, int width) {
    return switch (build) {
      case TRANSPARENCY -> Optional.of(transparentBlocks(context, atlas, width));
      case STICKY_BLOCKS -> Optional.of(pistonTable(context, width));
      case NETHER_ROOF -> Optional.of(TechniqueTables.roof(context, width));
      case BEDROCK_INSTANT_MINING -> Optional.of(TechniqueTables.instantMining(context, width));
      default -> Optional.empty();
    };
  }

  /** Blocks that carry no power, then blocks that do, each a small picture. */
  private static SheetElement transparentBlocks(
      SheetContext context, PictureAtlas atlas, int width) {
    List<SheetElement> transparent = new ArrayList<>();
    List<SheetElement> solid = new ArrayList<>();
    for (TransparentBlock block : TransparentBlock.ALL) {
      (block.conducts() ? solid : transparent).add(blockPicture(context, atlas, block));
    }
    return new LeftColumn(
        List.of(
            group(context, "transparency.list.transparent", transparent, width),
            group(context, "transparency.list.solid", solid, width),
            note(context, text(context, "transparency.list.dust"), width)),
        GROUP_GAP);
  }

  /** One block with dust on top where dust stands; its tooltip says what it carries and holds. */
  private static SheetElement blockPicture(
      SheetContext context, PictureAtlas atlas, TransparentBlock block) {
    ItemStack stack = RedstoneSheet.stack(context, block.itemId());
    MutableComponent name = stack.getHoverName().copy();
    if (block.half() != TransparentBlock.Half.WHOLE) {
      name.append(" ").append(text(context, "transparency.half." + block.half().key()));
    }
    List<FormattedCharSequence> tooltip =
        Tooltips.of(
            context.font(),
            name,
            List.of(
                text(context, block.conducts() ? "transparency.conducts" : "transparency.no_power"),
                text(
                    context, block.holdsDust() ? "transparency.dust_on" : "transparency.no_dust")));
    // Framed on the block and the cell above it, so blocks without dust keep the same scale.
    Scene scene = Scene.of(block.picture()).focusedOn(new AABB(0, 0, 0, 1, 2, 1));
    return atlas.addZoomOnly(scene, BLOCK_PICTURE, BLOCK_PICTURE, tooltip);
  }

  /**
   * Whether each kind of block sticks to slime and honey, one row per kind (its name and blocks,
   * then the answer), the push limit and the floor trap under it.
   */
  private static SheetElement pistonTable(SheetContext context, int width) {
    int blocksWidth = width - STICKY_CELL_WIDTH - CELL_GAP * 2;
    List<SheetElement> rows = new ArrayList<>();
    rows.add(
        tableRow(
            context.paragraph(Component.empty(), blocksWidth, SheetPalette.TEXT),
            blocksWidth,
            columnHeader(context, "slime_block", "sticky.column.drag")));
    for (PistonRules.Group group : PistonRules.Group.values()) {
      SheetElement blocks =
          group(
              context,
              "sticky.group." + group.key(),
              icons(context, group.blockIds()),
              blocksWidth);
      rows.add(tableRow(blocks, blocksWidth, dragCell(context, group.dragged())));
    }
    rows.add(
        note(
            context,
            RedstoneSheet.text(context, "sticky.note.limit", PistonRules.MAX_PUSHED),
            width));
    rows.add(note(context, text(context, "sticky.note.floor"), width));
    return new LeftColumn(rows, TABLE_ROW_GAP);
  }

  /** A row of the sticking table: the blocks, then the answer. */
  private static SheetElement tableRow(SheetElement blocks, int blocksWidth, SheetElement answer) {
    return new ElementRow(
        List.of(new LeftAligned(blocks, blocksWidth), new LeftAligned(answer, STICKY_CELL_WIDTH)),
        CELL_GAP * 2);
  }

  /** A column's icon over its bold name. */
  private static SheetElement columnHeader(SheetContext context, String itemId, String key) {
    ItemStack stack = RedstoneSheet.stack(context, itemId);
    Component name = text(context, key);
    return new ElementRow(
        List.of(
            new ItemIcon(stack, 1, Tooltips.ofItem(context.font(), stack, List.of(name))),
            context.paragraph(
                name.copy().withStyle(ChatFormatting.BOLD),
                STICKY_CELL_WIDTH - ItemIcon.ITEM_SIZE - CELL_GAP,
                SheetPalette.TEXT)),
        CELL_GAP);
  }

  private static String yesNo(boolean yes) {
    return yes ? "sticky.yes" : "sticky.no";
  }

  /** A dragged cell: yes, no, or only by its own kind. */
  private static SheetElement dragCell(SheetContext context, PistonRules.Drag drag) {
    return switch (drag) {
      case YES -> cell(context, yesNo(true), true);
      case NO -> cell(context, yesNo(false), false);
      case SAME_KIND_ONLY ->
          context.paragraph(
              text(context, "sticky.drag.same_kind_only").copy().withStyle(ChatFormatting.YELLOW),
              STICKY_CELL_WIDTH,
              SheetPalette.TEXT);
    };
  }

  /** A cell's text, green when the block goes along, red when it does not. */
  private static SheetElement cell(SheetContext context, String key, boolean goesAlong) {
    return context.paragraph(
        text(context, key).copy().withStyle(goesAlong ? ChatFormatting.GREEN : ChatFormatting.RED),
        STICKY_CELL_WIDTH,
        SheetPalette.TEXT);
  }

  /** Each block's item icon, its name in the tooltip. */
  private static List<SheetElement> icons(SheetContext context, List<String> blockIds) {
    List<SheetElement> icons = new ArrayList<>(blockIds.size());
    for (String blockId : blockIds) {
      ItemStack stack =
          new ItemStack(BuiltInRegistries.BLOCK.getValue(Identifier.parse(NAMESPACE + blockId)));
      icons.add(new ItemIcon(stack, 1, Tooltips.ofItem(context.font(), stack, List.of())));
    }
    return icons;
  }

  /** A bold heading, then its cells in rows as wide as {@code width}. */
  private static SheetElement group(
      SheetContext context, String headingKey, List<SheetElement> cells, int width) {
    List<SheetElement> rows = new ArrayList<>();
    rows.add(
        context.paragraph(
            text(context, headingKey).copy().withStyle(ChatFormatting.BOLD),
            width,
            SheetPalette.TEXT));
    List<SheetElement> row = new ArrayList<>();
    int used = 0;
    for (SheetElement cell : cells) {
      int needed = (row.isEmpty() ? 0 : CELL_GAP) + cell.minWidth();
      if (used + needed > width && !row.isEmpty()) {
        rows.add(new ElementRow(row, CELL_GAP));
        row = new ArrayList<>();
        used = 0;
        needed = cell.minWidth();
      }
      row.add(cell);
      used += needed;
    }
    if (!row.isEmpty()) {
      rows.add(new ElementRow(row, CELL_GAP));
    }
    return new LeftColumn(rows, LINE_GAP);
  }

  private static SheetElement note(SheetContext context, Component text, int width) {
    return context.paragraph(text, width, SheetPalette.MUTED_TEXT);
  }

  private static Component text(SheetContext context, String key) {
    return RedstoneSheet.text(context, key);
  }
}
