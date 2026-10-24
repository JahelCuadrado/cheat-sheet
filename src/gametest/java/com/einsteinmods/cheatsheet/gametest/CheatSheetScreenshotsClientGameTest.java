package com.einsteinmods.cheatsheet.gametest;

import com.einsteinmods.cheatsheet.client.CheatSheetClient;
import com.einsteinmods.cheatsheet.client.CheatSheetScreen;
import com.einsteinmods.cheatsheet.client.config.CheatSheetSettingsScreen;
import com.einsteinmods.cheatsheet.client.config.ClientPreferences;
import com.einsteinmods.cheatsheet.client.config.ClientSettings;
import com.einsteinmods.cheatsheet.client.picture.PictureAtlas;
import com.einsteinmods.cheatsheet.client.sheet.SheetContext;
import com.einsteinmods.cheatsheet.client.sheet.SheetSource;
import com.einsteinmods.cheatsheet.client.tabs.BeaconSheet;
import com.einsteinmods.cheatsheet.client.tabs.CheatSheetTabs;
import com.einsteinmods.cheatsheet.client.tabs.DiscsSheet;
import com.einsteinmods.cheatsheet.client.tabs.DyesSheet;
import com.einsteinmods.cheatsheet.client.tabs.EnchantmentsSheet;
import com.einsteinmods.cheatsheet.client.tabs.FarmingSheet;
import com.einsteinmods.cheatsheet.client.tabs.FoodSheet;
import com.einsteinmods.cheatsheet.client.tabs.OresSheet;
import com.einsteinmods.cheatsheet.client.tabs.PotionsSheet;
import com.einsteinmods.cheatsheet.client.tabs.TrimsSheet;
import com.einsteinmods.cheatsheet.client.tabs.VillagersSheet;
import com.einsteinmods.cheatsheet.core.SheetScale;
import com.mojang.blaze3d.platform.InputConstants;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import org.jspecify.annotations.Nullable;

/**
 * Visual check, not part of {@code build}: opens the cheat sheet in a world at several GUI sizes
 * and saves screenshots ({@code build/run/clientGameTest/screenshots}), plus the pause menu with
 * the book button (clicked: it opens the cheat sheet, Escape returns to the menu) and the settings
 * screen, and a card of several tabs zoomed (clicked; Escape and a click outside close the zoom
 * only). Also measures, in development only, how long each sheet takes to build and what one frame
 * of each open tab costs (time and bytes allocated on the render thread, vanilla's own render
 * states included).
 *
 * <p>GUI sizes match a 1920x1080 screen at GUI scale 2 (960x540), 3 (640x360) and 4 (480x270), and
 * the smallest the game allows (320x240), using small windows so the test stays light.
 */
public final class CheatSheetScreenshotsClientGameTest implements FabricClientGameTest {
  private static final String REPORT_PREFIX = "[cheatsheet-measure] ";
  private static final int WARM_BUILDS = 50;
  private static final int WARM_FRAMES = 200;
  private static final int MEASURED_FRAMES = 1000;
  private static final int SETTLE_TICKS = 3;
  private static final int SCROLL_NOTCHES = 50;
  private static final int RELOAD_TICKS = 1200;
  private static final String SPANISH = "es_es";

  /**
   * Every language the mod is translated to, to check that Ores, Beacon and Farming fit in each.
   */
  private static final List<String> LANGUAGES =
      List.of(
          "en_us", "es_es", "es_mx", "de_de", "fr_fr", "it_it", "nl_nl", "pl_pl", "pt_br", "pt_pt",
          "ru_ru", "uk_ua", "tr_tr", "ja_jp", "ko_kr", "zh_cn", "zh_tw");

  private static final String PAUSE_BUTTON_KEY = "cheatsheet.button";
  private static final double NANOS_PER_MILLI = 1_000_000.0;
  private static final double NANOS_PER_MICRO = 1_000.0;
  private static final long BYTES_PER_KB = 1024;

  /** Tabs that must fit one 960x540 sheet (villagers: its professions, the rest may scroll). */
  private static final List<String> ONE_SCREEN_TABS =
      List.of(
          TrimsSheet.ID,
          DiscsSheet.ID,
          DyesSheet.ID,
          EnchantmentsSheet.ID,
          PotionsSheet.ID,
          VillagersSheet.ID,
          FoodSheet.ID,
          BeaconSheet.ID,
          FarmingSheet.ID);

  private static final List<String> VILLAGERS_FOOD_BEACON =
      List.of(VillagersSheet.ID, FoodSheet.ID, BeaconSheet.ID);
  private static final int VILLAGER_PAGES = 3;
  private static final int BEACON_PAGES = 5;
  private static final int UNCAPPED_FPS = 260;
  private static final int WARM_TICKS = 40;
  private static final int MEASURED_TICKS = 60;
  private static final double NANOS_PER_SECOND = 1_000_000_000.0;
  private static final int VILLAGER_PAGE_NOTCHES = 15;
  private static final double TRADE_X = 0.24;
  private static final double TRADE_Y = 0.76;
  private static final double RECIPE_X = 0.458;
  private static final double RECIPE_Y = 0.417;
  private static final double LEVEL_BADGE_X = 0.1375;
  private static final double LEVEL_BADGE_Y = 0.537;
  private static final double TRIM_COLOUR_X = 0.8;
  private static final double TRIM_COLOUR_Y = 0.3;
  private static final double FOOD_X = 0.1;
  private static final double FOOD_Y = 0.42;

  /** Over the farming field's plan and over the first breeding card's portrait (GUI 960x540). */
  private static final double FIELD_X = 0.36;

  private static final double FIELD_Y = 0.25;
  private static final double FIRST_ANIMAL_X = 0.085;
  private static final double FIRST_ANIMAL_Y = 0.48;

  /** Over the enchanted pickaxe the 8-book anvil pyramid ends with (GUI 960x540). */
  private static final double ANVIL_DONE_X = 0.428;

  private static final double ANVIL_DONE_Y = 0.83;

  /** Over the first ore's icon (GUI 480x270, sheet at 1x: pose scale 1/2). */
  private static final double AUTO_ORE_X = 0.128;

  private static final double AUTO_ORE_Y = 0.167;

  /** Over the level 4 pyramid (GUI 640x360, sheet at 2x: pose scale 1). */
  private static final double AUTO_PYRAMID_X = 0.695;

  private static final double AUTO_PYRAMID_Y = 0.18;

  /** Over the settings' sheet size row (GUI 640x360). */
  private static final double SHEET_SIZE_ROW_X = 0.3;

  private static final double SHEET_SIZE_ROW_Y = 0.2875;

  /**
   * A tab shot at GUI 960x540 and 480x270, scrolled to the bottom and with a tooltip at 480x270:
   * the cursor at this fraction of the window (over an item or a picture of the first cards).
   */
  private record TabShot(String id, double tooltipX, double tooltipY) {}

  private static final List<TabShot> TAB_SHOTS =
      List.of(
          new TabShot("enchantments", 0.07, 0.54),
          new TabShot("potions", 0.21, 0.22),
          new TabShot("trims", 0.07, 0.41),
          new TabShot("discs", 0.08, 0.49),
          new TabShot("dyes", 0.12, 0.3));

