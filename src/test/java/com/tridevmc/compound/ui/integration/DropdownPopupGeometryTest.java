package com.tridevmc.compound.ui.integration;

import com.mojang.blaze3d.platform.InputConstants;
import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.element.Dropdown;
import com.tridevmc.compound.ui.element.ListItem;
import com.tridevmc.compound.ui.element.ScrollArea;
import com.tridevmc.compound.ui.element.Stack;
import com.tridevmc.compound.ui.element.Surface;
import com.tridevmc.compound.ui.event.MouseClickEvent;
import com.tridevmc.compound.ui.event.MouseScrollEvent;
import com.tridevmc.compound.ui.geometry.api.FrameGeometry;
import com.tridevmc.compound.ui.geometry.api.ITransform2D;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.tree.UITree;
import com.tridevmc.compound.ui.visual.BufferedImageScreenContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(MinecraftMockExtension.class)
class DropdownPopupGeometryTest {
    private final IScreenContext context = mock(IScreenContext.class);

    @Test
    void translatedPopupOpensAboveAndAllOptionsRemainReachable() {
        var dropdown = dropdown();
        var tree = this.mount(dropdown, () -> ITransform2D.translation(0, 250));
        try {
            this.open(tree, dropdown);
            var popup = popupGeometry(tree);
            assertInsideViewport(popup);
            assertEquals(120, popup.top(), 1e-8);
            assertEquals(250, popup.bottom(), 1e-8);
            var last = tree.findNodesByElementType(ListItem.class).getLast().getFrameGeometry();
            this.click(tree, (int) last.left() + 5, (int) last.top() + 5);
            assertEquals("7", dropdown.getSelected());
            assertFalse(dropdown.isOpen());
        } finally {
            tree.reset();
        }
    }

    @Test
    void scaledPopupClampsAtTheHorizontalEdgeAndMeasuresItsActualScrollViewport() {
        var dropdown = dropdown();
        var tree = this.mount(dropdown, () -> ITransform2D.of(350, 240, 0, 2, 2, 0, 0));
        try {
            this.open(tree, dropdown);
            var popup = popupGeometry(tree);
            assertInsideViewport(popup);
            assertEquals(200, popup.left(), 1e-8);
            assertEquals(400, popup.right(), 1e-8);
            assertEquals(240, popup.bottom(), 1e-8);
            assertTrue(tree.findNodesByElementType(ScrollArea.class).getFirst()
                    .getElement() instanceof ScrollArea area && area.getMaxScrollY() > 0);
            try (var image = new BufferedImageScreenContext(400, 300)) {
                tree.renderTree(image);
                assertEquals(0xFFA0A0A0, image.getImage().getRGB(200, 4));
                assertEquals(0, image.getImage().getRGB(199, 4));
                image.saveTo("build/screenshots/transformed-dropdown.png");
            }
        } finally {
            tree.reset();
        }
    }

    @Test
    void stableOpenPopupDoesNotInvalidateAncestorLayoutEveryFrame() {
        var dropdown = dropdown();
        var measures = new AtomicInteger();
        var tree = this.mount(dropdown, () -> ITransform2D.translation(20, 10), measures);
        try {
            this.open(tree, dropdown);
            tree.prepareFrame(400, 300, this.context);
            int before = measures.get();
            for (int frame = 0; frame < 3; frame++) {
                tree.prepareFrame(400, 300, this.context);
            }
            assertEquals(before, measures.get());
        } finally {
            tree.reset();
        }
    }

    @Test
    void openPopupTracksCurrentFrameTranslationAndScaleWithoutLayoutInvalidation() {
        var dropdown = dropdown();
        var transform = new AtomicReference<>(ITransform2D.translation(20, 10));
        var samples = new AtomicInteger();
        var tree = this.mount(dropdown, () -> {
            samples.incrementAndGet();
            return transform.get();
        });
        try {
            this.open(tree, dropdown);
            assertEquals(30, popupGeometry(tree).top(), 1e-8);
            int before = samples.get();
            transform.set(ITransform2D.of(350, 240, 0, 2, 2, 0, 0));
            tree.prepareFrame(400, 300, this.context);
            assertEquals(before + 1, samples.get());
            var popup = popupGeometry(tree);
            assertInsideViewport(popup);
            assertEquals(400, popup.right(), 1e-8);
            assertEquals(240, popup.bottom(), 1e-8);
            var first = tree.findNodesByElementType(ListItem.class).getFirst().getFrameGeometry();
            this.click(tree, (int) first.left() + 5, (int) first.top() + 5);
            assertEquals("0", dropdown.getSelected());
        } finally {
            tree.reset();
        }
    }

    @Test
    void reflectedFractionalScaleStillKeepsPopupInsideViewport() {
        var dropdown = dropdown();
        var tree = this.mount(dropdown, () -> ITransform2D.of(399.5, 29.5, 0, -1.5, -.75, 0, 0));
        try {
            this.open(tree, dropdown);
            assertInsideViewport(popupGeometry(tree));
        } finally {
            tree.reset();
        }
    }

