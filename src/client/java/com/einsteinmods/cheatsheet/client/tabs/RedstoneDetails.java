package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.picture.Cast;
import com.einsteinmods.cheatsheet.client.picture.PictureAtlas;
import com.einsteinmods.cheatsheet.client.picture.Scene;
import com.einsteinmods.cheatsheet.client.sheet.ElementRow;
import com.einsteinmods.cheatsheet.client.sheet.ItemIcon;
import com.einsteinmods.cheatsheet.client.sheet.SheetContext;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import com.einsteinmods.cheatsheet.client.sheet.SheetPalette;
import com.einsteinmods.cheatsheet.client.sheet.TextLabel;
import com.einsteinmods.cheatsheet.client.sheet.Tooltips;
import com.einsteinmods.cheatsheet.client.sheet.ZoomCard;
import com.einsteinmods.cheatsheet.core.ZoomSize;
import com.einsteinmods.cheatsheet.core.pictures.Diorama;
import com.einsteinmods.cheatsheet.core.redstone.ComparatorSignal;
import com.einsteinmods.cheatsheet.core.redstone.CrafterRules;
import com.einsteinmods.cheatsheet.core.redstone.RedstoneCard;
import com.einsteinmods.cheatsheet.core.redstone.RedstoneDioramas;
import com.einsteinmods.cheatsheet.core.redstone.RedstoneRules;
import com.einsteinmods.cheatsheet.core.redstone.SculkSensors;
import com.einsteinmods.cheatsheet.core.redstone.SignalContainer;
import com.einsteinmods.cheatsheet.core.redstone.SpecialSignals;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * The zooms of the redstone tab's reference cards: every number the card leaves out, laid out to be
 * read at a glance (an icon per line, a bold name, the numbers), sized to their content; the
 * crafter's, a guide that scrolls ({@link CrafterGuide}). Built on the first click on the card.
 */
final class RedstoneDetails {
  /** Width of a zoom of lines. */
  private static final int WIDTH = 300;

  /** Width of the ticks table: each timing fits one line beside its name. */
  private static final int TICKS_WIDTH = 360;

  /** Rows of the ticks table are the icons' height: no gap needed. */
  private static final int TICK_ROW_GAP = 1;

  /** Width of each of the sculk sensor's frequency columns. */
  private static final int FREQUENCY_COLUMN = 150;

  private static final int FREQUENCY_COLUMNS = 3;

  private static final int FREQUENCY_GAP = 8;
  private static final int SECTION_GAP = 6;
  private static final int LINE_GAP = 3;

  /** Space between the two columns of the comparator's and the special blocks' zooms. */
  private static final int COLUMN_GAP = 8;

  /** Width of each column of the special blocks' zoom. */
  private static final int SPECIAL_COLUMN = 240;

  private static final int TABLE_ROW_GAP = 2;
  private static final int TABLE_CELL_GAP = 3;
  private static final String LIST_SEPARATOR = ", ";

  /** A comparator seen from straight above, the south edge up: a camera at yaw 180, pitch 90. */
  private static final float TOP_VIEW_YAW = 180.0F;

  /** Side of a block seen from above beside a line. */
  private static final int TOP_VIEW_SIZE = 32;

  /** Side of a block in the comparator's worked examples: room for a two-digit number. */
  private static final int EXAMPLE_CELL = 26;

  private RedstoneDetails() {}

