package com.einsteinmods.cheatsheet.core.trims;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TrimTableTest {
  @Test
  void everyTemplateAndPatternIsListedOnce() {
    Set<String> items = new HashSet<>();
    Set<String> patterns = new HashSet<>();
    for (TrimTemplate template : TrimTable.TEMPLATES) {
      assertTrue(items.add(template.itemId()), "repeated " + template.itemId());
      template.patternId().ifPresent(pattern -> assertTrue(patterns.add(pattern), pattern));
    }
    assertEquals(18, patterns.size());
  }

  @Test
  void trimTemplatesAreNamedAfterTheirPattern() {
    for (TrimTemplate template : TrimTable.TEMPLATES) {
      template
          .patternId()
          .ifPresent(
              pattern ->
                  assertEquals(pattern + "_armor_trim_smithing_template", template.itemId()));
    }
  }

  @Test
  void everySourceHasATemplate() {
    for (TrimSource source : TrimSource.values()) {
      assertFalse(TrimTable.templatesFrom(source).isEmpty(), source + " has no template");
      assertFalse(source.lootTables().isEmpty(), source + " has no loot table");
    }
  }

  @Test
  void templatesFromKeepsTableOrder() {
    assertEquals(
        List.of("wayfinder", "shaper", "raiser", "host"),
        TrimTable.templatesFrom(TrimSource.TRAIL_RUINS).stream()
            .map(template -> template.patternId().orElseThrow())
            .toList());
  }
}
