package com.einsteinmods.cheatsheet.client.config;

import com.einsteinmods.cheatsheet.config.JsonConfigFile;
import com.einsteinmods.cheatsheet.core.commands.CustomCommands;
import java.nio.file.Path;
import org.jspecify.annotations.Nullable;

/**
 * The player's saved commands: read the first time they are needed (the Commands tab is built),
 * written on every change. A missing file is an empty list and is not created until the player
 * saves a command; an unreadable one is left as it is until then. Client thread only.
 */
public final class CustomCommandStore {
  private final JsonConfigFile<CustomCommandsFile> file;
  private @Nullable CustomCommands commands;

  public CustomCommandStore(Path path) {
    this.file = new JsonConfigFile<>(path, CustomCommandsFile.class, CustomCommandsFile.EMPTY);
  }

  /** The saved commands, read from the file on first use. */
  public CustomCommands commands() {
    CustomCommands loaded = commands;
    if (loaded == null) {
      loaded = file.read().map(CustomCommandsFile::toCommands).orElse(CustomCommands.EMPTY);
      commands = loaded;
    }
    return loaded;
  }

  /** Keeps {@code changed} and writes the file if it differs from what is kept. */
  public void update(CustomCommands changed) {
    if (!changed.equals(commands())) {
      commands = changed;
      file.save(CustomCommandsFile.from(changed));
    }
  }
}
