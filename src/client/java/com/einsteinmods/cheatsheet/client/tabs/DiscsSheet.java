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
import com.einsteinmods.cheatsheet.client.sheet.TextLabel;
import com.einsteinmods.cheatsheet.client.sheet.TileGrid;
import com.einsteinmods.cheatsheet.client.sheet.Tooltips;
import com.einsteinmods.cheatsheet.client.sheet.ZoomCard;
import com.einsteinmods.cheatsheet.core.ZoomSize;
import com.einsteinmods.cheatsheet.core.discs.Disc;
import com.einsteinmods.cheatsheet.core.discs.DiscSource;
import com.einsteinmods.cheatsheet.core.discs.DiscTable;
import com.einsteinmods.cheatsheet.core.discs.SongLength;
import com.einsteinmods.cheatsheet.core.pictures.PictureSize;
import com.einsteinmods.cheatsheet.core.pictures.Place;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.JukeboxSong;
import org.jspecify.annotations.Nullable;

/**
 * Music discs tab: one equal card per disc, in the game's order (discs of one source stay
 * together), eight per row on a 960-wide sheet so every disc fits one screen. Each card, every
 * piece always in the same place: the disc big (a trial chamber disc marked with the key or pot it
 * comes from), its song, artist and length; below, the main source's picture (place in miniature or
 * the mobs) with its name beside it; then every other source, a small picture and its name each.
 * How each source drops it is in the pictures' tooltips; the comparator signal a jukebox gives is
 * only in the disc's tooltip. Zoomed, a card shows the disc and its song as on the sheet, and every
 * source below, its picture bigger and its name under it. Sources come from the fixed table in
 * {@code core.discs} (loot is server data); song names, lengths and signals from the client's
 * synced jukebox songs.
 */
public final class DiscsSheet implements SheetSource {
  public static final String ID = "discs";

  /** Eight cards fill a 960-wide GUI (the sheet keeps 6 px margins), four a 480-wide one. */
  private static final int CARD_WIDTH = 114;

  private static final int CARD_GAP = 4;
  private static final int CARD_PADDING = 4;
  private static final int PART_GAP = 3;
  private static final int LINE_GAP = 1;
  private static final int OTHER_GAP = 1;

  /**
   * The main source's picture, and the smaller one of each other source, in sheet pixels: three
   * rows of cards fill the height of a 960x540 sheet.
   */
  private static final PictureSize SOURCE_SIZE = new PictureSize(50, 44);

  private static final PictureSize OTHER_SOURCE_SIZE = new PictureSize(20, 18);

  /** Every source in a zoomed card: its picture this big, its name under it. */
  private static final PictureSize ZOOM_SOURCE_SIZE = new PictureSize(64, 56);

  private static final int ZOOM_NAME_WRAP = 72;
  private static final int ZOOM_GAP = 6;

  /** Sources of a zoomed card side by side, four at most per row. */
  private static final int ZOOM_SOURCES_PER_ROW = 4;

  /** Room inside a card. */
  private static final int INNER_WIDTH = CARD_WIDTH - CARD_PADDING * 2;

  /** A ghast's tentacles hang this many blocks under its hitbox. */
  private static final float GHAST_TENTACLES = 2.5F;

  /** Songs are named "Artist - Song" (an en or em dash in some languages). */
  private static final List<String> ARTIST_SEPARATORS = List.of(" - ", " — ", " – ");

  /** A long song name takes two lines at most (cut, whole in the tooltip): cards stay short. */
  private static final int TITLE_LINES = 2;

  private static final String TRIAL_CHAMBERS = "trial_chambers";
  private static final String TIMES = "×";

  private static final CardTint SCULK = CardTint.of(0xFF1E9A9A);
  private static final CardTint DARK_OAK = CardTint.of(0xFF8B5A2B);
  private static final CardTint MINESHAFT = CardTint.of(0xFFA88A5A);
  private static final CardTint TERRACOTTA = CardTint.of(0xFFB0623E);
  private static final CardTint BLACKSTONE_GOLD = CardTint.of(0xFFB8862B);

  private static final List<SlotTile.Slot> SLOTS =
      List.of(SlotTile.Slot.START, SlotTile.Slot.START_DIVIDED, SlotTile.Slot.START);

  @Override
  public String id() {
    return ID;
  }

  @Override
  public ItemStack icon() {
    return new ItemStack(Items.MUSIC_DISC_CAT);
  }

