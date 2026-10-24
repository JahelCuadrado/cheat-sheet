package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.picture.Cast;
import com.einsteinmods.cheatsheet.client.picture.Casts;
import com.einsteinmods.cheatsheet.client.picture.Picture;
import com.einsteinmods.cheatsheet.client.picture.PictureAtlas;
import com.einsteinmods.cheatsheet.client.picture.Scene;
import com.einsteinmods.cheatsheet.client.sheet.CardTint;
import com.einsteinmods.cheatsheet.client.sheet.ElementRow;
import com.einsteinmods.cheatsheet.client.sheet.Section;
import com.einsteinmods.cheatsheet.client.sheet.Sheet;
import com.einsteinmods.cheatsheet.client.sheet.SheetContext;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import com.einsteinmods.cheatsheet.client.sheet.SheetPalette;
import com.einsteinmods.cheatsheet.client.sheet.SheetSource;
import com.einsteinmods.cheatsheet.client.sheet.Table;
import com.einsteinmods.cheatsheet.client.sheet.TextLabel;
import com.einsteinmods.cheatsheet.client.sheet.TileGrid;
import com.einsteinmods.cheatsheet.client.sheet.Tooltips;
import com.einsteinmods.cheatsheet.client.sheet.ZoomCard;
import com.einsteinmods.cheatsheet.core.ZoomSize;
import com.einsteinmods.cheatsheet.core.farming.AnimalOrigin;
import com.einsteinmods.cheatsheet.core.farming.AnimalOrigins;
import com.einsteinmods.cheatsheet.core.farming.AnimalVariants;
import com.einsteinmods.cheatsheet.core.farming.BreedingFood;
import com.einsteinmods.cheatsheet.core.farming.BreedingTable;
import com.einsteinmods.cheatsheet.core.farming.FarmPlan;
import com.einsteinmods.cheatsheet.core.farming.FarmPlans;
import com.einsteinmods.cheatsheet.core.farming.FarmRules;
import com.einsteinmods.cheatsheet.core.farming.PlanCell;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Farming tab, one screen at a glance: the farm layouts players keep looking up, as top-down plans
 * drawn with the game's textures, then what each animal is fed to breed.
 *
 * <p>"Crops": four equal tiles, each a plan with one short caption: the sugar cane layout that
 * fills a 16x16 field (zoomed, four layouts side by side as diagrams, with their canes, their share
 * of the field and why); a 9x9 field hydrated by one water block with crops in alternating rows;
 * pumpkin and melon stems each beside a free block for the fruit; cactus in a checkerboard on sand.
 * Each block's tooltip says the rule it shows. Plans and rules come from {@code core.farming}.
 *
 * <p>"Breeding": an equal card per animal, a small baked portrait of it (a heart when it must be
 * tamed first), its name and its breeding food, read from the item tags its {@code isFood} tests,
 * so data packs and mods are reflected; horses, donkeys and llamas use the items their code names.
 * Zoomed, a card shows the animal bigger with every food, where it is found, how to bring it home
 * or tame it and how it is ridden ({@code core.farming.AnimalOrigins}, checked against the game's
 * spawn data by a GameTest); its zoom-only portrait is baked on the first click. Beside it, a grid
 * of the animal's variants ({@link AnimalVariantsView}): each look, its name and where it comes
 * from.
 */
public final class FarmingSheet implements SheetSource {
  public static final String ID = "farming";

  /** Four crop tiles fill a row of a 960-wide sheet (6 px margins). */
  private static final int CROP_TILE_WIDTH = 234;

  /** Room for a portrait and five food icons: 24 cards make four rows of six at 960 wide. */
  private static final int ANIMAL_CARD_WIDTH = 138;

  /** An animal's portrait: a little taller than its name and food, room for a camel's humps. */
  private static final int PORTRAIT_WIDTH = 40;

  private static final int PORTRAIT_HEIGHT = 36;

  /**
   * Room around an animal's hitbox, as a share of its larger side: models stick out of it (a
   * horse's head, a sniffer's snout, a bee's wings, a nautilus' shell).
   */
  private static final double PORTRAIT_MARGIN = 0.2;

  private static final int PORTRAIT_GAP = 4;
  private static final int NAME_GAP = 2;