  /**
   * A tab whose card is zoomed: the first card found scanning right then down from this fraction of
   * the GUI (below the tab bar); {@code name} names its screenshots.
   */
  private record ZoomShot(String id, String name, double startX, double startY) {
    ZoomShot(String id, double startX, double startY) {
      this(id, id, startX, startY);
    }
  }

  /** Over the middle tab of the tab bar (GUI pixels right of the window's centre, from the top). */
  private static final int TAB_HOVER_X = 10;

  private static final int TAB_HOVER_Y = 14;

  private static final List<ZoomShot> ZOOM_SHOTS =
      List.of(
          new ZoomShot(DiscsSheet.ID, 0.1, 0.2),
          new ZoomShot(VillagersSheet.ID, 0.1, 0.25),
          new ZoomShot(TrimsSheet.ID, 0.3, 0.2),
          new ZoomShot(DyesSheet.ID, 0.1, 0.2));

  /** Over gold's and Nether gold's striped bonus strips (GUI 960x540, sheet at 1x). */
  private static final double GOLD_BONUS_X = 0.282;

  private static final double GOLD_BONUS_Y = 0.37;
  private static final double NETHER_GOLD_BONUS_X = 0.4635;
  private static final double NETHER_GOLD_BONUS_Y = 0.24;

  private static final int ZOOM_SCAN_STEP = 4;
  private static final int ZOOM_BUILDS = 20;

  /** A window size and GUI scale, named by the GUI size they give. */
  private record View(String name, int windowWidth, int windowHeight, int guiScale) {}

  private static final List<View> VIEWS =
      List.of(
          new View("gui-960x540", 960, 540, 1),
          new View("gui-640x360", 1280, 720, 2),
          new View("gui-480x270", 960, 540, 2),
          new View("gui-320x240", 640, 480, 2));

  /** {@code -PclientGameTestOnly=crisp}: only {@link #crispShots}. */
  private static final String ONLY_PROPERTY = "cheatsheet.clientGameTestOnly";

  private static final String CRISP_ONLY = "crisp";

  /** {@code -PclientGameTestOnly=fit}: only {@link #fitAtPlayerSetup}. */
  private static final String FIT_ONLY = "fit";

  /** A 1080p screen at GUI scale 3, the sheet on auto: the player's setup. */
  private static final View PLAYER_SETUP = new View("1920x1080-gui3", 1920, 1080, 3);

  /**
   * A 1080p screen's ratio at GUI scale 3, the sheet forced to 2x (before the fix, a pose of 2/3)
   * and on auto, and a 1080p screen itself at GUI scale 3 on auto (the player's setup).
   */
  private record CrispRun(View view, SheetScale scale, List<ZoomShot> shots) {}

  /** Where a zoom would be looked for on the ores tab, which must not zoom. */
  private static final ZoomShot ORES_SHOT = new ZoomShot(OresSheet.ID, 0.1, 0.2);

  private static final List<ZoomShot> CRISP_SHOTS =
      List.of(
          new ZoomShot(OresSheet.ID, 0.2, 0.4),
          new ZoomShot(DiscsSheet.ID, 0.1, 0.2),
          new ZoomShot(VillagersSheet.ID, 0.1, 0.25),
          new ZoomShot(TrimsSheet.ID, 0.3, 0.2));

  /** Over the conduit's frame tile, whose zoom is the step-by-step guide (GUI fractions). */
  private static final String BEACON_CONDUIT = "beacon-conduit";

  private static final double CONDUIT_X = 0.05;
  private static final double CONDUIT_Y = 0.45;

  /** Over the anvil guide's explanation (GUI fractions). */
  private static final double ANVIL_GUIDE_X = 0.05;

  private static final double ANVIL_GUIDE_Y = 0.75;

  /** Every tab, a card of each zoomed: the player's setup only. */
  private static final List<ZoomShot> EVERY_TAB_SHOTS =
      List.of(
          new ZoomShot(OresSheet.ID, 0.2, 0.4),
          new ZoomShot(EnchantmentsSheet.ID, 0.1, 0.15),
          new ZoomShot(EnchantmentsSheet.ID, "enchantments-anvil", ANVIL_GUIDE_X, ANVIL_GUIDE_Y),
          new ZoomShot(PotionsSheet.ID, 0.1, 0.15),
          new ZoomShot(TrimsSheet.ID, 0.1, 0.15),
          new ZoomShot(TrimsSheet.ID, "trims-colours", 0.6, 0.3),
          new ZoomShot(DiscsSheet.ID, 0.1, 0.2),
          new ZoomShot(DyesSheet.ID, 0.1, 0.15),
          new ZoomShot(VillagersSheet.ID, 0.1, 0.25),
          new ZoomShot(FoodSheet.ID, 0.1, 0.15),
          new ZoomShot(BeaconSheet.ID, 0.1, 0.15),
          new ZoomShot(BeaconSheet.ID, BEACON_CONDUIT, CONDUIT_X, CONDUIT_Y),
          new ZoomShot(FarmingSheet.ID, 0.1, 0.15),
          new ZoomShot(FarmingSheet.ID, "farming-cow", 0.13, 0.462),
          new ZoomShot(FarmingSheet.ID, "farming-sheep", 0.278, 0.462),
          new ZoomShot(FarmingSheet.ID, "farming-rabbit", 0.722, 0.462),
          new ZoomShot(FarmingSheet.ID, "farming-horse", 0.13, 0.572),
          new ZoomShot(FarmingSheet.ID, "farming-wolf", 0.574, 0.572),
          new ZoomShot(FarmingSheet.ID, "farming-cat", 0.722, 0.572),
          new ZoomShot(FarmingSheet.ID, "farming-llama", 0.426, 0.572),
          new ZoomShot(FarmingSheet.ID, "farming-sniffer", 0.426, 0.757));

  /** The zoomed card with the most variant portraits: its frame is measured. */
  private static final String MEASURED_VARIANTS = "farming-cat";

  /** Over one look of a zoomed breeding card's variants, at 1920x1080 (window fractions). */
  private static final Map<String, double[]> VARIANT_HOVERS =
      Map.of(
          "farming-cow", new double[] {0.824, 0.329},
          "farming-sheep", new double[] {0.55, 0.278},
          "farming-rabbit", new double[] {0.734, 0.245},
          "farming-wolf", new double[] {0.648, 0.27},
          "farming-cat", new double[] {0.884, 0.778});

  private static final List<CrispRun> CRISP_RUNS =
      List.of(
          new CrispRun(new View("1440x810-gui3", 1440, 810, 3), SheetScale.TWO, CRISP_SHOTS),
          new CrispRun(new View("1440x810-gui3", 1440, 810, 3), SheetScale.AUTO, CRISP_SHOTS),
          new CrispRun(
              new View("1920x1080-gui3", 1920, 1080, 3), SheetScale.AUTO, EVERY_TAB_SHOTS));