  @Override
  public Sheet build(SheetContext context) {
    ClientLevel level = context.minecraft().level;
    Optional<Registry<JukeboxSong>> songs =
        level == null ? Optional.empty() : level.registryAccess().lookup(Registries.JUKEBOX_SONG);

    PictureAtlas atlas = new PictureAtlas(ID);
    Map<DiscSource, Picture> pictures = new EnumMap<>(DiscSource.class);
    Map<DiscSource, Picture> smallPictures = new EnumMap<>(DiscSource.class);
    Map<DiscSource, Picture> zoomPictures = new EnumMap<>(DiscSource.class);
    int nameWidth = INNER_WIDTH - SOURCE_SIZE.width() - PART_GAP;
    int otherNameWidth = INNER_WIDTH - OTHER_SOURCE_SIZE.width() - PART_GAP;
    List<List<@Nullable SheetElement>> cards = new ArrayList<>(DiscTable.DISCS.size());
    List<Supplier<ZoomCard>> views = new ArrayList<>(DiscTable.DISCS.size());
    List<CardTint> tints = new ArrayList<>(DiscTable.DISCS.size());
    for (Disc disc : DiscTable.DISCS) {
      DiscSource main = disc.sources().getFirst();
      ItemStack stack = context.stack(disc.itemId());
      List<DiscSource> others =
          main == DiscSource.CRAFTED
              ? List.of(DiscTable.FRAGMENT_SOURCE)
              : disc.sources().subList(1, disc.sources().size());
      SheetElement mainPicture =
          main == DiscSource.CRAFTED
              ? new BoxedElement(fragments(context), SOURCE_SIZE.width(), SOURCE_SIZE.height())
              : pictures.computeIfAbsent(
                  main, source -> picture(context, atlas, source, SOURCE_SIZE));
      @Nullable SheetElement otherSources = null;
      if (!others.isEmpty()) {
        List<SheetElement> lines = new ArrayList<>(others.size());
        for (DiscSource other : others) {
          lines.add(
              new ElementRow(
                  List.of(
                      smallPictures.computeIfAbsent(
                          other, source -> picture(context, atlas, source, OTHER_SOURCE_SIZE)),
                      new Paragraph(
                          context.font(),
                          FittedText.hyphenated(
                              context.font(), name(context, other), otherNameWidth),
                          otherNameWidth,
                          SheetPalette.MUTED_TEXT)),
                  PART_GAP));
        }
        otherSources = new ElementColumn(lines, OTHER_GAP);
      }
      List<@Nullable SheetElement> card = new ArrayList<>(SLOTS.size());
      JukeboxSong song = song(stack, songs);
      card.add(header(context, disc, stack, song));
      CardTint tint = tint(main);
      views.add(() -> discZoom(context, disc, stack, song, tint, atlas, zoomPictures));
      card.add(
          new ElementRow(
              List.of(
                  mainPicture,
                  new Paragraph(
                      context.font(),
                      FittedText.hyphenated(context.font(), name(context, main), nameWidth),
                      nameWidth,
                      SheetPalette.TEXT)),
              PART_GAP));
      card.add(otherSources);
      cards.add(card);
      tints.add(tint(main));
    }
    List<SlotTile> uniform = SlotTile.uniform(SLOTS, cards, tints, CARD_WIDTH);
    List<SheetElement> tiles = new ArrayList<>(uniform.size());
    for (int index = 0; index < uniform.size(); index++) {
      tiles.add(ZoomViews.zoomedAs(uniform.get(index), views.get(index)));
    }
    TileGrid grid = new TileGrid(tiles, CARD_GAP);
    return Sheet.ofSections(
        List.of(Section.of(text(context, "section"), CardTint.STONE, grid)), atlas);
  }

