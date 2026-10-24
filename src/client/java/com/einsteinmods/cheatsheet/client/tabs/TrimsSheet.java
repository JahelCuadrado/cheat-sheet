package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.picture.Casts;
import com.einsteinmods.cheatsheet.client.picture.Picture;
import com.einsteinmods.cheatsheet.client.picture.PictureAtlas;
import com.einsteinmods.cheatsheet.client.picture.Scene;
import com.einsteinmods.cheatsheet.client.sheet.CardTint;
import com.einsteinmods.cheatsheet.client.sheet.ElementRow;
import com.einsteinmods.cheatsheet.client.sheet.ItemIcon;
import com.einsteinmods.cheatsheet.client.sheet.Paragraph;
import com.einsteinmods.cheatsheet.client.sheet.Section;
import com.einsteinmods.cheatsheet.client.sheet.Sheet;
import com.einsteinmods.cheatsheet.client.sheet.SheetContext;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import com.einsteinmods.cheatsheet.client.sheet.SheetPalette;
import com.einsteinmods.cheatsheet.client.sheet.SheetSource;
import com.einsteinmods.cheatsheet.client.sheet.Table;
import com.einsteinmods.cheatsheet.client.sheet.Tooltips;
import com.einsteinmods.cheatsheet.client.sheet.ZoomCard;
import com.einsteinmods.cheatsheet.core.ZoomSize;
import com.einsteinmods.cheatsheet.core.pictures.Place;
import com.einsteinmods.cheatsheet.core.trims.PatternNames;
import com.einsteinmods.cheatsheet.core.trims.TrimSource;
import com.einsteinmods.cheatsheet.core.trims.TrimTable;
import com.einsteinmods.cheatsheet.core.trims.TrimTemplate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import org.jspecify.annotations.Nullable;

/**
 * Armor trims tab, two tables side by side so the whole tab fits one 960x540 screen. "Where to find
 * each design": one row per smithing template (an armour stand in a full netherite set with that
 * trim in diamond, whole; the template and its design's name; the place in miniature and its name),
 * overworld places first, then the Nether, then the End, designs of one place together; the
 * netherite upgrade last; it splits into side-by-side blocks. "Every design in every material": one
 * single table, one row per trim material (its ingredient first), one column per design (its
 * template on top, named in its tooltip), each cell a whole armour stand wearing that design in
 * that material on netherite; when the sheet is too narrow for it, it overflows rather than
 * splitting. The patterns need the armour stand: a trimmed armour's item icon shows the material's
 * colour but never the pattern. Zoomed, a row of the list shows the armour stand big, the template
 * and the place each with its name under it; a row of the matrix shows every design's armour stand
 * big with its name under it. Where templates are found and how they are copied come from the fixed
 * table in {@code core.trims} (loot and recipes are server data); pattern names and materials come
 * from the client's synced registries, so data-pack materials show too.
 */
public final class TrimsSheet implements SheetSource {
  public static final String ID = "trims";

  /**
   * An armour stand in a trimmed set, and a place in miniature, in GUI pixels. The stand's whole
   * body ({@link Casts#MANNEQUIN_BODY}) is a little over half as wide as it is tall. Sized so ten
   * rows (the list in two blocks) fit beside the colour table in a 960x540 GUI.
   */
  private static final int MANNEQUIN_WIDTH = 24;

  private static final int MANNEQUIN_HEIGHT = 44;
  private static final int PLACE_WIDTH = 38;
  private static final int PLACE_HEIGHT = 40;

  /**
   * A cell of the colour matrix: a whole armour stand, so that eleven material rows and eighteen
   * design columns fit beside the list in a 960x540 GUI. Netherite armour: its near-black lets
   * every material's colour stand out, and it matches the sets of the first table. The netherite
   * material alone goes on iron: vanilla darkens it on netherite armour, nearly black on black.
   */
  private static final int SWATCH_WIDTH = 22;

  private static final int SWATCH_HEIGHT = 39;

  /** The colour matrix is one table: never split into side-by-side blocks. */
  private static final int ONE_BLOCK = 1;

