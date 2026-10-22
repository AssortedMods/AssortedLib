package com.grim3212.assorted.lib.client.manual.screen;

import com.grim3212.assorted.lib.client.manual.ManualBook;
import com.grim3212.assorted.lib.client.manual.ManualBookStyle;
import com.grim3212.assorted.lib.client.manual.ManualChapter;
import com.grim3212.assorted.lib.client.manual.ManualContent;
import com.grim3212.assorted.lib.client.manual.ManualPageEntry;
import com.grim3212.assorted.lib.client.manual.ManualPageView;
import com.grim3212.assorted.lib.manual.ManualLinks;
import com.grim3212.assorted.lib.manual.ManualPageRef;
import com.grim3212.assorted.lib.manual.ManualSection;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;

/** The manual, drawn as an open book. One screen swapping {@link ManualView}s, not a chain of them. */
public class ManualScreen extends Screen {

    private static final Component TITLE = Component.translatable("gui.assortedlib.manual.title");

    private static final int BAR_WIDTH = 3;

    /** Slop either side of the bar, which is too thin to aim at. */
    private static final int GRAB_SLOP = 3;

    /** Floor, so the thumb stays grabbable however long the page is. */
    private static final int MIN_THUMB = 10;
    private static final Component BULLET = Component.literal("* ").withStyle(ChatFormatting.BOLD);

    /** Where the book was last left, so reopening it does not start over at the index. */
    private static ManualView lastView = new ManualView.Index();

    private final List<Row> rows = new ArrayList<>();

    private ManualView view;
    private int left;
    private int top;

    /** One per page, so freezing one side's cycling does not freeze the other. */
    private int leftTick;
    private int rightTick;
    private boolean leftFrozen;
    private boolean rightFrozen;

    /** Scroll position in whole lines, bounded by the overflow the last frame reported. */
    private int leftScroll;
    private int rightScroll;
    private int leftOverflow;
    private int rightOverflow;

    /** Page whose scrollbar is held, or -1, and where in the thumb it was grabbed. */
    private int draggingSide = -1;
    private int dragGrabOffset;

    private ManualPageView leftPageView;
    private ManualPageView rightPageView;

    private ManualSpriteButton previousButton;
    private ManualSpriteButton nextButton;
    private ManualSpriteButton homeButton;

    public ManualScreen(ManualView view) {
        super(TITLE);
        this.view = view;
    }

    /** Opens at {@code ref} when it resolves, and otherwise wherever the book was last left. */
    public static ManualScreen openedAt(@Nullable ManualPageRef ref) {
        ManualView target = ref == null ? lastView : ManualContent.get().locate(ref)
                .<ManualView>map(location -> new ManualView.Pages(location.chapter().section(), location.chapter().id(), spreadStart(location.pageIndex())))
                .orElse(lastView);

        return new ManualScreen(target);
    }

    /** Spreads start on an even page, so a linked page lands on the left unless it is the last. */
    private static int spreadStart(int pageIndex) {
        return pageIndex - (pageIndex % 2);
    }

    /** The opening spread keeps its left page for the intro, so it holds half the rows later ones do. */
    private static int firstRow(int page, int rows) {
        return page == 0 ? 0 : rows + (page - 1) * rows * 2;
    }

    private static int rowsOn(int page, int rows) {
        return page == 0 ? rows : rows * 2;
    }

    private static boolean hasMoreRows(int page, int total, int rows) {
        return firstRow(page, rows) + rowsOn(page, rows) < total;
    }

    /** Re-read each use: a resource reload can change it under us. */
    private ManualBookStyle style() {
        return ManualContent.get().style();
    }