  /**
   * The disc big (with the key or pot of its trial chamber source on its corner), its song, artist
   * and length; the tooltip adds the length, the comparator signal while it plays and every source.
   */
  private static SheetElement header(
      SheetContext context, Disc disc, ItemStack stack, @Nullable JukeboxSong song) {
    Component name = song == null ? stack.getHoverName() : song.description();
    List<Component> details = new ArrayList<>();
    if (song != null) {
      details.add(text(context, "length", SongLength.format(song.lengthInSeconds())));
      details.add(text(context, "signal", song.comparatorOutput()));
    }
    for (DiscSource source : disc.sources()) {
      details.add(how(context, source));
    }
    IconCell icon =
        new IconCell(
            context.font(),
            List.of(new IconCell.Icon(stack, SheetContext.BIG)),
            badge(disc.sources().getFirst()),
            List.of(),
            Tooltips.of(context.font(), name, details));
    int textWidth = INNER_WIDTH - ItemIcon.ITEM_SIZE * SheetContext.BIG - PART_GAP;
    String full = name.getString();
    int separator = -1;
    int titleStart = 0;
    for (String candidate : ARTIST_SEPARATORS) {
      int found = full.indexOf(candidate);
      if (found >= 0) {
        separator = found;
        titleStart = found + candidate.length();
        break;
      }
    }
    List<SheetElement> lines = new ArrayList<>();
    lines.add(
        new ElementColumn(
            new ArrayList<>(
                FittedText.lines(
                    context.font(),
                    Component.literal(full.substring(titleStart)).withStyle(ChatFormatting.BOLD),
                    textWidth,
                    TITLE_LINES,
                    SheetPalette.TEXT)),
            0));
    if (separator >= 0) {
      lines.add(
          new TextLabel(
              context.font(),
              FittedText.fitted(
                  context.font(), Component.literal(full.substring(0, separator)), textWidth),
              SheetPalette.MUTED_TEXT,
              1,
              null));
    }
    if (song != null) {
      lines.add(
          context.label(
              Component.literal(SongLength.format(song.lengthInSeconds())),
              SheetPalette.HIGHLIGHT_TEXT));
    }
    return new ElementRow(List.of(icon, new ElementColumn(lines, LINE_GAP)), PART_GAP);
  }

  /**
   * A zoomed card: the disc and its song as on the sheet, then every source (the fragments for disc
   * 5), its picture bigger and its name under it.
   */
  private static ZoomCard discZoom(
      SheetContext context,
      Disc disc,
      ItemStack stack,
      @Nullable JukeboxSong song,
      CardTint tint,
      PictureAtlas atlas,
      Map<DiscSource, Picture> zoomPictures) {
    List<DiscSource> shown = new ArrayList<>(disc.sources());
    if (shown.getFirst() == DiscSource.CRAFTED) {
      shown.add(DiscTable.FRAGMENT_SOURCE);
    }
    List<SheetElement> sources = new ArrayList<>(shown.size());
    for (DiscSource source : shown) {
      SheetElement picture =
          source == DiscSource.CRAFTED
              ? new BoxedElement(
                  fragments(context), ZOOM_SOURCE_SIZE.width(), ZOOM_SOURCE_SIZE.height())
              : zoomPictures.computeIfAbsent(
                  source, key -> zoomPicture(context, atlas, key, ZOOM_SOURCE_SIZE));
      Component name = name(context, source);
      sources.add(
          new BoxedElement(
              ZoomViews.captioned(
                  context.font(), picture, name, ZOOM_NAME_WRAP, SheetPalette.TEXT, 2),
              ZOOM_NAME_WRAP,
              0));
    }
    int rowWidth = ZOOM_SOURCES_PER_ROW * (ZOOM_NAME_WRAP + ZOOM_GAP) - ZOOM_GAP;
    return ZoomViews.panel(
        new ZoomViews.CentredColumn(
            List.of(
                header(context, disc, stack, song), new CentredRows(sources, rowWidth, ZOOM_GAP)),
            ZOOM_GAP),
        tint,
        ZoomSize.MEDIUM);
  }

  /** Disc 5's source instead of a picture: a fragment big, "×9". */
  private static SheetElement fragments(SheetContext context) {
    ItemStack fragment = context.stack(DiscTable.FRAGMENT_ITEM_ID);
    return new ElementRow(
        List.of(
            context.item(
                fragment,
                SheetContext.BIG,
                text(context, "fragment", DiscTable.FRAGMENTS_PER_DISC),
                how(context, DiscTable.FRAGMENT_SOURCE)),
            context.label(
                Component.literal(TIMES + DiscTable.FRAGMENTS_PER_DISC),
                SheetPalette.HIGHLIGHT_TEXT)),
        0);
  }

  private static Picture picture(
      SheetContext context, PictureAtlas atlas, DiscSource source, PictureSize size) {
    return atlas.add(scene(source), size.width(), size.height(), sourceTooltip(context, source));
  }

