package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.picture.PictureAtlas;
import com.einsteinmods.cheatsheet.client.sheet.CardTint;
import com.einsteinmods.cheatsheet.client.sheet.SheetContext;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import com.einsteinmods.cheatsheet.client.sheet.SheetPalette;
import com.einsteinmods.cheatsheet.client.sheet.Tooltips;
import com.einsteinmods.cheatsheet.client.sheet.ZoomCard;
import com.einsteinmods.cheatsheet.core.ZoomSize;
import com.einsteinmods.cheatsheet.core.pictures.Diorama;
import com.einsteinmods.cheatsheet.core.redstone.NetherRoof;
import com.einsteinmods.cheatsheet.core.redstone.RedstoneBuild;
import com.einsteinmods.cheatsheet.core.redstone.RedstoneRules;
import com.einsteinmods.cheatsheet.core.redstone.SculkSensors;
import com.einsteinmods.cheatsheet.core.redstone.SorterFilter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * A mechanic's or circuit's zoom: its name and summary, then one column per step, each with its
 * number, a picture of the build so far and what to do. Built on the first click on its tile; its
 * pictures are zoom-only, baked at the zoom's scale.
 */
final class RedstoneGuide {
  static final int COLUMN_WIDTH = 128;
  static final int COLUMN_GAP = 6;
  static final int PICTURE_WIDTH = 120;
  static final int PICTURE_HEIGHT = 96;
  private static final int PART_GAP = 3;

  /** The finished circuit of a quick look: twice a step's picture. */
  private static final int QUICK_PICTURE_WIDTH = PICTURE_WIDTH * 2;

  private static final int QUICK_PICTURE_HEIGHT = PICTURE_HEIGHT * 2;
  private static final int SECTION_GAP = 4;
  private static final int BADGE_GAP = 3;
  private static final int BADGE_COLOR = SheetPalette.HIGHLIGHT_TEXT;

  /** The vertical signal's observer column, whose text gives the pulse's length. */
  private static final int OBSERVER_STEP = 4;

  private RedstoneGuide() {}

  /** The guide of {@code build}; adds its pictures to {@code atlas} as zoom-only. */
  static ZoomCard build(SheetContext context, PictureAtlas atlas, RedstoneBuild build) {
    CardTint tint = RedstoneSheet.tint(build);
    Component title = text(context, "build." + build.key() + ".title");
    int steps = build.steps().size();
    int width = steps * COLUMN_WIDTH + (steps - 1) * COLUMN_GAP;
    List<SheetElement> columns = new ArrayList<>(steps);
    for (int index = 0; index < steps; index++) {
      columns.add(step(context, atlas, build, index, tint));
    }
    List<SheetElement> parts =
        new ArrayList<>(
            List.of(
                new BoxedElement(
                    context.paragraph(
                        title.copy().withStyle(ChatFormatting.BOLD), width, tint.title()),
                    width,
                    0),
                new BoxedElement(
                    context.paragraph(
                        RedstoneSheet.buildKey(context, build), width, SheetPalette.HIGHLIGHT_TEXT),
                    width,
                    0),
                new TopRow(columns, COLUMN_WIDTH, COLUMN_GAP)));
    if (build.kind() != RedstoneBuild.Kind.MECHANIC) {
      // A circuit or technique opens on its card's build and its tables; the steps wait behind a
      // button.
      SheetElement stepRow = parts.removeLast();
      List<SheetElement> quick = new ArrayList<>();
      quick.add(
          atlas.addZoomOnly(
              RedstoneSheet.buildScene(build.cover()),
              QUICK_PICTURE_WIDTH,
              QUICK_PICTURE_HEIGHT,
              Tooltips.of(context.font(), title, List.of(RedstoneSheet.buildKey(context, build)))));
      RedstoneLists.of(context, atlas, build, width).ifPresent(quick::add);
      parts.add(new BoxedElement(new ZoomViews.CentredColumn(quick, SECTION_GAP), width, 0));
      return explained(context, parts, stepRow, tint);
    }
    RedstoneLists.of(context, atlas, build, width).ifPresent(parts::add);
    return ZoomViews.panel(new LeftColumn(parts, SECTION_GAP), tint, ZoomSize.LARGE);
  }

  /** {@code quick} (a quick look) with a button under it that shows or hides {@code details}. */
  static ZoomCard explained(
      SheetContext context, List<SheetElement> quick, SheetElement details, CardTint tint) {
    return ZoomViews.explained(
        context.font(),
        quick,
        text(context, "explanation.show"),
        text(context, "explanation.hide"),
        details,
        tint,
        SECTION_GAP);
  }