  /** The zoom of {@code card}. */
  static ZoomCard of(SheetContext context, PictureAtlas atlas, RedstoneCard card) {
    List<SheetElement> parts = new ArrayList<>();
    int width =
        switch (card) {
          case COMPARATOR -> comparator(context, atlas, parts);
          case SCULK_SENSORS -> sculkSensors(context, parts);
          case PRESSURE_PLATES -> pressurePlates(context, parts);
          case SPECIAL_SIGNALS -> specialSignals(context, parts);
          case TICKS -> ticks(context, parts);
          case CRAFTER -> CrafterGuide.build(context, atlas, parts);
        };
    parts.addFirst(header(context, card, width));
    if (card == RedstoneCard.CRAFTER) {
      // The compact machine and the signal table first; the guide waits behind a button.
      List<SheetElement> quick = new ArrayList<>(List.of(parts.getFirst()));
      quick.addAll(CrafterGuide.quickLook(context, atlas));
      return RedstoneGuide.explained(
          context,
          quick,
          new LeftColumn(parts.subList(1, parts.size()), SECTION_GAP),
          RedstoneSheet.REFERENCE_TINT);
    }
    SheetElement content = new LeftColumn(parts, SECTION_GAP);
    return card == RedstoneCard.COMPARATOR
        ? ZoomViews.scrollingPanel(content, RedstoneSheet.REFERENCE_TINT, ZoomSize.SMALL)
        : ZoomViews.panel(content, RedstoneSheet.REFERENCE_TINT, ZoomSize.MEDIUM);
  }

  /** The card's name and key fact, over the zoom's width. */
  private static SheetElement header(SheetContext context, RedstoneCard card, int width) {
    return new LeftColumn(
        List.of(
            context.paragraph(
                RedstoneSheet.cardTitle(context, card).copy().withStyle(ChatFormatting.BOLD),
                width,
                RedstoneSheet.REFERENCE_TINT.title()),
            context.paragraph(
                RedstoneSheet.cardKey(context, card), width, SheetPalette.HIGHLIGHT_TEXT)),
        LINE_GAP);
  }

  /** The four kinds of plate, then how long a touch keeps one on. */
  private static int pressurePlates(SheetContext context, List<SheetElement> parts) {
    int max = RedstoneRules.MAX_SIGNAL;
    parts.add(
        lines(
            line(context, "oak_pressure_plate", "plates.wood", "plates.wood.what", max),
            line(context, "stone_pressure_plate", "plates.stone", "plates.stone.what", max),
            line(
                context,
                "light_weighted_pressure_plate",
                "plates.light",
                "plates.light.what",
                max,
                RedstoneRules.entitiesForPlateSignal(max, RedstoneRules.LIGHT_PLATE_MAX_ENTITIES)),
            line(
                context,
                "heavy_weighted_pressure_plate",
                "plates.heavy",
                "plates.heavy.what",
                max,
                RedstoneRules.entitiesForPlateSignal(
                    max, RedstoneRules.HEAVY_PLATE_MAX_ENTITIES))));
    parts.add(
        note(
            context,
            text(
                context,
                "plates.timing",
                seconds(RedstoneRules.PLATE_PRESSED_TICKS),
                seconds(RedstoneRules.WEIGHTED_PLATE_PRESSED_TICKS)),
            WIDTH));
    return WIDTH;
  }

  /**
   * The two modes and what it reads, each beside the comparator seen from above as placed (the
   * front torch off, then on; then reading a chest at its input end), then three worked examples,
   * then the container table and its formula.
   */
  private static int comparator(
      SheetContext context, PictureAtlas atlas, List<SheetElement> parts) {
    SheetElement table = signalTable(context);
    int width = Math.max(WIDTH, table.minWidth());
    parts.add(
        lines(
            pictureLine(
                context,
                atlas,
                width,
                RedstoneDioramas.comparatorFromAbove(false),
                "comparator.compare",
                "comparator.compare.what"),
            pictureLine(
                context,
                atlas,
                width,
                RedstoneDioramas.comparatorFromAbove(true),
                "comparator.subtract",
                "comparator.subtract.what"),
            pictureLine(
                context,
                atlas,
                width,
                RedstoneDioramas.comparatorReadingChest(),
                "comparator.reads",
                "comparator.reads.what")));
    parts.add(comparatorExamples(context, atlas, width));
    parts.add(
        new LeftColumn(
            List.of(
                subheading(context, text(context, "table.title")),
                table,
                note(context, text(context, "table.formula"), width),
                note(context, text(context, "table.stacks"), width),
                note(context, crafterRule(context), width),
                note(
                    context,
                    text(
                        context,
                        "table.minecarts",
                        SignalContainer.CHEST.slots(),
                        SignalContainer.HOPPER.slots()),
                    width)),
            LINE_GAP));
    parts.add(CrafterSignalTable.build(context, width));
    return width;
  }

