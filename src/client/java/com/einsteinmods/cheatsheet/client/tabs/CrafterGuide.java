package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.picture.PictureAtlas;
import com.einsteinmods.cheatsheet.client.sheet.ElementRow;
import com.einsteinmods.cheatsheet.client.sheet.SheetContext;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import com.einsteinmods.cheatsheet.client.sheet.SheetPalette;
import com.einsteinmods.cheatsheet.client.sheet.Tooltips;
import com.einsteinmods.cheatsheet.core.pictures.Diorama;
import com.einsteinmods.cheatsheet.core.redstone.CrafterRules;
import com.einsteinmods.cheatsheet.core.redstone.RedstoneCard;
import com.einsteinmods.cheatsheet.core.redstone.RedstoneDioramas;
import com.einsteinmods.cheatsheet.core.redstone.RedstoneRules;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * The crafter's zoom, a guide that scrolls: how it works, two machines built step by step and a few
 * tips. {@code RedstoneMechanicsGameTest} runs every machine shown and checks every tip. Built on
 * the first click on its card; its pictures are zoom-only.
 */
final class CrafterGuide {
  private static final int STEP_COLUMNS = 3;

  /** Three step columns of {@link RedstoneGuide}. */
  private static final int WIDTH =
      STEP_COLUMNS * RedstoneGuide.COLUMN_WIDTH + (STEP_COLUMNS - 1) * RedstoneGuide.COLUMN_GAP;

  /** The screen and the simplest machine, beside the numbered explanation. */
  private static final int SIDE_WIDTH = RedstoneGuide.COLUMN_WIDTH;

  private static final int SIDE_PICTURE_HEIGHT = 72;
  private static final int SIDE_GAP = 8;
  private static final int HOW_STEPS = 4;
  private static final int LINE_GAP = 3;
  private static final int HOW_LINE_GAP = 4;
  private static final int PART_GAP = 5;

  /** The steps of how it works that give a number: the comparator's reading, the craft's delay. */
  private static final int HOW_COMPARATOR_STEP = 3;

  private static final int HOW_CRAFT_STEP = 4;

  /** Hoppers filling a machine in the tip that doubles its speed. */
  private static final int TWO_HOPPERS = 2;

  /** Nuggets for one gold block: nine for each of nine ingots. */
  private static final int NUGGETS_PER_BLOCK = CrafterRules.SLOTS * CrafterRules.SLOTS;

  private CrafterGuide() {}

  /**
   * The quick look: the most compact machine, big, with what it does, then the signal a comparator
   * reads for each mix of slots.
   */
  static List<SheetElement> quickLook(SheetContext context, PictureAtlas atlas) {
    Component name = text(context, "tip.compact");
    Component what =
        text(
            context,
            "tip.compact.what",
            CrafterRules.SLOTS,
            seconds(CrafterRules.ticksPerCraft(1)),
            seconds(CrafterRules.ticksPerCraft(TWO_HOPPERS)));
    SheetElement picture =
        atlas.addZoomOnly(
            RedstoneSheet.buildScene(RedstoneDioramas.compactMachine()),
            RedstoneGuide.PICTURE_WIDTH * 2,
            RedstoneGuide.PICTURE_HEIGHT * 2,
            Tooltips.of(context.font(), name, List.of(what)));
    return List.of(
        new BoxedElement(picture, WIDTH, 0),
        note(
            context,
            Component.empty()
                .append(name.copy().withStyle(ChatFormatting.BOLD))
                .append(" ")
                .append(what),
            WIDTH,
            SheetPalette.TEXT),
        CrafterSignalTable.build(context, WIDTH));
  }

  /** Adds the guide's parts to {@code parts}; returns its width. */
  static int build(SheetContext context, PictureAtlas atlas, List<SheetElement> parts) {
    parts.add(howItWorks(context, atlas));
    parts.add(ironMachine(context, atlas));
    parts.add(goldMachine(context, atlas));
    parts.add(tips(context, atlas));
    return WIDTH;
  }