  /** Room right of the portrait for the name and the food icons. */
  private static final int ANIMAL_TEXT_WIDTH =
      ANIMAL_CARD_WIDTH - SlotTiles.PADDING * 2 - PORTRAIT_WIDTH - PORTRAIT_GAP;

  private static final int CAPTION_WIDTH = CROP_TILE_WIDTH - SlotTiles.PADDING * 2;

  /** Food icons a card shows; the last one stands for the rest when there are more. */
  private static final int SHOWN_FOODS = 5;

  private static final int FOOD_GAP = 1;

  /** The zoomed animal: its portrait, and the width of everything under it. */
  private static final int ZOOM_PORTRAIT_WIDTH = 72;

  private static final int ZOOM_PORTRAIT_HEIGHT = 64;
  private static final int ZOOM_WIDTH = 220;
  private static final int ZOOM_FOODS_PER_ROW = 8;
  private static final int ZOOM_FOOD_ROWS = 2;
  private static final int ZOOM_SECTION_GAP = 6;
  private static final int ZOOM_LINE_GAP = 2;

  /** Between the zoomed animal's details and its variants beside them. */
  private static final int VARIANTS_GAP = 10;

  /** Biomes named in a zoomed animal's line; the rest are counted, all named in its tooltip. */
  private static final int SHOWN_BIOMES = 4;

  private static final String BIOME_KEY_PREFIX = "biome";
  private static final String NAME_SEPARATOR = ": ";
  private static final int HEART_SIZE = 8;
  private static final Identifier HEART = Identifier.withDefaultNamespace("hud/heart/full");

  /** Untinted texture. */
  private static final int WHITE = 0xFFFFFFFF;

  /** Water's colour in plains and most biomes ({@code OverworldBiomes.NORMAL_WATER_COLOR}). */
  private static final int WATER_COLOR = 0xFF3F76E4;

  /** The zoomed sugar cane layouts: flat colours like a printed diagram, 6 pixels a block. */
  private static final int DIAGRAM_CELL = 6;

  private static final int DIAGRAM_LINE = 0xFF1A1F1A;
  private static final int DIAGRAM_CANE = 0xFF94C93D;
  private static final int DIAGRAM_FREE = 0xFFE0E0E0;
  private static final int DIAGRAM_GAP = 3;
  private static final int LAYOUT_GAP = 10;
  private static final int ZOOM_GAP = 6;
  private static final int PERCENT = 100;

  /** A share of the field: whole, or with one decimal (62.5). */
  private static final String SHARE_PATTERN = "0.#";

  private static final char LANGUAGE_SEPARATOR = '_';
  private static final char LOCALE_SEPARATOR = '-';

  /** A fully grown stem's tint ({@code BlockTintSources.stem} at age 7). */
  private static final int GROWN_STEM_COLOR = 0xFFE0C71C;

  private static final String PLUS = "+";
  private static final String LIST_SEPARATOR = ", ";

  private static final SpriteId WATER = block("water_still");
  private static final SpriteId DIRT = block("dirt");
  private static final SpriteId FARMLAND = block("farmland_moist");
  private static final SpriteId SAND = block("sand");
  private static final SpriteId SUGAR_CANE =
      Sheets.ITEMS_MAPPER.defaultNamespaceApply("sugar_cane");
  private static final SpriteId WHEAT = block("wheat_stage7");
  private static final SpriteId CARROTS = block("carrots_stage3");
  private static final SpriteId PUMPKIN_STEM = block("pumpkin_stem");
  private static final SpriteId MELON_STEM = block("melon_stem");
  private static final SpriteId PUMPKIN = block("pumpkin_top");
  private static final SpriteId MELON = block("melon_top");
  private static final SpriteId CACTUS = block("cactus_top");

  /** Every texture the plans draw: the client GameTest checks none is missing. */
  public static final List<SpriteId> SPRITES =
      List.of(
          WATER,
          DIRT,
          FARMLAND,
          SAND,
          SUGAR_CANE,
          WHEAT,
          CARROTS,
          PUMPKIN_STEM,
          MELON_STEM,
          PUMPKIN,
          MELON,
          CACTUS);

  /** Title, plan and caption, centred. */
  private static final List<Table.Align> CROP_SLOTS = Collections.nCopies(3, Table.Align.CENTER);

  private static final List<Table.Align> ANIMAL_SLOTS = List.of(Table.Align.START);

  @Override
  public String id() {
    return ID;
  }