  /** Three circuits seen from above, each signal written on its dust, with the numbers in words. */
  private static SheetElement comparatorExamples(
      SheetContext context, PictureAtlas atlas, int width) {
    int input = ComparatorSignal.EXAMPLE_INPUT;
    int stronger = ComparatorSignal.EXAMPLE_STRONGER_SIDE;
    int subtracted = ComparatorSignal.EXAMPLE_SUBTRACTED_SIDE;
    int passed = ComparatorSignal.output(input, 0, false);
    int blocked = ComparatorSignal.output(input, stronger, false);
    int remainder = ComparatorSignal.output(input, subtracted, true);
    return new LeftColumn(
        List.of(
            subheading(context, text(context, "comparator.examples")),
            note(context, text(context, "comparator.examples.legend"), width),
            comparatorExample(
                context,
                atlas,
                width,
                input,
                0,
                false,
                "comparator.example.pass",
                text(context, "comparator.example.pass.what", input, 0, passed)),
            comparatorExample(
                context,
                atlas,
                width,
                input,
                stronger,
                false,
                "comparator.example.block",
                text(
                    context,
                    "comparator.example.block.what",
                    input,
                    stronger,
                    blocked,
                    input,
                    input)),
            comparatorExample(
                context,
                atlas,
                width,
                input,
                subtracted,
                true,
                "comparator.example.subtract",
                text(
                    context,
                    "comparator.example.subtract.what",
                    input,
                    subtracted,
                    input,
                    subtracted,
                    remainder))),
        LINE_GAP);
  }

  /** One worked example: {@code input} and {@code side} come in, the output is worked out here. */
  private static SheetElement comparatorExample(
      SheetContext context,
      PictureAtlas atlas,
      int width,
      int input,
      int side,
      boolean subtract,
      String nameKey,
      Component what) {
    int output = ComparatorSignal.output(input, side, subtract);
    Diorama diorama = RedstoneDioramas.comparatorExample(input, side, subtract);
    Component name = text(context, nameKey);
    NumberedPicture picture =
        new NumberedPicture(
            atlas.addZoomOnly(
                topView(diorama),
                EXAMPLE_CELL * diorama.width(),
                EXAMPLE_CELL * diorama.depth(),
                Tooltips.of(context.font(), name, List.of(what))),
            context.font(),
            EXAMPLE_CELL,
            List.of(
                new NumberedPicture.CellNumber(
                    RedstoneDioramas.EXAMPLE_INPUT_COLUMN, RedstoneDioramas.EXAMPLE_ROW, input),
                new NumberedPicture.CellNumber(
                    RedstoneDioramas.EXAMPLE_COMPARATOR_COLUMN,
                    RedstoneDioramas.EXAMPLE_SIDE_ROW,
                    side),
                new NumberedPicture.CellNumber(
                    RedstoneDioramas.EXAMPLE_OUTPUT_COLUMN, RedstoneDioramas.EXAMPLE_ROW, output)));
    return new ElementRow(
        List.of(
            picture,
            context.paragraph(
                Component.empty()
                    .append(name.copy().withStyle(ChatFormatting.BOLD))
                    .append(" ")
                    .append(what),
                width - picture.minWidth() - ElementRow.GAP,
                SheetPalette.TEXT)),
        ElementRow.GAP);
  }

  private static Component crafterRule(SheetContext context) {
    return text(context, "table.crafter", CrafterRules.SLOTS);
  }

