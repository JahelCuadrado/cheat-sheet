package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.sheet.CardTint;
import com.einsteinmods.cheatsheet.client.sheet.ElementRow;
import com.einsteinmods.cheatsheet.client.sheet.Section;
import com.einsteinmods.cheatsheet.client.sheet.Sheet;
import com.einsteinmods.cheatsheet.client.sheet.SheetContext;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import com.einsteinmods.cheatsheet.client.sheet.SheetPalette;
import com.einsteinmods.cheatsheet.client.sheet.SheetSource;
import com.einsteinmods.cheatsheet.client.sheet.TileGrid;
import com.einsteinmods.cheatsheet.core.ZoomSize;
import com.einsteinmods.cheatsheet.core.dyes.DyeRecipe;
import com.einsteinmods.cheatsheet.core.dyes.DyeRecipeKind;
import com.einsteinmods.cheatsheet.core.dyes.DyeTable;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * Dyes tab: sixteen equal tiles tinted like their dye, in two rows of eight on a 960-wide sheet
 * (fewer per row when narrow), like a printed dye chart. Each tile, top to bottom: the dye very big
 * and its name; the plants and items it comes from, big, in centred rows (smelted ones beside a
 * furnace, "×2" when one gives two dyes); below a divider, the dye mixes that make it, one "A + B →
 * ×n" row each. Ingredients are icons named in their tooltips. Every tile has the same slots, so
 * the same kind of recipe sits at the same height in each; unused slots stay blank. Recipes come
 * from the fixed table in {@code core.dyes} (recipes are server data).
 */
public final class DyesSheet implements SheetSource {
  public static final String ID = "dyes";

  /**
   * Eight tiles fill a 960-wide GUI and four a 480-wide one (the sheet keeps 6 px margins); the
   * longest mix (four dyes, "→ ×4") just fits.
   */
  private static final int TILE_WIDTH = 114;

  /** Two even rows of eight on the widest sheets, never one long row. */
  private static final int MAX_PER_ROW = 8;

  private static final int TILE_GAP = 4;
  private static final int SOURCE_GAP = 2;

  /** The dye itself, and the plants and items it comes from: they fill the tile's height. */
  private static final int DYE_SCALE = 3;

  private static final int SOURCE_SCALE = SheetContext.BIG;

  /** Item sprites have transparent edges: "+" signs sit right against them. */
  private static final int MIX_GAP = 0;

  private static final int TILE_PADDING = 4;

  /** The first mix sits below a divider, centred like every row of the tile. */
  private static final SlotTile.Slot CENTRED_DIVIDED = new SlotTile.Slot(true, true);

  private static final String PLUS = "+";
  private static final String ARROW = "→";
  private static final String TIMES = "×";
  private static final int WHITE = 0xFFFFFFFF;

  /** Accents whose brightest channel is below this are too dark for a card on a dark screen. */
  private static final int DARK_ACCENT = 0x50;

  private static final float DARK_LIFT = 0.3F;

  @Override
  public String id() {
    return ID;
  }

  @Override
  public ItemStack icon() {
    return new ItemStack(Items.DYE.red());
  }

  /** A dye tile tells little more bigger: a small zoom. */
  @Override
  public ZoomSize zoomSize() {
    return ZoomSize.SMALL;
  }