    @Override
    protected void init() {
        ManualBookStyle style = this.style();
        this.left = (this.width - style.width()) / 2;
        this.top = (this.height - style.height()) / 2;

        int controlsY = this.top + style.controlsY();
        int centre = this.left + style.width() / 2;

        this.previousButton = this.addRenderableWidget(new ManualSpriteButton(this.left + 14, controlsY, 18, 10,
                ManualBook.Sprites.PAGE_BACKWARD, ManualBook.Sprites.PAGE_BACKWARD_HIGHLIGHTED,
                Component.translatable("gui.assortedlib.manual.previous"), button -> this.turnBack(), true));

        this.nextButton = this.addRenderableWidget(new ManualSpriteButton(this.left + style.width() - 32, controlsY, 18, 10,
                ManualBook.Sprites.PAGE_FORWARD, ManualBook.Sprites.PAGE_FORWARD_HIGHLIGHTED,
                Component.translatable("gui.assortedlib.manual.next"), button -> this.turnForward(), true));

        this.homeButton = this.addRenderableWidget(new ManualSpriteButton(centre - 4, controlsY, 9, 10,
                ManualBook.Sprites.HOME, ManualBook.Sprites.HOME_HIGHLIGHTED,
                Component.translatable("gui.assortedlib.manual.index"), button -> this.show(new ManualView.Index()), false));

        this.refresh();
    }

    // ---- navigation -------------------------------------------------------------------------

    private void show(ManualView target) {
        this.view = target;
        lastView = target;
        this.leftTick = 0;
        this.rightTick = 0;
        this.leftFrozen = false;
        this.rightFrozen = false;
        this.leftScroll = 0;
        this.rightScroll = 0;
        this.leftOverflow = 0;
        this.rightOverflow = 0;
        this.draggingSide = -1;
        this.refresh();
    }

    private void turnForward() {
        switch (this.view) {
            case ManualView.Index index -> this.show(new ManualView.Index(index.page() + 1));
            case ManualView.Chapters chapters -> this.show(new ManualView.Chapters(chapters.section(), chapters.page() + 1));
            case ManualView.Pages pages -> this.show(new ManualView.Pages(pages.section(), pages.chapter(), pages.leftPage() + 2));
        }
    }

    /** Back a page, or out a level once there are none left to turn. Never touches the history. */
    private void turnBack() {
        switch (this.view) {
            case ManualView.Index index -> this.show(new ManualView.Index(Math.max(0, index.page() - 1)));
            case ManualView.Chapters chapters -> this.show(chapters.page() > 0
                    ? new ManualView.Chapters(chapters.section(), chapters.page() - 1)
                    : new ManualView.Index());
            case ManualView.Pages pages -> this.show(pages.leftPage() > 0
                    ? new ManualView.Pages(pages.section(), pages.chapter(), pages.leftPage() - 2)
                    : new ManualView.Chapters(pages.section()));
        }
    }

    private void follow(ManualPageRef ref) {
        ManualContent.get().locate(ref).ifPresent(location ->
                this.show(new ManualView.Pages(location.chapter().section(), location.chapter().id(), spreadStart(location.pageIndex()))));
    }

    // ---- content ----------------------------------------------------------------------------

    private Optional<ManualChapter> chapter(ManualView.Pages pages) {
        return ManualContent.get().chapter(pages.section(), pages.chapter());
    }

    /** Rebuilds the clickable rows and the control states for the view now showing. */
    private void refresh() {
        this.rows.clear();

        ManualContent content = ManualContent.get();
        boolean canGoForward = false;
        boolean canGoBack = false;

        switch (this.view) {
            case ManualView.Index index -> {
                List<ManualSection> sections = content.sections();
                this.addRows(index.page(), sections.size(), row -> {
                    ManualSection section = sections.get(row);
                    return new Entry(section.title(), () -> this.show(new ManualView.Chapters(section.modId())));
                });
                canGoForward = hasMoreRows(index.page(), sections.size(), this.style().entriesPerPage());
                canGoBack = index.page() > 0;
            }
            case ManualView.Chapters chapters -> {
                List<ManualChapter> list = content.chaptersOf(chapters.section());
                this.addRows(chapters.page(), list.size(), row -> {
                    ManualChapter chapter = list.get(row);
                    return new Entry(chapter.title(), () -> this.show(new ManualView.Pages(chapters.section(), chapter.id(), 0)));
                });
                canGoForward = hasMoreRows(chapters.page(), list.size(), this.style().entriesPerPage());
                canGoBack = true;
            }
            case ManualView.Pages pages -> {
                ManualChapter chapter = this.chapter(pages).orElse(null);
                if (chapter != null) {
                    canGoForward = pages.leftPage() + 2 < chapter.pageCount();
                }
                canGoBack = true;
            }
        }

        this.previousButton.visible = canGoBack;
        this.nextButton.visible = canGoForward;
        this.homeButton.visible = !(this.view instanceof ManualView.Index);
    }