  @Override
  public void runTest(ClientGameTestContext context) {
    try (TestSingleplayerContext world = context.worldBuilder().create()) {
      world.getConnection().waitForChunksRender();
      if (CRISP_ONLY.equals(System.getProperty(ONLY_PROPERTY))) {
        crispShots(context);
        return;
      }
      if (FIT_ONLY.equals(System.getProperty(ONLY_PROPERTY))) {
        fitAtPlayerSetup(context);
        return;
      }
      context.runOnClient(CheatSheetScreenshotsClientGameTest::measureBuild);

      context.setScreen(
          () ->
              new CheatSheetScreen(
                  null,
                  CheatSheetClient.settings(),
                  CheatSheetClient.keyMappings(),
                  CheatSheetTabs.ALL));
      cardZoomShots(context);
      measureSheetHeights(context);
      autoScaleShots(context);
      for (View view : VIEWS) {
        show(context, view);
        screenshot(context, "ores-" + view.name());
      }

      // A tooltip: the cursor over the middle of the first chart's ore icons.
      View scaleFour = VIEWS.get(2);
      show(context, scaleFour);
      context
          .getInput()
          .setCursorPos(scaleFour.windowWidth() * 0.55, scaleFour.windowHeight() * 0.3);
      context.waitTicks(SETTLE_TICKS);
      screenshot(context, "ores-tooltip-" + scaleFour.name());
      context.getInput().setCursorPos(0, 0);
      context.runOnClient(client -> measureFrame(client, OresSheet.ID));

      // Scrolled to the bottom where the sheet is taller than the screen.
      for (View view : List.of(scaleFour, VIEWS.getLast())) {
        show(context, view);
        context.getInput().setCursorPos(view.windowWidth() / 2.0, view.windowHeight() / 2.0);
        context.getInput().scroll(-SCROLL_NOTCHES);
        context.getInput().setCursorPos(0, 0);
        context.waitTicks(SETTLE_TICKS);
        screenshot(context, "ores-scrolled-" + view.name());
      }

      // The other tabs: whole sheet at scale 2 and 4, scrolled to the bottom at 4, a tooltip and
      // the frame cost.
      for (TabShot tab : TAB_SHOTS) {
        selectTab(context, tab.id());
        for (View view : List.of(VIEWS.getFirst(), scaleFour)) {
          show(context, view);
          // Out of the sheet after the resize, so no tooltip covers it.
          context.getInput().setCursorPos(0, 0);
          context.waitTicks(SETTLE_TICKS);
          screenshot(context, tab.id() + "-" + view.name());
        }
        context.runOnClient(client -> measureFrame(client, tab.id()));
        context
            .getInput()
            .setCursorPos(
                scaleFour.windowWidth() * tab.tooltipX(),
                scaleFour.windowHeight() * tab.tooltipY());
        context.waitTicks(SETTLE_TICKS);
        screenshot(context, tab.id() + "-tooltip-" + scaleFour.name());
        context
            .getInput()
            .setCursorPos(scaleFour.windowWidth() / 2.0, scaleFour.windowHeight() / 2.0);
        context.getInput().scroll(-SCROLL_NOTCHES);
        context.getInput().setCursorPos(0, 0);
        context.waitTicks(SETTLE_TICKS);
        screenshot(context, tab.id() + "-scrolled-" + scaleFour.name());
      }
      trimsAtEverySize(context);
      discsAndDyesAtSmallSizes(context);
      enchantmentsPotionsFoodAtSmallest(context);
      beaconAtEverySize(context);
      farmingShots(context);
      anvilShots(context);
      selectTab(context, OresSheet.ID);
      villagersFoodBeacon(context);

      // Spanish, the other language written by hand.
      View scaleThree = VIEWS.get(1);
      show(context, scaleThree);
      CompletableFuture<Void> reload =
          context.computeOnClient(
              client -> {
                client.getLanguageManager().setSelected(SPANISH);
                client.options.languageCode = SPANISH;
                return client.reloadResourcePacks();
              });
      context.waitFor(client -> reload.isDone() && client.gui.overlay() == null, RELOAD_TICKS);
      context.setScreen(
          () ->
              new CheatSheetScreen(
                  null,
                  CheatSheetClient.settings(),
                  CheatSheetClient.keyMappings(),
                  CheatSheetTabs.ALL));
      // The new screen opens on the last tab shown; the shot is of the ores tab, cursor away.
      context.getInput().setCursorPos(0, 0);
      selectTab(context, OresSheet.ID);
      context.waitTicks(SETTLE_TICKS);
      screenshot(context, "ores-es_es-" + scaleThree.name());
      for (TabShot tab : TAB_SHOTS) {
        selectTab(context, tab.id());
        screenshot(context, tab.id() + "-es_es-" + scaleThree.name());
      }
      for (String tab : VILLAGERS_FOOD_BEACON) {
        selectTab(context, tab);
        screenshot(context, tab + "-es_es-" + scaleThree.name());
      }
      selectTab(context, FarmingSheet.ID);
      screenshot(context, FarmingSheet.ID + "-es_es-" + scaleThree.name());
      show(context, VIEWS.getFirst());
      screenshot(context, FarmingSheet.ID + "-es_es-" + VIEWS.getFirst().name());
      selectTab(context, EnchantmentsSheet.ID);
      screenshot(context, EnchantmentsSheet.ID + "-es_es-" + VIEWS.getFirst().name());
      show(context, scaleThree);

      View scaleTwo = VIEWS.getFirst();
      show(context, scaleTwo);
      context.setScreen(() -> new PauseScreen(true));
      context.waitTicks(SETTLE_TICKS);
      screenshot(context, "pause-menu-" + scaleTwo.name());
      // The book button opens the cheat sheet; Escape goes back to the pause menu.
      context.clickScreenButton(PAUSE_BUTTON_KEY);
      context.waitForScreen(CheatSheetScreen.class);
      context.getInput().pressKey(InputConstants.KEY_ESCAPE);
      context.waitForScreen(PauseScreen.class);
      context.setScreen(
          () ->
              new CheatSheetSettingsScreen(
                  null, CheatSheetClient.settings(), CheatSheetClient.keyMappings()));
      context.waitTicks(SETTLE_TICKS);
      screenshot(context, "settings-" + scaleTwo.name());
      autoScaleInEveryLanguage(context);
      context.setScreen(() -> null);
    }
  }

  /**
   * The trims tables at the two sizes the tab loop skips (GUI 640x360 and 320x240), each also
   * scrolled to the bottom; at 960x540, the frame cost with the whole sheet on screen and a tooltip
   * over a cell of the colour table.
   */
  private static void trimsAtEverySize(ClientGameTestContext context) {
    selectTab(context, TrimsSheet.ID);
    for (View view : List.of(VIEWS.get(1), VIEWS.getLast())) {
      show(context, view);
      context.getInput().setCursorPos(0, 0);
      context.waitTicks(SETTLE_TICKS);
      screenshot(context, "trims-" + view.name());
      context.getInput().setCursorPos(view.windowWidth() / 2.0, view.windowHeight() / 2.0);
      context.getInput().scroll(-SCROLL_NOTCHES);
      context.getInput().setCursorPos(0, 0);
      context.waitTicks(SETTLE_TICKS);
      screenshot(context, "trims-scrolled-" + view.name());
    }
    // The whole sheet on screen at 960x540: every picture drawn, the dearest frame.
    View scaleTwo = VIEWS.getFirst();
    show(context, scaleTwo);
    selectTab(context, OresSheet.ID);
    selectTab(context, TrimsSheet.ID);
    context.runOnClient(client -> measureFrame(client, TrimsSheet.ID));
    measureRenderThread(context, TrimsSheet.ID);
    context
        .getInput()
        .setCursorPos(
            scaleTwo.windowWidth() * TRIM_COLOUR_X, scaleTwo.windowHeight() * TRIM_COLOUR_Y);
    context.waitTicks(SETTLE_TICKS);
    screenshot(context, "trims-colour-tooltip-" + scaleTwo.name());
    context.getInput().setCursorPos(0, 0);
  }