  /**
   * The crafter's screen with an example recipe (a chest: the centre slot disabled) and the
   * simplest machine, beside the numbered steps of using it.
   */
  private static SheetElement howItWorks(SheetContext context, PictureAtlas atlas) {
    Font font = context.font();
    ItemStack planks = RedstoneSheet.stack(context, "oak_planks");
    ItemStack chest = RedstoneSheet.stack(context, "chest");
    List<FormattedCharSequence> plankTooltip = Tooltips.ofItem(font, planks, List.of());
    List<CraftingGrid.Slot> slots = new ArrayList<>(CrafterRules.SLOTS);
    for (int slot = 0; slot < CrafterRules.SLOTS; slot++) {
      slots.add(
          slot == CrafterRules.EXAMPLE_DISABLED_SLOT
              ? CraftingGrid.Slot.disabled(
                  Tooltips.of(
                      font, text(context, "disabled"), List.of(text(context, "disabled.what"))))
              : new CraftingGrid.Slot(planks, plankTooltip));
    }
    SheetElement grid =
        new CraftingGrid(font, slots, chest, Tooltips.ofItem(font, chest, List.of()));
    Component machine = text(context, "machine");
    SheetElement picture =
        atlas.addZoomOnly(
            RedstoneSheet.buildScene(RedstoneDioramas.crafter()),
            SIDE_WIDTH,
            SIDE_PICTURE_HEIGHT,
            Tooltips.of(
                font, RedstoneSheet.cardTitle(context, RedstoneCard.CRAFTER), List.of(machine)));
    SheetElement side =
        new LeftColumn(
            List.of(
                grid,
                note(context, text(context, "screen"), SIDE_WIDTH),
                picture,
                note(context, machine, SIDE_WIDTH)),
            LINE_GAP);
    List<SheetElement> steps = new ArrayList<>(HOW_STEPS);
    for (int number = 1; number <= HOW_STEPS; number++) {
      steps.add(
          context.paragraph(
              Component.empty()
                  .append(
                      Component.literal(number + ". ")
                          .withStyle(ChatFormatting.BOLD)
                          .withColor(SheetPalette.HIGHLIGHT_TEXT))
                  .append(howStep(context, number)),
              WIDTH - SIDE_WIDTH - SIDE_GAP,
              SheetPalette.TEXT));
    }
    return part(
        context,
        "how",
        new ElementRow(List.of(side, new LeftColumn(steps, HOW_LINE_GAP)), SIDE_GAP));
  }

  /** Step {@code number} of how it works, with its numbers. */
  private static Component howStep(SheetContext context, int number) {
    String key = "how." + number;
    return switch (number) {
      case HOW_COMPARATOR_STEP -> text(context, key, CrafterRules.SLOTS);
      case HOW_CRAFT_STEP -> text(context, key, seconds(CrafterRules.CRAFT_DELAY_TICKS));
      default -> text(context, key);
    };
  }

  /** The iron blocks machine: its steps, then the machine at work and why nine dust. */
  private static SheetElement ironMachine(SheetContext context, PictureAtlas atlas) {
    Component title = text(context, "iron");
    List<Diorama> pictures = RedstoneDioramas.ironMachine();
    List<SheetElement> columns = new ArrayList<>(pictures.size());
    for (int index = 0; index < pictures.size(); index++) {
      columns.add(
          stepColumn(
              context, atlas, pictures.get(index), index, title, "iron.step.", CrafterRules.SLOTS));
    }
    Component why = text(context, "iron.why", CrafterRules.SLOTS);
    Component cycle =
        text(
            context,
            "iron.cycle",
            seconds(RedstoneRules.HOPPER_TICKS_PER_ITEM),
            seconds(CrafterRules.ticksPerCraft(1)));
    int textWidth = WIDTH - RedstoneGuide.PICTURE_WIDTH - SIDE_GAP;
    SheetElement picture =
        atlas.addZoomOnly(
            RedstoneSheet.buildScene(RedstoneDioramas.ironMachineFull()),
            RedstoneGuide.PICTURE_WIDTH,
            RedstoneGuide.PICTURE_HEIGHT,
            Tooltips.of(context.font(), title, List.of(why)));
    return part(
        context,
        "iron",
        note(context, text(context, "iron.intro"), WIDTH, SheetPalette.TEXT),
        new TopRow(columns, RedstoneGuide.COLUMN_WIDTH, RedstoneGuide.COLUMN_GAP),
        new ElementRow(
            List.of(
                picture,
                new LeftColumn(
                    List.of(
                        note(context, why, textWidth, SheetPalette.TEXT),
                        note(context, cycle, textWidth, SheetPalette.TEXT)),
                    LINE_GAP)),
            SIDE_GAP));
  }

