package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.sheet.TextLabel;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * Text that never gets wider than its room, whatever the language: cut on one line, or wrapped (a
 * word longer than the room breaks, hyphenated). Tiles of a fixed width then hold any translation.
 * Build time only.
 */
final class FittedText {
  private static final String ELLIPSIS = "…";
  private static final String HYPHEN = "-";
  private static final String WORD_SEPARATOR = " ";
  private static final char LINE_BREAK = '\n';

  /** Scripts written without spaces: the font breaks their lines anywhere, with no hyphen. */
  private static final Set<Character.UnicodeScript> UNSPACED_SCRIPTS =
      EnumSet.of(
          Character.UnicodeScript.HAN,
          Character.UnicodeScript.HIRAGANA,
          Character.UnicodeScript.KATAKANA,
          Character.UnicodeScript.HANGUL);

  private FittedText() {}

  /**
   * {@code text} (one style) with every word wider than {@code room} broken into hyphenated pieces
   * on lines of their own, so wrapping it at {@code room} never splits a word without a hyphen. The
   * same text when no word is that wide.
   */
  static Component hyphenated(Font font, Component text, int room) {
    Style style = text.getStyle();
    int pieceRoom = Math.max(1, room - font.width(Component.literal(HYPHEN).withStyle(style)));
    StringBuilder out = new StringBuilder();
    boolean changed = false;
    String[] words = text.getString().split(WORD_SEPARATOR, -1);
    for (int index = 0; index < words.length; index++) {
      if (index > 0) {
        out.append(WORD_SEPARATOR);
      }
      String rest = words[index];
      if (font.width(Component.literal(rest).withStyle(style)) <= room || unspaced(rest)) {
        out.append(rest);
        continue;
      }
      changed = true;
      while (font.width(Component.literal(rest).withStyle(style)) > room) {
        String piece = font.getSplitter().plainHeadByWidth(rest, pieceRoom, style);
        if (piece.isEmpty()) {
          piece = rest.substring(0, 1);
        }
        out.append(piece).append(HYPHEN).append(LINE_BREAK);
        rest = rest.substring(piece.length());
      }
      out.append(rest);
    }
    return changed ? Component.literal(out.toString()).withStyle(style) : text;
  }

  private static boolean unspaced(String word) {
    return !word.isEmpty()
        && UNSPACED_SCRIPTS.contains(Character.UnicodeScript.of(word.codePointAt(0)));
  }

  /** {@code text} on one line of {@code room} pixels, cut with an ellipsis when longer. */
  static FormattedCharSequence fitted(Font font, Component text, int room) {
    if (font.width(text) <= room) {
      return text.getVisualOrderText();
    }
    String cut =
        font.plainSubstrByWidth(text.getString(), Math.max(0, room - font.width(ELLIPSIS)));
    return Component.literal(cut.stripTrailing() + ELLIPSIS).getVisualOrderText();
  }

  /** {@code text} wrapped at {@code room}, one label per line, each with {@code tooltip}. */
  static List<TextLabel> lines(
      Font font,
      Component text,
      int room,
      int color,
      @Nullable List<FormattedCharSequence> tooltip) {
    List<TextLabel> lines = new ArrayList<>();
    for (FormattedCharSequence line : font.split(text, room)) {
      lines.add(new TextLabel(font, line, color, 1, tooltip));
    }
    return List.copyOf(lines);
  }

  /**
   * {@code text} wrapped at {@code room} into at most {@code maxLines} lines, the last cut with an
   * ellipsis when more would be needed.
   */
  static List<TextLabel> lines(Font font, Component text, int room, int maxLines, int color) {
    List<FormattedText> parts = font.getSplitter().splitLines(text, room, Style.EMPTY);
    List<TextLabel> lines = new ArrayList<>(Math.min(parts.size(), maxLines));
    int kept = parts.size() <= maxLines ? parts.size() : maxLines - 1;
    String full = text.getString();
    int consumed = 0;
    for (int index = 0; index < kept; index++) {
      FormattedText part = parts.get(index);
      lines.add(new TextLabel(font, Language.getInstance().getVisualOrder(part), color, 1, null));
      String shown = part.getString();
      consumed = Math.max(consumed, full.indexOf(shown, consumed) + shown.length());
    }
    if (kept < parts.size()) {
      Component rest =
          Component.literal(full.substring(consumed).strip()).withStyle(text.getStyle());
      lines.add(new TextLabel(font, fittedStyled(font, rest, room), color, 1, null));
    }
    return List.copyOf(lines);
  }

  /** Like {@link #fitted} but keeping the text's style (bold) on the cut line. */
  private static FormattedCharSequence fittedStyled(Font font, Component text, int room) {
    if (font.width(text) <= room) {
      return text.getVisualOrderText();
    }
    String cut = text.getString();
    while (!cut.isEmpty()
        && font.width(Component.literal(cut + ELLIPSIS).withStyle(text.getStyle())) > room) {
      cut = cut.substring(0, cut.length() - 1);
    }
    return Component.literal(cut.stripTrailing() + ELLIPSIS)
        .withStyle(text.getStyle())
        .getVisualOrderText();
  }
}
