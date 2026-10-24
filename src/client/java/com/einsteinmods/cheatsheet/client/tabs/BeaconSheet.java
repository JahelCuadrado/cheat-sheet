package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.picture.PictureAtlas;
import com.einsteinmods.cheatsheet.client.picture.Scene;
import com.einsteinmods.cheatsheet.client.sheet.CardTint;
import com.einsteinmods.cheatsheet.client.sheet.ElementRow;
import com.einsteinmods.cheatsheet.client.sheet.ItemIcon;
import com.einsteinmods.cheatsheet.client.sheet.Section;
import com.einsteinmods.cheatsheet.client.sheet.Sheet;
import com.einsteinmods.cheatsheet.client.sheet.SheetContext;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import com.einsteinmods.cheatsheet.client.sheet.SheetPalette;
import com.einsteinmods.cheatsheet.client.sheet.SheetSource;
import com.einsteinmods.cheatsheet.client.sheet.Table;
import com.einsteinmods.cheatsheet.client.sheet.TileGrid;
import com.einsteinmods.cheatsheet.client.sheet.Tooltips;
import com.einsteinmods.cheatsheet.client.sheet.ZoomCard;
import com.einsteinmods.cheatsheet.core.beacon.BeaconDioramas;
import com.einsteinmods.cheatsheet.core.beacon.BeaconPyramid;
import com.einsteinmods.cheatsheet.core.beacon.ConduitFrame;
import com.einsteinmods.cheatsheet.core.beacon.ConduitMaterials;
import com.einsteinmods.cheatsheet.core.beacon.GridRecipe;
import com.einsteinmods.cheatsheet.core.beacon.MaterialSource;
import com.einsteinmods.cheatsheet.core.dyes.DyeRecipe;
import com.einsteinmods.cheatsheet.core.dyes.DyeTable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Hud;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.phys.AABB;

/**
 * Beacon and conduit tab, one screen at a glance: two rows of equal tiles. "Beacon": a tile per
 * pyramid level (the pyramid in miniature, blocks needed, range, effects it unlocks) and what it is
 * built of and paid with. "Conduit": the shape of its frame (the three rings where blocks count),
 * the frame sizes at which its range grows, its crafting recipe and its frame blocks'. Details (a
 * pyramid's bottom layer, where each material comes from) are in the tooltips. The frame's and the
 * range's tiles zoom as a step-by-step guide to building a conduit ({@link ConduitTutorial}).
 *
 * <p>Effects and block lists come from the game ({@code BeaconBlockEntity.BEACON_EFFECTS}, block
 * and item tags); counts and ranges from {@code core.beacon}, which follows the block entities;
 * recipes and sources from the fixed table {@link ConduitMaterials} (server data).
 */
public final class BeaconSheet implements SheetSource {
  public static final String ID = "beacon";

  /**
   * Tiles are this wide: seven fit a row of a 960-pixel-wide sheet, but five no longer fit a
   * 640-pixel-wide one. So auto sizing ({@code core.SheetScaling}) never blows the tab up to a
   * 640x360 canvas, which on a 1080p screen filled it edge to edge: the tab is drawn at the 960x540
   * canvas of the other tabs, its two rows of five tiles about two thirds as big, seen at a glance.
   */
  private static final int TILE_WIDTH = 128;

  /** Width texts wrap at inside a tile. */
  private static final int TEXT_WIDTH = TILE_WIDTH - SlotTiles.PADDING * 2;

  /** Pyramids and frames in miniature, in sheet pixels. */
  private static final int PYRAMID_WIDTH = 72;

  private static final int PYRAMID_HEIGHT = 40;
  private static final int FRAME_PICTURE_SIZE = 48;

  /** Every pyramid framed on the biggest's box (and the frame on the full frame's): one scale. */
  private static final AABB PYRAMID_BOX =
      new AABB(
          0,
          0,
          0,
          BeaconDioramas.PYRAMID_SIDE,
          BeaconDioramas.PYRAMID_HEIGHT,
          BeaconDioramas.PYRAMID_SIDE);

