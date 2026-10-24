package com.einsteinmods.cheatsheet.client.sheet;

/**
 * Which card of a sheet is at a point and where it is: the result of a hit test, written into an
 * object the screen keeps, so testing every frame (the hover highlight) allocates nothing.
 *
 * <p>{@code group} is the section's index (or -1 on a sheet of masonry cards), {@code card} the
 * card's index in it; the bounds are in sheet pixels from the corner of what was tested (a body,
 * then the sheet once the section moves them).
 */
public final class CardSpot {
  /** No section: a card of a masonry sheet. */
  public static final int NO_GROUP = -1;

  private int group = NO_GROUP;
  private int card;
  private int x;
  private int y;
  private int width;
  private int height;

  /** Sets the card and its bounds; the group is left as it is. */
  public void set(int card, int x, int y, int width, int height) {
    this.card = card;
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }

  /** Puts the spot in section {@code group}, its bounds moved by ({@code dx}, {@code dy}). */
  public void moveInto(int group, int dx, int dy) {
    this.group = group;
    this.x += dx;
    this.y += dy;
  }

  public int group() {
    return group;
  }

  public int card() {
    return card;
  }

  public int x() {
    return x;
  }

  public int y() {
    return y;
  }

  public int width() {
    return width;
  }

  public int height() {
    return height;
  }
}