  /**
   * Names wrap at this width, or at their longest word up to {@link #NAME_CAP}, so a long place
   * name stays narrow; only a word wider than the cap splits. Capped, both tables stay side by side
   * on a 960-wide sheet in every language.
   */
  private static final int NAME_WRAP = 50;

  private static final int NAME_CAP = 58;

  private static final int CELL_GAP = 2;

  /** Zoomed list row: the armour stand and the place twice their size on the sheet. */
  private static final int BIG_MANNEQUIN_WIDTH = MANNEQUIN_WIDTH * 2;

  private static final int BIG_MANNEQUIN_HEIGHT = MANNEQUIN_HEIGHT * 2;
  private static final int BIG_PLACE_WIDTH = PLACE_WIDTH * 2;
  private static final int BIG_PLACE_HEIGHT = PLACE_HEIGHT * 2;
  private static final int ZOOM_GAP = 8;

  /** A name under a zoomed picture wraps at this width. */
  private static final int ZOOM_NAME_WRAP = 72;

  /**
   * Zoomed matrix row: every design's armour stand about 1.5 times its cell, its name under it, in
   * rows of nine.
   */
  private static final int ZOOM_SWATCH_WIDTH = 30;

  private static final int ZOOM_SWATCH_HEIGHT = 54;
  private static final int ZOOM_SWATCH_CELL = 50;
  private static final int ZOOM_SWATCHES_PER_ROW = 9;
  private static final int ZOOM_SWATCH_GAP = 2;

  /** Where the elder guardian floats: over the middle of the monument, in block units. */
  private static final double GUARDIAN_X = 2.5;

  private static final double GUARDIAN_Y = 3.8;
  private static final double GUARDIAN_Z = 2.5;

  private static final String TRIAL_CHAMBERS = "trial_chambers";

  /** Vanilla's name for the netherite upgrade (the item itself is named "Smithing Template"). */
  private static final String NETHERITE_UPGRADE_KEY =
      "item.minecraft.netherite_upgrade_smithing_template.new";

  private static final char LANGUAGE_SEPARATOR = '_';
  private static final char LOCALE_SEPARATOR = '-';
  private static final int DEFAULT_MATERIAL_COLOR = 0xFFFFFFFF;

  private static final List<Table.Align> BY_PLACE_COLUMNS =
      List.of(Table.Align.CENTER, Table.Align.START, Table.Align.START);

  @Override
  public String id() {
    return ID;
  }

  @Override
  public ItemStack icon() {
    return new ItemStack(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE);
  }

  @Override
  public Sheet build(SheetContext context) {
    ClientLevel level = context.minecraft().level;
    Optional<Registry<TrimPattern>> patterns =
        level == null ? Optional.empty() : level.registryAccess().lookup(Registries.TRIM_PATTERN);
    Optional<Registry<TrimMaterial>> materials =
        level == null ? Optional.empty() : level.registryAccess().lookup(Registries.TRIM_MATERIAL);
    Optional<Holder.Reference<TrimMaterial>> diamond =
        materials.flatMap(registry -> registry.get(TrimMaterials.DIAMOND));
    PictureAtlas atlas = new PictureAtlas(ID);

    List<Design> designs = designs(context, patterns);

    Map<TrimSource, Picture> places = places(context, atlas);
    Map<TrimSource, Picture> bigPlaces = new EnumMap<>(TrimSource.class);
    List<List<SheetElement>> rows = new ArrayList<>(designs.size());
    for (Design design : designs) {
      rows.add(
          List.of(
              lookCell(design, diamond, atlas),
              designCell(context, design),
              whereCell(context, design.template().source(), places)));
    }
    List<Section> sections = new ArrayList<>();
    sections.add(
        Section.of(
            text(context, "section.by_place"),
            CardTint.STONE,
            new Table(
                    BY_PLACE_COLUMNS,
                    // The armour stand's column is as narrow as the stand: a title would widen it.
                    List.of(
                        Blank.INSTANCE,
                        columnTitle(context, "design"),
                        columnTitle(context, "where")),
                    rows,
                    CardTint.STONE)
                .withRowZooms(
                    ZoomViews.kept(
                        designs.size(),
                        index ->
                            designZoom(context, designs.get(index), diamond, atlas, bigPlaces)))));
    Table colours = coloursTable(context, designs, materials, atlas);
    if (colours != null) {
      sections.add(
          Section.of(
                  text(context, "section.colours"),
                  CardTint.STONE,
                  new CappedBody(colours, ONE_BLOCK))
              .besidePrevious());
    }
    return Sheet.ofSections(sections, atlas);
  }

