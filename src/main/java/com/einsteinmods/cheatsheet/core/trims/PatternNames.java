package com.einsteinmods.cheatsheet.core.trims;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Short names for trim patterns in a table: the words every pattern's name shares ("Sentry Armor
 * Trim", "Dune Armor Trim" → "Sentry", "Dune"; "Diseño de armadura de centinela" → "Centinela") are
 * left out, in any language. Only whole words go: the shared part is cut back to a word separator,
 * so languages without spaces (Japanese, Chinese) keep their full names.
 */
public final class PatternNames {
  private static final String SEPARATORS = " -'’";

  private PatternNames() {}

  /**
   * {@code names} without the words they all start or end with, first letter capitalised when a
   * start was removed (as {@code locale} capitalises). Unchanged when fewer than two names, or when
   * removing would leave a name empty.
   */
  public static List<String> shorten(List<String> names, Locale locale) {
    if (names.size() < 2) {
      return List.copyOf(names);
    }
    String first = names.getFirst();
    int prefix = first.length();
    int suffix = first.length();
    for (String name : names) {
      prefix = Math.min(prefix, commonPrefix(first, name));
      suffix = Math.min(suffix, commonSuffix(first, name));
    }
    prefix = lastSeparatorEnd(first, prefix);
    suffix = firstSeparatorStart(first, suffix);
    List<String> shortened = new ArrayList<>(names.size());
    for (String name : names) {
      int end = name.length() - suffix;
      if (end <= prefix) {
        return List.copyOf(names);
      }
      String part = name.substring(prefix, end);
      if (prefix > 0) {
        part = part.substring(0, 1).toUpperCase(locale) + part.substring(1);
      }
      shortened.add(part);
    }
    return List.copyOf(shortened);
  }

  private static int commonPrefix(String first, String other) {
    int length = Math.min(first.length(), other.length());
    int index = 0;
    while (index < length && first.charAt(index) == other.charAt(index)) {
      index++;
    }
    return index;
  }

  private static int commonSuffix(String first, String other) {
    int length = Math.min(first.length(), other.length());
    int count = 0;
    while (count < length
        && first.charAt(first.length() - 1 - count) == other.charAt(other.length() - 1 - count)) {
      count++;
    }
    return count;
  }

  /** Length of the shared start cut back to just after its last separator (0 if none). */
  private static int lastSeparatorEnd(String name, int prefix) {
    for (int index = prefix - 1; index >= 0; index--) {
      if (SEPARATORS.indexOf(name.charAt(index)) >= 0) {
        return index + 1;
      }
    }
    return 0;
  }

  /** Length of the shared end cut back to start at its first separator (0 if none). */
  private static int firstSeparatorStart(String name, int suffix) {
    for (int index = name.length() - suffix; index < name.length(); index++) {
      if (SEPARATORS.indexOf(name.charAt(index)) >= 0) {
        return name.length() - index;
      }
    }
    return 0;
  }
}