  /** Discs and dyes at the two sizes the tab loop skips (GUI 640x360 and 320x240). */
  private static void discsAndDyesAtSmallSizes(ClientGameTestContext context) {
    for (String tab : List.of(DiscsSheet.ID, DyesSheet.ID)) {
      selectTab(context, tab);
      for (View view : List.of(VIEWS.get(1), VIEWS.getLast())) {
        show(context, view);
        context.getInput().setCursorPos(0, 0);
        context.waitTicks(SETTLE_TICKS);
        screenshot(context, tab + "-" + view.name());
      }
    }
  }

  /**
   * Enchantments, Potions and Food at the smallest GUI (320x240), also scrolled to the bottom; the
   * first food's tooltip.
   */
  private static void enchantmentsPotionsFoodAtSmallest(ClientGameTestContext context) {
    View smallest = VIEWS.getLast();
    for (String tab : List.of(EnchantmentsSheet.ID, PotionsSheet.ID, FoodSheet.ID)) {
      selectTab(context, tab);
      show(context, smallest);
      context.getInput().setCursorPos(0, 0);
      context.waitTicks(SETTLE_TICKS);
      screenshot(context, tab + "-" + smallest.name());
      if (tab.equals(FoodSheet.ID)) {
        context
            .getInput()
            .setCursorPos(smallest.windowWidth() * FOOD_X, smallest.windowHeight() * FOOD_Y);
        context.waitTicks(SETTLE_TICKS);
        screenshot(context, tab + "-tooltip-" + smallest.name());
      }
      context.getInput().setCursorPos(smallest.windowWidth() / 2.0, smallest.windowHeight() / 2.0);
      context.getInput().scroll(-SCROLL_NOTCHES);
      context.getInput().setCursorPos(0, 0);
      context.waitTicks(SETTLE_TICKS);
      screenshot(context, tab + "-scrolled-" + smallest.name());
    }
  }

  /**
   * The beacon tiles at the two sizes the villagers/food/beacon loop skips (GUI 640x360 and
   * 320x240), each also scrolled to the bottom (the recipe tiles); at 480x270 and 320x240, page by
   * page down to the recipes.
   */
  private static void beaconAtEverySize(ClientGameTestContext context) {
    selectTab(context, BeaconSheet.ID);
    for (View view : List.of(VIEWS.get(1), VIEWS.getLast())) {
      show(context, view);
      context.getInput().setCursorPos(0, 0);
      context.waitTicks(SETTLE_TICKS);
      screenshot(context, "beacon-" + view.name());
      context.getInput().setCursorPos(view.windowWidth() / 2.0, view.windowHeight() / 2.0);
      context.getInput().scroll(-SCROLL_NOTCHES);
      context.getInput().setCursorPos(0, 0);
      context.waitTicks(SETTLE_TICKS);
      screenshot(context, "beacon-scrolled-" + view.name());
    }
    for (View view : List.of(VIEWS.get(2), VIEWS.getLast())) {
      show(context, view);
      // Back to the top: changing tabs resets the scroll.
      selectTab(context, OresSheet.ID);
      selectTab(context, BeaconSheet.ID);
      for (int page = 1; page <= BEACON_PAGES; page++) {
        context.getInput().setCursorPos(view.windowWidth() / 2.0, view.windowHeight() / 2.0);
        context.getInput().scroll(-VILLAGER_PAGE_NOTCHES);
        context.getInput().setCursorPos(0, 0);
        context.waitTicks(SETTLE_TICKS);
        screenshot(context, "beacon-page-" + page + "-" + view.name());
      }
    }
    selectTab(context, OresSheet.ID);
  }

  /**
   * The farming tab at GUI 960x540 (the whole tab on one screen), 640x360 and 480x270, a block's
   * and a breeding card's tooltip and the frame cost at 960x540; fails if a plan texture is
   * missing.
   */
  private static void farmingShots(ClientGameTestContext context) {
    context.runOnClient(
        client -> {
          for (SpriteId sprite : FarmingSheet.SPRITES) {
            if (client
                .getAtlasManager()
                .get(sprite)
                .contents()
                .name()
                .equals(MissingTextureAtlasSprite.getLocation())) {
              throw new AssertionError("missing farming texture " + sprite.texture());
            }
          }
        });
    selectTab(context, FarmingSheet.ID);
    for (View view : List.of(VIEWS.get(1), VIEWS.get(2), VIEWS.getFirst())) {
      show(context, view);
      context.getInput().setCursorPos(0, 0);
      context.waitTicks(SETTLE_TICKS);
      screenshot(context, FarmingSheet.ID + "-" + view.name());
      report("%s at %s: %s", FarmingSheet.ID, view.name(), layoutReport(context));
    }
    View scaleTwo = VIEWS.getFirst();
    context.runOnClient(client -> measureFrame(client, FarmingSheet.ID));
    context
        .getInput()
        .setCursorPos(scaleTwo.windowWidth() * FIELD_X, scaleTwo.windowHeight() * FIELD_Y);
    context.waitTicks(SETTLE_TICKS);
    screenshot(context, FarmingSheet.ID + "-field-tooltip-" + scaleTwo.name());
    context
        .getInput()
        .setCursorPos(
            scaleTwo.windowWidth() * FIRST_ANIMAL_X, scaleTwo.windowHeight() * FIRST_ANIMAL_Y);
    context.waitTicks(SETTLE_TICKS);
    screenshot(context, FarmingSheet.ID + "-animal-tooltip-" + scaleTwo.name());
    context.getInput().setCursorPos(0, 0);
    selectTab(context, OresSheet.ID);
  }

  /**
   * The enchantments tab at GUI 960x540 (the whole tab on one screen): the frame cost with the
   * anvil card shown and the tooltip of the gear the 8-book pyramid ends with.
   */
  private static void anvilShots(ClientGameTestContext context) {
    selectTab(context, EnchantmentsSheet.ID);
    View scaleTwo = VIEWS.getFirst();
    show(context, scaleTwo);
    context.getInput().setCursorPos(0, 0);
    context.waitTicks(SETTLE_TICKS);
    context.runOnClient(client -> measureFrame(client, EnchantmentsSheet.ID));
    context
        .getInput()
        .setCursorPos(
            scaleTwo.windowWidth() * ANVIL_DONE_X, scaleTwo.windowHeight() * ANVIL_DONE_Y);
    context.waitTicks(SETTLE_TICKS);
    screenshot(context, EnchantmentsSheet.ID + "-anvil-tooltip-" + scaleTwo.name());
    context.getInput().setCursorPos(0, 0);
    selectTab(context, OresSheet.ID);
  }

