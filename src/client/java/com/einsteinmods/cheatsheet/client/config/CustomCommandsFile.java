package com.einsteinmods.cheatsheet.client.config;

import com.einsteinmods.cheatsheet.core.commands.CommandText;
import com.einsteinmods.cheatsheet.core.commands.CustomCommand;
import com.einsteinmods.cheatsheet.core.commands.CustomCommands;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * On-disk shape of {@code config/cheatsheet-commands.json}: the player's saved commands. Fields are
 * nullable so a missing or hand-broken value is skipped instead of failing the whole file.
 *
 * <p>Bump {@link #CURRENT_SCHEMA_VERSION} when a field is renamed or changes meaning, and migrate
 * older files in {@link #toCommands()}. Schema history: 1 is the first version (one {@code command}
 * per entry); 2 keeps several, in order, in {@code commands} (a version 1 {@code command} is read
 * as a one-line list and written back in the new shape on the next save).
 *
 * @param schemaVersion version of this shape
 * @param commands the saved entries in the order they were added
 */
public record CustomCommandsFile(int schemaVersion, @Nullable List<@Nullable Entry> commands) {
  public static final int CURRENT_SCHEMA_VERSION = 2;
  public static final String FILE_NAME = "cheatsheet-commands.json";

  public static final CustomCommandsFile EMPTY = from(CustomCommands.EMPTY);

  private static final String LINE_BREAK = "\n";

  /**
   * One saved entry as written: its name and its commands without the slash.
   *
   * @param command version 1 only: the single command; never written now
   * @param commands version 2: the commands in the order they run
   */
  public record Entry(
      @Nullable String name, @Nullable String command, @Nullable List<@Nullable String> commands) {}

  public static CustomCommandsFile from(CustomCommands commands) {
    List<@Nullable Entry> entries = new ArrayList<>(commands.entries().size());
    for (CustomCommand command : commands.entries()) {
      entries.add(new Entry(command.name(), null, command.lines()));
    }
    return new CustomCommandsFile(CURRENT_SCHEMA_VERSION, entries);
  }

  /**
   * The commands, each cleaned like typed ones ({@code CommandText}); entries left unusable are
   * skipped, and only the first {@code MAX_CUSTOM_COMMANDS} are kept.
   */
  public CustomCommands toCommands() {
    List<@Nullable Entry> written = commands;
    if (written == null) {
      return CustomCommands.EMPTY;
    }
    List<CustomCommand> kept = new ArrayList<>(written.size());
    for (Entry entry : written) {
      if (entry != null && entry.name() != null) {
        String name = entry.name();
        lines(entry).flatMap(lines -> CustomCommand.of(name, lines)).ifPresent(kept::add);
      }
    }
    return new CustomCommands(kept);
  }

  /**
   * The entry's commands as one text, a command per line: the version 2 list, or else the version 1
   * single command (cleaned as one line, as it was typed then); empty when there is neither.
   */
  private static Optional<String> lines(Entry entry) {
    List<@Nullable String> list = entry.commands();
    if (list != null) {
      List<String> present = new ArrayList<>(list.size());
      for (String line : list) {
        if (line != null) {
          present.add(line);
        }
      }
      return Optional.of(String.join(LINE_BREAK, present));
    }
    String single = entry.command();
    return single == null ? Optional.empty() : CommandText.sanitizeCommand(single);
  }
}