    @Test
    void fractionalTranslationBackdropDismissesAtViewportEdge() {
        var dropdown = dropdown();
        var tree = this.mount(dropdown, () -> ITransform2D.translation(100.25, 40.25));
        try {
            this.open(tree, dropdown);
            this.click(tree, 0, 299);
            assertFalse(dropdown.isOpen());
        } finally {
            tree.reset();
        }
    }

    @Test
    void initialSelectedOptionRevealsUsingTheTransformAtTheOpeningFrame() {
        var dropdown = dropdown();
        dropdown.setSelected("7");
        var transform = new AtomicReference<>(ITransform2D.translation(20, 10));
        var tree = this.mount(dropdown, transform::get);
        try {
            this.click(tree, 25, 15);
            transform.set(ITransform2D.of(50, 100, 0, 3, 3, 0, 0));
            tree.prepareFrame(400, 300, this.context);
            assertLastOptionVisible(tree);
        } finally {
            tree.reset();
        }
    }

    @Test
    void shrinkingPopupRevealsFocusedOptionAndPreservesLaterWheelScrolling() {
        var dropdown = dropdown();
        dropdown.setSelected("7");
        var transform = new AtomicReference<>(ITransform2D.translation(20, 10));
        var tree = this.mount(dropdown, transform::get);
        try {
            this.open(tree, dropdown);
            transform.set(ITransform2D.of(50, 100, 0, 3, 3, 0, 0));
            tree.prepareFrame(400, 300, this.context);
            assertLastOptionVisible(tree);
            var area = (ScrollArea) tree.findNodesByElementType(ScrollArea.class).getFirst().getElement();
            int before = area.getScrollYState().get();
            assertTrue(tree.dispatchScroll(80, 180, new MouseScrollEvent(80, 180, 1)));
            tree.prepareFrame(400, 300, this.context);
            tree.prepareFrame(400, 300, this.context);
            assertEquals(before - 16, area.getScrollYState().get());
        } finally {
            tree.reset();
        }
    }

    private static void assertLastOptionVisible(UITree tree) {
        var last = tree.findNodesByElementType(ListItem.class).getLast().getFrameGeometry();
        var viewport = tree.findNodesByElementType(ScrollArea.class).getFirst().getFrameGeometry();
        assertTrue(last.top() >= viewport.top(), "The selected option starts inside the viewport");
        assertTrue(last.bottom() <= viewport.bottom(), "The selected option ends inside the viewport");
    }

    @Test
    void rotatedPopupReportsTheNativeClippingLimitation() {
        var dropdown = dropdown();
        var tree = this.mount(dropdown, () -> ITransform2D.of(100, 100, .1, 1, 1, 0, 0));
        try {
            var anchor = dropdown.getNode().getFrameGeometry();
            this.click(tree, (int) ((anchor.left() + anchor.right()) / 2),
                    (int) ((anchor.top() + anchor.bottom()) / 2));
            var error = assertThrows(UnsupportedOperationException.class,
                    () -> tree.prepareFrame(400, 300, this.context));
            assertTrue(error.getMessage().contains("stencil backend"));
        } finally {
            tree.reset();
        }
    }

    private UITree mount(Dropdown<String> dropdown, Supplier<ITransform2D> transform) {
        return this.mount(dropdown, transform, new AtomicInteger());
    }

    private UITree mount(Dropdown<String> dropdown, Supplier<ITransform2D> transform,
            AtomicInteger measures) {
        var tree = new UITree();
        ICompositionScope.root(tree).e(new Stack() {
            @Override
            public Size measure(Constraints constraints, LayoutProperties props, List<Size> children) {
                measures.incrementAndGet();
                return super.measure(constraints, props, children);
            }
        }, root -> {
            root.layout().fillMax();
            root.e(new Stack(), translated -> {
                translated.layout().fixedSize(100, 20);
                translated.transform(transform);
                translated.e(dropdown, control -> control.layout().fixedSize(100, 20));
            });
        });
        tree.prepareFrame(400, 300, this.context);
        return tree;
    }

    private void open(UITree tree, Dropdown<String> dropdown) {
        var anchor = dropdown.getNode().getFrameGeometry();
        this.click(tree, (int) ((anchor.left() + Math.min(400, anchor.right())) / 2),
                (int) ((anchor.top() + anchor.bottom()) / 2));
        tree.prepareFrame(400, 300, this.context);
        assertTrue(dropdown.isOpen());
    }

    private void click(UITree tree, int x, int y) {
        assertTrue(tree.dispatchClick(x, y, new MouseClickEvent(
                x, y, InputConstants.MOUSE_BUTTON_LEFT, false, false, false)));
    }

    private static Dropdown<String> dropdown() {
        return new Dropdown<>(List.of("0", "1", "2", "3", "4", "5", "6", "7"));
    }

    private static FrameGeometry popupGeometry(UITree tree) {
        return tree.findNodesByElementType(Surface.class).getFirst().getFrameGeometry();
    }

    private static void assertInsideViewport(FrameGeometry geometry) {
        assertTrue(geometry.left() >= -1e-8);
        assertTrue(geometry.top() >= -1e-8);
        assertTrue(geometry.right() <= 400 + 1e-8);
        assertTrue(geometry.bottom() <= 300 + 1e-8);
    }
}
