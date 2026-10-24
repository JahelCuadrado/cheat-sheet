package com.einsteinmods.cheatsheet.client;

import com.einsteinmods.cheatsheet.client.config.CheatSheetSettingsScreen;
import com.einsteinmods.cheatsheet.client.config.ClientSettings;
import com.einsteinmods.cheatsheet.client.picture.PictureAtlas;
import com.einsteinmods.cheatsheet.client.sheet.Card;
import com.einsteinmods.cheatsheet.client.sheet.CardSpot;
import com.einsteinmods.cheatsheet.client.sheet.CardZoom;
import com.einsteinmods.cheatsheet.client.sheet.Section;
import com.einsteinmods.cheatsheet.client.sheet.Sheet;
import com.einsteinmods.cheatsheet.client.sheet.SheetContext;
import com.einsteinmods.cheatsheet.client.sheet.SheetPalette;
import com.einsteinmods.cheatsheet.client.sheet.SheetSource;
import com.einsteinmods.cheatsheet.client.sheet.ZoomCard;
import com.einsteinmods.cheatsheet.core.CardBounds;
import com.einsteinmods.cheatsheet.core.ScrollRange;
import com.einsteinmods.cheatsheet.core.SheetLayout;
import com.einsteinmods.cheatsheet.core.SheetScaling;
import com.einsteinmods.cheatsheet.core.ZoomSize;
import com.einsteinmods.cheatsheet.core.sections.SectionsLayout;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * The cheat sheet: a row of tabs on top and, below, the selected tab's sheet, scrolled with the
 * mouse wheel when taller than the screen. A sheet of cards is packed into the column count that
 * makes it shortest; a sheet of sections (tables, grids) is stacked in reading order.
 *
 * <p>The sheet has its own scale, apart from the player's GUI scale: a whole number of window
 * pixels per sheet pixel, the player's setting or, on auto, the largest at which the whole tab fits
 * ({@link SheetScaling}). While the cheat sheet is shown, the window's GUI scale is set to it, so
 * the game renders every item icon, text and picture at its final size (vanilla renders GUI items
 * into an atlas at 16 x GUI scale pixels: drawn under any other pose they are resampled, which is
 * what blurred them). The tooltips follow that scale; the tab bar and the settings button do not:
 * they are drawn at the player's GUI scale ({@link BarButton}), so they keep one size and place on
 * screen on every tab, and the sheet starts below them. The player's own GUI scale is put back when
 * the screen is removed (closed, or the settings opened); it is only the window's runtime value,
 * never the saved option, so a crash leaves nothing behind. Anything that resizes the GUI while the
 * screen is open (a window resize) lands in {@link #init}, which sets it again.
 *
 * <p>Every card (a masonry card, a tile, a table row) lights up under the mouse; a click zooms it:
 * drawn again bigger, or as a view its tab lays out for the zoom, on a panel centred where it was
 * ({@link CardZoom}), as big as its tab or the card asks ({@link ZoomSize}). The sheet and the tab
 * bar stay drawn behind it, blurred (when the player's menu blur is on) and veiled, so the player
 * never leaves the sheet; the GUI scale does not change. Tooltips work inside the zoom. A click
 * outside the zoom or Escape closes it, and only then does Escape close the cheat sheet. A tab can
 * opt out ({@link SheetSource#zoomsCards}): no highlight, no zoom.
 *
 * <p>Cost: nothing while closed (no tick or render hook). Each sheet is built the first time its
 * tab is shown and kept while the screen lives (also across a visit to the settings); layout runs
 * on resize, tab change and return from the settings only (auto lays out once per candidate scale,
 * at most four). A frame culls cards outside the view and issues the cards' draw calls; it
 * allocates nothing of its own (the hover hit test writes into one kept {@link CardSpot}); a zoom
 * is built once per click, and while it is open a frame adds the veil, the zoom's panel and the
 * card's draw calls to the sheet's; the blur is the one the menu background already does, moved
 * above the sheet, so it costs no extra pass. A sheet's pictures are baked once into a texture the
 * first frame it shows (see {@code PictureAtlas}), a zoom's pictures and the scaled icons once at
 * their own size, and all are freed when the screen is removed.
 */
public final class CheatSheetScreen extends Screen {
  private static final Component TITLE = Component.translatable("cheatsheet.title");
  private static final Component SETTINGS = Component.translatable("cheatsheet.settings");
  private static final String TAB_KEY_PREFIX = "cheatsheet.tab.";

  private static final int MARGIN = 6;
  private static final int TOP = 4;
  private static final int TAB_GAP = 2;
  private static final int SHEET_GAP = 4;
  private static final int CARD_GAP = 4;
  private static final int SCROLL_STEP = 24;
  private static final int SCROLL_BAR_WIDTH = 3;
  private static final int SCROLL_BAR_INSET = 3;
  private static final int MIN_THUMB_HEIGHT = 10;

  /**
   * Menu blur radius below which vanilla draws no blur ({@code Screen#extractBlurredBackground}).
   */
  private static final float MIN_BLUR = 1.0F;

  /** The mouse while a zoom is open, for the widgets behind it: nowhere, so nothing lights up. */
  private static final int NO_MOUSE = -1;

  /** The tab bar's bottom, in bar pixels. */
  private static final int BAR_BOTTOM = TOP + BarButton.SIZE;

  private final @Nullable Screen parent;
  private final ClientSettings settings;
  private final CheatSheetKeyMappings keyMappings;
  private final List<SheetSource> sources;
  private final Sheet[] sheets;
  private final List<SheetTabButton> tabButtons = new ArrayList<>();
  private @Nullable SheetContext context;
  private @Nullable SheetLayout layout;
  private @Nullable SectionsLayout sectionsLayout;
  private int selected;
  private int scroll;

  /** Window pixels per sheet pixel, as of the last layout: the GUI scale while shown. */
  private int sheetScale = 1;

  /**
   * Window pixels per bar pixel: the player's GUI scale (fewer if the bar would not fit), whatever
   * the sheet scale, so the tab bar has the same size on screen on every tab.
   */
  private int barScale = 1;

  /** The sheet's top, below the tab bar, in sheet pixels (GUI pixels while the sheet is shown). */
  private int sheetTop = BAR_BOTTOM + SHEET_GAP;

  /** The window in sheet pixels. */
  private int canvasWidth;

  private int canvasHeight;

  /** The tooltip under the mouse this frame, drawn at the GUI scale; null for none. */
  private @Nullable List<FormattedCharSequence> hoveredTooltip;

  /** The card under the mouse, found every frame (no allocation), and the zoomed card if any. */
  private final CardSpot hoveredCard = new CardSpot();

  private @Nullable CardZoom zoom;

  public CheatSheetScreen(
      @Nullable Screen parent,
      ClientSettings settings,
      CheatSheetKeyMappings keyMappings,
      List<SheetSource> sources) {
    super(TITLE);
    if (sources.isEmpty()) {
      throw new IllegalArgumentException("the cheat sheet needs at least one tab");
    }
    this.parent = parent;
    this.settings = settings;
    this.keyMappings = keyMappings;
    this.sources = List.copyOf(sources);
    this.sheets = new Sheet[sources.size()];
    this.selected = Math.clamp(settings.startTabIndex(), 0, sources.size() - 1);
  }

  /** Index of the tab shown. */
  public int selectedTab() {
    return selected;
  }

  /** Window pixels per sheet pixel the shown tab is drawn at. */
  public int sheetScale() {
    return sheetScale;
  }

  /** Whether the shown tab fits the window without scrolling. */
  public boolean fitsWithoutScrolling() {
    return sheetHeight() <= viewHeight();
  }

  /**
   * Height of the laid-out sheet of the shown tab in sheet pixels, or 0 before the first layout.
   */
  public int sheetHeight() {
    SectionsLayout sectioned = sectionsLayout;
    if (sectioned != null) {
      return sectioned.contentHeight();
    }
    SheetLayout current = layout;
    return current == null ? 0 : current.contentHeight();
  }

  /** Shows tab {@code index}, building its sheet the first time. */
  public void selectTab(int index) {
    if (index == selected) {
      return;
    }
    selected = index;
    scroll = 0;
    relayout();
    if (minecraft.getWindow().getGuiScale() != sheetScale) {
      // Auto chose another scale for this tab: the tab bar is laid out again at it.
      rebuildWidgets();
      return;
    }
    for (int tab = 0; tab < tabButtons.size(); tab++) {
      tabButtons.get(tab).setSelected(tab == selected);
    }
  }

  /** The sheet of tab {@code index}, built on first use and kept while the screen lives. */
  public Sheet sheet(int index) {
    Sheet sheet = sheets[index];
    if (sheet == null) {
      SheetContext buildContext = context;
      if (buildContext == null) {
        buildContext = new SheetContext(minecraft, font);
        context = buildContext;
      }
      sheet = sources.get(index).build(buildContext);
      sheets[index] = sheet;
    }
    return sheet;
  }

  /**
   * Lays the shown tab out and sets the GUI scale to its sheet scale, then places the widgets at
   * it. Also runs on every GUI resize (the game first puts the player's scale back).
   */
  @Override
  protected void init() {
    relayout();
    useGuiScale(sheetScale);
    int barWidth = minecraft.getWindow().getWidth() / barScale;
    int settingsX = barWidth - MARGIN - BarButton.SIZE;
    SettingsBarButton settingsButton = new SettingsBarButton(SETTINGS, button -> openSettings());
    settingsButton.place(settingsX, TOP, barScale, sheetScale);
    addRenderableWidget(settingsButton);
    addTabButtons(barWidth, settingsX - TAB_GAP * 2);
  }

  /**
   * Sets the window's GUI scale (not the player's option) to {@code scale}, and the screen's size
   * to the window's at it. The rest of what {@code Minecraft#resizeGui} does is not needed: the
   * screen sizes itself, and the mouse is converted to GUI pixels at every event and frame.
   */
  private void useGuiScale(int scale) {
    Window window = minecraft.getWindow();
    if (window.getGuiScale() != scale) {
      window.setGuiScale(scale);
    }
    width = window.getGuiScaledWidth();
    height = window.getGuiScaledHeight();
  }

  /** Puts the player's GUI scale back, as {@code Minecraft#resizeGui} computes it. */
  private void restorePlayerGuiScale() {
    Window window = minecraft.getWindow();
    window.setGuiScale(
        window.calculateScale(minecraft.options.guiScale().get(), minecraft.isEnforceUnicode()));
  }

  /**
   * Square tabs, icon only, centred on a bar {@code barWidth} bar pixels wide in the space left of
   * {@code right}.
   */
  private void addTabButtons(int barWidth, int right) {
    tabButtons.clear();
    List<SheetTabButton> buttons = new ArrayList<>(sources.size());
    int totalWidth = TAB_GAP * (sources.size() - 1);
    for (int index = 0; index < sources.size(); index++) {
      SheetSource source = sources.get(index);
      int tab = index;
      SheetTabButton button =
          SheetTabButton.create(source.icon(), tabName(source), pressed -> selectTab(tab));
      button.setSelected(index == selected);
      buttons.add(button);
      totalWidth += BarButton.SIZE;
    }
    int x = Math.max(MARGIN, Math.min((barWidth - totalWidth) / 2, right - totalWidth));
    for (SheetTabButton button : buttons) {
      button.place(x, TOP, barScale, sheetScale);
      x += BarButton.SIZE + TAB_GAP;
      tabButtons.add(addRenderableWidget(button));
    }
  }

  /** Picks the sheet scale and lays the shown tab out at it. */
  private void relayout() {
    Window window = minecraft.getWindow();
    int windowWidth = window.getWidth();
    int windowHeight = window.getHeight();
    barScale = barScale(window);
    int chosen =
        SheetScaling.choose(
            settings.preferences().sheetScale(),
            windowWidth,
            windowHeight,
            candidate -> layOutAt(candidate, windowWidth, windowHeight));
    // The last candidate tried may not be the one chosen (or none was tried): lay out again.
    layOutAt(chosen, windowWidth, windowHeight);
    scroll = ScrollRange.clamp(scroll, sheetHeight(), viewHeight());
  }

  /** Lays the shown tab out at {@code scale}; whether it then fits without scrolling. */
  private boolean layOutAt(int scale, int windowWidth, int windowHeight) {
    // A zoom is laid out for the old size and tab: closed, the player clicks again.
    dropZoom();
    sheetScale = scale;
    canvasWidth = SheetScaling.toSheet(windowWidth, scale);
    canvasHeight = SheetScaling.toSheet(windowHeight, scale);
    sheetTop = Math.ceilDiv(BAR_BOTTOM * barScale, scale) + SHEET_GAP;
    Sheet sheet = sheet(selected);
    if (sheet.hasSections()) {
      layout = null;
      sectionsLayout = Section.layout(sheet.sections(), font, canvasWidth - MARGIN * 2);
    } else {
      sectionsLayout = null;
      layout = SheetLayout.pack(sheet.sizes(), canvasWidth - MARGIN * 2, Card.MIN_WIDTH, CARD_GAP);
    }
    return fitsWithoutScrolling();
  }

  /**
   * The player's GUI scale, as {@code Minecraft#resizeGui} computes it, or the largest below it at
   * which every tab and the settings button fit across the window.
   */
  private int barScale(Window window) {
    int playerScale =
        window.calculateScale(minecraft.options.guiScale().get(), minecraft.isEnforceUnicode());
    int buttons = sources.size() + 1;
    int barWidth = MARGIN * 2 + buttons * BarButton.SIZE + (buttons - 1) * TAB_GAP + TAB_GAP;
    return Math.clamp(window.getWidth() / barWidth, 1, Math.max(1, playerScale));
  }

  /** Height of the view below the tab bar, in sheet pixels. */
  private int viewHeight() {
    return canvasHeight - sheetTop - MARGIN;
  }

  private void openSettings() {
    minecraft.gui.setScreen(new CheatSheetSettingsScreen(this, settings, keyMappings));
  }

  @Override
  public void onClose() {
    minecraft.gui.setScreen(parent);
  }

  /**
   * Called on close and when the settings open, before the next screen is laid out: puts the
   * player's GUI scale back, remembers the tab (file written if changed) and frees the pictures'
   * and scaled icons' textures (baked again if the sheets are shown again).
   */
  @Override
  public void removed() {
    dropZoom();
    restorePlayerGuiScale();
    CheatSheetClient.scaledItemIcons().release();
    settings.rememberLastOpenedTab(sources.get(selected).id());
    for (Sheet sheet : sheets) {
      if (sheet != null) {
        sheet.release();
      }
    }
  }

  /** Whether a card is zoomed. */
  public boolean isZoomOpen() {
    return zoom != null;
  }

  /** Forgets the zoom and frees what it baked; the caller sets the GUI scale. */
  private void dropZoom() {
    CardZoom open = zoom;
    if (open != null) {
      zoom = null;
      open.close();
    }
  }

  /** Closes the zoom; the sheet behind it is already drawn at its scale. */
  private void closeZoom() {
    dropZoom();
  }

  /** Escape closes the zoom first; the cheat sheet only when nothing is zoomed. */
  @Override
  public boolean keyPressed(KeyEvent event) {
    if (zoom != null && event.isEscape()) {
      closeZoom();
      return true;
    }
    return super.keyPressed(event);
  }

  /**
   * With a zoom open, a click outside it closes it and nothing else happens. Otherwise the tab bar
   * and the settings button come first, then a left click on a card zooms it.
   */
  @Override
  public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
    CardZoom open = zoom;
    if (open != null) {
      if (!open.contains(event.x(), event.y())) {
        closeZoom();
      }
      return true;
    }
    if (super.mouseClicked(event, doubleClick)) {
      return true;
    }
    if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !zoomsCards()) {
      return false;
    }
    CardSpot spot = new CardSpot();
    ZoomCard card = cardAt(event.x(), event.y(), spot);
    if (card == null) {
      return false;
    }
    Window window = minecraft.getWindow();
    int windowWidth = window.getWidth();
    int windowHeight = window.getHeight();
    ZoomSize size = card.size();
    if (size == null) {
      size = sources.get(selected).zoomSize();
    }
    int scale = size.scale(sheetScale, windowWidth, windowHeight, card.width(), card.height());
    zoom =
        CardZoom.open(
            card,
            sheet(selected).pictures(),
            windowWidth,
            windowHeight,
            sheetScale,
            scale,
            (spot.x() + spot.width() / 2) * sheetScale,
            (spot.y() + spot.height() / 2) * sheetScale);
    return true;
  }

  /** Whether the shown tab's cards light up and zoom. */
  private boolean zoomsCards() {
    return sources.get(selected).zoomsCards();
  }

  /**
   * The card at the point (sheet pixels: GUI pixels while the sheet is shown), or null; its bounds
   * on the screen in sheet pixels go into {@code spot}.
   */
  private @Nullable ZoomCard cardAt(double guiX, double guiY, CardSpot spot) {
    int mouseX = (int) Math.floor(guiX);
    int mouseY = (int) Math.floor(guiY);
    if (mouseY < sheetTop || mouseY >= sheetTop + viewHeight()) {
      return null;
    }
    int originY = sheetTop - scroll;
    SectionsLayout sectioned = sectionsLayout;
    SheetLayout current = layout;
    Sheet sheet = sheet(selected);
    if (sectioned != null) {
      if (!Section.locateCard(
          font, sheet.sections(), sectioned, mouseX - MARGIN, mouseY - originY, spot)) {
        return null;
      }
      ZoomCard card = Section.zoomCard(sheet.sections(), sectioned, spot);
      spot.set(spot.card(), MARGIN + spot.x(), originY + spot.y(), spot.width(), spot.height());
      return card;
    }
    if (current != null) {
      int index = cardIndexAt(current, mouseX - MARGIN, mouseY - originY);
      if (index >= 0) {
        CardBounds bounds = current.cards().get(index);
        spot.set(index, MARGIN + bounds.x(), originY + bounds.y(), bounds.width(), bounds.height());
        return sheet.cards().get(index).zoomCard(bounds.width(), bounds.height());
      }
    }
    return null;
  }

  /** Index of the masonry card at ({@code x}, {@code y}) from the sheet's corner, or -1. */
  private static int cardIndexAt(SheetLayout current, int x, int y) {
    List<CardBounds> bounds = current.cards();
    for (int index = 0; index < bounds.size(); index++) {
      CardBounds card = bounds.get(index);
      int localX = x - card.x();
      int localY = y - card.y();
      if (localX >= 0 && localX < card.width() && localY >= 0 && localY < card.height()) {
        return index;
      }
    }
    return -1;
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
    CardZoom open = zoom;
    if (open != null) {
      return true;
    }
    if (layout == null && sectionsLayout == null) {
      return false;
    }
    int offset = scroll - (int) Math.round(scrollY * SCROLL_STEP);
    scroll = ScrollRange.clamp(offset, sheetHeight(), viewHeight());
    return true;
  }

  @Override
  public void extractRenderState(
      GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    CheatSheetClient.scaledItemIcons().beginFrame();
    CardZoom open = zoom;
    // Behind a zoom, the sheet and the tab bar are drawn as they are, without the mouse.
    int sheetMouseX = open == null ? mouseX : NO_MOUSE;
    int sheetMouseY = open == null ? mouseY : NO_MOUSE;
    SectionsLayout sectioned = sectionsLayout;
    SheetLayout current = layout;
    hoveredTooltip = null;
    if (sectioned != null) {
      extractSections(graphics, sectioned, sheetMouseX, sheetMouseY);
    } else if (current != null) {
      extractSheet(graphics, current, sheetMouseX, sheetMouseY);
    }
    super.extractRenderState(graphics, sheetMouseX, sheetMouseY, partialTick);
    if (open != null) {
      graphics.nextStratum();
      if (blursMenus()) {
        graphics.blurBeforeThisStratum();
      }
      graphics.fill(0, 0, width, height, SheetPalette.ZOOM_VEIL);
      open.extract(graphics);
      hoveredTooltip = open.tooltipAt(mouseX, mouseY);
    }
    List<FormattedCharSequence> tooltip = hoveredTooltip;
    if (tooltip != null) {
      graphics.setTooltipForNextFrame(font, tooltip, mouseX, mouseY);
    }
  }

  /**
   * With a zoom open, the menu blur moves from under the sheet to above it (vanilla blurs once per
   * frame): the world and the sheet behind the zoom are blurred together.
   */
  @Override
  protected void extractBlurredBackground(GuiGraphicsExtractor graphics) {
    if (zoom == null) {
      super.extractBlurredBackground(graphics);
    }
  }

  /** Whether the player's menu blur is on (vanilla skips it below {@link #MIN_BLUR}). */
  private boolean blursMenus() {
    return minecraft.options.getMenuBackgroundBlurriness() >= MIN_BLUR;
  }

  private void extractSheet(
      GuiGraphicsExtractor graphics, SheetLayout current, int mouseX, int mouseY) {
    Sheet sheet = sheet(selected);
    List<Card> cards = sheet.cards();
    List<CardBounds> bounds = current.cards();
    PictureAtlas pictures = sheet.pictures();
    if (pictures != null) {
      pictures.beginFrame(graphics, sheetScale);
    }
    int viewBottom = sheetTop + viewHeight();
    int originY = sheetTop - scroll;
    graphics.enableScissor(0, sheetTop, canvasWidth, viewBottom);
    for (int index = 0; index < cards.size(); index++) {
      CardBounds card = bounds.get(index);
      int top = originY + card.y();
      if (top >= viewBottom || top + card.height() <= sheetTop) {
        continue;
      }
      cards.get(index).extract(graphics, MARGIN + card.x(), top, card.width(), card.height());
    }
    graphics.disableScissor();
    extractScrollBar(graphics, current.contentHeight(), viewBottom);

    if (mouseY < sheetTop || mouseY >= viewBottom) {
      return;
    }
    for (int index = 0; index < cards.size(); index++) {
      CardBounds card = bounds.get(index);
      int localX = mouseX - MARGIN - card.x();
      int localY = mouseY - originY - card.y();
      if (localX >= 0 && localX < card.width() && localY >= 0 && localY < card.height()) {
        hoveredTooltip = cards.get(index).tooltipAt(localX, localY, card.width());
        if (zoomsCards()) {
          extractHover(
              graphics, MARGIN + card.x(), originY + card.y(), card.width(), card.height());
        }
        return;
      }
    }
  }

  /**
   * Sections in reading order, culled to the view, and the tooltip under the mouse ({@code mouseX},
   * {@code mouseY} in sheet pixels).
   */
  private void extractSections(
      GuiGraphicsExtractor graphics, SectionsLayout sectioned, int mouseX, int mouseY) {
    Sheet sheet = sheet(selected);
    List<Section> sections = sheet.sections();
    PictureAtlas pictures = sheet.pictures();
    if (pictures != null) {
      pictures.beginFrame(graphics, sheetScale);
    }
    int viewBottom = sheetTop + viewHeight();
    int originY = sheetTop - scroll;
    graphics.enableScissor(0, sheetTop, canvasWidth, viewBottom);
    Section.extract(graphics, font, sections, sectioned, MARGIN, originY, sheetTop, viewBottom);
    graphics.disableScissor();
    extractScrollBar(graphics, sectioned.contentHeight(), viewBottom);
    if (mouseY < sheetTop || mouseY >= viewBottom) {
      return;
    }
    hoveredTooltip =
        Section.tooltipAt(font, sections, sectioned, mouseX - MARGIN, mouseY - originY);
    CardSpot spot = hoveredCard;
    if (zoomsCards()
        && Section.locateCard(font, sections, sectioned, mouseX - MARGIN, mouseY - originY, spot)) {
      extractHover(graphics, MARGIN + spot.x(), originY + spot.y(), spot.width(), spot.height());
    }
  }

  /** The hover highlight over a card, clipped to the view, above the sheet. */
  private void extractHover(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
    graphics.nextStratum();
    graphics.enableScissor(0, sheetTop, canvasWidth, sheetTop + viewHeight());
    graphics.fill(x, y, x + width, y + height, SheetPalette.CARD_HOVER_FILL);
    graphics.outline(x, y, width, height, SheetPalette.CARD_HOVER_BORDER);
    graphics.disableScissor();
  }

  private void extractScrollBar(GuiGraphicsExtractor graphics, int contentHeight, int viewBottom) {
    int viewHeight = viewBottom - sheetTop;
    int maxOffset = ScrollRange.maxOffset(contentHeight, viewHeight);
    if (maxOffset == 0) {
      return;
    }
    int right = canvasWidth - SCROLL_BAR_INSET;
    int left = right - SCROLL_BAR_WIDTH;
    graphics.fill(left, sheetTop, right, viewBottom, SheetPalette.SCROLL_BAR_TRACK);
    int thumbHeight = Math.max(MIN_THUMB_HEIGHT, viewHeight * viewHeight / contentHeight);
    int thumbTop = sheetTop + (viewHeight - thumbHeight) * scroll / maxOffset;
    graphics.fill(left, thumbTop, right, thumbTop + thumbHeight, SheetPalette.SCROLL_BAR_THUMB);
  }

  private static Component tabName(SheetSource source) {
    return Component.translatable(TAB_KEY_PREFIX + source.id());
  }
}