  /**
   * Villagers, Food and Beacon tabs at GUI scale 2 and 4 with their frame cost at scale 2 (the most
   * cards on screen); Villagers also scrolled page by page and to the bottom, with a trade's, a
   * recipe ingredient's and a level badge's tooltip.
   */
  private static void villagersFoodBeacon(ClientGameTestContext context) {
    View scaleTwo = VIEWS.getFirst();
    View scaleFour = VIEWS.get(2);
    // Out of the sheet, so no tooltip covers the shots.
    context.getInput().setCursorPos(0, 0);
    for (String tab : VILLAGERS_FOOD_BEACON) {
      selectTab(context, tab);
      for (View view : List.of(scaleTwo, scaleFour)) {
        show(context, view);
        screenshot(context, tab + "-" + view.name());
      }
      show(context, scaleTwo);
      context.runOnClient(client -> measureFrame(client, tab));
      measureRenderThread(context, tab);
    }
    // Closing frees the pictures' textures (the villagers' were baked above).
    int villagers = CheatSheetTabs.ids().indexOf(VillagersSheet.ID);
    PictureAtlas pictures =
        context.computeOnClient(
            client ->
                client.gui.screen() instanceof CheatSheetScreen screen
                    ? screen.sheet(villagers).pictures()
                    : null);
    context.setScreen(() -> null);
    if (pictures == null || pictures.textureBytes() != 0) {
      throw new AssertionError("closing the cheat sheet must free its pictures' texture");
    }
    measureRenderThread(context, "no screen");
    context.setScreen(
        () ->
            new CheatSheetScreen(
                null,
                CheatSheetClient.settings(),
                CheatSheetClient.keyMappings(),
                CheatSheetTabs.ALL));
    context.waitTicks(SETTLE_TICKS);
    selectTab(context, VillagersSheet.ID);
    for (View view : List.of(scaleTwo, scaleFour)) {
      show(context, view);
      for (int page = 1; page <= VILLAGER_PAGES; page++) {
        context.getInput().setCursorPos(view.windowWidth() / 2.0, view.windowHeight() / 2.0);
        context.getInput().scroll(-VILLAGER_PAGE_NOTCHES);
        context.getInput().setCursorPos(0, 0);
        context.waitTicks(SETTLE_TICKS);
        screenshot(context, "villagers-scrolled-" + page + "-" + view.name());
      }
      context.getInput().setCursorPos(view.windowWidth() / 2.0, view.windowHeight() / 2.0);
      context.getInput().scroll(-SCROLL_NOTCHES);
      context.getInput().setCursorPos(0, 0);
      context.waitTicks(SETTLE_TICKS);
      screenshot(context, "villagers-scrolled-bottom-" + view.name());
    }
    // A tooltip: the cursor over an enchanted trade (random price) of the first profession card.
    show(context, scaleFour);
    // Back to the top: changing tabs resets the scroll.
    selectTab(context, FoodSheet.ID);
    selectTab(context, VillagersSheet.ID);
    context
        .getInput()
        .setCursorPos(scaleFour.windowWidth() * TRADE_X, scaleFour.windowHeight() * TRADE_Y);
    context.waitTicks(SETTLE_TICKS);
    screenshot(context, "villagers-tooltip-" + scaleFour.name());
    // And over an ingredient of the first card's workstation recipe.
    context
        .getInput()
        .setCursorPos(scaleFour.windowWidth() * RECIPE_X, scaleFour.windowHeight() * RECIPE_Y);
    context.waitTicks(SETTLE_TICKS);
    screenshot(context, "villagers-recipe-tooltip-" + scaleFour.name());
    // And over the first card's novice badge: the trades that have no slot.
    context
        .getInput()
        .setCursorPos(
            scaleFour.windowWidth() * LEVEL_BADGE_X, scaleFour.windowHeight() * LEVEL_BADGE_Y);
    context.waitTicks(SETTLE_TICKS);
    screenshot(context, "villagers-level-tooltip-" + scaleFour.name());
    context.getInput().setCursorPos(0, 0);
  }

  /**
   * At GUI 960x540 and 640x360, a card of each {@link #ZOOM_SHOTS} tab: under the mouse (hover
   * highlight), clicked (zoomed) and, at 960x540, with the mouse over the zoom; checks that Escape
   * and a click outside close the zoom and leave the cheat sheet open. At 960x540 also measures
   * what a click costs (building the zoom) and a frame with the zoom open.
   */
  private static void cardZoomShots(ClientGameTestContext context) {
    for (View view : List.of(VIEWS.getFirst(), VIEWS.get(1))) {
      show(context, view);
      selectTab(context, OresSheet.ID);
      if (view == VIEWS.getFirst()) {
        bonusTooltips(context, view);
      }
      if (context.computeOnClient(client -> tryFindCard(client, ORES_SHOT)) != null) {
        throw new AssertionError("the ores tab must not zoom its cards");
      }
      for (ZoomShot shot : ZOOM_SHOTS) {
        selectTab(context, shot.id());
        double[] point = context.computeOnClient(client -> findCard(client, shot));
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitTicks(SETTLE_TICKS);
        expectZoom(context, false, "Escape must close the zoom and keep the cheat sheet open");
        context.getInput().setCursorPos(point[0] * point[2], point[1] * point[2]);
        context.waitTicks(SETTLE_TICKS);
        screenshot(context, "zoom-hover-" + shot.id() + "-" + view.name());
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTicks(SETTLE_TICKS);
        expectZoom(context, true, "a click on a card must zoom it");
        context.getInput().setCursorPos(0, 0);
        context.waitTicks(SETTLE_TICKS);
        screenshot(context, "zoom-" + shot.id() + "-" + view.name());
        if (view == VIEWS.getFirst()) {
          context.runOnClient(client -> measureZoom(client, shot.id(), point));
          context.getInput().setCursorPos(view.windowWidth() / 2.0, view.windowHeight() / 2.0);
          context.waitTicks(SETTLE_TICKS);
          screenshot(context, "zoom-tooltip-" + shot.id() + "-" + view.name());
          context.getInput().setCursorPos(0, 0);
        }
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTicks(SETTLE_TICKS);
        expectZoom(context, false, "a click outside the zoom must close it");
      }
    }
    selectTab(context, OresSheet.ID);
  }