  @Override
  public ItemStack icon() {
    return new ItemStack(Items.SUGAR_CANE);
  }

  @Override
  public Sheet build(SheetContext context) {
    AtlasManager atlases = context.minecraft().getAtlasManager();
    SlotTiles cropTiles = new SlotTiles(CROP_TILE_WIDTH);
    SlotTiles.Layout crops = cropTiles.layout(CardTint.GREEN, CROP_SLOTS);
    crops.add(sugarCaneTile(context, atlases));
    crops.add(fieldTile(context, atlases));
    crops.add(stemsTile(context, atlases));
    crops.add(cactusTile(context, atlases));
    cropTiles.build();
    List<SheetElement> cropCards = new ArrayList<>(crops.tiles());
    cropCards.set(0, ZoomViews.zoomedAs(cropCards.getFirst(), () -> sugarCaneZoom(context)));

    PictureAtlas atlas = new PictureAtlas(ID);
    SlotTiles animalTiles = new SlotTiles(ANIMAL_CARD_WIDTH);
    SlotTiles.Layout animals = animalTiles.layout(CardTint.GOLD, ANIMAL_SLOTS);
    List<Supplier<ZoomCard>> animalZooms = new ArrayList<>();
    for (BreedingFood animal : BreedingTable.ANIMALS) {
      AnimalCard card = animalCard(context, animal, atlas);
      if (card != null) {
        animals.add(List.of(card.card()));
        animalZooms.add(card.zoom());
      }
    }
    animalTiles.build();
    List<SheetElement> animalCards = new ArrayList<>(animals.tiles().size());
    for (int index = 0; index < animals.tiles().size(); index++) {
      animalCards.add(ZoomViews.zoomedAs(animals.tiles().get(index), animalZooms.get(index)));
    }

    List<Section> sections = new ArrayList<>();
    sections.add(
        Section.of(text(context, "crops"), CardTint.GREEN, new TileGrid(cropCards, SlotTiles.GAP)));
    if (!animalCards.isEmpty()) {
      sections.add(
          Section.of(
              text(context, "breeding"), CardTint.GOLD, new TileGrid(animalCards, SlotTiles.GAP)));
    }
    return Sheet.ofSections(sections, atlas);
  }

  /** The sugar cane layout that fills a 16x16 field, textures at half size. */
  private static List<SheetElement> sugarCaneTile(SheetContext context, AtlasManager atlases) {
    Map<PlanCell, FarmPlanView.Look> looks = new EnumMap<>(PlanCell.class);
    looks.put(
        PlanCell.WATER,
        water(context, atlases, name(Blocks.WATER), text(context, "sugar_cane.water")));
    looks.put(PlanCell.SUGAR_CANE, look(atlases, DIRT, SUGAR_CANE, WHITE, caneTooltip(context)));
    FarmPlan plan = FarmPlans.CANE_DENSEST;
    return cropTile(
        context,
        name(Blocks.SUGAR_CANE),
        new FarmPlanView(plan, looks, FarmPlanView.SMALL_BLOCK),
        text(
            context,
            "sugar_cane.caption",
            plan.count(PlanCell.SUGAR_CANE),
            plan.width(),
            plan.height(),
            share(context, plan)));
  }

