package com.einsteinmods.cheatsheet.core.commands;

import java.util.ArrayList;
import java.util.List;

/**
 * The player's saved commands, in the order they were added; never more than {@link
 * CommandText#MAX_CUSTOM_COMMANDS}. Immutable: every change returns a new list.
 */
public record CustomCommands(List<CustomCommand> entries) {
  public static final CustomCommands EMPTY = new CustomCommands(List.of());

  public CustomCommands {
    if (entries.size() > CommandText.MAX_CUSTOM_COMMANDS) {
      entries = entries.subList(0, CommandText.MAX_CUSTOM_COMMANDS);
    }
    entries = List.copyOf(entries);
  }

  /** Whether no more can be added. */
  public boolean isFull() {
    return entries.size() >= CommandText.MAX_CUSTOM_COMMANDS;
  }

  /** Whether an entry already sends exactly {@code lines}, whatever its name. */
  public boolean containsLines(List<String> lines) {
    for (CustomCommand entry : entries) {
      if (entry.lines().equals(lines)) {
        return true;
      }
    }
    return false;
  }

  /** With {@code command} added last; unchanged when full. */
  public CustomCommands with(CustomCommand command) {
    if (isFull()) {
      return this;
    }
    List<CustomCommand> changed = new ArrayList<>(entries);
    changed.add(command);
    return new CustomCommands(changed);
  }

  /** With entry {@code index} replaced; unchanged when there is no such entry. */
  public CustomCommands replaced(int index, CustomCommand command) {
    if (index < 0 || index >= entries.size()) {
      return this;
    }
    List<CustomCommand> changed = new ArrayList<>(entries);
    changed.set(index, command);
    return new CustomCommands(changed);
  }

  /** Without entry {@code index}; unchanged when there is no such entry. */
  public CustomCommands without(int index) {
    if (index < 0 || index >= entries.size()) {
      return this;
    }
    List<CustomCommand> changed = new ArrayList<>(entries);
    changed.remove(index);
    return new CustomCommands(changed);
  }
}
