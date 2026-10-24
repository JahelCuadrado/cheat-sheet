package com.einsteinmods.cheatsheet.core.sections;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class StretchedSectionTest {
  private static final SectionsLayout.Spacing SPACING = new SectionsLayout.Spacing(10, 8, 8);
  private static final int NARROW = 100;
  private static final int HEIGHT = 50;
  private static final int WIDE_MAX = 300;

  /** A body of one shape that spreads up to {@link #WIDE_MAX} pixels. */
  private record Spreading() implements SectionBody {
    @Override
    public List<BodyShape> shapes() {
      return List.of(new BodyShape(1, NARROW, HEIGHT));
    }

    @Override
    public BodyShape stretch(BodyShape fitted, int availableWidth) {
      return new BodyShape(
          fitted.split(), Math.clamp(availableWidth, fitted.width(), WIDE_MAX), fitted.height());
    }
  }

  @Test
  void aSectionAloneTakesItsStretchedShapeCentred() {
    SectionsLayout layout = SectionsLayout.of(List.of(List.of(new Spreading())), 500, SPACING);
    SectionsLayout.PlacedSection placed = layout.sections().getFirst();
    assertEquals(WIDE_MAX, placed.shape().width());
    assertEquals((500 - WIDE_MAX) / 2, placed.x());
    assertEquals(SPACING.titleHeight() + HEIGHT, layout.contentHeight());
  }

  @Test
  void bodiesThatDoNotSpreadKeepTheirFittedShape() {
    SectionBody grid = new GridGeometry(40, 20, 3, 2);
    SectionsLayout layout = SectionsLayout.of(List.of(List.of(grid)), 500, SPACING);
    assertEquals(grid.fit(500), layout.sections().getFirst().shape());
  }
}
