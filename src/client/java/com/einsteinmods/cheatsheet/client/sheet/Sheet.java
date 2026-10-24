package com.einsteinmods.cheatsheet.client.sheet;

import com.einsteinmods.cheatsheet.client.picture.PictureAtlas;
import com.einsteinmods.cheatsheet.core.CardSize;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * One tab's content, in reading order: either cards (packed masonry style) or sections (titled
 * tables and grids stacked in rows), and the atlas their pictures are drawn from (null for a sheet
 * without pictures). Built once per cheat sheet screen.
 */
public record Sheet(List<Card> cards, List<Section> sections, @Nullable PictureAtlas pictures) {
  public Sheet {
    cards = List.copyOf(cards);
    sections = List.copyOf(sections);
    if (!cards.isEmpty() && !sections.isEmpty()) {
      throw new IllegalArgumentException("a sheet holds cards or sections, not both");
    }
  }

  public Sheet(List<Card> cards, @Nullable PictureAtlas pictures) {
    this(cards, List.of(), pictures);
  }

  public Sheet(List<Card> cards) {
    this(cards, null);
  }

  /** A sheet of sections in reading order. */
  public static Sheet ofSections(List<Section> sections, @Nullable PictureAtlas pictures) {
    return new Sheet(List.of(), sections, pictures);
  }

  /** Whether the sheet is laid out as sections rather than cards. */
  public boolean hasSections() {
    return !sections.isEmpty();
  }

  public static Sheet of(Card... cards) {
    return new Sheet(List.of(cards));
  }

  public List<CardSize> sizes() {
    List<CardSize> sizes = new ArrayList<>(cards.size());
    for (Card card : cards) {
      sizes.add(card.size());
    }
    return sizes;
  }

  /** Frees the pictures' texture; it is baked again if the sheet is shown again. */
  public void release() {
    if (pictures != null) {
      pictures.release();
    }
  }
}