  /**
   * A template, its pattern (none for the netherite upgrade), its full name and the short one shown
   * in the tables, and its tooltip.
   */
  private record Design(
      TrimTemplate template,
      ItemStack stack,
      Optional<Holder.Reference<TrimPattern>> pattern,
      Component name,
      Component shortName,
      List<FormattedCharSequence> tooltip) {}

  /** Trim templates in table order, then the netherite upgrade; names without shared words. */
  private static List<Design> designs(
      SheetContext context, Optional<Registry<TrimPattern>> patterns) {
    List<Design> trims = new ArrayList<>();
    List<Design> upgrades = new ArrayList<>();
    for (TrimTemplate template : TrimTable.TEMPLATES) {
      Design design = design(context, template, patterns);
      (template.patternId().isEmpty() ? upgrades : trims).add(design);
    }
    List<String> names = new ArrayList<>(trims.size());
    for (Design design : trims) {
      names.add(design.name().getString());
    }
    List<String> shortNames = PatternNames.shorten(names, locale(context));
    List<Design> designs = new ArrayList<>(trims.size() + upgrades.size());
    for (int index = 0; index < trims.size(); index++) {
      Design design = trims.get(index);
      designs.add(
          new Design(
              design.template(),
              design.stack(),
              design.pattern(),
              design.name(),
              Component.literal(shortNames.get(index)),
              design.tooltip()));
    }
    designs.addAll(upgrades);
    return designs;
  }

  /** The locale of the game's language ({@code es_es} → es-ES), for capitalising names. */
  private static Locale locale(SheetContext context) {
    String code = context.minecraft().getLanguageManager().getSelected();
    return Locale.forLanguageTag(code.replace(LANGUAGE_SEPARATOR, LOCALE_SEPARATOR));
  }

  private static Design design(
      SheetContext context, TrimTemplate template, Optional<Registry<TrimPattern>> patterns) {
    ItemStack stack = context.stack(template.itemId());
    Optional<Holder.Reference<TrimPattern>> pattern =
        template
            .patternId()
            .flatMap(
                id ->
                    patterns.flatMap(
                        registry -> registry.get(Identifier.withDefaultNamespace(id))));
    Component name =
        Component.translatableWithFallback(NETHERITE_UPGRADE_KEY, stack.getHoverName().getString());
    if (template.patternId().isPresent()) {
      name = pattern.map(found -> found.value().description()).orElse(stack.getHoverName());
    }
    List<Component> details =
        List.of(
            text(context, "where." + template.source().key()),
            text(
                context,
                "copy",
                TrimTable.COPY_DIAMONDS,
                context.stack(template.copyBlockId()).getHoverName()));
    return new Design(
        template, stack, pattern, name, name, Tooltips.of(context.font(), name, details));
  }

  /**
   * The template at 2x and its design's short name, both with the design's tooltip; the key that
   * opens the vault sits on a trial chamber template's corner.
   */
  private static SheetElement designCell(SheetContext context, Design design) {
    return new ElementRow(
        List.of(
            new IconCell(
                context.font(),
                List.of(new IconCell.Icon(design.stack(), SheetContext.BIG)),
                vaultKey(design.template().source()),
                List.of(),
                design.tooltip()),
            name(context, design.shortName(), SheetPalette.TEXT)),
        CELL_GAP);
  }

  /** The place in miniature and its name. */
  private static SheetElement whereCell(
      SheetContext context, TrimSource source, Map<TrimSource, Picture> places) {
    return new ElementRow(
        List.of(
            places.get(source),
            name(context, text(context, "source." + placeKey(source)), SheetPalette.MUTED_TEXT)),
        CELL_GAP);
  }

