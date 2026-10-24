package com.einsteinmods.cheatsheet.client.config;

import com.einsteinmods.cheatsheet.core.SheetScale;
import org.jspecify.annotations.Nullable;

/**
 * On-disk shape of {@code config/cheatsheet.json}. Fields are nullable so a missing or unknown
 * value (Gson reads both as null) gets its default instead of {@code false}.
 *
 * <p>Bump {@link #CURRENT_SCHEMA_VERSION} when a field is renamed or changes meaning, and migrate
 * older files in {@link #toPreferences()}. Tab ids are checked against the tab list by {@code
 * ClientSettings}, which knows it.
 *
 * <p>Schema history: 2 added {@code sheetScale} ({@code "auto"}, {@code "1"} to {@code "4"});
 * version 1 files have none and get auto, as any unknown value does.
 */
public record ClientSettingsFile(
    int schemaVersion,
    @Nullable Boolean pauseButton,
    @Nullable String startTab,
    @Nullable String lastOpenedTab,
    @Nullable String sheetScale) {
  public static final int CURRENT_SCHEMA_VERSION = 2;
  public static final String FILE_NAME = "cheatsheet.json";

  public static final ClientSettingsFile DEFAULTS = from(ClientPreferences.DEFAULTS);

  public static ClientSettingsFile from(ClientPreferences preferences) {
    return new ClientSettingsFile(
        CURRENT_SCHEMA_VERSION,
        preferences.pauseButton(),
        preferences.startTab(),
        preferences.lastOpenedTab(),
        preferences.sheetScale().id());
  }

  public ClientPreferences toPreferences() {
    ClientPreferences defaults = ClientPreferences.DEFAULTS;
    return new ClientPreferences(
        orDefault(pauseButton, defaults.pauseButton()),
        orDefault(startTab, defaults.startTab()),
        orDefault(lastOpenedTab, defaults.lastOpenedTab()),
        sheetScale == null ? defaults.sheetScale() : SheetScale.fromId(sheetScale));
  }

  private static <T> T orDefault(@Nullable T value, T defaultValue) {
    return value == null ? defaultValue : value;
  }
}
