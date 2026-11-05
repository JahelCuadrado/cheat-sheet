package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.sheet.SheetSource;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

/** Every tab, in display order. Adding a tab: one line here, one source class, its lang keys. */
public final class CheatSheetTabs {
  public static final List<SheetSource> ALL = tabs();

  private CheatSheetTabs() {}

  private static List<SheetSource> tabs() {
    List<SheetSource> tabs = new ArrayList<>();
    // One self-contained line per tab, so tabs added in parallel branches merge by keeping both.
    tabs.add(new OresSheet());
    tabs.add(new EnchantmentsSheet());
    tabs.add(new PotionsSheet());
    tabs.add(new TrimsSheet());
    tabs.add(new DiscsSheet());
    tabs.add(new DyesSheet());
    tabs.add(new VillagersSheet());
    tabs.add(new FoodSheet());
    tabs.add(new BeaconSheet());
    tabs.add(new FarmingSheet());
    tabs.add(new RedstoneSheet());
    tabs.add(new CommandsSheet());
    return List.copyOf(tabs);
  }

  /** The tabs shown to the player now ({@link SheetSource#availableFor}), in display order. */
  public static List<SheetSource> availableFor(Minecraft minecraft) {
    List<SheetSource> shown = new ArrayList<>(ALL.size());
    for (SheetSource source : ALL) {
      if (source.availableFor(minecraft)) {
        shown.add(source);
      }
    }
    return List.copyOf(shown);
  }

  public static List<String> ids() {
    List<String> ids = new ArrayList<>(ALL.size());
    for (SheetSource source : ALL) {
      ids.add(source.id());
    }
    return List.copyOf(ids);
  }
}
