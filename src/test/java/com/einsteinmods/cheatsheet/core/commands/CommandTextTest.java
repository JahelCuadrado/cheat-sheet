package com.einsteinmods.cheatsheet.core.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CommandTextTest {
  @Test
  void trimsAndStripsOneLeadingSlash() {
    assertEquals(Optional.of("time set day"), CommandText.sanitizeCommand("  /time set day  "));
    assertEquals(Optional.of("/say hi"), CommandText.sanitizeCommand("//say hi"));
    assertEquals(Optional.of("say hi"), CommandText.sanitizeCommand("/ say hi"));
  }

  @Test
  void keepsOneLineAndDropsDisallowedCharacters() {
    assertEquals(Optional.of("say a b"), CommandText.sanitizeCommand("say a\nb"));
    assertEquals(Optional.of("say ab"), CommandText.sanitizeCommand("say a\u0000\u007f§b"));
    assertEquals(Optional.of("say ab"), CommandText.sanitizeCommand("say a\u0085b"));
    assertEquals(Optional.of("say ñ 日本"), CommandText.sanitizeCommand("say ñ 日本"));
  }

  @Test
  void rejectsEmptyAndTooLong() {
    assertTrue(CommandText.sanitizeCommand("   ").isEmpty());
    assertTrue(CommandText.sanitizeCommand("/").isEmpty());
    String longest = "say " + "x".repeat(CommandText.MAX_COMMAND_LENGTH - 4);
    assertEquals(Optional.of(longest), CommandText.sanitizeCommand(longest));
    assertTrue(CommandText.sanitizeCommand(longest + "x").isEmpty());
    assertTrue(CommandText.sanitizeCommand("/" + longest).isPresent(), "the slash does not count");
  }

  @Test
  void namesKeepTheirSlashAndHaveTheirOwnLimit() {
    assertEquals(Optional.of("/home"), CommandText.sanitizeName(" /home "));
    assertTrue(CommandText.sanitizeName("\t\n").isEmpty());
    assertTrue(CommandText.sanitizeName("n".repeat(CommandText.MAX_NAME_LENGTH)).isPresent());
    assertTrue(CommandText.sanitizeName("n".repeat(CommandText.MAX_NAME_LENGTH + 1)).isEmpty());
  }

  @Test
  void linesAreCleanedOneByOneAndBlankOnesSkipped() {
    assertEquals(
        Optional.of(List.of("time set day", "weather clear")),
        CommandText.sanitizeLines(" /time set day \r\n\n  / \nweather clear\n"));
    assertEquals(Optional.of(List.of("say a b")), CommandText.sanitizeLines("say a\tb"));
    assertTrue(CommandText.sanitizeLines(" \n / \n").isEmpty());
  }

  @Test
  void linesRejectTooManyOrTooLongInsteadOfCutting() {
    String line = "time set day\n";
    assertEquals(
        CommandText.MAX_LINES,
        CommandText.sanitizeLines(line.repeat(CommandText.MAX_LINES)).orElseThrow().size());
    assertTrue(CommandText.sanitizeLines(line.repeat(CommandText.MAX_LINES + 1)).isEmpty());
    String tooLong = "say " + "x".repeat(CommandText.MAX_COMMAND_LENGTH);
    assertTrue(CommandText.sanitizeLines("time set day\n" + tooLong).isEmpty());
  }
}