  /** A design's or place's name, wrapped narrow (see {@link #NAME_CAP}). */
  private static Paragraph name(SheetContext context, Component name, int color) {
    int wrap = Math.clamp(SpotTile.widestWord(context.font(), name), NAME_WRAP, NAME_CAP);
    return new Paragraph(
        context.font(), FittedText.hyphenated(context.font(), name, wrap), wrap, color);
  }

  /** The whole armour stand in the trimmed set; untrimmed netherite for the netherite upgrade. */
  private static SheetElement lookCell(
      Design design, Optional<Holder.Reference<TrimMaterial>> material, PictureAtlas atlas) {
    return atlas.add(
        lookScene(design, material), MANNEQUIN_WIDTH, MANNEQUIN_HEIGHT, design.tooltip());
  }

  private static Scene lookScene(Design design, Optional<Holder.Reference<TrimMaterial>> material) {
    Scene scene =
        design.pattern().isPresent() && material.isPresent()
            ? Scene.of(Casts.mannequin(design.pattern().get(), material.get()))
            : Scene.of(Casts.mannequin());
    return scene.focusedOn(Casts.MANNEQUIN_BODY);
  }

  /** An armour stand wearing {@code design} in {@code material}: a cell of the matrix. */
  private static Scene swatchScene(Design design, Holder.Reference<TrimMaterial> material) {
    return Scene.of(Casts.mannequin(design.pattern().get(), material, armorFor(material)))
        .focusedOn(Casts.MANNEQUIN_BODY);
  }

  /**
   * A zoomed row of the list: the armour stand big; the template and its design's name under it;
   * the place, bigger, and its name under it.
   */
  private static ZoomCard designZoom(
      SheetContext context,
      Design design,
      Optional<Holder.Reference<TrimMaterial>> material,
      PictureAtlas atlas,
      Map<TrimSource, Picture> bigPlaces) {
    TrimSource source = design.template().source();
    Picture stand =
        atlas.addZoomOnly(
            lookScene(design, material),
            BIG_MANNEQUIN_WIDTH,
            BIG_MANNEQUIN_HEIGHT,
            design.tooltip());
    SheetElement template =
        new IconCell(
            context.font(),
            List.of(new IconCell.Icon(design.stack(), SheetContext.BIG)),
            vaultKey(source),
            List.of(),
            design.tooltip());
    Picture place =
        bigPlaces.computeIfAbsent(
            source,
            key ->
                atlas.addZoomOnly(
                    scene(key),
                    BIG_PLACE_WIDTH,
                    BIG_PLACE_HEIGHT,
                    Tooltips.of(
                        context.font(),
                        text(context, "source." + placeKey(key)),
                        List.of(text(context, "where." + key.key())))));
    return ZoomViews.panel(
        new ElementRow(
            List.of(
                stand,
                ZoomViews.captioned(
                    context.font(), template, design.name(), ZOOM_NAME_WRAP, SheetPalette.TEXT, 1),
                ZoomViews.captioned(
                    context.font(),
                    place,
                    text(context, "source." + placeKey(source)),
                    ZOOM_NAME_WRAP,
                    SheetPalette.MUTED_TEXT,
                    1)),
            ZOOM_GAP),
        CardTint.STONE,
        ZoomSize.MEDIUM);
  }

