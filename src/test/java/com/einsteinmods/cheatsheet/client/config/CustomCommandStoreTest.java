package com.einsteinmods.cheatsheet.client.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.einsteinmods.cheatsheet.core.commands.CommandText;
import com.einsteinmods.cheatsheet.core.commands.CustomCommand;
import com.einsteinmods.cheatsheet.core.commands.CustomCommands;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CustomCommandStoreTest {
  private static final CustomCommand HOME = new CustomCommand("Home", "tp @s 0 64 0");

  @Test
  void missingFileIsEmptyAndNotCreated(@TempDir Path directory) {
    Path path = directory.resolve(CustomCommandsFile.FILE_NAME);
    CustomCommandStore store = new CustomCommandStore(path);
    assertEquals(CustomCommands.EMPTY, store.commands());
    assertFalse(Files.exists(path), "nothing is written until a command is saved");
  }

  @Test
  void savedCommandsAreReadBack(@TempDir Path directory) {
    Path path = directory.resolve(CustomCommandsFile.FILE_NAME);
    new CustomCommandStore(path).update(CustomCommands.EMPTY.with(HOME));
    assertTrue(Files.exists(path));
    assertEquals(List.of(HOME), new CustomCommandStore(path).commands().entries());
  }

  @Test
  void handEditedEntriesAreCleanedOrSkipped(@TempDir Path directory) throws IOException {
    Path path = directory.resolve(CustomCommandsFile.FILE_NAME);
    String tooLong = "x".repeat(CommandText.MAX_COMMAND_LENGTH + 1);
    Files.writeString(
        path,
        """
        {"schemaVersion": 1, "commands": [
          {"name": " Home ", "command": "/tp @s 0 64 0"},
          {"name": "No command"},
          null,
          {"name": "Too long", "command": "%s"},
          {"name": "", "command": "time set day"}
        ]}
        """
            .formatted(tooLong),
        StandardCharsets.UTF_8);
    assertEquals(List.of(HOME), new CustomCommandStore(path).commands().entries());
  }

  @Test
  void severalLinesAreReadBackInOrder(@TempDir Path directory) {
    Path path = directory.resolve(CustomCommandsFile.FILE_NAME);
    CustomCommand combo = new CustomCommand("Combo", List.of("time set day", "weather clear"));
    new CustomCommandStore(path).update(CustomCommands.EMPTY.with(combo).with(HOME));
    assertEquals(List.of(combo, HOME), new CustomCommandStore(path).commands().entries());
  }

  @Test
  void versionOneFilesBecomeOneLineEntries(@TempDir Path directory) throws IOException {
    Path path = directory.resolve(CustomCommandsFile.FILE_NAME);
    Files.writeString(
        path,
        """
        {"schemaVersion": 1, "commands": [
          {"name": "Home", "command": "/tp @s 0 64 0"},
          {"name": "Broken", "command": "say a\nb"}
        ]}
        """,
        StandardCharsets.UTF_8);
    CustomCommandStore store = new CustomCommandStore(path);
    assertEquals(List.of(HOME, new CustomCommand("Broken", "say a b")), store.commands().entries());
    store.update(store.commands().without(1));
    String rewritten = Files.readString(path, StandardCharsets.UTF_8);
    assertTrue(rewritten.contains("\"schemaVersion\": 2"), rewritten);
    assertFalse(rewritten.contains("\"command\":"), rewritten);
    assertEquals(List.of(HOME), new CustomCommandStore(path).commands().entries());
  }

  @Test
  void versionTwoEntriesSkipNullLinesAndRejectTooMany(@TempDir Path directory) throws IOException {
    Path path = directory.resolve(CustomCommandsFile.FILE_NAME);
    String tooMany = "\"time set day\",".repeat(CommandText.MAX_LINES) + "\"time set day\"";
    Files.writeString(
        path,
        """
        {"schemaVersion": 2, "commands": [
          {"name": "Combo", "commands": ["/time set day", null, " ", "weather clear"]},
          {"name": "Too many", "commands": [%s]},
          {"name": "Empty", "commands": []}
        ]}
        """
            .formatted(tooMany),
        StandardCharsets.UTF_8);
    assertEquals(
        List.of(new CustomCommand("Combo", List.of("time set day", "weather clear"))),
        new CustomCommandStore(path).commands().entries());
  }

  @Test
  void brokenFileGivesAnEmptyListAndIsKept(@TempDir Path directory) throws IOException {
    Path path = directory.resolve(CustomCommandsFile.FILE_NAME);
    String broken = "{not json";
    Files.writeString(path, broken, StandardCharsets.UTF_8);
    assertEquals(CustomCommands.EMPTY, new CustomCommandStore(path).commands());
    assertEquals(broken, Files.readString(path, StandardCharsets.UTF_8), "left for a hand fix");
  }
}
