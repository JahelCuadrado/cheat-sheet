package com.einsteinmods.cheatsheet.core.sections;

import java.util.List;

/** The geometry of a section's body (a table, a grid of tiles): the shapes it can take. */
public interface SectionBody {
  /**
   * Every distinct shape, narrowest first; each is wider and shorter than the one before (a split
   * that would not shorten the body is left out). Never empty.
   */
  List<BodyShape> shapes();

  /** The narrowest shape. */
  default BodyShape narrowest() {
    return shapes().getFirst();
  }

  /**
   * The shortest shape no wider than {@code availableWidth} (the narrowest when none fits: the
   * sheet then overflows rather than overlapping).
   */
  default BodyShape fit(int availableWidth) {
    BodyShape best = narrowest();
    for (BodyShape shape : shapes()) {
      if (shape.width() <= availableWidth && shape.height() < best.height()) {
        best = shape;
      }
    }
    return best;
  }

  /**
   * The shape {@code fitted} (from {@link #fit}) takes when its section is alone in its row and
   * {@code availableWidth} pixels are offered: by default unchanged; a body that can spread out (a
   * chart with wider columns) returns a wider shape, never narrower than {@code fitted} and never
   * taller.
   */
  default BodyShape stretch(BodyShape fitted, int availableWidth) {
    return fitted;
  }
}