  private static final AABB FRAME_BOX =
      new AABB(
          0, 0, 0, BeaconDioramas.FRAME_SIDE, BeaconDioramas.FRAME_SIDE, BeaconDioramas.FRAME_SIDE);

  private static final int EFFECT_SIZE = 12;

  private static final int LINE_GAP = 1;
  private static final int ICON_GAP = 3;
  private static final int LIST_GAP = 2;
  private static final int RECIPE_GAP = 3;
  private static final int FRAME_ICON_COLUMNS = 4;
  private static final int STEP_COLUMNS = 2;
  private static final int STEP_COLUMN_GAP = 8;
  private static final String RANGE_SIGN = "↔ ";
  private static final String COUNT_SIGN = " × ";
  private static final String TIMES = "×";
  private static final String LIST_SEPARATOR = ", ";
  private static final String NAMESPACE = "minecraft:";

  /** Frame recipes per tile, one grid below the other. */
  private static final int RECIPES_PER_TILE = 2;

  private static final List<Table.Align> PYRAMID_SLOTS = centred(5);
  private static final List<Table.Align> BUILD_SLOTS = Collections.nCopies(5, Table.Align.START);
  private static final List<Table.Align> SHAPE_SLOTS = centred(4);
  private static final List<Table.Align> STEPS_SLOTS = Collections.nCopies(3, Table.Align.START);
  private static final List<Table.Align> RECIPE_SLOTS = Collections.nCopies(3, Table.Align.START);
  private static final List<Table.Align> FRAME_RECIPE_SLOTS =
      Collections.nCopies(RECIPES_PER_TILE, Table.Align.START);

  @Override
  public String id() {
    return ID;
  }

  @Override
  public ItemStack icon() {
    return new ItemStack(Items.BEACON);
  }

  @Override
  public Sheet build(SheetContext context) {
    PictureAtlas atlas = new PictureAtlas(ID);
    // One section: every tile of the tab has the same size.
    SlotTiles tiles = new SlotTiles(TILE_WIDTH);

    SlotTiles.Layout pyramids = tiles.layout(CardTint.AQUA, PYRAMID_SLOTS);
    for (int level = 1; level <= BeaconPyramid.MAX_LEVELS; level++) {
      pyramids.add(pyramidTile(context, atlas, level));
    }
    SlotTiles.Layout build = tiles.layout(CardTint.STONE, BUILD_SLOTS);
    build.add(buildTile(context));

    SlotTiles.Layout shape = tiles.layout(CardTint.BLUE, SHAPE_SLOTS);
    shape.add(shapeTile(context, atlas));
    SlotTiles.Layout steps = tiles.layout(CardTint.BLUE, STEPS_SLOTS);
    steps.add(stepsTile(context));
    SlotTiles.Layout conduitRecipe = tiles.layout(CardTint.STONE, RECIPE_SLOTS);
    conduitRecipe.add(conduitRecipeTile(context));
    SlotTiles.Layout frameRecipes = tiles.layout(CardTint.STONE, FRAME_RECIPE_SLOTS);
    List<GridRecipe> frame = ConduitMaterials.FRAME_RECIPES;
    for (int first = 0; first < frame.size(); first += RECIPES_PER_TILE) {
      List<SheetElement> grids = new ArrayList<>(RECIPES_PER_TILE);
      for (int index = first; index < first + RECIPES_PER_TILE; index++) {
        grids.add(index < frame.size() ? craftingGrid(context, frame.get(index)) : SlotTiles.EMPTY);
      }
      frameRecipes.add(grids);
    }
    tiles.build();

    List<SheetElement> beacon = new ArrayList<>(pyramids.tiles());
    beacon.addAll(build.tiles());
    // The frame's and the range's tiles zoom as one step-by-step card, built on the first click.
    IntFunction<ZoomCard> tutorial =
        ZoomViews.kept(1, unused -> ConduitTutorial.build(context, atlas));
    List<SheetElement> conduit = new ArrayList<>();
    for (SheetElement tile : shape.tiles()) {
      conduit.add(ZoomViews.zoomedAs(tile, () -> tutorial.apply(0)));
    }
    for (SheetElement tile : steps.tiles()) {
      conduit.add(ZoomViews.zoomedAs(tile, () -> tutorial.apply(0)));
    }
    conduit.addAll(conduitRecipe.tiles());
    conduit.addAll(frameRecipes.tiles());
    return Sheet.ofSections(
        List.of(
            Section.of(
                Component.translatable("block.minecraft.beacon"),
                CardTint.AQUA,
                new TileGrid(beacon, SlotTiles.GAP)),
            Section.of(
                Component.translatable("block.minecraft.conduit"),
                CardTint.BLUE,
                new TileGrid(conduit, SlotTiles.GAP))),
        atlas);
  }

