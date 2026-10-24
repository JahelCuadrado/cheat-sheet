package com.einsteinmods.cheatsheet.core.discs;

import java.util.List;

/** One music disc (item id without namespace) and every way to get it, in display order. */
public record Disc(String itemId, List<DiscSource> sources) {
  public Disc {
    if (sources.isEmpty()) {
      throw new IllegalArgumentException(itemId + " needs a source");
    }
    sources = List.copyOf(sources);
  }
}
