package com.einsteinmods.cheatsheet.core.trims;

import java.util.Optional;

/**
 * One smithing template (item id without namespace), the trim pattern it applies (none for the
 * netherite upgrade), where it is found and the block that copies it: 7 diamonds + the template +
 * that block make two templates.
 */
public record TrimTemplate(
    String itemId, Optional<String> patternId, TrimSource source, String copyBlockId) {
  private static final String TRIM_TEMPLATE_SUFFIX = "_armor_trim_smithing_template";

  /** A trim template, named after its pattern as vanilla does. */
  static TrimTemplate trim(String patternId, TrimSource source, String copyBlockId) {
    return new TrimTemplate(
        patternId + TRIM_TEMPLATE_SUFFIX, Optional.of(patternId), source, copyBlockId);
  }
}