  /**
   * Pyramid of {@code level} levels: the pyramid in miniature (its bottom layer in the tooltip),
   * its name, total blocks (big), the range and the effects it unlocks.
   */
  private static List<SheetElement> pyramidTile(
      SheetContext context, PictureAtlas atlas, int level) {
    List<Holder<MobEffect>> effects = BeaconBlockEntity.BEACON_EFFECTS.get(level - 1);
    boolean secondary = level >= BeaconPyramid.SECONDARY_LEVEL;
    Component title = text(context, "pyramid.level", level);
    List<Component> details = new ArrayList<>();
    details.add(text(context, "pyramid.blocks", BeaconPyramid.totalBlocks(level)));
    details.add(
        text(
            context,
            "pyramid.layer",
            BeaconPyramid.layerSide(level),
            BeaconPyramid.layerBlocks(level)));
    details.add(text(context, "pyramid.range", BeaconPyramid.range(level)));
    details.add(text(context, "pyramid.unlocks", effectNames(effects)));
    if (secondary) {
      details.add(text(context, "pyramid.secondary"));
    }
    SheetElement picture =
        atlas.add(
            Scene.of(BeaconDioramas.pyramid(level)).focusedOn(PYRAMID_BOX),
            PYRAMID_WIDTH,
            PYRAMID_HEIGHT,
            Tooltips.of(context.font(), title, details));

    List<SheetElement> unlocked = new ArrayList<>();
    for (Holder<MobEffect> effect : effects) {
      Component name = effect.value().getDisplayName();
      unlocked.add(
          iconLine(
              context,
              line(
                  context,
                  new IconGrid.Flow(context.font())
                      .sprite(Hud.getMobEffectSprite(effect), EFFECT_SIZE)
                      .build(Tooltips.of(context.font(), name, List.of()))),
              name,
              SheetPalette.TEXT));
    }
    if (secondary) {
      unlocked.add(centred(context, text(context, "pyramid.secondary")));
    }
    return List.of(
        picture,
        centredTitle(context, title, CardTint.AQUA),
        totalBlocks(context, BeaconPyramid.totalBlocks(level)),
        // Wrapped: in some languages the range does not fit one line of a tile.
        new CentredText(
            context.font(),
            Component.literal(RANGE_SIGN)
                .append(text(context, "pyramid.range", BeaconPyramid.range(level))),
            TEXT_WIDTH,
            SheetPalette.TEXT),
        new LeftColumn(unlocked, LINE_GAP));
  }

  /** The pyramid's block count, big, with "blocks in total" wrapped beside it. */
  private static SheetElement totalBlocks(SheetContext context, int blocks) {
    SheetElement count =
        context.bigLabel(Component.literal(Integer.toString(blocks)), SheetPalette.HIGHLIGHT_TEXT);
    return new ElementRow(
        List.of(
            count,
            context.paragraph(
                text(context, "pyramid.total"),
                TEXT_WIDTH - count.minWidth() - ElementRow.GAP,
                SheetPalette.MUTED_TEXT)),
        ElementRow.GAP);
  }