  /**
   * Ores, discs, villagers and trims for each of {@link #CRISP_RUNS}, each normal and with a card
   * zoomed (a tab that does not zoom stays as it is), to compare crops of icons and pictures
   * against their native look. Also checks that closing the cheat sheet puts the player's GUI scale
   * back.
   */
  private static void crispShots(ClientGameTestContext context) {
    for (CrispRun run : CRISP_RUNS) {
      SheetScale scale = run.scale();
      show(context, run.view());
      context.runOnClient(
          client -> {
            ClientSettings settings = CheatSheetClient.settings();
            ClientPreferences current = settings.preferences();
            settings.update(
                new ClientPreferences(
                    current.pauseButton(), current.startTab(), current.lastOpenedTab(), scale));
          });
      context.setScreen(
          () ->
              new CheatSheetScreen(
                  null,
                  CheatSheetClient.settings(),
                  CheatSheetClient.keyMappings(),
                  CheatSheetTabs.ALL));
      context.waitTicks(SETTLE_TICKS);
      for (ZoomShot shot : run.shots()) {
        selectTab(context, shot.id());
        int sheetScale =
            context.computeOnClient(
                client ->
                    client.gui.screen() instanceof CheatSheetScreen screen
                        ? screen.sheetScale()
                        : -1);
        String suffix = run.view().name() + "-sheet-" + scale.id() + "-x" + sheetScale;
        context.getInput().setCursorPos(0, 0);
        context.waitTicks(SETTLE_TICKS);
        screenshot(context, "crisp-" + shot.name() + "-" + suffix);
        double[] point = context.computeOnClient(client -> tryFindCard(client, shot));
        if (point == null) {
          report("crisp: %s does not zoom", shot.id());
          continue;
        }
        context.waitTicks(SETTLE_TICKS);
        screenshot(context, "crisp-zoom-" + shot.name() + "-" + suffix);
        if (run.shots() == EVERY_TAB_SHOTS) {
          // The zoom is centred on the clicked card: the mouse there is over it, with a tooltip.
          context.getInput().setCursorPos(point[0] * point[2], point[1] * point[2]);
          context.waitTicks(SETTLE_TICKS);
          screenshot(context, "crisp-zoom-tooltip-" + shot.name() + "-" + suffix);
          double[] variant = VARIANT_HOVERS.get(shot.name());
          if (variant != null) {
            context
                .getInput()
                .setCursorPos(
                    variant[0] * run.view().windowWidth(), variant[1] * run.view().windowHeight());
            context.waitTicks(SETTLE_TICKS);
            screenshot(context, "crisp-zoom-variant-tooltip-" + shot.name() + "-" + suffix);
          }
          if (shot.name().equals(MEASURED_VARIANTS)) {
            context.getInput().setCursorPos(0, 0);
            context.runOnClient(client -> measureFrame(client, "zoomed " + MEASURED_VARIANTS));
          }
          context.getInput().setCursorPos(0, 0);
        }
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitTicks(SETTLE_TICKS);
        expectZoom(context, false, "Escape must close the zoom and keep the cheat sheet open");
      }
      if (run.shots() == EVERY_TAB_SHOTS) {
        int guiScale = context.computeOnClient(client -> client.getWindow().getGuiScale());
        context
            .getInput()
            .setCursorPos(
                run.view().windowWidth() / 2.0 + TAB_HOVER_X * guiScale, TAB_HOVER_Y * guiScale);
        context.waitTicks(SETTLE_TICKS);
        screenshot(context, "crisp-tab-bar-" + run.view().name());
        context.getInput().setCursorPos(0, 0);
      }
      context.setScreen(() -> null);
      int restored = context.computeOnClient(client -> client.getWindow().getGuiScale());
      if (restored != run.view().guiScale()) {
        throw new AssertionError(
            "closing the cheat sheet must put GUI scale " + run.view().guiScale() + " back");
      }
      report("crisp: GUI scale after closing at sheet %s: %d", scale.id(), restored);
    }
    context.runOnClient(
        client -> {
          ClientSettings settings = CheatSheetClient.settings();
          ClientPreferences current = settings.preferences();
          settings.update(
              new ClientPreferences(
                  current.pauseButton(),
                  current.startTab(),
                  current.lastOpenedTab(),
                  SheetScale.AUTO));
        });
  }

  /** Like {@link #findCard}, or null when no card of the shown tab zooms. */
  private static double @Nullable [] tryFindCard(Minecraft client, ZoomShot shot) {
    try {
      return findCard(client, shot);
    } catch (AssertionError noCard) {
      return null;
    }
  }

  /** The tooltips of gold's and Nether gold's striped bonus strips. */
  private static void bonusTooltips(ClientGameTestContext context, View view) {
    context
        .getInput()
        .setCursorPos(view.windowWidth() * GOLD_BONUS_X, view.windowHeight() * GOLD_BONUS_Y);
    context.waitTicks(SETTLE_TICKS);
    screenshot(context, "ores-bonus-tooltip-" + view.name());
    context
        .getInput()
        .setCursorPos(
            view.windowWidth() * NETHER_GOLD_BONUS_X, view.windowHeight() * NETHER_GOLD_BONUS_Y);
    context.waitTicks(SETTLE_TICKS);
    screenshot(context, "ores-nether-bonus-tooltip-" + view.name());
    context.getInput().setCursorPos(0, 0);
  }

  /**
   * Clicks (no real input) right then down from the shot's start until a card zooms; that point in
   * GUI pixels and the GUI scale it was clicked at (the zoom raises it). The zoom is left open.
   */
  private static double[] findCard(Minecraft client, ZoomShot shot) {
    if (!(client.gui.screen() instanceof CheatSheetScreen screen)) {
      throw new AssertionError("the cheat sheet must be open");
    }
    int width = client.getWindow().getGuiScaledWidth();
    int height = client.getWindow().getGuiScaledHeight();
    int guiScale = client.getWindow().getGuiScale();
    for (int y = (int) (height * shot.startY()); y < height; y += ZOOM_SCAN_STEP) {
      for (int x = (int) (width * shot.startX()); x < width; x += ZOOM_SCAN_STEP) {
        screen.mouseClicked(leftClick(x, y), false);
        if (screen.isZoomOpen()) {
          return new double[] {x, y, guiScale};
        }
      }
    }
    throw new AssertionError("no card to zoom on " + shot.id());
  }

  private static MouseButtonEvent leftClick(double x, double y) {
    return new MouseButtonEvent(x, y, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
  }

  private static void expectZoom(ClientGameTestContext context, boolean open, String message) {
    boolean ok =
        context.computeOnClient(
            client ->
                client.gui.screen() instanceof CheatSheetScreen screen
                    && screen.isZoomOpen() == open);
    if (!ok) {
      throw new AssertionError(message);
    }
  }

  /**
   * What a click on the card at {@code card} (GUI pixels) costs (Escape and click again: the zoom
   * is built anew) and one frame with it open.
   */
  private static void measureZoom(Minecraft client, String tab, double[] card) {
    if (!(client.gui.screen() instanceof CheatSheetScreen screen)) {
      throw new AssertionError("the cheat sheet must be open");
    }
    double x = card[0];
    double y = card[1];
    KeyEvent escape = new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0);
    long nanos = 0;
    for (int build = 0; build < ZOOM_BUILDS; build++) {
      screen.keyPressed(escape);
      long start = System.nanoTime();
      screen.mouseClicked(leftClick(x, y), false);
      nanos += System.nanoTime() - start;
    }
    report("zoom build of %s: %.3f ms per click", tab, nanos / NANOS_PER_MILLI / ZOOM_BUILDS);
    measureFrame(client, "zoomed " + tab);
  }

