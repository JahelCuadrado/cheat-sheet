package com.einsteinmods.cheatsheet.core.pictures;

/** Size of a picture in GUI pixels. */
public record PictureSize(int width, int height) {
  public PictureSize {
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("bad picture size: " + width + "x" + height);
    }
  }
}