  /**
   * A zoomed row of the matrix: the material's ingredient and name, then every design on it, its
   * armour stand big and its name under it.
   */
  private static ZoomCard materialZoom(
      SheetContext context,
      ItemStack ingredient,
      Component name,
      Holder.Reference<TrimMaterial> material,
      List<Design> trims,
      PictureAtlas atlas) {
    List<SheetElement> cells = new ArrayList<>(trims.size());
    for (Design design : trims) {
      Picture stand =
          atlas.addZoomOnly(
              swatchScene(design, material),
              ZOOM_SWATCH_WIDTH,
              ZOOM_SWATCH_HEIGHT,
              Tooltips.of(context.font(), design.name(), List.of(name)));
      SheetElement cell =
          ZoomViews.captioned(
              context.font(), stand, design.shortName(), ZOOM_SWATCH_CELL, SheetPalette.TEXT, 2);
      cells.add(new BoxedElement(cell, ZOOM_SWATCH_CELL, cell.height()));
    }
    int rowWidth = ZOOM_SWATCHES_PER_ROW * (ZOOM_SWATCH_CELL + ZOOM_SWATCH_GAP) - ZOOM_SWATCH_GAP;
    SheetElement header =
        new ElementRow(
            List.of(
                context.item(ingredient, SheetContext.BIG, material.value().description()),
                context.label(name.copy().withStyle(ChatFormatting.BOLD), SheetPalette.TEXT)),
            ElementRow.GAP);
    return ZoomViews.panel(
        new ZoomViews.CentredColumn(
            List.of(header, new CentredRows(cells, rowWidth, ZOOM_SWATCH_GAP)), ZOOM_GAP),
        CardTint.STONE,
        ZoomSize.MEDIUM);
  }

  /** One picture per place, shared by its rows; its tooltip says where in it to look. */
  private static Map<TrimSource, Picture> places(SheetContext context, PictureAtlas atlas) {
    Map<TrimSource, Picture> places = new EnumMap<>(TrimSource.class);
    TrimSource[] sources = TrimSource.values();
    int first = 0;
    for (int index = 0; index < sources.length; index++) {
      boolean lastOfPlace =
          index == sources.length - 1
              || !placeKey(sources[index + 1]).equals(placeKey(sources[index]));
      if (!lastOfPlace) {
        continue;
      }
      List<Component> where = new ArrayList<>();
      for (int member = first; member <= index; member++) {
        where.add(text(context, "where." + sources[member].key()));
      }
      Picture place =
          atlas.add(
              scene(sources[index]),
              PLACE_WIDTH,
              PLACE_HEIGHT,
              Tooltips.of(
                  context.font(), text(context, "source." + placeKey(sources[index])), where));
      for (int member = first; member <= index; member++) {
        places.put(sources[member], place);
      }
      first = index + 1;
    }
    return places;
  }

  /**
   * Every trim material (rows: its ingredient) in every design (columns: its template): a whole
   * armour stand wearing it. Null without a world.
   */
  private static @Nullable Table coloursTable(
      SheetContext context,
      List<Design> designs,
      Optional<Registry<TrimMaterial>> materials,
      PictureAtlas atlas) {
    if (materials.isEmpty()) {
      return null;
    }
    List<Design> trims = new ArrayList<>(designs.size());
    List<SheetElement> header = new ArrayList<>(designs.size() + 1);
    header.add(Blank.INSTANCE);
    for (Design design : designs) {
      if (design.pattern().isPresent()) {
        trims.add(design);
        header.add(new ItemIcon(design.stack(), 1, design.tooltip()));
      }
    }
    List<List<SheetElement>> rows = new ArrayList<>();
    List<ItemStack> ingredients = new ArrayList<>();
    List<Component> names = new ArrayList<>();
    List<Holder.Reference<TrimMaterial>> rowMaterials = new ArrayList<>();
    for (Item item : BuiltInRegistries.ITEM) {
      Holder<TrimMaterial> provided = item.components().get(DataComponents.PROVIDES_TRIM_MATERIAL);
      if (provided == null || provided.unwrapKey().isEmpty()) {
        continue;
      }
      Optional<Holder.Reference<TrimMaterial>> material =
          materials.get().get(provided.unwrapKey().get().identifier());
      if (material.isEmpty()) {
        continue;
      }
      ItemStack ingredient = new ItemStack(item);
      TextColor color = material.get().value().description().getStyle().getColor();
      Component name =
          ingredient
              .getHoverName()
              .copy()
              .withColor(color == null ? DEFAULT_MATERIAL_COLOR : color.getValue());
      List<SheetElement> row = new ArrayList<>(trims.size() + 1);
      row.add(
          new ItemIcon(
              ingredient,
              1,
              Tooltips.of(context.font(), name, List.of(material.get().value().description()))));
      for (Design design : trims) {
        row.add(
            atlas.add(
                swatchScene(design, material.get()),
                SWATCH_WIDTH,
                SWATCH_HEIGHT,
                Tooltips.of(context.font(), design.name(), List.of(name))));
      }
      rows.add(row);
      ingredients.add(ingredient);
      names.add(name);
      rowMaterials.add(material.get());
    }
    if (trims.isEmpty() || rows.isEmpty()) {
      return null;
    }
    List<Table.Align> align = new ArrayList<>(header.size());
    for (int index = 0; index < header.size(); index++) {
      align.add(Table.Align.CENTER);
    }
    return new Table(align, header, rows, CardTint.STONE, Table.TIGHT_PADDING_X)
        .withRowZooms(
            ZoomViews.kept(
                rows.size(),
                index ->
                    materialZoom(
                        context,
                        ingredients.get(index),
                        names.get(index),
                        rowMaterials.get(index),
                        trims,
                        atlas)));
  }

