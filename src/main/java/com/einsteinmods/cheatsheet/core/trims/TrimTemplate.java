package com.einsteinmods.cheatsheet.core.trims;

import java.util.List;
import java.util.Optional;

/**
 * One smithing template (item id without namespace), the trim pattern it applies (none for the
 * netherite upgrade), where it is found and the blocks that copy it: 7 diamonds + the template +
 * one of those blocks make two templates.
 */
public record TrimTemplate(
    String itemId, Optional<String> patternId, TrimSource source, List<String> copyBlockIds) {
  private static final String TRIM_TEMPLATE_SUFFIX = "_armor_trim_smithing_template";

  /** A trim template, named after its pattern as vanilla does. */
  static TrimTemplate trim(String patternId, TrimSource source, String... copyBlockIds) {
    return new TrimTemplate(
        patternId + TRIM_TEMPLATE_SUFFIX, Optional.of(patternId), source, List.of(copyBlockIds));
  }
}