  /** The two crafters in a row: their steps, then what comes out. */
  private static SheetElement goldMachine(SheetContext context, PictureAtlas atlas) {
    Component title = text(context, "gold");
    List<Diorama> pictures = RedstoneDioramas.goldMachine();
    List<SheetElement> columns = new ArrayList<>(pictures.size());
    for (int index = 0; index < pictures.size(); index++) {
      columns.add(
          stepColumn(
              context, atlas, pictures.get(index), index, title, "gold.step.", CrafterRules.SLOTS));
    }
    return part(
        context,
        "gold",
        note(context, text(context, "gold.intro"), WIDTH, SheetPalette.TEXT),
        new TopRow(columns, RedstoneGuide.COLUMN_WIDTH, RedstoneGuide.COLUMN_GAP),
        note(
            context,
            text(
                context,
                "gold.result",
                NUGGETS_PER_BLOCK,
                seconds(CrafterRules.SLOTS * CrafterRules.ticksPerCraft(1))),
            WIDTH,
            SheetPalette.TEXT));
  }

  /** Step {@code index + 1} of a machine, its text under {@code keyPrefix}. */
  private static SheetElement stepColumn(
      SheetContext context,
      PictureAtlas atlas,
      Diorama diorama,
      int index,
      Component title,
      String keyPrefix,
      Object... args) {
    int number = index + 1;
    return RedstoneGuide.stepColumn(
        context, atlas, diorama, number, title, text(context, keyPrefix + number, args));
  }

  /** The tips, one line each. */
  private static SheetElement tips(SheetContext context, PictureAtlas atlas) {
    return part(
        context,
        "tips",
        tip(context, "chest", "any", CrafterRules.SLOTS),
        tip(context, "crafter", "incomplete"),
        tip(context, "hopper", "under"),
        tip(context, "clock", "faster", seconds(CrafterRules.ticksPerCraft(TWO_HOPPERS))),
        tip(context, "torch", "order"));
  }

  /** A tip: an item's icon, its bold name, then what to know, wrapped beside the icon. */
  private static SheetElement tip(SheetContext context, String itemId, String key, Object... args) {
    ItemStack stack = RedstoneSheet.stack(context, itemId);
    return RedstoneDetails.iconLine(
        context,
        WIDTH,
        stack,
        text(context, "tip." + key),
        text(context, "tip." + key + ".what", args));
  }

  /** A part of the guide: its heading, then {@code elements}. */
  private static SheetElement part(SheetContext context, String key, SheetElement... elements) {
    List<SheetElement> column = new ArrayList<>(elements.length + 1);
    column.add(RedstoneDetails.subheading(context, text(context, key)));
    column.addAll(List.of(elements));
    return new LeftColumn(column, PART_GAP);
  }

  private static SheetElement note(SheetContext context, Component text, int width) {
    return note(context, text, width, SheetPalette.MUTED_TEXT);
  }

  private static SheetElement note(SheetContext context, Component text, int width, int color) {
    return context.paragraph(text, width, color);
  }

  private static String seconds(int gameTicks) {
    return RedstoneSheet.seconds(gameTicks);
  }

  private static Component text(SheetContext context, String key, Object... args) {
    return RedstoneSheet.text(context, "crafter." + key, args);
  }
}