  /** An empty header cell: the corner above the material column. */
  private enum Blank implements SheetElement {
    INSTANCE;

    @Override
    public int minWidth() {
      return 1;
    }

    @Override
    public int height() {
      return 1;
    }

    @Override
    public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
      // Nothing to draw: the corner stays blank.
    }
  }

  /** Netherite armour, but iron under the netherite material (dark grey shows on light grey). */
  private static Casts.ArmorSet armorFor(Holder<TrimMaterial> material) {
    return material.is(TrimMaterials.NETHERITE) ? Casts.ArmorSet.IRON : Casts.ArmorSet.NETHERITE;
  }

  private static SheetElement columnTitle(SheetContext context, String key) {
    return context.label(text(context, "column." + key), SheetPalette.HIGHLIGHT_TEXT);
  }

  /** The place in miniature; the elder guardian floats over its monument. */
  private static Scene scene(TrimSource source) {
    return switch (source) {
      case PILLAGER_OUTPOST -> Scene.of(Place.PILLAGER_OUTPOST.diorama());
      case DESERT_PYRAMID -> Scene.of(Place.DESERT_PYRAMID.diorama());
      case SHIPWRECK -> Scene.of(Place.SHIPWRECK.diorama());
      case JUNGLE_TEMPLE -> Scene.of(Place.JUNGLE_TEMPLE.diorama());
      case OCEAN_MONUMENT ->
          Scene.of(
              Place.OCEAN_MONUMENT.diorama(),
              Casts.elderGuardian(GUARDIAN_X, GUARDIAN_Y, GUARDIAN_Z));
      case WOODLAND_MANSION -> Scene.of(Place.WOODLAND_MANSION.diorama());
      case STRONGHOLD -> Scene.of(Place.STRONGHOLD.diorama());
      case ANCIENT_CITY -> Scene.of(Place.ANCIENT_CITY.diorama());
      case TRAIL_RUINS -> Scene.of(Place.TRAIL_RUINS.diorama());
      case TRIAL_VAULT, OMINOUS_VAULT -> Scene.of(Place.TRIAL_CHAMBERS.diorama());
      case NETHER_FORTRESS -> Scene.of(Place.NETHER_FORTRESS.diorama());
      case BASTION -> Scene.of(Place.BASTION.diorama());
      case END_CITY -> Scene.of(Place.END_CITY.diorama());
    };
  }

  /** Trial chamber vaults share a place; every other source is its own. */
  private static String placeKey(TrimSource source) {
    return switch (source) {
      case TRIAL_VAULT, OMINOUS_VAULT -> TRIAL_CHAMBERS;
      default -> source.key();
    };
  }

  /** The key that opens the vault, telling the two trial chamber templates apart. */
  private static @Nullable ItemStack vaultKey(TrimSource source) {
    return switch (source) {
      case TRIAL_VAULT -> new ItemStack(Items.TRIAL_KEY);
      case OMINOUS_VAULT -> new ItemStack(Items.OMINOUS_TRIAL_KEY);
      default -> null;
    };
  }

  private static Component text(SheetContext context, String key, Object... args) {
    return context.tabText(ID, key, args);
  }
}
