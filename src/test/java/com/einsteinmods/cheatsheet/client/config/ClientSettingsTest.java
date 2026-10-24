package com.einsteinmods.cheatsheet.client.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.einsteinmods.cheatsheet.config.JsonConfigFile;
import com.einsteinmods.cheatsheet.core.SheetScale;
import com.einsteinmods.cheatsheet.core.StartTab;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClientSettingsTest {
  private static final List<String> TABS = List.of("ores", "potions");
  private static final FileTime LONG_AGO = FileTime.fromMillis(0L);

  private static JsonConfigFile<ClientSettingsFile> file(Path directory) {
    return new JsonConfigFile<>(
        directory.resolve(ClientSettingsFile.FILE_NAME),
        ClientSettingsFile.class,
        ClientSettingsFile.DEFAULTS);
  }

  private static ClientSettings loadFrom(Path directory, String json) throws IOException {
    Files.writeString(
        directory.resolve(ClientSettingsFile.FILE_NAME), json, StandardCharsets.UTF_8);
    return new ClientSettings(file(directory), TABS);
  }

  @Test
  void missingFileGivesDefaultsAndIsWritten(@TempDir Path directory) {
    ClientSettings settings = new ClientSettings(file(directory), TABS);
    assertEquals(ClientPreferences.DEFAULTS, settings.preferences());
    assertEquals(0, settings.startTabIndex());
    assertEquals(
        ClientSettingsFile.DEFAULTS, file(directory).load(), "defaults written on first start");
  }

  @Test
  void missingFieldsTakeTheirDefault(@TempDir Path directory) throws IOException {
    ClientSettings settings = loadFrom(directory, "{\"schemaVersion\": 1, \"pauseButton\": false}");
    assertFalse(settings.preferences().pauseButton());
    assertEquals(StartTab.LAST_OPENED, settings.preferences().startTab());
  }

  @Test
  void unknownStartTabFallsBackToLastOpened(@TempDir Path directory) throws IOException {
    ClientSettings settings =
        loadFrom(
            directory,
            "{\"schemaVersion\": 1, \"startTab\": \"removed\", \"lastOpenedTab\": \"potions\"}");
    assertEquals(StartTab.LAST_OPENED, settings.preferences().startTab());
    assertEquals(1, settings.startTabIndex());
  }

  @Test
  void lastOpenedTabIsWrittenOnlyWhenItChanges(@TempDir Path directory) throws IOException {
    ClientSettings settings = new ClientSettings(file(directory), TABS);
    Path path = directory.resolve(ClientSettingsFile.FILE_NAME);
    settings.rememberLastOpenedTab("potions");
    assertEquals("potions", file(directory).load().lastOpenedTab());
    Files.setLastModifiedTime(path, LONG_AGO);
    settings.rememberLastOpenedTab("potions");
    assertEquals(LONG_AGO, Files.getLastModifiedTime(path), "unchanged tab: no write");
    assertEquals(1, settings.startTabIndex());
  }

  @Test
  void versionOneFileGetsAutoSheetScaleAndIsKept(@TempDir Path directory) throws IOException {
    ClientSettings settings =
        loadFrom(directory, "{\"schemaVersion\": 1, \"startTab\": \"potions\"}");
    assertEquals(SheetScale.AUTO, settings.preferences().sheetScale());
    assertEquals("potions", settings.preferences().startTab());
  }

  @Test
  void sheetScaleIsReadBackAndUnknownValuesAreAuto(@TempDir Path directory) throws IOException {
    ClientSettings settings = new ClientSettings(file(directory), TABS);
    settings.update(new ClientPreferences(true, StartTab.LAST_OPENED, "", SheetScale.THREE));
    assertEquals("3", file(directory).load().sheetScale());
    assertEquals(ClientSettingsFile.CURRENT_SCHEMA_VERSION, file(directory).load().schemaVersion());
    assertEquals(
        SheetScale.THREE, new ClientSettings(file(directory), TABS).preferences().sheetScale());
    ClientSettings unknown =
        loadFrom(directory, "{\"schemaVersion\": 2, \"sheetScale\": \"huge\"}");
    assertEquals(SheetScale.AUTO, unknown.preferences().sheetScale());
  }
}