  /**
   * The zoomed sugar cane card: the four layouts side by side, each numbered, as a diagram, with
   * its canes and their share of the field; why below.
   */
  private static ZoomCard sugarCaneZoom(SheetContext context) {
    Map<PlanCell, PlanDiagram.Swatch> swatches = new EnumMap<>(PlanCell.class);
    swatches.put(
        PlanCell.WATER,
        new PlanDiagram.Swatch(
            WATER_COLOR, tooltip(context, name(Blocks.WATER), text(context, "sugar_cane.water"))));
    swatches.put(PlanCell.SUGAR_CANE, new PlanDiagram.Swatch(DIAGRAM_CANE, caneTooltip(context)));
    swatches.put(
        PlanCell.DIRT,
        new PlanDiagram.Swatch(
            DIAGRAM_FREE, tooltip(context, name(Blocks.DIRT), text(context, "sugar_cane.free"))));
    List<SheetElement> layouts = new ArrayList<>(FarmPlans.CANE_LAYOUTS.size());
    for (int index = 0; index < FarmPlans.CANE_LAYOUTS.size(); index++) {
      FarmPlan plan = FarmPlans.CANE_LAYOUTS.get(index);
      layouts.add(
          new ZoomViews.CentredColumn(
              List.of(
                  context.label(
                      Component.literal(Integer.toString(index + 1)).withStyle(ChatFormatting.BOLD),
                      CardTint.GREEN.title()),
                  new PlanDiagram(plan, swatches, DIAGRAM_CELL, DIAGRAM_LINE),
                  context.bigLabel(
                      Component.literal(Integer.toString(plan.count(PlanCell.SUGAR_CANE))),
                      SheetPalette.HIGHLIGHT_TEXT),
                  context.label(share(context, plan), SheetPalette.TEXT)),
              DIAGRAM_GAP));
    }
    ElementRow row = new ElementRow(layouts, LAYOUT_GAP);
    return ZoomViews.panel(
        new ZoomViews.CentredColumn(
            List.of(
                context.label(
                    name(Blocks.SUGAR_CANE).copy().withStyle(ChatFormatting.BOLD),
                    CardTint.GREEN.title()),
                row,
                context.paragraph(
                    text(
                        context,
                        "sugar_cane.zoom",
                        FarmPlans.CANE_DENSEST.count(PlanCell.SUGAR_CANE)
                            - FarmPlans.CANE_CROSSES.count(PlanCell.SUGAR_CANE)),
                    row.minWidth(),
                    SheetPalette.TEXT)),
            ZOOM_GAP),
        CardTint.GREEN,
        ZoomSize.LARGE);
  }

  private static List<FormattedCharSequence> caneTooltip(SheetContext context) {
    return tooltip(
        context,
        name(Blocks.SUGAR_CANE),
        text(context, "sugar_cane.cane", FarmRules.MAX_PLANT_HEIGHT));
  }

  /** The share of {@code plan} that is cane, "75%" in the game's language. */
  private static Component share(SheetContext context, FarmPlan plan) {
    double percent =
        (double) plan.count(PlanCell.SUGAR_CANE) * PERCENT / (plan.width() * plan.height());
    return text(
        context,
        "sugar_cane.share",
        new DecimalFormat(SHARE_PATTERN, DecimalFormatSymbols.getInstance(locale(context)))
            .format(percent));
  }

  /** The game's language as a {@link Locale}, to write numbers its way. */
  static Locale locale(SheetContext context) {
    String code = context.minecraft().getLanguageManager().getSelected();
    return Locale.forLanguageTag(code.replace(LANGUAGE_SEPARATOR, LOCALE_SEPARATOR));
  }

  /** A 9x9 field around one water block, crops in alternating rows. */
  private static List<SheetElement> fieldTile(SheetContext context, AtlasManager atlases) {
    Component water =
        text(
            context,
            "field.water",
            FarmRules.HYDRATION_REACH,
            FarmRules.HYDRATED_SIDE,
            FarmRules.HYDRATED_SIDE);
    Component rows = text(context, "field.crop");
    Map<PlanCell, FarmPlanView.Look> looks = new EnumMap<>(PlanCell.class);
    looks.put(PlanCell.WATER, water(context, atlases, name(Blocks.WATER), water));
    looks.put(
        PlanCell.WHEAT,
        look(atlases, FARMLAND, WHEAT, WHITE, tooltip(context, name(Blocks.WHEAT), rows)));
    looks.put(
        PlanCell.CARROTS,
        look(atlases, FARMLAND, CARROTS, WHITE, tooltip(context, name(Blocks.CARROTS), rows)));
    return cropTile(
        context,
        name(Blocks.FARMLAND),
        new FarmPlanView(FarmPlans.FIELD, looks),
        text(context, "field.caption", FarmRules.HYDRATION_REACH));
  }