  /**
   * Whole frames, not just the sheet's extract: CPU time of the render thread per frame (world, GUI
   * extract and GUI render) with the frame cap lifted, to compare against no screen.
   */
  private static void measureRenderThread(ClientGameTestContext context, String label) {
    int cap = context.computeOnClient(client -> client.options.framerateLimit().get());
    context.runOnClient(client -> client.options.framerateLimit().set(UNCAPPED_FPS));
    long thread = context.computeOnClient(client -> Thread.currentThread().threadId());
    ThreadMXBean threads = ManagementFactory.getThreadMXBean();
    context.waitTicks(WARM_TICKS);
    long cpuBefore = threads.getThreadCpuTime(thread);
    long wallBefore = System.nanoTime();
    context.waitTicks(MEASURED_TICKS);
    long cpu = threads.getThreadCpuTime(thread) - cpuBefore;
    double seconds = (System.nanoTime() - wallBefore) / NANOS_PER_SECOND;
    int fps = context.computeOnClient(Minecraft::getFps);
    report(
        "render thread with %s: %d FPS, %.0f µs CPU per frame",
        label, fps, cpu / NANOS_PER_MICRO / (fps * seconds));
    context.runOnClient(client -> client.options.framerateLimit().set(cap));
  }

  /**
   * Ores, Beacon and the settings screen at GUI 640x360, 960x540 and 480x270 with the sheet scale
   * on auto (the default), named by the GUI size and the sheet scale auto chose.
   */
  private static void autoScaleShots(ClientGameTestContext context) {
    for (View view : List.of(VIEWS.get(1), VIEWS.getFirst(), VIEWS.get(2))) {
      show(context, view);
      // Out of the sheet after the resize, so no tooltip covers it.
      context.getInput().setCursorPos(0, 0);
      for (String tab : List.of(OresSheet.ID, BeaconSheet.ID)) {
        selectTab(context, tab);
        int scale =
            context.computeOnClient(
                client ->
                    client.gui.screen() instanceof CheatSheetScreen screen
                        ? screen.sheetScale()
                        : -1);
        screenshot(context, "auto-" + tab + "-" + view.name() + "-sheet-x" + scale);
      }
      // Same beacon tiles at pose scales 1, 1 and 1/2: what drawing at another scale costs.
      context.runOnClient(client -> measureFrame(client, BeaconSheet.ID));
    }
    // Hit tests in sheet pixels, tooltips at the GUI scale.
    View scaleFour = VIEWS.get(2);
    selectTab(context, OresSheet.ID);
    context
        .getInput()
        .setCursorPos(scaleFour.windowWidth() * AUTO_ORE_X, scaleFour.windowHeight() * AUTO_ORE_Y);
    context.waitTicks(SETTLE_TICKS);
    screenshot(context, "auto-ores-tooltip-" + scaleFour.name());
    View scaleThree = VIEWS.get(1);
    show(context, scaleThree);
    selectTab(context, BeaconSheet.ID);
    context
        .getInput()
        .setCursorPos(
            scaleThree.windowWidth() * AUTO_PYRAMID_X, scaleThree.windowHeight() * AUTO_PYRAMID_Y);
    context.waitTicks(SETTLE_TICKS);
    screenshot(context, "auto-beacon-tooltip-" + scaleThree.name());
    context.getInput().setCursorPos(0, 0);
    selectTab(context, OresSheet.ID);
    for (View view : List.of(VIEWS.get(1), VIEWS.getFirst(), VIEWS.get(2))) {
      show(context, view);
      context.setScreen(
          () ->
              new CheatSheetSettingsScreen(
                  null, CheatSheetClient.settings(), CheatSheetClient.keyMappings()));
      context.waitTicks(SETTLE_TICKS);
      screenshot(context, "auto-settings-" + view.name());
    }
    View scaleThreeSettings = VIEWS.get(1);
    show(context, scaleThreeSettings);
    context
        .getInput()
        .setCursorPos(
            scaleThreeSettings.windowWidth() * SHEET_SIZE_ROW_X,
            scaleThreeSettings.windowHeight() * SHEET_SIZE_ROW_Y);
    context.waitTicks(SETTLE_TICKS);
    screenshot(context, "auto-settings-sheet-size-" + scaleThreeSettings.name());
    context.getInput().setCursorPos(0, 0);
    context.setScreen(
        () ->
            new CheatSheetScreen(
                null,
                CheatSheetClient.settings(),
                CheatSheetClient.keyMappings(),
                CheatSheetTabs.ALL));
    context.waitTicks(SETTLE_TICKS);
    selectTab(context, OresSheet.ID);
  }

  /**
   * The sheet scale auto picks for Ores, Beacon and Farming at GUI 640x360 (the canvas of a 1080p
   * screen at GUI scale 3) in every language: longer translations wrap into more lines. Also, at
   * GUI 960x540 (a 1080p screen's auto sheet scale), whether each tab that must fit one screen
   * still does.
   */
  private static void autoScaleInEveryLanguage(ClientGameTestContext context) {
    View view = VIEWS.get(1);
    show(context, view);
    context.setScreen(
        () ->
            new CheatSheetScreen(
                null,
                CheatSheetClient.settings(),
                CheatSheetClient.keyMappings(),
                CheatSheetTabs.ALL));
    for (String language : LANGUAGES) {
      CompletableFuture<Void> reload =
          context.computeOnClient(
              client -> {
                client.getLanguageManager().setSelected(language);
                client.options.languageCode = language;
                return client.reloadResourcePacks();
              });
      context.waitFor(client -> reload.isDone() && client.gui.overlay() == null, RELOAD_TICKS);
      // A new screen builds its sheets in the new language.
      context.setScreen(
          () ->
              new CheatSheetScreen(
                  null,
                  CheatSheetClient.settings(),
                  CheatSheetClient.keyMappings(),
                  CheatSheetTabs.ALL));
      for (String tab : List.of(OresSheet.ID, BeaconSheet.ID, FarmingSheet.ID)) {
        selectTab(context, tab);
        report("%s in %s at %s: %s", tab, language, view.name(), layoutReport(context));
      }
      show(context, VIEWS.getFirst());
      for (String tab : ONE_SCREEN_TABS) {
        selectTab(context, tab);
        report("%s in %s at %s: %s", tab, language, VIEWS.getFirst().name(), layoutReport(context));
      }
      show(context, view);
    }
  }

  /**
   * At the player's setup, in every language: whether each tab fits one screen (reported), and
   * shots of the tab bar on Ores and Beacon, the Beacon and Enchantments tabs and the conduit's
   * zoom, to check that the bar keeps its size and the guides their room.
   */
  private static void fitAtPlayerSetup(ClientGameTestContext context) {
    show(context, PLAYER_SETUP);
    for (String language : LANGUAGES) {
      CompletableFuture<Void> reload =
          context.computeOnClient(
              client -> {
                client.getLanguageManager().setSelected(language);
                client.options.languageCode = language;
                return client.reloadResourcePacks();
              });
      context.waitFor(client -> reload.isDone() && client.gui.overlay() == null, RELOAD_TICKS);
      context.setScreen(
          () ->
              new CheatSheetScreen(
                  null,
                  CheatSheetClient.settings(),
                  CheatSheetClient.keyMappings(),
                  CheatSheetTabs.ALL));
      context.getInput().setCursorPos(0, 0);
      for (String tab : CheatSheetTabs.ids()) {
        selectTab(context, tab);
        report(
            "fit: %s in %s at %s: %s", tab, language, PLAYER_SETUP.name(), layoutReport(context));
      }
      for (String tab : List.of(OresSheet.ID, BeaconSheet.ID, EnchantmentsSheet.ID)) {
        selectTab(context, tab);
        screenshot(context, "fit-" + tab + "-" + language + "-" + PLAYER_SETUP.name());
      }
      selectTab(context, BeaconSheet.ID);
      ZoomShot conduit = new ZoomShot(BeaconSheet.ID, BEACON_CONDUIT, CONDUIT_X, CONDUIT_Y);
      if (context.computeOnClient(client -> tryFindCard(client, conduit)) != null) {
        context.waitTicks(SETTLE_TICKS);
        screenshot(context, "fit-zoom-" + BEACON_CONDUIT + "-" + language);
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitTicks(SETTLE_TICKS);
      }
    }
    context.setScreen(() -> null);
  }

