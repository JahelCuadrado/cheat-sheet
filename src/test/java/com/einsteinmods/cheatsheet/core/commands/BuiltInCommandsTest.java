package com.einsteinmods.cheatsheet.core.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class BuiltInCommandsTest {
  private static final Pattern KEY = Pattern.compile("[a-z][a-z_]*");
  private static final Pattern RELATIVE = Pattern.compile("~(-?\\d*)");
  private static final int MIN_SINGLE_COMMANDS = 40;
  private static final int MAX_SINGLE_COMMANDS = 70;
  private static final int MIN_COMBOS = 15;
  private static final int MAX_COMBOS = 25;

  @Test
  void keysAreUniqueLangKeySafeIds() {
    Set<String> keys = new HashSet<>();
    for (BuiltInCommand command : BuiltInCommands.ALL) {
      assertTrue(KEY.matcher(command.key()).matches(), command.key());
      assertTrue(keys.add(command.key()), "duplicate key " + command.key());
    }
  }

  @Test
  void everyLineFitsTheChatBoxAndIsClean() {
    for (BuiltInCommand command : BuiltInCommands.ALL) {
      assertFalse(command.lines().isEmpty(), command.key());
      assertTrue(command.lines().size() <= CommandText.MAX_LINES, command.key());
      for (String line : command.lines()) {
        assertTrue(line.length() <= CommandText.MAX_COMMAND_LENGTH, command.key());
        assertEquals(
            line, CommandText.sanitizeCommand(line).orElseThrow(), command.key() + " is not clean");
        assertFalse(line.startsWith("/"), command.key());
      }
    }
  }

  @Test
  void everyGroupHasEntriesInListOrder() {
    int total = 0;
    CommandGroup previous = CommandGroup.values()[0];
    for (BuiltInCommand command : BuiltInCommands.ALL) {
      assertTrue(command.group().ordinal() >= previous.ordinal(), "groups out of order");
      previous = command.group();
    }
    for (CommandGroup group : CommandGroup.values()) {
      List<BuiltInCommand> commands = BuiltInCommands.of(group);
      assertFalse(commands.isEmpty(), group.id());
      total += commands.size();
    }
    assertEquals(BuiltInCommands.ALL.size(), total);
  }

  @Test
  void singleGroupsHoldSingleCommandsAndComboGroupsHoldCombos() {
    int singles = 0;
    int combos = 0;
    for (BuiltInCommand command : BuiltInCommands.ALL) {
      assertEquals(command.group().combos(), command.isCombo(), command.key());
      if (command.isCombo()) {
        combos++;
        assertTrue(command.lines().size() >= 2, command.key());
      } else {
        singles++;
        assertEquals(1, command.lines().size(), command.key());
      }
    }
    assertTrue(
        singles >= MIN_SINGLE_COMMANDS && singles <= MAX_SINGLE_COMMANDS, "singles: " + singles);
    assertTrue(combos >= MIN_COMBOS && combos <= MAX_COMBOS, "combos: " + combos);
  }

  @Test
  void entriesWithADestroyingLineAreMarkedDangerous() {
    for (BuiltInCommand command : BuiltInCommands.ALL) {
      if (CommandDanger.anyDestroys(command.lines())) {
        assertTrue(command.dangerous(), command.key() + " must ask before running");
      }
    }
  }

  @Test
  void comboLineCounts() {
    assertEquals(16, lines("full_kit"));
    assertEquals(4, lines("armour_kit"));
    assertEquals(2, lines("kill_dragon"));
    assertEquals(11, lines("default_rules"));
    assertTrue(byKey("kill_dragon").dangerous());
    assertTrue(byKey("flat_area").dangerous());
    assertTrue(byKey("reset_player").dangerous());
    assertFalse(byKey("full_kit").dangerous());
  }

  /**
   * Beyond what {@link CommandDanger} reads from the text: setblock replaces the block under you
   * (chest and contents included) and peaceful discards every hostile mob ({@code
   * Mob#checkDespawn}).
   */
  @Test
  void commandsThatRemoveThingsWithoutADestroyingNameAskToo() {
    assertTrue(byKey("setblock").dangerous());
    assertTrue(byKey("difficulty").dangerous());
    assertTrue(byKey("gold_under_players").dangerous());
    assertTrue(byKey("lightning").dangerous());
  }

  /** The sizes the sheet states: a change of a coordinate must also change its description. */
  @Test
  void statedBoxesMatchTheCoordinates() {
    List<Integer> clearing = List.of(-15, 0, -15, 15, 20, 15);
    assertEquals(clearing, offsets("flatten", 0));
    assertEquals(clearing, offsets("flat_area", 0));
    assertEquals(List.of(-15, -1, -15, 15, -1, 15), offsets("flat_area", 1));
    List<Integer> drained = List.of(-15, -10, -15, 15, 10, 15);
    assertEquals(drained, offsets("remove_water", 0));
    assertEquals(drained, offsets("remove_lava", 0));
    assertEquals(List.of(1, 0, 1, 10, 4, 10), offsets("fill", 0));
  }

  @Test
  void cloneCopiesFourteenBlocksEast() {
    List<Integer> clone = offsets("clone", 0);
    assertEquals(14, clone.get(6) - clone.get(0));
    assertEquals(clone.get(1), clone.get(7));
    assertEquals(clone.get(2), clone.get(8));
  }

  /** The relative coordinates of a command line, in order. */
  private static List<Integer> offsets(String key, int line) {
    List<Integer> offsets = new ArrayList<>();
    Matcher matcher = RELATIVE.matcher(byKey(key).lines().get(line));
    while (matcher.find()) {
      offsets.add(matcher.group(1).isEmpty() ? 0 : Integer.parseInt(matcher.group(1)));
    }
    return offsets;
  }

  private static int lines(String key) {
    return byKey(key).lines().size();
  }

  private static BuiltInCommand byKey(String key) {
    for (BuiltInCommand command : BuiltInCommands.ALL) {
      if (command.key().equals(key)) {
        return command;
      }
    }
    throw new AssertionError("no built-in " + key);
  }
}