  /** Blocks a comparator reads by something other than items, one line each, in two columns. */
  private static int specialSignals(SheetContext context, List<SheetElement> parts) {
    String anchor =
        String.join(
            LIST_SEPARATOR,
            SpecialSignals.respawnAnchorSignals().stream().map(String::valueOf).toList());
    int max = RedstoneRules.MAX_SIGNAL;
    parts.add(
        new TopRow(
            List.of(
                lines(
                    special(context, "composter", SpecialSignals.COMPOSTER_MAX),
                    special(context, "cake", SpecialSignals.cake(0), cakeSlice()),
                    special(context, "lectern", max),
                    special(context, "jukebox", SpecialSignals.JUKEBOX_MIN, max),
                    special(context, "item_frame", SpecialSignals.ITEM_FRAME_ROTATIONS),
                    special(
                        context,
                        "cauldron",
                        SpecialSignals.CAULDRON_MAX,
                        SpecialSignals.LAVA_CAULDRON),
                    special(context, "respawn_anchor", anchor),
                    special(context, "beehive", SpecialSignals.BEEHIVE_MAX),
                    special(context, "end_portal_frame", SpecialSignals.END_PORTAL_FRAME_EYE)),
                lines(
                    line(
                        context,
                        SPECIAL_COLUMN,
                        "oak_shelf",
                        "special.shelf.name",
                        "special.shelf",
                        SpecialSignals.SHELF_LEFT,
                        SpecialSignals.SHELF_MIDDLE,
                        SpecialSignals.SHELF_RIGHT),
                    special(context, "chiseled_bookshelf", SpecialSignals.CHISELED_BOOKSHELF_SLOTS),
                    special(context, "copper_bulb", SpecialSignals.COPPER_BULB_LIT),
                    special(
                        context, "copper_golem_statue", SpecialSignals.COPPER_GOLEM_POSES.size()),
                    special(context, "sculk_sensor", max),
                    special(
                        context,
                        "creaking_heart",
                        SpecialSignals.creakingHeart(0),
                        SpecialSignals.CREAKING_HEART_RANGE),
                    special(context, "command_block"))),
            SPECIAL_COLUMN,
            COLUMN_GAP));
    return SPECIAL_COLUMN * 2 + COLUMN_GAP;
  }

  /** What one slice of cake is worth: the signal drops this much per bite. */
  private static int cakeSlice() {
    return SpecialSignals.cake(0) - SpecialSignals.cake(1);
  }

  /** Signal, timing, wool, the calibrated sensor, then the frequency table. */
  private static int sculkSensors(SheetContext context, List<SheetElement> parts) {
    int width = FREQUENCY_COLUMN * FREQUENCY_COLUMNS + FREQUENCY_GAP * (FREQUENCY_COLUMNS - 1);
    parts.add(
        lines(
            line(
                context,
                width,
                "redstone",
                "sculk.signal",
                "sculk.signal.what",
                RedstoneRules.MAX_SIGNAL,
                RedstoneRules.MAX_SIGNAL),
            line(
                context,
                width,
                "clock",
                "sculk.timing",
                "sculk.timing.what",
                seconds(SculkSensors.TRAVEL_TICKS_PER_BLOCK),
                seconds(SculkSensors.ACTIVE_TICKS),
                seconds(SculkSensors.CALIBRATED_ACTIVE_TICKS),
                seconds(SculkSensors.COOLDOWN_TICKS)),
            line(context, width, "white_wool", "sculk.wool", "sculk.wool.what"),
            line(
                context,
                width,
                "calibrated_sculk_sensor",
                "sculk.calibrated",
                "sculk.calibrated.what")));
    int perColumn = Math.ceilDiv(SculkSensors.FREQUENCIES.size(), FREQUENCY_COLUMNS);
    List<SheetElement> columns = new ArrayList<>();
    List<SheetElement> column = new ArrayList<>();
    for (SculkSensors.Frequency frequency : SculkSensors.FREQUENCIES) {
      column.add(frequencyLine(context, frequency.frequency()));
      if (column.size() == perColumn) {
        columns.add(new LeftColumn(column, LINE_GAP));
        column = new ArrayList<>();
      }
    }
    if (!column.isEmpty()) {
      columns.add(new LeftColumn(column, LINE_GAP));
    }
    parts.add(
        new LeftColumn(
            List.of(
                subheading(context, text(context, "sculk.frequencies")),
                new TopRow(columns, FREQUENCY_COLUMN, FREQUENCY_GAP)),
            LINE_GAP));
    return width;
  }