  /** Height, sheet scale, canvas and fit of the tab shown on the open cheat sheet. */
  private static String layoutReport(ClientGameTestContext context) {
    return context.computeOnClient(
        client ->
            client.gui.screen() instanceof CheatSheetScreen screen
                ? String.format(
                    Locale.ROOT,
                    "%d, sheet scale %d (canvas %dx%d), %s",
                    screen.sheetHeight(),
                    screen.sheetScale(),
                    client.getWindow().getWidth() / screen.sheetScale(),
                    client.getWindow().getHeight() / screen.sheetScale(),
                    screen.fitsWithoutScrolling() ? "fits" : "scrolls")
                : "no cheat sheet");
  }

  /**
   * Laid-out height of every tab at every GUI size, to compare layouts, with the sheet scale auto
   * chose (window pixels per sheet pixel) and whether the tab fits without scrolling.
   */
  private static void measureSheetHeights(ClientGameTestContext context) {
    for (View view : VIEWS) {
      show(context, view);
      for (String tab : CheatSheetTabs.ids()) {
        selectTab(context, tab);
        report("sheet height of %s at %s: %s", tab, view.name(), layoutReport(context));
      }
    }
    selectTab(context, OresSheet.ID);
  }

  /** Shows the tab with this id on the open cheat sheet. */
  private static void selectTab(ClientGameTestContext context, String tabId) {
    int index = CheatSheetTabs.ids().indexOf(tabId);
    if (index < 0) {
      throw new AssertionError("no tab " + tabId);
    }
    context.runOnClient(
        client -> {
          if (!(client.gui.screen() instanceof CheatSheetScreen screen)) {
            throw new AssertionError("the cheat sheet must be open");
          }
          screen.selectTab(index);
        });
    context.waitTicks(SETTLE_TICKS);
  }

  private static void show(ClientGameTestContext context, View view) {
    context.getInput().resizeWindow(view.windowWidth(), view.windowHeight());
    context.runOnClient(
        client -> {
          client.options.guiScale().set(view.guiScale());
          client.resizeGui();
        });
    context.waitTicks(SETTLE_TICKS);
  }

  private static void screenshot(ClientGameTestContext context, String name) {
    Path path = context.takeScreenshot(name);
    System.out.println(REPORT_PREFIX + "screenshot " + path.toAbsolutePath());
  }

  /** First build (class loading included) and the average of warm builds of every tab. */
  private static void measureBuild(Minecraft client) {
    SheetContext sheetContext = new SheetContext(client, client.font);
    for (SheetSource source : CheatSheetTabs.ALL) {
      long start = System.nanoTime();
      source.build(sheetContext);
      long first = System.nanoTime() - start;
      start = System.nanoTime();
      for (int build = 0; build < WARM_BUILDS; build++) {
        source.build(sheetContext);
      }
      long warm = (System.nanoTime() - start) / WARM_BUILDS;
      report(
          "build %s: first %.2f ms, warm %.3f ms",
          source.id(), first / NANOS_PER_MILLI, warm / NANOS_PER_MILLI);
    }
  }

  /**
   * Extract cost of the open cheat sheet: time and render-thread allocation per frame. With
   * pictures, also measured with them released (same layout, no picture drawn): the difference is
   * what drawing the pictures costs.
   */
  private static void measureFrame(Minecraft client, String tab) {
    Screen screen = client.gui.screen();
    if (!(screen instanceof CheatSheetScreen cheatSheet)) {
      throw new AssertionError("the cheat sheet must be open, found " + screen);
    }
    double[] cost = extractCost(client, screen);
    report(
        "frame of %s at %dx%d GUI: %.1f µs, %.0f bytes allocated (vanilla render states included)",
        tab,
        client.getWindow().getGuiScaledWidth(),
        client.getWindow().getGuiScaledHeight(),
        cost[0],
        cost[1]);
    PictureAtlas pictures = cheatSheet.sheet(cheatSheet.selectedTab()).pictures();
    if (pictures != null) {
      report(
          "pictures of %s: last bake %.2f ms CPU, texture %d KB on the GPU",
          tab, pictures.bakeNanos() / NANOS_PER_MILLI, pictures.textureBytes() / BYTES_PER_KB);
      pictures.release();
      double[] without = extractCost(client, screen);
      report(
          "frame of %s without pictures: %.1f µs, %.0f bytes: pictures cost %.1f µs, %.0f bytes",
          tab, without[0], without[1], cost[0] - without[0], cost[1] - without[1]);
    }
  }

  /** Average extract time (µs) and bytes allocated per frame, after warming up. */
  private static double[] extractCost(Minecraft client, Screen screen) {
    int mouseX = 0;
    int mouseY = 0;
    for (int frame = 0; frame < WARM_FRAMES; frame++) {
      extract(client, screen, mouseX, mouseY);
    }
    com.sun.management.ThreadMXBean threads =
        (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    long thread = Thread.currentThread().threadId();
    long nanos = 0;
    long bytesBefore = threads.getThreadAllocatedBytes(thread);
    long statesBytes = 0;
    for (int frame = 0; frame < MEASURED_FRAMES; frame++) {
      long beforeState = threads.getThreadAllocatedBytes(thread);
      GuiGraphicsExtractor graphics =
          new GuiGraphicsExtractor(client, new GuiRenderState(), mouseX, mouseY);
      statesBytes += threads.getThreadAllocatedBytes(thread) - beforeState;
      long start = System.nanoTime();
      screen.extractRenderState(graphics, mouseX, mouseY, 0.0F);
      nanos += System.nanoTime() - start;
    }
    long bytes = threads.getThreadAllocatedBytes(thread) - bytesBefore - statesBytes;
    return new double[] {
      nanos / NANOS_PER_MICRO / MEASURED_FRAMES, (double) bytes / MEASURED_FRAMES
    };
  }

  private static void extract(Minecraft client, Screen screen, int mouseX, int mouseY) {
    screen.extractRenderState(
        new GuiGraphicsExtractor(client, new GuiRenderState(), mouseX, mouseY),
        mouseX,
        mouseY,
        0.0F);
  }

  private static void report(String format, Object... args) {
    System.out.println(REPORT_PREFIX + String.format(Locale.ROOT, format, args));
  }
}