  /** Step {@code index + 1}: its number, the build so far and what to do. */
  private static SheetElement step(
      SheetContext context, PictureAtlas atlas, RedstoneBuild build, int index, CardTint tint) {
    int number = index + 1;
    return stepColumn(
        context,
        atlas,
        build.steps().get(index),
        number,
        text(context, "build." + build.key() + ".title"),
        stepText(context, build, number));
  }

  /** One numbered step: its number, {@code diorama} as a picture and {@code what}. */
  static SheetElement stepColumn(
      SheetContext context,
      PictureAtlas atlas,
      Diorama diorama,
      int number,
      Component title,
      Component what) {
    Font font = context.font();
    List<FormattedCharSequence> tooltip = Tooltips.of(font, title, List.of(what));
    SheetElement heading =
        new IconGrid.Builder(font, 1, 0, 0)
            .cell(
                new IconGrid.Flow(font)
                    .badge(BADGE_COLOR, Component.literal(Integer.toString(number)))
                    .gap(BADGE_GAP)
                    .build(tooltip))
            .build();
    SheetElement picture =
        atlas.addZoomOnly(
            RedstoneSheet.buildScene(diorama), PICTURE_WIDTH, PICTURE_HEIGHT, tooltip);
    return new ZoomViews.CentredColumn(
        List.of(heading, picture, new CentredText(font, what, COLUMN_WIDTH, SheetPalette.TEXT)),
        PART_GAP);
  }

  /** What step {@code number} of {@code build} says, with its numbers. */
  private static Component stepText(SheetContext context, RedstoneBuild build, int number) {
    String key = "build." + build.key() + ".step." + number;
    boolean last = number == build.steps().size();
    if (!last) {
      return switch (build) {
        case T_FLIP_FLOP ->
            number == 2 ? text(context, key, RedstoneRules.MAX_SIGNAL) : text(context, key);
        case NETHER_ROOF ->
            text(context, key, NetherRoof.ALWAYS_Y, NetherRoof.ladderFeetY(NetherRoof.ALWAYS_Y));
        case VERTICAL_SIGNAL ->
            number == OBSERVER_STEP
                ? text(context, key, RedstoneSheet.seconds(RedstoneRules.OBSERVER_PULSE_TICKS))
                : text(context, key);
        default -> text(context, key);
      };
    }
    return switch (build) {
      case OBSERVER_CLOCK ->
          text(
              context,
              key,
              RedstoneSheet.seconds(RedstoneRules.OBSERVER_PULSE_TICKS),
              RedstoneSheet.seconds(RedstoneRules.OBSERVER_CLOCK_PERIOD_TICKS));
      case REPEATER_CLOCK ->
          text(
              context,
              key,
              RedstoneSheet.seconds(RedstoneRules.STONE_BUTTON_TICKS),
              RedstoneSheet.seconds(RedstoneRules.repeaterClockOffTicks()),
              RedstoneSheet.seconds(RedstoneRules.STONE_BUTTON_TICKS));
      case HOPPER_CLOCK ->
          text(
              context,
              key,
              RedstoneSheet.seconds(RedstoneRules.HOPPER_CLOCK_TICKS_PER_ITEM),
              RedstoneRules.HOPPER_CLOCK_EXAMPLE_ITEMS,
              RedstoneSheet.seconds(
                  RedstoneRules.hopperClockCycleTicks(RedstoneRules.HOPPER_CLOCK_EXAMPLE_ITEMS)));
      case PULSE_LIMITER ->
          text(context, key, RedstoneSheet.seconds(RedstoneRules.OBSERVER_PULSE_TICKS));
      case PULSE_EXTENDER ->
          text(context, key, RedstoneSheet.seconds(RedstoneRules.PULSE_EXTENDER_HOLD_TICKS));
      case ITEM_SORTER ->
          text(
              context,
              key,
              SorterFilter.ITEMS,
              SorterFilter.lockedSignal(),
              SorterFilter.openSignal());
      case VERTICAL_SIGNAL -> text(context, key, SculkSensors.RANGE);
      case NETHER_ROOF -> text(context, key, NetherRoof.WALK_Y);
      case AUTO_DROPPER ->
          text(context, key, RedstoneSheet.seconds(RedstoneRules.HOPPER_TICKS_PER_ITEM));
      default -> text(context, key);
    };
  }

  private static Component text(SheetContext context, String key, Object... args) {
    return RedstoneSheet.text(context, key, args);
  }
}
