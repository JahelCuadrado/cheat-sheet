package com.einsteinmods.cheatsheet.core.trims;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class PatternNamesTest {
  @Test
  void dropsTheWordsEveryNameEndsWith() {
    assertEquals(
        List.of("Sentry", "Dune", "Wayfinder"),
        PatternNames.shorten(
            List.of("Sentry Armor Trim", "Dune Armor Trim", "Wayfinder Armor Trim"), Locale.US));
  }

  @Test
  void dropsTheWordsEveryNameStartsWithAndCapitalises() {
    assertEquals(
        List.of("Centinela", "Duna", "Silencio"),
        PatternNames.shorten(
            List.of(
                "Diseño de armadura de centinela",
                "Diseño de armadura de duna",
                "Diseño de armadura de silencio"),
            Locale.forLanguageTag("es-ES")));
  }

  @Test
  void cutsAtHyphens() {
    assertEquals(
        List.of("Wächter", "Dünen"),
        PatternNames.shorten(
            List.of("Wächter-Rüstungsbesatz", "Dünen-Rüstungsbesatz"), Locale.GERMANY));
  }

  @Test
  void neverCutsInsideAWord() {
    // Both end in "t Trim": only " Trim" goes.
    assertEquals(
        List.of("Bolt", "Host"),
        PatternNames.shorten(List.of("Bolt Trim", "Host Trim"), Locale.US));
    // No separator (Japanese): the shared end stays.
    List<String> japanese = List.of("歩哨の装飾", "砂丘の装飾");
    assertEquals(japanese, PatternNames.shorten(japanese, Locale.JAPAN));
  }

  @Test
  void keepsNamesThatWouldBecomeEmpty() {
    List<String> names = List.of("Sentry Trim", "Sentry Trim");
    assertEquals(names, PatternNames.shorten(names, Locale.US));
    assertEquals(List.of("Alone Trim"), PatternNames.shorten(List.of("Alone Trim"), Locale.US));
  }
}
