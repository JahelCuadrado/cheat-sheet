package com.einsteinmods.cheatsheet.core.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CustomCommandsTest {
  private static final CustomCommand HOME = new CustomCommand("Home", "tp @s 0 64 0");
  private static final CustomCommand DAY = new CustomCommand("Day", "time set day");

  @Test
  void ofCleansBothTexts() {
    assertEquals(HOME, CustomCommand.of("  Home ", "/tp @s 0 64 0\n").orElseThrow());
    assertTrue(CustomCommand.of("", "time set day").isEmpty());
    assertTrue(CustomCommand.of("Day", " / ").isEmpty());
    assertThrows(IllegalArgumentException.class, () -> new CustomCommand("Day", "/time set day"));
  }

  @Test
  void keepsSeveralLinesInOrder() {
    CustomCommand combo = CustomCommand.of("Combo", "/time set day\n\nweather clear").orElseThrow();
    assertEquals(List.of("time set day", "weather clear"), combo.lines());
    assertTrue(combo.isCombo());
    assertFalse(HOME.isCombo());
    assertThrows(
        IllegalArgumentException.class, () -> new CustomCommand("Combo", List.of("a", "")));
  }

  @Test
  void findsAnEntryByItsLines() {
    CustomCommands saved = CustomCommands.EMPTY.with(HOME);
    assertTrue(saved.containsLines(List.of("tp @s 0 64 0")));
    assertFalse(saved.containsLines(List.of("tp @s 0 64 0", "time set day")));
  }

  @Test
  void addReplaceAndRemoveReturnNewLists() {
    CustomCommands one = CustomCommands.EMPTY.with(HOME);
    CustomCommands two = one.with(DAY);
    assertEquals(List.of(HOME), one.entries());
    assertEquals(List.of(HOME, DAY), two.entries());
    assertEquals(List.of(DAY, DAY), two.replaced(0, DAY).entries());
    assertEquals(List.of(DAY), two.without(0).entries());
    assertSame(two, two.without(2));
    assertSame(two, two.replaced(-1, DAY));
  }

  @Test
  void neverHoldsMoreThanTheLimit() {
    List<CustomCommand> many = new ArrayList<>();
    for (int index = 0; index <= CommandText.MAX_CUSTOM_COMMANDS; index++) {
      many.add(new CustomCommand("Day " + index, "time set day"));
    }
    CustomCommands capped = new CustomCommands(many);
    assertEquals(CommandText.MAX_CUSTOM_COMMANDS, capped.entries().size());
    assertTrue(capped.isFull());
    assertSame(capped, capped.with(HOME));
    assertFalse(capped.without(0).isFull());
  }
}