  /** One frequency: its number, then what makes it. */
  private static SheetElement frequencyLine(SheetContext context, int frequency) {
    Font font = context.font();
    String number = Integer.toString(frequency);
    int numberWidth = font.width("15");
    SheetElement badge =
        new BoxedElement(
            new TextLabel(
                font,
                Component.literal(number).getVisualOrderText(),
                SheetPalette.HIGHLIGHT_TEXT,
                1,
                null),
            numberWidth,
            0);
    return new ElementRow(
        List.of(
            badge,
            context.paragraph(
                text(context, "sculk.frequency." + number),
                FREQUENCY_COLUMN - numberWidth - ElementRow.GAP,
                SheetPalette.TEXT)),
        ElementRow.GAP);
  }

  /** The tick, the redstone tick, then each component's delay or pulse. */
  private static int ticks(SheetContext context, List<SheetElement> parts) {
    parts.add(
        note(
            context,
            text(
                context,
                "ticks.units",
                seconds(1),
                RedstoneRules.TICKS_PER_SECOND,
                RedstoneRules.GAME_TICKS_PER_REDSTONE_TICK,
                seconds(RedstoneRules.GAME_TICKS_PER_REDSTONE_TICK)),
            TICKS_WIDTH));
    int minDelay = RedstoneRules.REPEATER_MIN_DELAY;
    int maxDelay = RedstoneRules.REPEATER_MAX_DELAY;
    parts.add(
        tickTable(
            context,
            List.of(
                new TickRow(
                    "repeater",
                    "repeater",
                    List.of(
                        seconds(RedstoneRules.repeaterTicks(minDelay)),
                        seconds(RedstoneRules.repeaterTicks(maxDelay)),
                        minDelay,
                        maxDelay)),
                new TickRow(
                    "redstone_torch",
                    "torch",
                    List.of(
                        seconds(RedstoneRules.TORCH_DELAY_TICKS),
                        RedstoneRules.TORCH_BURNOUT_TOGGLES,
                        seconds(RedstoneRules.TORCH_BURNOUT_WINDOW_TICKS),
                        seconds(RedstoneRules.TORCH_RESTART_TICKS))),
                new TickRow(
                    "comparator",
                    "comparator",
                    List.of(seconds(RedstoneRules.COMPARATOR_DELAY_TICKS))),
                new TickRow(
                    "observer",
                    "observer",
                    List.of(
                        seconds(RedstoneRules.OBSERVER_PULSE_TICKS),
                        seconds(RedstoneRules.OBSERVER_PULSE_TICKS))),
                new TickRow("piston", "piston", List.of(seconds(RedstoneRules.PISTON_MOVE_TICKS))),
                new TickRow(
                    "redstone_lamp", "lamp", List.of(seconds(RedstoneRules.LAMP_OFF_DELAY_TICKS))),
                new TickRow(
                    "dropper", "dropper", List.of(seconds(RedstoneRules.DROPPER_DELAY_TICKS))),
                new TickRow(
                    "hopper", "hopper", List.of(seconds(RedstoneRules.HOPPER_TICKS_PER_ITEM))),
                new TickRow(
                    "stone_button",
                    "buttons",
                    List.of(
                        seconds(RedstoneRules.STONE_BUTTON_TICKS),
                        seconds(RedstoneRules.WOODEN_BUTTON_TICKS))),
                new TickRow(
                    "oak_pressure_plate",
                    "plates",
                    List.of(
                        seconds(RedstoneRules.PLATE_PRESSED_TICKS),
                        seconds(RedstoneRules.WEIGHTED_PLATE_PRESSED_TICKS))),
                new TickRow(
                    "sculk_sensor",
                    "sculk",
                    List.of(
                        seconds(SculkSensors.ACTIVE_TICKS),
                        seconds(SculkSensors.CALIBRATED_ACTIVE_TICKS),
                        seconds(SculkSensors.COOLDOWN_TICKS))))));
    return TICKS_WIDTH;
  }

