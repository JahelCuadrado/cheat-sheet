package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.picture.PictureAtlas;
import com.einsteinmods.cheatsheet.client.sheet.ElementRow;
import com.einsteinmods.cheatsheet.client.sheet.ItemIcon;
import com.einsteinmods.cheatsheet.client.sheet.SheetContext;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import com.einsteinmods.cheatsheet.client.sheet.SheetPalette;
import com.einsteinmods.cheatsheet.client.sheet.Tooltips;
import com.einsteinmods.cheatsheet.client.sheet.ZoomCard;
import com.einsteinmods.cheatsheet.core.ZoomSize;
import com.einsteinmods.cheatsheet.core.mobfarms.FarmFact;
import com.einsteinmods.cheatsheet.core.mobfarms.MobFarm;
import com.einsteinmods.cheatsheet.core.mobfarms.MobSpace;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * A farm's zoom: its name and key fact; on the left its picture and the room each of its mobs needs
 * to spawn, on the right every condition needed to build it, one line each (an icon, a bold label,
 * the condition with its numbers). Built on the first click on its card; the picture is zoom-only,
 * baked at the zoom's scale.
 */
final class FarmDetails {
  private static final int LEFT_WIDTH = 140;
  private static final int PICTURE_HEIGHT = 96;
  private static final int FACTS_WIDTH = 420;
  private static final int LINE_GAP = 3;
  private static final int SPACE_GAP = 1;
  private static final int SECTION_GAP = 4;
  private static final String SPAWN_EGG_SUFFIX = "_spawn_egg";
  private static final String NAMESPACE = "minecraft:";

  private FarmDetails() {}

  static ZoomCard of(SheetContext context, PictureAtlas atlas, MobFarm farm) {
    Component title = RedstoneSheet.farmTitle(context, farm);
    Component key = RedstoneSheet.farmKey(context, farm);
    List<FormattedCharSequence> tooltip = Tooltips.of(context.font(), title, List.of(key));
    List<SheetElement> left = new ArrayList<>();
    left.add(atlas.addZoomOnly(FarmPictures.scene(farm), LEFT_WIDTH, PICTURE_HEIGHT, tooltip));
    if (!farm.spaces().isEmpty()) {
      left.add(spaces(context, farm.spaces()));
    }
    List<SheetElement> lines = new ArrayList<>(farm.facts().size());
    for (FarmFact fact : farm.facts()) {
      lines.add(line(context, fact));
    }
    boolean fortress = farm == MobFarm.WITHER_SKELETON_FARM;
    if (fortress) {
      lines.add(FortressPiecesTable.build(context, FACTS_WIDTH));
    }
    int width = LEFT_WIDTH + ElementRow.GAP + FACTS_WIDTH;
    SheetElement header =
        new LeftColumn(
            List.of(
                context.paragraph(
                    title.copy().withStyle(ChatFormatting.BOLD),
                    width,
                    RedstoneSheet.FARM_TINT.title()),
                context.paragraph(key, width, SheetPalette.HIGHLIGHT_TEXT)),
            LINE_GAP);
    SheetElement content =
        new LeftColumn(
            List.of(
                header,
                new TopAligned(
                    new LeftColumn(left, SECTION_GAP),
                    LEFT_WIDTH + ElementRow.GAP,
                    new LeftColumn(lines, LINE_GAP))),
            SECTION_GAP);
    // The fortress table makes its card taller than the window: it scrolls, like the guides.
    return fortress
        ? ZoomViews.scrollingPanel(content, RedstoneSheet.FARM_TINT, ZoomSize.SMALL)
        : ZoomViews.panel(content, RedstoneSheet.FARM_TINT, ZoomSize.LARGE);
  }

  /** "Room needed", then each mob: its spawn egg, its name and the blocks it takes. */
  private static SheetElement spaces(SheetContext context, List<MobSpace> spaces) {
    List<SheetElement> rows = new ArrayList<>(spaces.size() + 1);
    rows.add(
        context.paragraph(
            RedstoneSheet.text(context, "farm.label." + FarmFact.Label.ROOM.key())
                .copy()
                .withStyle(ChatFormatting.BOLD),
            LEFT_WIDTH,
            SheetPalette.TEXT));
    for (MobSpace space : spaces) {
      Component name =
          BuiltInRegistries.ENTITY_TYPE
              .getValue(Identifier.parse(space.entityId()))
              .getDescription();
      Component size =
          RedstoneSheet.text(
              context,
              "farm.space.size",
              space.blocksAcross(),
              space.blocksAcross(),
              space.blocksHigh());
      ItemStack egg = context.stack(NAMESPACE + space.path() + SPAWN_EGG_SUFFIX);
      List<FormattedCharSequence> tooltip =
          Tooltips.of(
              context.font(),
              name,
              List.of(
                  RedstoneSheet.text(
                      context,
                      "farm.space.what",
                      Float.toString(space.spawnWidth()),
                      Float.toString(space.spawnHeight()))));
      SheetElement icon = new ItemIcon(egg, 1, tooltip);
      rows.add(
          new ElementRow(
              List.of(
                  icon,
                  context.paragraph(
                      Component.empty()
                          .append(name)
                          .append(" ")
                          .append(size.copy().withStyle(ChatFormatting.YELLOW)),
                      LEFT_WIDTH - icon.minWidth() - ElementRow.GAP,
                      SheetPalette.TEXT)),
              ElementRow.GAP));
    }
    return new LeftColumn(rows, SPACE_GAP);
  }

  /** {@code fact}'s icon, its label in bold, then its text wrapped beside the icon. */
  private static SheetElement line(SheetContext context, FarmFact fact) {
    ItemStack stack = RedstoneSheet.stack(context, fact.iconItemId());
    Component what = RedstoneSheet.text(context, "farm." + fact.textKey(), fact.args().toArray());
    Component label = RedstoneSheet.text(context, "farm.label." + fact.label().key());
    SheetElement icon = new ItemIcon(stack, 1, Tooltips.of(context.font(), label, List.of(what)));
    return new ElementRow(
        List.of(
            icon,
            context.paragraph(
                Component.empty()
                    .append(label.copy().withStyle(ChatFormatting.BOLD))
                    .append(" ")
                    .append(what),
                FACTS_WIDTH - icon.minWidth() - ElementRow.GAP,
                SheetPalette.TEXT)),
        ElementRow.GAP);
  }
}
