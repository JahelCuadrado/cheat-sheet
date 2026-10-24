package com.einsteinmods.cheatsheet.core.ores;

/** How a placement spreads its heights between its limits (vanilla height providers). */
public enum HeightShape {
  /** {@code UniformHeight}: every height equally likely. */
  UNIFORM,
  /**
   * {@code TrapezoidHeight} without plateau ("triangle"): the sum of two uniform draws, most likely
   * in the middle of the range.
   */
  TRIANGLE
}