  /**
   * One line of the ticks table: an item's icon, the name under {@code ticks.<key>} and its timing
   * under {@code ticks.<key>.what} with {@code args}.
   */
  private record TickRow(String itemId, String key, List<Object> args) {}

  /** Icons, bold names in a column of their own, then each timing. */
  private static SheetElement tickTable(SheetContext context, List<TickRow> rows) {
    Font font = context.font();
    List<Component> names = new ArrayList<>(rows.size());
    int nameWidth = 0;
    for (TickRow row : rows) {
      Component name = text(context, "ticks." + row.key()).copy().withStyle(ChatFormatting.BOLD);
      names.add(name);
      nameWidth = Math.max(nameWidth, font.width(name));
    }
    List<SheetElement> lines = new ArrayList<>(rows.size());
    for (int index = 0; index < rows.size(); index++) {
      TickRow row = rows.get(index);
      ItemStack stack = RedstoneSheet.stack(context, row.itemId());
      Component what = text(context, "ticks." + row.key() + ".what", row.args().toArray());
      SheetElement icon = new ItemIcon(stack, 1, Tooltips.ofItem(font, stack, List.of(what)));
      lines.add(
          new ElementRow(
              List.of(
                  icon,
                  new LeftAligned(label(font, names.get(index), SheetPalette.TEXT), nameWidth),
                  context.paragraph(
                      what,
                      TICKS_WIDTH - icon.minWidth() - nameWidth - ElementRow.GAP * 2,
                      SheetPalette.TEXT)),
              ElementRow.GAP));
    }
    return new LeftColumn(lines, TICK_ROW_GAP);
  }

  /**
   * Items (stacking to 64) for each signal in each container size, one row per size; each count's
   * tooltip gives the count in stacks and for items stacking to 16 or not at all.
   */
  private static SheetElement signalTable(SheetContext context) {
    Font font = context.font();
    List<SheetElement> firstColumn = new ArrayList<>();
    firstColumn.add(label(font, text(context, "table.signal"), SheetPalette.MUTED_TEXT));
    for (SignalContainer container : SignalContainer.values()) {
      firstColumn.add(containerIcons(context, container));
    }
    int firstWidth = 0;
    for (SheetElement cell : firstColumn) {
      firstWidth = Math.max(firstWidth, cell.minWidth());
    }
    int cellWidth = 0;
    for (SignalContainer container : SignalContainer.values()) {
      for (int items : container.itemsPerSignal()) {
        cellWidth = Math.max(cellWidth, font.width(Integer.toString(items)));
      }
    }
    List<SheetElement> rows = new ArrayList<>();
    List<SheetElement> header = new ArrayList<>();
    header.add(new BoxedElement(firstColumn.getFirst(), firstWidth, 0));
    for (int signal = 1; signal <= RedstoneRules.MAX_SIGNAL; signal++) {
      header.add(
          new BoxedElement(
              label(font, Component.literal(Integer.toString(signal)), SheetPalette.HIGHLIGHT_TEXT),
              cellWidth,
              0));
    }
    rows.add(new ElementRow(header, TABLE_CELL_GAP));
    int row = 1;
    for (SignalContainer container : SignalContainer.values()) {
      List<SheetElement> cells = new ArrayList<>();
      cells.add(new BoxedElement(firstColumn.get(row++), firstWidth, 0));
      int[] items = container.itemsPerSignal();
      for (int signal = 1; signal <= RedstoneRules.MAX_SIGNAL; signal++) {
        cells.add(
            new BoxedElement(
                new TextLabel(
                    font,
                    Component.literal(Integer.toString(items[signal - 1])).getVisualOrderText(),
                    SheetPalette.TEXT,
                    1,
                    cellTooltip(context, container, signal, items[signal - 1])),
                cellWidth,
                0));
      }
      rows.add(new ElementRow(cells, TABLE_CELL_GAP));
    }
    rows.add(crafterRow(context, firstWidth, cellWidth));
    return new LeftColumn(rows, TABLE_ROW_GAP);
  }