  /** Stems along a water row, each beside a free block for its fruit. */
  private static List<SheetElement> stemsTile(SheetContext context, AtlasManager atlases) {
    Component stem = text(context, "stems.stem");
    Component fruit = text(context, "stems.fruit");
    Map<PlanCell, FarmPlanView.Look> looks = new EnumMap<>(PlanCell.class);
    looks.put(
        PlanCell.WATER, water(context, atlases, name(Blocks.WATER), text(context, "stems.water")));
    looks.put(
        PlanCell.PUMPKIN_STEM,
        look(
            atlases,
            FARMLAND,
            PUMPKIN_STEM,
            GROWN_STEM_COLOR,
            tooltip(context, name(Blocks.PUMPKIN_STEM), stem)));
    looks.put(
        PlanCell.MELON_STEM,
        look(
            atlases,
            FARMLAND,
            MELON_STEM,
            GROWN_STEM_COLOR,
            tooltip(context, name(Blocks.MELON_STEM), stem)));
    looks.put(
        PlanCell.PUMPKIN,
        look(atlases, PUMPKIN, null, WHITE, tooltip(context, name(Blocks.PUMPKIN), fruit)));
    looks.put(
        PlanCell.MELON,
        look(atlases, MELON, null, WHITE, tooltip(context, name(Blocks.MELON), fruit)));
    looks.put(
        PlanCell.DIRT,
        look(
            atlases,
            DIRT,
            null,
            WHITE,
            tooltip(context, name(Blocks.DIRT), text(context, "stems.free"))));
    return cropTile(
        context,
        text(context, "stems.title"),
        new FarmPlanView(FarmPlans.STEMS, looks),
        text(context, "stems.caption"));
  }

  /** Cactus in a checkerboard on sand. */
  private static List<SheetElement> cactusTile(SheetContext context, AtlasManager atlases) {
    Map<PlanCell, FarmPlanView.Look> looks = new EnumMap<>(PlanCell.class);
    looks.put(
        PlanCell.CACTUS,
        look(
            atlases,
            SAND,
            CACTUS,
            WHITE,
            tooltip(
                context,
                name(Blocks.CACTUS),
                text(context, "cactus.cactus", FarmRules.MAX_PLANT_HEIGHT))));
    looks.put(
        PlanCell.SAND,
        look(
            atlases,
            SAND,
            null,
            WHITE,
            tooltip(context, name(Blocks.SAND), text(context, "cactus.sand"))));
    return cropTile(
        context,
        name(Blocks.CACTUS),
        new FarmPlanView(FarmPlans.CACTUS, looks),
        text(context, "cactus.caption"));
  }

  /** A crop tile's three slots: its title, the plan and the caption under it. */
  private static List<SheetElement> cropTile(
      SheetContext context, Component title, FarmPlanView plan, Component caption) {
    return List.of(
        new CentredText(
            context.font(),
            title.copy().withStyle(ChatFormatting.BOLD),
            CAPTION_WIDTH,
            CardTint.GREEN.title()),
        plan,
        new CentredText(context.font(), caption, CAPTION_WIDTH, SheetPalette.TEXT));
  }

  /** An animal's card on the sheet and the view it zooms as, built on its first click. */
  private record AnimalCard(SheetElement card, Supplier<ZoomCard> zoom) {}