  @Override
  public Sheet build(SheetContext context) {
    ItemStack furnace = new ItemStack(Items.FURNACE);
    int mixSlots = 0;
    for (String color : DyeTable.COLORS) {
      int mixes = 0;
      for (DyeRecipe recipe : DyeTable.recipesFor(color)) {
        mixes += recipe.isMix() ? 1 : 0;
      }
      mixSlots = Math.max(mixSlots, mixes);
    }
    List<SlotTile.Slot> slots = new ArrayList<>();
    slots.add(SlotTile.Slot.CENTRED);
    slots.add(SlotTile.Slot.CENTRED);
    slots.add(SlotTile.Slot.CENTRED);
    for (int slot = 0; slot < mixSlots; slot++) {
      slots.add(slot == 0 ? CENTRED_DIVIDED : SlotTile.Slot.CENTRED);
    }

    List<List<@Nullable SheetElement>> tiles = new ArrayList<>(DyeTable.COLORS.size());
    List<CardTint> tints = new ArrayList<>(DyeTable.COLORS.size());
    for (DyeColor color : DyeColor.values()) {
      ItemStack dye = context.stack(DyeRecipe.dyeItemId(color.getSerializedName()));
      CardTint tint = tint(color);
      List<SheetElement> sources = new ArrayList<>();
      List<@Nullable SheetElement> mixes = new ArrayList<>();
      for (DyeRecipe recipe : DyeTable.recipesFor(color.getSerializedName())) {
        Component makes = text(context, "makes", recipe.count(), dye.getHoverName());
        if (recipe.isMix()) {
          mixes.add(mix(context, recipe, makes));
        } else {
          sources.add(source(context, recipe, makes, furnace));
        }
      }
      List<@Nullable SheetElement> tile = new ArrayList<>(slots.size());
      tile.add(context.item(dye, DYE_SCALE));
      tile.add(
          new CentredText(
              context.font(),
              dye.getHoverName().copy().withStyle(ChatFormatting.BOLD),
              TILE_WIDTH - TILE_PADDING * 2,
              tint.title()));
      tile.add(
          sources.isEmpty()
              ? null
              : new CentredRows(sources, TILE_WIDTH - TILE_PADDING * 2, SOURCE_GAP));
      for (int slot = 0; slot < mixSlots; slot++) {
        tile.add(slot < mixes.size() ? mixes.get(slot) : null);
      }
      tiles.add(tile);
      tints.add(tint);
    }
    TileGrid grid = new TileGrid(SlotTile.uniform(slots, tiles, tints, TILE_WIDTH), TILE_GAP);
    return Sheet.ofSections(
        List.of(
            Section.of(
                text(context, "section"), CardTint.STONE, new CappedBody(grid, MAX_PER_ROW))),
        null);
  }

  /**
   * A plant or item, "×2" after it when it gives two dyes; a smelted one sits beside a furnace. Its
   * name is in the tooltip.
   */
  private static SheetElement source(
      SheetContext context, DyeRecipe recipe, Component makes, ItemStack furnace) {
    ItemStack ingredient = context.stack(recipe.ingredients().getFirst());
    List<SheetElement> parts = new ArrayList<>();
    if (recipe.kind() == DyeRecipeKind.SMELTING) {
      Component smelt = text(context, "smelt");
      parts.add(context.item(ingredient, SOURCE_SCALE, smelt, makes));
      parts.add(context.item(furnace, SOURCE_SCALE, smelt, makes));
    } else {
      parts.add(context.item(ingredient, SOURCE_SCALE, makes));
    }
    if (recipe.count() > 1) {
      parts.add(
          context.label(Component.literal(TIMES + recipe.count()), SheetPalette.HIGHLIGHT_TEXT));
    }
    return new ElementRow(parts, MIX_GAP);
  }

  /** The dye's colour; black is lifted towards grey so its card and title stay readable. */
  private static CardTint tint(DyeColor color) {
    int accent = color.getTextureDiffuseColor();
    int brightest = Math.max(ARGB.red(accent), Math.max(ARGB.green(accent), ARGB.blue(accent)));
    if (brightest < DARK_ACCENT) {
      accent = ARGB.srgbLerp(DARK_LIFT, accent, WHITE);
    }
    return CardTint.of(accent);
  }

  /** Ingredients side by side with a "+" between them, then "→ ×n". */
  private static SheetElement mix(SheetContext context, DyeRecipe recipe, Component makes) {
    List<SheetElement> parts = new ArrayList<>();
    for (String ingredient : recipe.ingredients()) {
      if (!parts.isEmpty()) {
        parts.add(context.label(Component.literal(PLUS), SheetPalette.MUTED_TEXT));
      }
      parts.add(context.item(context.stack(ingredient), 1, makes));
    }
    parts.add(context.label(Component.literal(ARROW), SheetPalette.MUTED_TEXT));
    parts.add(
        context.label(Component.literal(TIMES + recipe.count()), SheetPalette.HIGHLIGHT_TEXT));
    return new ElementRow(parts, MIX_GAP);
  }

  private static Component text(SheetContext context, String key, Object... args) {
    return context.tabText(ID, key, args);
  }
}