  /**
   * What the pyramid is built of (with its layers' sizes) and what pays for an effect (how long it
   * lasts in the payment items' tooltips).
   */
  private static List<SheetElement> buildTile(SheetContext context) {
    List<ItemStack> blocks = new ArrayList<>();
    for (Holder<Block> block :
        BuiltInRegistries.BLOCK.getTagOrEmpty(BlockTags.BEACON_BASE_BLOCKS)) {
      blocks.add(new ItemStack(block.value()));
    }
    List<ItemStack> payments = new ArrayList<>();
    for (Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.BEACON_PAYMENT_ITEMS)) {
      payments.add(new ItemStack(item.value()));
    }
    Component paymentNote =
        text(
            context,
            "payment.note",
            BeaconPyramid.effectSeconds(1),
            BeaconPyramid.effectSeconds(BeaconPyramid.MAX_LEVELS));
    return List.of(
        title(context, text(context, "base"), CardTint.STONE),
        iconRow(context, blocks, List.of()),
        muted(context, text(context, "base.note", layerSides())),
        title(context, text(context, "payment"), CardTint.STONE),
        iconRow(context, payments, List.of(paymentNote)));
  }

  /**
   * Item icons in a row, each with its name and {@code details} as tooltip; a dash when the tag is
   * empty.
   */
  private static SheetElement iconRow(
      SheetContext context, List<ItemStack> stacks, List<Component> details) {
    if (stacks.isEmpty()) {
      return context.label(Component.literal("–"), SheetPalette.MUTED_TEXT);
    }
    List<SheetElement> icons = new ArrayList<>(stacks.size());
    for (ItemStack stack : stacks) {
      icons.add(new ItemIcon(stack, 1, Tooltips.ofItem(context.font(), stack, details)));
    }
    return new ElementRow(icons, LIST_GAP);
  }

  /**
   * The 42 places where frame blocks count (the water around the conduit in its tooltip), the rule
   * in one line and the frame blocks.
   */
  private static List<SheetElement> shapeTile(SheetContext context, PictureAtlas atlas) {
    Component title = text(context, "conduit.ring");
    Component rule = text(context, "conduit.rule", ConduitFrame.MIN_ACTIVE_BLOCKS);
    List<ItemStack> frame = new ArrayList<>();
    for (Holder<Block> block :
        BuiltInRegistries.BLOCK.getTagOrEmpty(BlockTags.CONDUIT_EFFECT_BLOCK)) {
      frame.add(new ItemStack(block.value()));
    }
    IconGrid.Builder icons =
        new IconGrid.Builder(context.font(), FRAME_ICON_COLUMNS, ICON_GAP, ICON_GAP);
    for (ItemStack stack : frame) {
      icons.cell(
          new IconGrid.Flow(context.font())
              .item(stack, null)
              .build(Tooltips.ofItem(context.font(), stack, List.of())));
    }
    return List.of(
        framePicture(
            context,
            atlas,
            ConduitFrame.FULL_FRAME_BLOCKS,
            Tooltips.of(
                context.font(),
                title,
                List.of(
                    rule,
                    text(
                        context,
                        "conduit.ring.detail",
                        ConduitFrame.MIN_ACTIVE_BLOCKS,
                        ConduitFrame.FULL_FRAME_BLOCKS),
                    text(context, "conduit.water")))),
        centredTitle(context, title, CardTint.BLUE),
        new CentredText(context.font(), rule, TEXT_WIDTH, SheetPalette.TEXT),
        frame.isEmpty() ? SlotTiles.EMPTY : icons.build());
  }

  /**
   * Conduit Power's range at each frame size from the minimum to the full frame, in two columns:
   * the frame block with the count on it and the range (the step's name in its tooltip), then what
   * the full frame adds.
   */
  private static List<SheetElement> stepsTile(SheetContext context) {
    Component power = MobEffects.CONDUIT_POWER.value().getDisplayName();
    List<FormattedCharSequence> powerTooltip =
        Tooltips.of(context.font(), power, List.of(text(context, "conduit.power")));
    ItemStack frameBlock = stack(context, ConduitMaterials.FRAME_RECIPES.getFirst().resultId());
    IconGrid.Builder lines =
        new IconGrid.Builder(context.font(), STEP_COLUMNS, STEP_COLUMN_GAP, LIST_GAP);
    for (int blocks : ConduitFrame.rangeSteps()) {
      int range = ConduitFrame.range(blocks);
      boolean full = blocks >= ConduitFrame.FULL_FRAME_BLOCKS;
      String step =
          blocks == ConduitFrame.MIN_ACTIVE_BLOCKS
              ? "conduit.step.minimum"
              : full ? "conduit.step.full" : "conduit.step.partial";
      List<Component> details = new ArrayList<>();
      details.add(text(context, "conduit.blocks", blocks));
      details.add(text(context, "conduit.range", range));
      if (full) {
        details.add(text(context, "conduit.attack", ConduitFrame.ATTACK_RANGE));
      }
      List<FormattedCharSequence> tooltip =
          Tooltips.of(context.font(), text(context, step), details);
      // The frame block with the count on it, as a stack of that many, then the range.
      lines.cell(
          new IconGrid.Flow(context.font())
              .item(frameBlock, Integer.toString(blocks))
              .text(Component.literal(" " + RANGE_SIGN), SheetPalette.MUTED_TEXT)
              .text(Component.literal(Integer.toString(range)), SheetPalette.HIGHLIGHT_TEXT)
              .build(tooltip));
    }
    return List.of(
        iconLine(
            context,
            line(
                context,
                new IconGrid.Flow(context.font())
                    .sprite(Hud.getMobEffectSprite(MobEffects.CONDUIT_POWER), EFFECT_SIZE)
                    .build(powerTooltip)),
            power.copy().withStyle(ChatFormatting.BOLD),
            CardTint.BLUE.title()),
        lines.build(),
        muted(context, text(context, "conduit.attack", ConduitFrame.ATTACK_RANGE)));
  }

  /** The full frame around the conduit, at the full frame's scale. */
  private static SheetElement framePicture(
      SheetContext context, PictureAtlas atlas, int blocks, List<FormattedCharSequence> tooltip) {
    return new ItemOverPicture(
        atlas.add(
            Scene.of(BeaconDioramas.conduitFrame(blocks)).focusedOn(FRAME_BOX),
            FRAME_PICTURE_SIZE,
            FRAME_PICTURE_SIZE,
            tooltip),
        new ItemStack(Items.CONDUIT));
  }

  /** The conduit's recipe: its name, the grid and each ingredient (its sources in its tooltip). */
  private static List<SheetElement> conduitRecipeTile(SheetContext context) {
    GridRecipe recipe = ConduitMaterials.CONDUIT_RECIPE;
    List<SheetElement> ingredients = new ArrayList<>();
    for (Map.Entry<String, Integer> ingredient : recipe.counts().entrySet()) {
      ItemStack stack = stack(context, ingredient.getKey());
      ingredients.add(
          iconLine(
              context,
              new ItemIcon(stack, 1, ingredientTooltip(context, ingredient.getKey())),
              Component.literal(ingredient.getValue() + COUNT_SIGN).append(stack.getHoverName()),
              SheetPalette.TEXT));
    }
    return List.of(
        title(context, text(context, "recipes"), CardTint.STONE),
        craftingGrid(context, recipe),
        new LeftColumn(ingredients, RECIPE_GAP));
  }

  /** A crafting grid; each ingredient's tooltip says where it comes from. */
  private static SheetElement craftingGrid(SheetContext context, GridRecipe recipe) {
    ItemStack result = stack(context, recipe.resultId());
    List<CraftingGrid.Slot> slots = new ArrayList<>(GridRecipe.SIDE * GridRecipe.SIDE);
    for (Optional<String> slot : recipe.slots()) {
      if (slot.isEmpty()) {
        slots.add(CraftingGrid.Slot.EMPTY);
        continue;
      }
      slots.add(
          new CraftingGrid.Slot(
              stack(context, slot.get()), ingredientTooltip(context, slot.get())));
    }
    List<Component> resultDetails = new ArrayList<>();
    for (Map.Entry<String, Integer> ingredient : recipe.counts().entrySet()) {
      resultDetails.add(
          Component.literal(ingredient.getValue() + COUNT_SIGN)
              .append(stack(context, ingredient.getKey()).getHoverName()));
    }
    if (recipe.shapeless()) {
      resultDetails.add(text(context, "shapeless"));
    }
    return new CraftingGrid(
        context.font(), slots, result, Tooltips.ofItem(context.font(), result, resultDetails));
  }

  /** The item's name, then one line per place it comes from (black dye: what it is made of). */
  private static List<FormattedCharSequence> ingredientTooltip(
      SheetContext context, String itemId) {
    ItemStack stack = stack(context, itemId);
    List<Component> lines = new ArrayList<>();
    for (MaterialSource source : ConduitMaterials.sourcesOf(itemId)) {
      lines.add(sourceText(context, source));
    }
    if (itemId.equals(ConduitMaterials.BLACK_DYE)) {
      for (DyeRecipe dye : DyeTable.recipesFor(ConduitMaterials.BLACK_DYE_COLOR)) {
        lines.add(stack(context, dye.ingredients().getFirst()).getHoverName());
      }
    }
    return Tooltips.ofItem(context.font(), stack, lines);
  }

  /** Where a material comes from, in one line ({@code source.<key>}). */
  static Component sourceText(SheetContext context, MaterialSource source) {
    String key = "source." + source.key();
    return source.amount().isPresent()
        ? text(context, key, source.amount().getAsInt())
        : text(context, key);
  }

  /** An icon with text wrapped beside it, so the line never widens its tile. */
  private static SheetElement iconLine(
      SheetContext context, SheetElement icon, Component text, int color) {
    return new ElementRow(
        List.of(
            icon, context.paragraph(text, TEXT_WIDTH - icon.minWidth() - ElementRow.GAP, color)),
        ElementRow.GAP);
  }

  /** One drawn line (icons, sprites and text side by side) as an element. */
  private static SheetElement line(SheetContext context, IconGrid.Cell cell) {
    return new IconGrid.Builder(context.font(), 1, 0, 0).cell(cell).build();
  }

  private static SheetElement title(SheetContext context, Component title, CardTint tint) {
    return context.paragraph(title.copy().withStyle(ChatFormatting.BOLD), TEXT_WIDTH, tint.title());
  }

  private static SheetElement centredTitle(SheetContext context, Component title, CardTint tint) {
    return new CentredText(
        context.font(), title.copy().withStyle(ChatFormatting.BOLD), TEXT_WIDTH, tint.title());
  }

  /** A muted note with its lines centred, for centred tiles. */
  private static SheetElement centred(SheetContext context, Component note) {
    return new CentredText(context.font(), note, TEXT_WIDTH, SheetPalette.MUTED_TEXT);
  }

  private static SheetElement muted(SheetContext context, Component note) {
    return context.paragraph(note, TEXT_WIDTH, SheetPalette.MUTED_TEXT);
  }

  private static ItemStack stack(SheetContext context, String itemId) {
    return context.stack(NAMESPACE + itemId);
  }

  private static List<Table.Align> centred(int slots) {
    return Collections.nCopies(slots, Table.Align.CENTER);
  }

  /** "3×3, 5×5, 7×7, 9×9": each layer's side, from the top. */
  private static String layerSides() {
    StringBuilder sides = new StringBuilder();
    for (int level = 1; level <= BeaconPyramid.MAX_LEVELS; level++) {
      if (level > 1) {
        sides.append(LIST_SEPARATOR);
      }
      int side = BeaconPyramid.layerSide(level);
      sides.append(side).append(TIMES).append(side);
    }
    return sides.toString();
  }

  private static Component effectNames(List<Holder<MobEffect>> effects) {
    MutableComponent names = Component.empty();
    for (int index = 0; index < effects.size(); index++) {
      if (index > 0) {
        names.append(LIST_SEPARATOR);
      }
      names.append(effects.get(index).value().getDisplayName());
    }
    return names;
  }

  private static Component text(SheetContext context, String key, Object... args) {
    return context.tabText(ID, key, args);
  }
}