  /** A source's picture for zoomed cards only. */
  private static Picture zoomPicture(
      SheetContext context, PictureAtlas atlas, DiscSource source, PictureSize size) {
    return atlas.addZoomOnly(
        scene(source), size.width(), size.height(), sourceTooltip(context, source));
  }

  private static List<FormattedCharSequence> sourceTooltip(
      SheetContext context, DiscSource source) {
    return Tooltips.of(context.font(), name(context, source), List.of(how(context, source)));
  }

  private static @Nullable JukeboxSong song(
      ItemStack stack, Optional<Registry<JukeboxSong>> songs) {
    JukeboxPlayable playable = stack.get(DataComponents.JUKEBOX_PLAYABLE);
    if (playable == null || songs.isEmpty()) {
      return null;
    }
    Holder<JukeboxSong> holder = playable.song();
    return holder
        .unwrapKey()
        .flatMap(key -> songs.get().get(key.identifier()))
        .map(Holder::value)
        .orElse(null);
  }

  /** The place in miniature, or the mobs whose drop it is. */
  private static Scene scene(DiscSource source) {
    return switch (source) {
      case CREEPER_KILLED_BY_SKELETON -> Scene.of(Casts.creeperAndSkeleton());
      case DUNGEON -> Scene.of(Place.DUNGEON.diorama());
      case ANCIENT_CITY -> Scene.of(Place.ANCIENT_CITY.diorama());
      case WOODLAND_MANSION -> Scene.of(Place.WOODLAND_MANSION.diorama());
      case STRONGHOLD -> Scene.of(Place.STRONGHOLD.diorama());
      case MINESHAFT -> Scene.of(Place.MINESHAFT.diorama());
      case TRIAL_VAULT, OMINOUS_VAULT, TRIAL_POTS -> Scene.of(Place.TRIAL_CHAMBERS.diorama());
      case TRAIL_RUINS -> Scene.of(Place.TRAIL_RUINS.diorama());
      case BASTION -> Scene.of(Place.BASTION.diorama());
      case GHAST_OWN_FIREBALL -> Scene.of(Casts.ghast()).withExtraBelow(GHAST_TENTACLES);
      case CHICKEN_JOCKEY -> Scene.of(Casts.chickenJockey());
      case CRAFTED -> throw new IllegalArgumentException("crafting is not a place");
    };
  }

  /** The source's short name: the place or the mob. */
  private static Component name(SheetContext context, DiscSource source) {
    return text(context, "source." + nameKey(source));
  }

  private static Component how(SheetContext context, DiscSource source) {
    return text(context, "how." + source.key());
  }

  /** Trial chamber sources share a name; every other source has its own. */
  private static String nameKey(DiscSource source) {
    return switch (source) {
      case TRIAL_VAULT, OMINOUS_VAULT, TRIAL_POTS -> TRIAL_CHAMBERS;
      default -> source.key();
    };
  }

  /** Tells the three trial chamber discs apart: vault key, ominous key, decorated pot. */
  private static @Nullable ItemStack badge(DiscSource source) {
    return switch (source) {
      case TRIAL_VAULT -> new ItemStack(Items.TRIAL_KEY);
      case OMINOUS_VAULT -> new ItemStack(Items.OMINOUS_TRIAL_KEY);
      case TRIAL_POTS -> new ItemStack(Items.DECORATED_POT);
      default -> null;
    };
  }

  private static CardTint tint(DiscSource source) {
    return switch (source) {
      case CREEPER_KILLED_BY_SKELETON -> CardTint.GREEN;
      case DUNGEON, STRONGHOLD -> CardTint.STONE;
      case ANCIENT_CITY -> SCULK;
      case WOODLAND_MANSION -> DARK_OAK;
      case MINESHAFT -> MINESHAFT;
      case TRIAL_VAULT, OMINOUS_VAULT, TRIAL_POTS -> CardTint.ORANGE;
      case TRAIL_RUINS -> TERRACOTTA;
      case BASTION -> BLACKSTONE_GOLD;
      case GHAST_OWN_FIREBALL -> CardTint.NETHER;
      case CHICKEN_JOCKEY -> CardTint.GOLD;
      case CRAFTED -> CardTint.AQUA;
    };
  }

  private static Component text(SheetContext context, String key, Object... args) {
    return context.tabText(ID, key, args);
  }
}