    /** The opening spread puts rows on the right only, beside its intro; later ones fill both sides. */
    private void addRows(int page, int total, IntFunction<Entry> entries) {
        int rows = this.style().entriesPerPage();
        int first = firstRow(page, rows);
        int shown = Math.min(rowsOn(page, rows), Math.max(0, total - first));

        for (int i = 0; i < shown; i++) {
            boolean onLeftPage = page > 0 && i < rows;
            this.addRow(onLeftPage ? 0 : 1, onLeftPage ? i : i % rows, entries.apply(first + i));
        }
    }

    private void addRow(int side, int slot, Entry entry) {
        Component bulleted = Component.empty().append(BULLET).append(entry.label());
        ManualBookStyle style = this.style();
        int x = this.left + style.contentX(side);
        int y = this.top + style.contentY() + slot * style.entryHeight();
        this.rows.add(new Row(x, y, this.font.width(bulleted), this.font.lineHeight, bulleted, entry.action()));
    }

    // ---- drawing ----------------------------------------------------------------------------

    @Override
    public void tick() {
        if (!this.leftFrozen) {
            this.leftTick++;
        }
        if (!this.rightFrozen) {
            this.rightTick++;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        ManualBookStyle style = this.style();
        graphics.blit(RenderPipelines.GUI_TEXTURED, style.texture(), this.left, this.top, 0.0F, 0.0F,
                style.width(), style.height(), style.textureWidth(), style.textureHeight());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.leftPageView = this.pageView(graphics, 0, mouseX, mouseY);
        this.rightPageView = this.pageView(graphics, 1, mouseX, mouseY);

        switch (this.view) {
            case ManualView.Index index -> this.drawIndex(index);
            case ManualView.Chapters chapters -> this.drawChapters(chapters);
            case ManualView.Pages pages -> this.drawPages(pages);
        }

        this.drawRows(graphics, mouseX, mouseY);

        // What the pages asked for while drawing decides next tick's cycling and scroll bounds.
        this.leftFrozen = this.leftPageView.animationFrozen();
        this.rightFrozen = this.rightPageView.animationFrozen();
        this.leftOverflow = this.leftPageView.overflowLines();
        this.rightOverflow = this.rightPageView.overflowLines();
        this.leftScroll = Math.min(this.leftScroll, this.leftOverflow);
        this.rightScroll = Math.min(this.rightScroll, this.rightOverflow);

        this.drawScrollbar(graphics, 0, mouseX, mouseY);
        this.drawScrollbar(graphics, 1, mouseX, mouseY);

        if (linkedPage(this.leftPageView) != null || linkedPage(this.rightPageView) != null) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }

        super.extractRenderState(graphics, mouseX, mouseY, a);

        // Last, so a tooltip is never painted over by the other page.
        this.leftPageView.drawHoveredTooltip();
        this.rightPageView.drawHoveredTooltip();
    }

    private ManualPageView pageView(GuiGraphicsExtractor graphics, int side, int mouseX, int mouseY) {
        ManualBookStyle style = this.style();
        return new ManualPageView(graphics, this.font, style,
                this.left + style.contentX(side), this.top + style.contentY(),
                style.contentWidth(), style.contentHeight(), mouseX, mouseY,
                side == 0 ? this.leftScroll : this.rightScroll);
    }