  /**
   * The crafter's row: slots, not items. Each slot in use gives 1 whatever its stack size, up to 9,
   * so the higher signals stay empty ({@link CrafterRules#comparatorSignal}).
   */
  private static SheetElement crafterRow(SheetContext context, int firstWidth, int cellWidth) {
    Font font = context.font();
    ItemStack crafter = RedstoneSheet.stack(context, "crafter");
    List<FormattedCharSequence> tooltip =
        Tooltips.of(font, crafter.getHoverName(), List.of(crafterRule(context)));
    List<SheetElement> cells = new ArrayList<>();
    cells.add(
        new BoxedElement(
            new ElementRow(
                List.of(
                    new ItemIcon(crafter, 1, tooltip),
                    new TextLabel(
                        font,
                        Component.literal(Integer.toString(CrafterRules.SLOTS))
                            .getVisualOrderText(),
                        SheetPalette.MUTED_TEXT,
                        1,
                        tooltip)),
                1),
            firstWidth,
            0));
    for (int signal = 1; signal <= RedstoneRules.MAX_SIGNAL; signal++) {
      SheetElement cell =
          signal <= CrafterRules.SLOTS
              ? new TextLabel(
                  font,
                  Component.literal(Integer.toString(signal)).getVisualOrderText(),
                  SheetPalette.TEXT,
                  1,
                  Tooltips.of(
                      font,
                      text(context, "table.cell", signal),
                      List.of(text(context, "table.crafter.cell", signal))))
              : label(font, Component.empty(), SheetPalette.TEXT);
      cells.add(new BoxedElement(cell, cellWidth, 0));
    }
    return new ElementRow(cells, TABLE_CELL_GAP);
  }

  /** The containers of a row: icons and slot count; their names and slots in the tooltip. */
  private static SheetElement containerIcons(SheetContext context, SignalContainer container) {
    MutableComponent names = Component.empty();
    List<ItemStack> stacks = new ArrayList<>();
    for (String itemId : container.itemIds()) {
      ItemStack stack = RedstoneSheet.stack(context, itemId);
      if (!stacks.isEmpty()) {
        names.append(LIST_SEPARATOR);
      }
      names.append(stack.getHoverName());
      stacks.add(stack);
    }
    if (container == SignalContainer.DOUBLE_CHEST) {
      names = text(context, "table.double_chest").copy();
    }
    List<FormattedCharSequence> tooltip =
        Tooltips.of(
            context.font(), names, List.of(text(context, "table.slots", container.slots())));
    List<SheetElement> icons = new ArrayList<>();
    for (ItemStack stack : stacks) {
      icons.add(new ItemIcon(stack, 1, tooltip));
    }
    if (container == SignalContainer.DOUBLE_CHEST) {
      icons.add(new ItemIcon(stacks.getFirst(), 1, tooltip));
    }
    icons.add(
        new TextLabel(
            context.font(),
            Component.literal(Integer.toString(container.slots())).getVisualOrderText(),
            SheetPalette.MUTED_TEXT,
            1,
            tooltip));
    return new ElementRow(icons, 1);
  }

  /** "Signal 7: 744 items (11 stacks + 40)", then the counts for 16-stacks and unstackables. */
  private static List<FormattedCharSequence> cellTooltip(
      SheetContext context, SignalContainer container, int signal, int items) {
    int stacks = items / ComparatorSignal.STACK_64;
    int rest = items % ComparatorSignal.STACK_64;
    List<Component> lines = new ArrayList<>();
    lines.add(text(context, "table.stacks_of_64", items, stacks, rest));
    lines.add(
        countLine(
            context,
            "table.stacks_of_16",
            ComparatorSignal.itemsForSignal(container.slots(), ComparatorSignal.STACK_16, signal)));
    lines.add(
        countLine(
            context,
            "table.unstackable",
            ComparatorSignal.itemsForSignal(
                container.slots(), ComparatorSignal.UNSTACKABLE, signal)));
    return Tooltips.of(context.font(), text(context, "table.cell", signal), lines);
  }

