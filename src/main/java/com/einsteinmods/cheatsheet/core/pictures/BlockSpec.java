package com.einsteinmods.cheatsheet.core.pictures;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A block state written as in commands, {@code oak_stairs[facing=east,half=top]}: the block id
 * (namespace {@code minecraft} when none is given) and its properties. Only the client turns it
 * into a real block state, so this stays plain Java.
 *
 * @param blockId namespaced block id, {@code minecraft:oak_stairs}
 * @param properties property names to values, in the order written
 */
public record BlockSpec(String blockId, Map<String, String> properties) {
  private static final String DEFAULT_NAMESPACE = "minecraft:";
  private static final String NAMESPACE_SEPARATOR = ":";
  private static final char PROPERTIES_START = '[';
  private static final char PROPERTIES_END = ']';
  private static final String PROPERTY_SEPARATOR = ",";
  private static final String VALUE_SEPARATOR = "=";
  private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
  private static final Pattern NAME = Pattern.compile("[a-z0-9_]+");

  public BlockSpec {
    if (!ID.matcher(blockId).matches()) {
      throw new IllegalArgumentException("bad block id: " + blockId);
    }
    for (Map.Entry<String, String> property : properties.entrySet()) {
      if (!NAME.matcher(property.getKey()).matches()
          || !NAME.matcher(property.getValue()).matches()) {
        throw new IllegalArgumentException("bad property of " + blockId + ": " + property);
      }
    }
    properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
  }

  /** Parses {@code id} or {@code id[name=value,...]}; throws on anything malformed. */
  public static BlockSpec parse(String text) {
    int open = text.indexOf(PROPERTIES_START);
    String id = open < 0 ? text : text.substring(0, open);
    if (!id.contains(NAMESPACE_SEPARATOR)) {
      id = DEFAULT_NAMESPACE + id;
    }
    Map<String, String> properties = new LinkedHashMap<>();
    if (open >= 0) {
      if (text.charAt(text.length() - 1) != PROPERTIES_END) {
        throw new IllegalArgumentException("unclosed properties: " + text);
      }
      for (String pair : text.substring(open + 1, text.length() - 1).split(PROPERTY_SEPARATOR)) {
        String[] parts = pair.split(VALUE_SEPARATOR, -1);
        if (parts.length != 2 || properties.put(parts[0], parts[1]) != null) {
          throw new IllegalArgumentException("bad property '" + pair + "' in " + text);
        }
      }
    }
    return new BlockSpec(id, properties);
  }
}