    /** Null when the page fits. */
    private Scrollbar scrollbar(int side) {
        int overflow = side == 0 ? this.leftOverflow : this.rightOverflow;
        if (overflow <= 0) {
            return null;
        }

        ManualBookStyle style = this.style();
        int track = style.contentHeight();
        int top = this.top + style.contentY();
        // Right of the text it belongs to.
        int x = this.left + style.contentX(side) + style.contentWidth() + 1;

        int visibleLines = track / ManualPageView.LINE_HEIGHT;
        int thumb = Math.max(MIN_THUMB, track * visibleLines / (visibleLines + overflow));
        int scroll = side == 0 ? this.leftScroll : this.rightScroll;
        int thumbY = top + (track - thumb) * scroll / overflow;

        return new Scrollbar(x, top, track, thumbY, thumb, overflow);
    }

    private void drawScrollbar(GuiGraphicsExtractor graphics, int side, int mouseX, int mouseY) {
        Scrollbar bar = this.scrollbar(side);
        if (bar == null) {
            return;
        }

        // Lights up on hover as well as while held, as an affordance.
        boolean active = this.draggingSide == side || (bar.grabbed(mouseX, mouseY) && bar.onThumb(mouseY));
        // As a vanilla scroll area does.
        if (this.draggingSide == side) {
            graphics.requestCursor(CursorTypes.RESIZE_NS);
        } else if (this.draggingSide < 0 && bar.grabbed(mouseX, mouseY)) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
        int color = active ? this.style().hoveredTextColor() : this.style().mutedTextColor();
        graphics.fill(bar.x(), bar.top(), bar.x() + BAR_WIDTH, bar.top() + bar.track(), this.style().mutedTextColor() & 0x30FFFFFF);
        graphics.fill(bar.x(), bar.thumbY(), bar.x() + BAR_WIDTH, bar.thumbY() + bar.thumb(), color);
    }

    /** Puts the thumb's top at {@code thumbY}, clamped to the overflow. */
    private void scrollTo(int side, Scrollbar bar, int thumbY) {
        int travel = bar.track() - bar.thumb();
        int scroll = travel <= 0 ? 0 : Math.clamp(Math.round((thumbY - bar.top()) * (float) bar.overflow() / travel), 0, bar.overflow());

        if (side == 0) {
            this.leftScroll = scroll;
        } else {
            this.rightScroll = scroll;
        }
    }

    /** Centred on a page, not the spread: across the book a heading lands on the dark spine. */
    private void drawHeader(ManualPageView view, Component title) {
        view.centeredText(title, this.style().titleY() - this.style().contentY(), this.style().titleColor());
    }

    private void drawIndex(ManualView.Index index) {
        this.drawHeader(this.leftPageView, TITLE);

        if (index.page() > 0) {
            return;
        }

        this.leftPageView.text(Component.translatable("gui.assortedlib.manual.index.intro"), 0);
        if (ManualContent.get().sections().isEmpty()) {
            this.rightPageView.text(Component.translatable("gui.assortedlib.manual.index.empty"), 0);
        }
    }

    private void drawChapters(ManualView.Chapters chapters) {
        ManualContent.get().section(chapters.section()).ifPresent(section -> {
            this.drawHeader(this.leftPageView, section.title());
            if (chapters.page() == 0) {
                this.leftPageView.text(section.description(), 0);
            }
        });
    }

    private void drawPages(ManualView.Pages pages) {
        ManualChapter chapter = this.chapter(pages).orElse(null);
        if (chapter == null) {
            this.drawHeader(this.leftPageView, TITLE);
            this.leftPageView.text(Component.translatable("gui.assortedlib.manual.chapter_missing"), 0);
            return;
        }

        this.drawPage(chapter, pages.leftPage(), this.leftPageView, this.leftTick);
        this.drawPage(chapter, pages.leftPage() + 1, this.rightPageView, this.rightTick);
    }

    /** Nothing at all when the spread runs out mid way. */
    private void drawPage(ManualChapter chapter, int index, ManualPageView view, int animationTick) {
        ManualPageEntry entry = chapter.page(index).orElse(null);
        if (entry == null) {
            return;
        }

        this.drawHeader(view, entry.page().title().orElseGet(chapter::title));
        entry.page().render(view, animationTick);
        view.centeredText(Component.translatable("gui.assortedlib.manual.page_indicator", index + 1, chapter.pageCount()),
                this.style().footerY() - this.style().contentY(), this.style().mutedTextColor());
    }

