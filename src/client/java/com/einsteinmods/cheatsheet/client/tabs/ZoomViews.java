package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.sheet.CardTint;
import com.einsteinmods.cheatsheet.client.sheet.ElementCard;
import com.einsteinmods.cheatsheet.client.sheet.Panels;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import com.einsteinmods.cheatsheet.client.sheet.ZoomCard;
import com.einsteinmods.cheatsheet.core.ZoomSize;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * Pieces of the views tabs lay out for the zoom ({@link SheetElement#zoomView()}, {@code
 * Table#withRowZooms}): a picture with its name under it, a tinted panel around a view, and views
 * built on their first click and kept for the next.
 */
final class ZoomViews {
  /** Space between a view's panel and its content. */
  static final int PADDING = 5;

  /** Space between a picture and its name under it. */
  static final int CAPTION_GAP = 2;

  private ZoomViews() {}

  /**
   * {@code picture} with {@code name} centred under it, wrapped at {@code wrapWidth} (a word wider
   * than that hyphenated), its lines taking at least {@code lines} lines' room so that captions of
   * one row of pictures end at the same height.
   */
  static SheetElement captioned(
      Font font, SheetElement picture, Component name, int wrapWidth, int color, int lines) {
    CentredText caption =
        new CentredText(font, FittedText.hyphenated(font, name, wrapWidth), wrapWidth, color);
    int room = lines * font.lineHeight;
    if (caption.height() >= room) {
      return new CentredColumn(List.of(picture, caption), CAPTION_GAP);
    }
    return new CentredColumn(
        List.of(picture, caption, new Spacer(room - caption.height() - CAPTION_GAP)), CAPTION_GAP);
  }

  /** Empty room under a short caption. */
  private record Spacer(int height) implements SheetElement {
    @Override
    public int minWidth() {
      return 0;
    }

    @Override
    public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
      // Room only: nothing to draw.
    }
  }

  /** {@code content} on a panel tinted {@code tint}, zoomed {@code size}. */
  static ZoomCard panel(SheetElement content, CardTint tint, ZoomSize size) {
    return new ElementCard(new TintedPanel(content, tint), size);
  }

  /**
   * Views by index, each built by {@code build} on its first click and kept: a view's zoom-only
   * pictures are added to the atlas once.
   */
  static IntFunction<ZoomCard> kept(int count, IntFunction<ZoomCard> build) {
    ZoomCard[] views = new ZoomCard[count];
    return index -> {
      ZoomCard view = views[index];
      if (view == null) {
        view = build.apply(index);
        views[index] = view;
      }
      return view;
    };
  }

  /**
   * {@code element} unchanged on the sheet, zoomed as the view {@code view} builds on its first
   * click (kept for the next).
   */
  static SheetElement zoomedAs(SheetElement element, Supplier<ZoomCard> view) {
    return new WithZoomView(element, view);
  }

  /** An element that zooms as a view of its own; everything else is the element's. */
  private static final class WithZoomView implements SheetElement {
    private final SheetElement element;
    private final Supplier<ZoomCard> build;
    private @Nullable ZoomCard view;

    WithZoomView(SheetElement element, Supplier<ZoomCard> build) {
      this.element = element;
      this.build = build;
    }

    @Override
    public int minWidth() {
      return element.minWidth();
    }

    @Override
    public int height() {
      return element.height();
    }

    @Override
    public int width(int available) {
      return element.width(available);
    }

    @Override
    public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
      element.extract(graphics, x, y, width);
    }

    @Override
    public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
      return element.tooltipAt(localX, localY, width);
    }

    @Override
    public ZoomCard zoomView() {
      ZoomCard built = view;
      if (built == null) {
        built = build.get();
        view = built;
      }
      return built;
    }
  }

  /** Elements one below the other, each centred across the widest. Drawing allocates nothing. */
  static final class CentredColumn implements SheetElement {
    private final SheetElement[] children;
    private final int[] childXs;
    private final int[] childYs;
    private final int width;
    private final int height;

    CentredColumn(List<SheetElement> children, int gap) {
      if (children.isEmpty()) {
        throw new IllegalArgumentException("a column needs elements");
      }
      this.children = children.toArray(new SheetElement[0]);
      this.childXs = new int[this.children.length];
      this.childYs = new int[this.children.length];
      int widest = 0;
      int top = 0;
      for (int index = 0; index < this.children.length; index++) {
        widest = Math.max(widest, this.children[index].minWidth());
        childYs[index] = top;
        top += this.children[index].height() + gap;
      }
      for (int index = 0; index < this.children.length; index++) {
        childXs[index] = (widest - this.children[index].minWidth()) / 2;
      }
      this.width = widest;
      this.height = top - gap;
    }

    @Override
    public int minWidth() {
      return width;
    }

    @Override
    public int height() {
      return height;
    }

    @Override
    public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
      for (int index = 0; index < children.length; index++) {
        SheetElement child = children[index];
        child.extract(graphics, x + childXs[index], y + childYs[index], child.minWidth());
      }
    }

    @Override
    public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
      for (int index = 0; index < children.length; index++) {
        SheetElement child = children[index];
        int childX = localX - childXs[index];
        int childY = localY - childYs[index];
        if (childX >= 0 && childX < child.minWidth() && childY >= 0 && childY < child.height()) {
          return child.tooltipAt(childX, childY, child.minWidth());
        }
      }
      return null;
    }
  }

  /** A view on a tinted rounded panel, {@link #PADDING} around it. */
  private record TintedPanel(SheetElement content, CardTint tint) implements SheetElement {
    @Override
    public int minWidth() {
      return content.minWidth() + PADDING * 2;
    }

    @Override
    public int height() {
      return content.height() + PADDING * 2;
    }

    @Override
    public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
      Panels.rounded(graphics, x, y, minWidth(), height(), tint.fill(), tint.border());
      content.extract(graphics, x + PADDING, y + PADDING, content.minWidth());
    }

    @Override
    public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
      int contentX = localX - PADDING;
      int contentY = localY - PADDING;
      if (contentX < 0
          || contentY < 0
          || contentX >= content.minWidth()
          || contentY >= content.height()) {
        return null;
      }
      return content.tooltipAt(contentX, contentY, content.minWidth());
    }
  }
}