  private static Component countLine(SheetContext context, String key, OptionalInt items) {
    return items.isPresent()
        ? text(context, key, items.getAsInt())
        : text(context, key, text(context, "table.none"));
  }

  /** A block a comparator reads: its icon, its name in bold, then what it gives. */
  private static SheetElement special(SheetContext context, String itemId, Object... args) {
    ItemStack stack = RedstoneSheet.stack(context, itemId);
    return iconLine(
        context,
        SPECIAL_COLUMN,
        stack,
        stack.getHoverName(),
        text(context, "special." + itemId, args));
  }

  /** An item's icon, a bold name from {@code nameKey}, then {@code whatKey}'s text. */
  private static SheetElement line(
      SheetContext context, String itemId, String nameKey, String whatKey, Object... args) {
    return line(context, WIDTH, itemId, nameKey, whatKey, args);
  }

  private static SheetElement line(
      SheetContext context,
      int width,
      String itemId,
      String nameKey,
      String whatKey,
      Object... args) {
    return iconLine(
        context,
        width,
        RedstoneSheet.stack(context, itemId),
        text(context, nameKey),
        text(context, whatKey, args));
  }

  /** {@code stack}'s icon, then {@code name} in bold and {@code what}, wrapped beside it. */
  static SheetElement iconLine(
      SheetContext context, int width, ItemStack stack, Component name, Component what) {
    SheetElement icon =
        new ItemIcon(stack, 1, Tooltips.ofItem(context.font(), stack, List.of(what)));
    return new ElementRow(
        List.of(
            icon,
            context.paragraph(
                Component.empty()
                    .append(name.copy().withStyle(ChatFormatting.BOLD))
                    .append(" ")
                    .append(what),
                width - icon.minWidth() - ElementRow.GAP,
                SheetPalette.TEXT)),
        ElementRow.GAP);
  }

  /**
   * {@code diorama} seen from straight above (north up), then {@code nameKey}'s text in bold and
   * {@code whatKey}'s, wrapped beside it.
   */
  private static Scene topView(Diorama diorama) {
    return new Scene(
        Optional.of(diorama),
        Cast.NONE,
        TOP_VIEW_YAW,
        Scene.TOP_VIEW_PITCH,
        0.0F,
        Optional.of(new AABB(0, 0, 0, diorama.width(), diorama.height(), diorama.depth())));
  }

  private static SheetElement pictureLine(
      SheetContext context,
      PictureAtlas atlas,
      int width,
      Diorama diorama,
      String nameKey,
      String whatKey) {
    Component name = text(context, nameKey);
    Component what = text(context, whatKey);
    SheetElement picture =
        atlas.addZoomOnly(
            topView(diorama),
            TOP_VIEW_SIZE,
            TOP_VIEW_SIZE * diorama.depth(),
            Tooltips.of(context.font(), name, List.of(what)));
    return new ElementRow(
        List.of(
            picture,
            context.paragraph(
                Component.empty()
                    .append(name.copy().withStyle(ChatFormatting.BOLD))
                    .append(" ")
                    .append(what),
                width - picture.minWidth() - ElementRow.GAP,
                SheetPalette.TEXT)),
        ElementRow.GAP);
  }

  /** Lines one under the other. */
  private static SheetElement lines(SheetElement... lines) {
    return new LeftColumn(List.of(lines), LINE_GAP);
  }

  private static SheetElement note(SheetContext context, Component text, int width) {
    return context.paragraph(text, width, SheetPalette.MUTED_TEXT);
  }

  static SheetElement subheading(SheetContext context, Component text) {
    return context.label(
        text.copy().withStyle(ChatFormatting.BOLD), RedstoneSheet.REFERENCE_TINT.title());
  }

  private static TextLabel label(Font font, Component text, int color) {
    return new TextLabel(font, text.getVisualOrderText(), color, 1, null);
  }

  private static String seconds(int gameTicks) {
    return RedstoneSheet.seconds(gameTicks);
  }

  private static Component text(SheetContext context, String key, Object... args) {
    return RedstoneSheet.text(context, key, args);
  }
}