    private void drawRows(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (Row row : this.rows) {
            boolean hovered = row.contains(mouseX, mouseY);
            int color = hovered ? this.style().hoveredTextColor() : this.style().textColor();
            if (hovered) {
                graphics.requestCursor(CursorTypes.POINTING_HAND);
            }
            graphics.text(this.font, row.label(), row.x(), row.y(), color, false);
        }
    }

    // ---- input ------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            // Asked first: the bar sits over the page margin and must win over what is behind it.
            for (int side = 0; side < 2; side++) {
                Scrollbar bar = this.scrollbar(side);
                if (bar == null || !bar.grabbed(event.x(), event.y())) {
                    continue;
                }

                if (bar.onThumb(event.y())) {
                    // Keep the grab offset, so the thumb does not jump under the cursor.
                    this.dragGrabOffset = (int) event.y() - bar.thumbY();
                } else {
                    // On the track: bring the thumb to the cursor and carry on dragging.
                    this.dragGrabOffset = bar.thumb() / 2;
                    this.scrollTo(side, bar, (int) event.y() - this.dragGrabOffset);
                }

                this.draggingSide = side;
                return true;
            }

            for (Row row : this.rows) {
                if (row.contains(event.x(), event.y())) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
                    row.action().run();
                    return true;
                }
            }

            if (this.followHoveredItem(this.leftPageView) || this.followHoveredItem(this.rightPageView)) {
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    /** An item drawn on a page is a link to its own page. */
    private boolean followHoveredItem(@Nullable ManualPageView view) {
        ManualPageRef ref = linkedPage(view);
        if (ref == null) {
            return false;
        }

        this.follow(ref);
        return true;
    }

    /** Where the item under the cursor links to, or null when it links nowhere. */
    @Nullable
    private static ManualPageRef linkedPage(@Nullable ManualPageView view) {
        if (view == null) {
            return null;
        }

        ItemStack hovered = view.hoveredStack();
        return hovered.isEmpty() ? null : ManualLinks.pageFor(hovered);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.draggingSide >= 0) {
            Scrollbar bar = this.scrollbar(this.draggingSide);
            if (bar != null) {
                this.scrollTo(this.draggingSide, bar, (int) event.y() - this.dragGrabOffset);
            }
            return true;
        }

        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.draggingSide >= 0 && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            this.draggingSide = -1;
            return true;
        }

        return super.mouseReleased(event);
    }

    /** Scrolls whichever page the cursor is over. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0 && this.leftPageView != null && this.rightPageView != null) {
            int step = (int) -Math.signum(scrollY);

            if (this.leftPageView.isMouseOver(0, 0, this.style().contentWidth(), this.style().contentHeight())) {
                this.leftScroll = Math.clamp(this.leftScroll + step, 0, this.leftOverflow);
                return true;
            }
            if (this.rightPageView.isMouseOver(0, 0, this.style().contentWidth(), this.style().contentHeight())) {
                this.rightScroll = Math.clamp(this.rightScroll + step, 0, this.rightOverflow);
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** A list line, before it has been placed. */
    private record Entry(Component label, Runnable action) {
    }

    /** This frame's scrollbar geometry. */
    private record Scrollbar(int x, int top, int track, int thumbY, int thumb, int overflow) {

        /** Includes {@link #GRAB_SLOP} either side. */
        boolean grabbed(double mouseX, double mouseY) {
            return mouseX >= this.x - GRAB_SLOP && mouseX < this.x + BAR_WIDTH + GRAB_SLOP
                    && mouseY >= this.top && mouseY < this.top + this.track;
        }

        boolean onThumb(double mouseY) {
            return mouseY >= this.thumbY && mouseY < this.thumbY + this.thumb;
        }
    }

    private record Row(int x, int y, int width, int height, Component label, Runnable action) {

        boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + this.height;
        }
    }
}
