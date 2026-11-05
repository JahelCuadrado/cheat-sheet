package com.einsteinmods.cheatsheet.core.mobfarms;

import java.util.List;
import java.util.Locale;

/**
 * One condition of a farm, as its zoom lists it: an item's icon, a bold label and the text under
 * {@code cheatsheet.tab.redstone.farm.<textKey>} filled with {@code args}.
 *
 * @param iconItemId the item whose icon heads the line
 * @param label what kind of condition it is
 * @param textKey the text's key below {@code farm.}
 * @param args the numbers the text shows, in order
 */
public record FarmFact(String iconItemId, Label label, String textKey, List<Object> args) {
  public FarmFact {
    args = List.copyOf(args);
  }

  /** The bold label in front of a condition. */
  public enum Label {
    WHAT,
    HOW,
    WHERE,
    LIGHT,
    DISTANCE,
    CAP,
    FLOOR,
    ROOM,
    RATE,
    NEEDS,
    CHANCE,
    COPY_CHANCE,
    LOW_HEALTH,
    WHEN,
    STOPS,
    TIP;

    /** Lower-case id for lang keys. */
    public String key() {
      return name().toLowerCase(Locale.ROOT);
    }
  }
}