  /**
   * One animal's card: its portrait (names, food and taming in its tooltip; a heart when it must be
   * tamed), its name and its food. Null when the animal or its food is not in the game.
   */
  private static @Nullable AnimalCard animalCard(
      SheetContext context, BreedingFood animal, PictureAtlas atlas) {
    List<EntityType<?>> types = new ArrayList<>(animal.entityIds().size());
    for (String id : animal.entityIds()) {
      BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(id)).ifPresent(types::add);
    }
    List<ItemStack> foods = foods(animal.food());
    if (types.isEmpty() || foods.isEmpty()) {
      return null;
    }
    MutableComponent names = Component.empty();
    for (int index = 0; index < types.size(); index++) {
      if (index > 0) {
        names.append(LIST_SEPARATOR);
      }
      names.append(types.get(index).getDescription());
    }
    List<Component> details = new ArrayList<>();
    details.add(text(context, "breed", itemNames(foods, 0)));
    if (animal.tamed()) {
      details.add(text(context, "tame"));
    }
    List<FormattedCharSequence> tooltip = Tooltips.of(context.font(), names, details);
    Picture picture =
        atlas.add(portraitScene(types.getFirst()), PORTRAIT_WIDTH, PORTRAIT_HEIGHT, tooltip);
    SheetElement portrait = animal.tamed() ? new TamedPortrait(picture) : picture;
    TextLabel name =
        new TextLabel(
            context.font(),
            FittedText.fitted(
                context.font(),
                types.getFirst().getDescription().copy().withStyle(ChatFormatting.BOLD),
                ANIMAL_TEXT_WIDTH),
            CardTint.GOLD.title(),
            1,
            tooltip);
    SheetElement card =
        new ElementRow(
            List.of(
                portrait,
                new ElementColumn(
                    List.of(name, foodIcons(context, foods, SHOWN_FOODS, SHOWN_FOODS)), NAME_GAP)),
            PORTRAIT_GAP);
    return new AnimalCard(card, () -> animalZoom(context, animal, types, foods, tooltip, atlas));
  }

  /**
   * Up to {@code limit} of {@code foods} in rows of {@code columns}; when there are more, the last
   * one shown counts them ("+3", named in its tooltip).
   */
  private static SheetElement foodIcons(
      SheetContext context, List<ItemStack> foods, int limit, int columns) {
    boolean more = foods.size() > limit;
    int shown = Math.min(foods.size(), limit);
    IconGrid.Builder icons =
        new IconGrid.Builder(context.font(), Math.min(shown, columns), FOOD_GAP, FOOD_GAP);
    for (int index = 0; index < shown; index++) {
      ItemStack food = foods.get(index);
      boolean last = more && index == shown - 1;
      List<FormattedCharSequence> foodTooltip =
          last
              ? Tooltips.ofItem(
                  context.font(), food, List.of(text(context, "more", itemNames(foods, shown))))
              : Tooltips.ofItem(context.font(), food, List.of());
      icons.cell(
          new IconGrid.Flow(context.font())
              .item(food, last ? PLUS + (foods.size() - shown) : null)
              .build(foodTooltip));
    }
    return icons.build();
  }

  /**
   * The zoomed animal: its portrait big beside its name and every food; then where it is found
   * (biomes, the first few named, every one in the tooltip; structures; other ways), how to bring
   * it home or tame it (with the items it takes) and whether it is ridden (with what).
   */
  private static ZoomCard animalZoom(
      SheetContext context,
      BreedingFood animal,
      List<EntityType<?>> types,
      List<ItemStack> foods,
      List<FormattedCharSequence> tooltip,
      PictureAtlas atlas) {
    Font font = context.font();
    Picture picture =
        atlas.addZoomOnly(
            portraitScene(types.getFirst()), ZOOM_PORTRAIT_WIDTH, ZOOM_PORTRAIT_HEIGHT, tooltip);
    SheetElement portrait = animal.tamed() ? new TamedPortrait(picture) : picture;
    int headWidth = ZOOM_WIDTH - ZOOM_PORTRAIT_WIDTH - PORTRAIT_GAP;
    SheetElement head =
        new ElementColumn(
            List.of(
                new TextLabel(
                    font,
                    FittedText.fitted(
                        font,
                        types.getFirst().getDescription().copy().withStyle(ChatFormatting.BOLD),
                        headWidth),
                    CardTint.GOLD.title(),
                    1,
                    tooltip),
                context.label(text(context, "zoom.breed"), SheetPalette.MUTED_TEXT),
                foodIcons(context, foods, ZOOM_FOOD_ROWS * ZOOM_FOODS_PER_ROW, ZOOM_FOODS_PER_ROW)),
            NAME_GAP);
    List<SheetElement> parts = new ArrayList<>();
    parts.add(new ElementRow(List.of(portrait, head), PORTRAIT_GAP));
    Optional<AnimalOrigin> found = AnimalOrigins.of(animal.entityIds().getFirst());
    if (found.isPresent()) {
      AnimalOrigin origin = found.get();
      List<SheetElement> where = new ArrayList<>();
      for (AnimalOrigin.Source source : origin.sources()) {
        where.add(sourceLines(context, animal, source));
      }
      parts.add(zoomSection(context, "zoom.where", List.of(), where));
      parts.add(
          zoomSection(
              context,
              "zoom.get",
              origin.tamingItems().map(FarmingSheet::foods).orElse(List.of()),
              List.of(zoomText(context, text(context, "get." + origin.obtainKey())))));
      List<ItemStack> rideItems = new ArrayList<>();
      for (String itemId : origin.riding().itemIds()) {
        ItemStack stack = context.stack(itemId);
        if (!stack.isEmpty()) {
          rideItems.add(stack);
        }
      }
      parts.add(
          zoomSection(
              context,
              "zoom.ride",
              rideItems,
              List.of(
                  zoomText(
                      context,
                      text(context, "ride." + origin.riding().name().toLowerCase(Locale.ROOT))))));
    }
    SheetElement details = new ElementColumn(parts, ZOOM_SECTION_GAP);
    SheetElement variants =
        AnimalVariants.of(animal.entityIds().getFirst())
            .map(set -> AnimalVariantsView.of(context, set, atlas))
            .orElse(null);
    if (variants == null) {
      return ZoomViews.panel(details, CardTint.GOLD, ZoomSize.MEDIUM);
    }
    return ZoomViews.panel(
        AnimalVariantsView.beside(details, variants, VARIANTS_GAP), CardTint.GOLD, ZoomSize.LARGE);
  }

  /** A part of the zoomed animal: its heading, the items it names (if any) and its lines. */
  private static SheetElement zoomSection(
      SheetContext context, String headingKey, List<ItemStack> items, List<SheetElement> lines) {
    List<SheetElement> children = new ArrayList<>();
    children.add(
        context.label(
            text(context, headingKey).copy().withStyle(ChatFormatting.BOLD),
            CardTint.GOLD.title()));
    if (!items.isEmpty()) {
      children.add(foodIcons(context, items, ZOOM_FOODS_PER_ROW, ZOOM_FOODS_PER_ROW));
    }
    children.addAll(lines);
    return new ElementColumn(children, ZOOM_LINE_GAP);
  }

  /**
   * One way an animal is found, wrapped to the zoom's width; biomes beyond a few in its tooltip.
   */
  private static SheetElement sourceLines(
      SheetContext context, BreedingFood animal, AnimalOrigin.Source source) {
    return switch (source) {
      case AnimalOrigin.NaturalSpawn spawn -> {
        Component title =
            spawn.entityId().equals(animal.entityIds().getFirst())
                ? text(context, "zoom.biomes")
                : entityName(spawn.entityId());
        MutableComponent shown =
            spawn.entityId().equals(animal.entityIds().getFirst())
                ? Component.empty()
                : entityName(spawn.entityId()).copy().append(NAME_SEPARATOR);
        yield biomeLines(context, title, shown, spawn.biomeIds(), null);
      }
      case AnimalOrigin.BeeNests nests ->
          biomeLines(
              context,
              entityName(animal.entityIds().getFirst()),
              null,
              nests.biomeIds(),
              "place.bee_nests");
      case AnimalOrigin.StructureTemplates templates ->
          zoomText(context, text(context, "place." + templates.placeKey()));
      case AnimalOrigin.StructureSpawn spawn ->
          zoomText(context, text(context, "place." + spawn.placeKey()));
      case AnimalOrigin.Archaeology archaeology ->
          zoomText(context, text(context, "place." + archaeology.placeKey()));
      case AnimalOrigin.Special special ->
          zoomText(context, text(context, "place." + special.placeKey()));
    };
  }

  /**
   * The first {@link #SHOWN_BIOMES} of {@code biomeIds} by name, "+N" for the rest, after {@code
   * prefix} or inside the text {@code key} names; every name in the tooltip.
   */
  private static SheetElement biomeLines(
      SheetContext context,
      Component title,
      @Nullable MutableComponent prefix,
      List<String> biomeIds,
      @Nullable String key) {
    MutableComponent shown = Component.empty();
    MutableComponent all = Component.empty();
    for (int index = 0; index < biomeIds.size(); index++) {
      Component name = biomeName(biomeIds.get(index));
      if (index > 0) {
        all.append(LIST_SEPARATOR);
      }
      all.append(name);
      if (index < SHOWN_BIOMES) {
        if (index > 0) {
          shown.append(LIST_SEPARATOR);
        }
        shown.append(name);
      }
    }
    if (biomeIds.size() > SHOWN_BIOMES) {
      shown.append(LIST_SEPARATOR).append(PLUS + (biomeIds.size() - SHOWN_BIOMES));
    }
    Component line =
        key != null ? text(context, key, shown) : (prefix == null ? shown : prefix.append(shown));
    List<FormattedCharSequence> tooltip = Tooltips.of(context.font(), title, List.of(all));
    return new ElementColumn(
        new ArrayList<>(
            FittedText.lines(context.font(), line, ZOOM_WIDTH, SheetPalette.TEXT, tooltip)),
        0);
  }

  /** {@code text} wrapped to the zoom's width. */
  private static SheetElement zoomText(SheetContext context, Component text) {
    return context.paragraph(text, ZOOM_WIDTH, SheetPalette.TEXT);
  }

  private static Component biomeName(String biomeId) {
    return Component.translatable(Identifier.parse(biomeId).toLanguageKey(BIOME_KEY_PREFIX));
  }

  private static Component entityName(String entityId) {
    return BuiltInRegistries.ENTITY_TYPE
        .getOptional(Identifier.parse(entityId))
        .map(EntityType::getDescription)
        .orElse(Component.literal(entityId));
  }

  /**
   * The animal alone, facing the viewer, framed on its hitbox with {@link #PORTRAIT_MARGIN} around
   * it (none below: it stands on the picture's bottom).
   */
  private static Scene portraitScene(EntityType<?> type) {
    return portraitScene(type, portrait(type));
  }

  /** {@code cast}, an animal of {@code type}, framed like {@link #portraitScene(EntityType)}. */
  static Scene portraitScene(EntityType<?> type, Cast cast) {
    EntityDimensions size = type.getDimensions();
    double margin = Math.max(size.width(), size.height()) * PORTRAIT_MARGIN;
    double side = size.width() / 2 + margin;
    return Scene.of(cast).focusedOn(new AABB(-side, 0, -side, side, size.height() + margin, side));
  }

  /** The animal alone, facing the viewer. */
  private static Cast portrait(EntityType<?> type) {
    return stage -> {
      Optional<? extends Entity> mob = stage.spawn(type, 0, 0, 0, Casts.FACING_VIEWER);
      return mob.isEmpty() ? List.of() : List.of(mob.get());
    };
  }

  /** A portrait with a heart in its corner: the animal must be tamed before it breeds. */
  private record TamedPortrait(Picture picture) implements SheetElement {
    @Override
    public int minWidth() {
      return picture.minWidth();
    }

    @Override
    public int height() {
      return picture.height();
    }

    @Override
    public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
      picture.extract(graphics, x, y, width);
      graphics.blitSprite(
          RenderPipelines.GUI_TEXTURED,
          HEART,
          x + picture.minWidth() - HEART_SIZE,
          y + picture.height() - HEART_SIZE,
          HEART_SIZE,
          HEART_SIZE);
    }

    @Override
    public List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
      return picture.tooltipAt(localX, localY, width);
    }
  }

  /** The food's items: the tag's (in the game's order) or the fixed ones that exist. */
  private static List<ItemStack> foods(BreedingFood.Food food) {
    List<ItemStack> stacks = new ArrayList<>();
    switch (food) {
      case BreedingFood.TagFood tag -> {
        TagKey<Item> key = TagKey.create(Registries.ITEM, Identifier.parse(tag.tagId()));
        for (Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(key)) {
          stacks.add(new ItemStack(item.value()));
        }
      }
      case BreedingFood.FixedFood fixed -> {
        for (String id : fixed.itemIds()) {
          BuiltInRegistries.ITEM
              .getOptional(Identifier.parse(id))
              .ifPresent(item -> stacks.add(new ItemStack(item)));
        }
      }
    }
    return stacks;
  }

  /** Names of {@code stacks} from {@code first} on, comma separated. */
  private static Component itemNames(List<ItemStack> stacks, int first) {
    MutableComponent names = Component.empty();
    for (int index = first; index < stacks.size(); index++) {
      if (index > first) {
        names.append(LIST_SEPARATOR);
      }
      names.append(stacks.get(index).getHoverName());
    }
    return names;
  }

  private static FarmPlanView.Look water(
      SheetContext context, AtlasManager atlases, Component name, Component rule) {
    return new FarmPlanView.Look(
        atlases.get(WATER), WATER_COLOR, null, WHITE, tooltip(context, name, rule));
  }

  private static FarmPlanView.Look look(
      AtlasManager atlases,
      SpriteId ground,
      @Nullable SpriteId top,
      int topTint,
      List<FormattedCharSequence> tooltip) {
    return new FarmPlanView.Look(
        atlases.get(ground), WHITE, top == null ? null : atlases.get(top), topTint, tooltip);
  }

  private static List<FormattedCharSequence> tooltip(
      SheetContext context, Component title, Component rule) {
    return Tooltips.of(context.font(), title, List.of(rule));
  }

  private static Component name(Block block) {
    return block.getName();
  }

  private static SpriteId block(String path) {
    return Sheets.BLOCKS_MAPPER.defaultNamespaceApply(path);
  }

  private static Component text(SheetContext context, String key, Object... args) {
    return context.tabText(ID, key, args);
  }
}
