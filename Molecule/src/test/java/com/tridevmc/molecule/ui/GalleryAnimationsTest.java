package com.tridevmc.molecule.ui;

import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.IInternalCompoundUI;
import com.tridevmc.compound.ui.debug.LayoutDebugRenderer;
import com.tridevmc.compound.ui.element.Box;
import com.tridevmc.compound.ui.element.GradientRect;
import com.tridevmc.compound.ui.element.Label;
import com.tridevmc.compound.ui.element.Rect;
import com.tridevmc.compound.ui.element.Tabs;
import com.tridevmc.compound.ui.element.Text;
import com.tridevmc.compound.ui.event.MouseClickEvent;
import com.tridevmc.compound.ui.event.MouseDragEvent;
import com.tridevmc.compound.ui.event.MouseMoveEvent;
import com.tridevmc.compound.ui.event.MouseReleaseEvent;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.screen.ScreenContextFixture;
import com.tridevmc.compound.ui.tree.UITree;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyFloat;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MinecraftMockExtension.class)
class GalleryAnimationsTest {
    private UITree tree;
    private GalleryAnimations gallery;
    private IScreenContext context;
    private Matrix3x2fStack pose;

    @BeforeEach
    void setup() {
        this.tree = new UITree();
        this.gallery = spy(new GalleryAnimations());
        this.context = mock(IScreenContext.class);
        this.pose = new Matrix3x2fStack(16);
        when(this.context.getActiveStack()).thenReturn(this.pose);
        var font = Minecraft.getInstance().font;
        when(this.context.getFont()).thenReturn(font);
        when(font.width("DVD")).thenReturn(18);
        ICompositionScope.root(this.tree).e(this.gallery, scope -> scope.layout().fillMax());
        this.frame(0);
    }

    private void frame(long nanos) {
        when(this.context.frameNanos()).thenReturn(nanos);
        this.tree.layoutAndRender(380, 210, this.context);
    }

    @Test
    void renderFramesDoNotRecomposeOrMeasureAndRestorePoseAndClip() {
        var node = this.tree.getRoot();
        var measured = node.getMeasuredSize();
        clearInvocations(this.gallery, this.context);
        this.frame(500_000_000);
        this.frame(750_000_000);
        assertEquals(.75, this.gallery.model.timeline.elapsedSeconds());
        verify(this.gallery, never()).compose(any());
        verify(this.gallery, never()).measure(any(), any(), any());
        assertSame(measured, node.getMeasuredSize());
        assertEquals(new Matrix3x2f(), new Matrix3x2f(this.pose));
        verify(this.context, times(42)).enableScissor(anyInt(), anyInt(), anyInt(), anyInt());
        verify(this.context, times(42)).disableScissor();
        verify(this.context, times(2)).frameNanos();
    }

    @Test
    void movingDvdCanBeDraggedThroughTreeAndReleased() {
        this.frame(500_000_000);
        var dvd = this.gallery.model.dvd;
        var bounds = this.gallery.canvas.getBounds();
        int x = (int) (bounds.left() + dvd.x() + 2);
        int y = (int) (bounds.top() + AnimationGeometry.DVD_TOP + dvd.y() + 2);
        assertTrue(this.tree.dispatchClick(x, y, new MouseClickEvent(x, y, InputConstants.MOUSE_BUTTON_LEFT, false, false, false)));
        assertTrue(dvd.isDragging());
        assertTrue(this.tree.dispatchMouseDrag(900, 900, new MouseDragEvent(InputConstants.MOUSE_BUTTON_LEFT, 900, 900, 1, 1)));
        assertEquals(dvd.maxX(), dvd.x());
        assertEquals(dvd.maxY(), dvd.y());
        assertTrue(this.tree.dispatchMouseRelease(900, 900, new MouseReleaseEvent(900, 900, InputConstants.MOUSE_BUTTON_LEFT)));
        assertFalse(dvd.isDragging());
        this.frame(600_000_000);
        assertTrue(dvd.x() < dvd.maxX());
    }

    @Test
    void hoverPausesOnlyMarqueeAndSharedPauseResetAndSpeedWork() {
        var bounds = this.gallery.canvas.getBounds();
        int y = bounds.top() + 85;
        this.tree.dispatchMouseMove(20, y, new MouseMoveEvent(20, y, 0, 0));
        assertTrue(this.gallery.model.marquee.isPaused());
        this.frame(500_000_000);
        assertEquals(0, this.gallery.model.marquee.elapsedSeconds());
        assertEquals(.5, this.gallery.model.timeline.elapsedSeconds());
        this.gallery.model.togglePause();
        this.frame(1_000_000_000);
        assertEquals(.5, this.gallery.model.timeline.elapsedSeconds());
        this.gallery.model.reset();
        assertTrue(this.gallery.model.timeline.isPaused());
        assertEquals(0, this.gallery.model.timeline.elapsedSeconds());
        this.gallery.model.hover(false);
        this.gallery.model.togglePause();
        this.gallery.model.speed();
        this.frame(1_250_000_000);
        assertEquals(.5, this.gallery.model.timeline.elapsedSeconds());
    }

    @Test
    void detachRemountFreezesAndCompositionDeduplicates() {
        this.frame(500_000_000);
        this.tree.recomposeNode(this.tree.getRoot());
        this.frame(1_000_000_000);
        assertEquals(1, this.gallery.model.timeline.elapsedSeconds());
        this.tree.reset();
        this.frame(10_000_000_000L);
        ICompositionScope.root(this.tree).e(this.gallery, scope -> scope.layout().fillMax());
        this.frame(20_000_000_000L);
        assertEquals(1, this.gallery.model.timeline.elapsedSeconds());
        this.frame(20_500_000_000L);
        assertEquals(1.5, this.gallery.model.timeline.elapsedSeconds());
    }

    @Test
    void sampledSpinPoseAndPaletteRetargetAreIntermediateAndClickable() {
        var captured = new ArrayList<Matrix3x2f>();
        doAnswer(invocation -> {
            if (((Float) invocation.getArgument(2)) == 24F) captured.add(new Matrix3x2f(this.pose));
            return null;
        }).when(this.context).drawGradientRect(anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyInt(), anyInt());
        this.gallery.model.changePalette();
        this.frame(375_000_000);
        int sampled = this.gallery.model.palette.value();
        assertNotEquals(0xFF4285F4, sampled);
        assertNotEquals(0xFFFF7043, sampled);
        this.gallery.model.changePalette();
        assertEquals(sampled, this.gallery.model.palette.value());
        assertEquals(1, captured.size());
        assertEquals(Math.sqrt(.5), captured.getFirst().m00(), .00001);
        assertEquals(Math.sqrt(.5), captured.getFirst().m01(), .00001);
        var bounds = this.gallery.canvas.getBounds();
        int x = (int) (bounds.left() + bounds.width() * .22);
        int y = bounds.top() + 68;
        assertTrue(this.tree.dispatchClick(x, y, new MouseClickEvent(x, y, InputConstants.MOUSE_BUTTON_LEFT, false, false, false)));
        assertEquals(.125, this.gallery.model.spinTurns());
        this.frame(750_000_000);
        assertEquals(0, this.gallery.model.spinTurns(), .00001);
    }

    @Test
    void resizeRebasesAndDetachClearsDragAndHover() {
        this.frame(500_000_000);
        var dvd = this.gallery.model.dvd;
        long collisions = dvd.collisions();
        when(this.context.frameNanos()).thenReturn(1_000_000_000L);
        this.tree.layoutAndRender(70, 145, this.context);
        assertEquals(collisions, dvd.collisions());
        assertTrue(dvd.x() <= dvd.maxX());
        assertTrue(dvd.y() <= dvd.maxY());
        assertTrue(dvd.beginDrag(dvd.x(), dvd.y(), 18, 9));
        this.gallery.model.hover(true);
        this.tree.reset();
        assertFalse(dvd.isDragging());
        assertFalse(this.gallery.model.hovered);
    }

    @Test
    void reverseClicksAndSharedButtonPreserveRenderedPoses() {
        var spins = new ArrayList<Matrix3x2f>();
        var marquees = new ArrayList<Matrix3x2f>();
        doAnswer(invocation -> {
            if (((Float) invocation.getArgument(2)) == 24F) spins.add(new Matrix3x2f(this.pose));
            return null;
        }).when(this.context).drawGradientRect(anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyInt(), anyInt());
        doAnswer(invocation -> {
            var text = (Component) invocation.getArgument(0);
            if (text.getString().startsWith("Compound animations")) {
                marquees.add(new Matrix3x2f(this.pose));
            }
            return null;
        }).when(this.context).drawTextWithShadow(any(), anyFloat(), anyFloat());
        this.frame(750_000_000);
        var bounds = this.gallery.canvas.getBounds();
        int x = (int) (bounds.left() + bounds.width() * .22);
        int y = bounds.top() + 53;
        assertTrue(this.tree.dispatchClick(x, y, new MouseClickEvent(x, y, InputConstants.MOUSE_BUTTON_LEFT, false, false, false)));
        this.frame(750_000_000);
        assertEquals(spins.get(0), spins.get(1));
        assertEquals(marquees.get(0), marquees.get(1));
        y = bounds.top() + 85;
        assertTrue(this.tree.dispatchClick(20, y, new MouseClickEvent(20, y, InputConstants.MOUSE_BUTTON_LEFT, false, false, false)));
        this.frame(750_000_000);
        assertEquals(marquees.get(1), marquees.get(2));
        this.tree.dispatchMouseRelease(20, y, new MouseReleaseEvent(20, y, 0));
        this.frame(1_000_000_000);
        assertTrue(spins.getLast().m01() < spins.get(2).m01());
        assertTrue(marquees.getLast().m20() > marquees.get(2).m20());
        var beforeSpin = spins.getLast();
        var beforeMarquee = marquees.getLast();
        var reverse = this.tree.findNodesByElementType(Label.class).stream()
                .filter(node -> ((Label) node.getElement()).getText().getString().equals("Reverse"))
                .findFirst().orElseThrow().getParent().getParent().getParent().getElement().getBounds();
        x = reverse.left() + 1;
        y = reverse.top() + 1;
        assertTrue(this.tree.dispatchClick(x, y, new MouseClickEvent(x, y,
                InputConstants.MOUSE_BUTTON_LEFT, false, false, false)));
        this.frame(1_000_000_000);
        assertEquals(beforeSpin, spins.getLast());
        assertEquals(beforeMarquee, marquees.getLast());
        this.frame(1_250_000_000);
        assertTrue(spins.getLast().m01() > beforeSpin.m01());
        assertTrue(marquees.getLast().m20() < beforeMarquee.m20());
    }

    @Test
    void stationaryPointerPausesMarqueeAfterRecomposeResizeAndRemount() {
        int y = this.gallery.canvas.getBounds().top() + 85;
        this.tree.dispatchMouseMove(20, y, new MouseMoveEvent(20, y, 0, 0));
        assertTrue(this.gallery.model.marquee.isPaused());
        this.tree.recomposeNode(this.tree.getRoot());
        this.frame(0);
        assertTrue(this.gallery.model.hovered);
        assertTrue(this.gallery.model.marquee.isPaused());
        this.frame(500_000_000);
        assertEquals(0, this.gallery.model.marquee.elapsedSeconds());
        when(this.context.frameNanos()).thenReturn(500_000_000L);
        this.tree.layoutAndRender(400, 230, this.context);
        assertTrue(this.gallery.model.marquee.isPaused());
        this.tree.detachNode(this.tree.getNodeForElement(this.gallery.canvas));
        this.tree.recomposeNode(this.tree.getRoot());
        this.frame(500_000_000);
        assertTrue(this.gallery.model.hovered);
        assertTrue(this.gallery.model.marquee.isPaused());
        this.frame(1_000_000_000);
        assertEquals(0, this.gallery.model.marquee.elapsedSeconds());
        this.tree.dispatchMouseMove(20, y - 30, new MouseMoveEvent(20, y - 30, 0, -30));
        assertFalse(this.gallery.model.marquee.isPaused());
        this.frame(1_500_000_000);
        assertEquals(.5, this.gallery.model.marquee.elapsedSeconds());
    }

    @Test
    void stationaryPointerReentersHoveredMarqueeWhenTabReturns() {
        this.tree.reset();
        var tabs = new Tabs();
        tabs.addTab("Animations", scope -> scope.e(this.gallery, body -> body.layout().fillMax()));
        tabs.addTab("Other", scope -> scope.e(new Box(), body -> body.layout().fillMax()));
        ICompositionScope.root(this.tree).e(tabs, scope -> scope.layout().fillMax());
        this.frame(0);
        int y = this.gallery.canvas.getBounds().top() + 85;
        this.tree.dispatchMouseMove(20, y, new MouseMoveEvent(20, y, 0, 0));
        assertTrue(this.gallery.model.marquee.isPaused());
        tabs.setSelectedTab(1);
        this.frame(0);
        assertFalse(this.tree.hasNode(this.gallery.canvas));
        tabs.setSelectedTab(0);
        this.frame(0);
        assertTrue(this.tree.hasNode(this.gallery.canvas));
        assertTrue(this.gallery.model.hovered);
        assertTrue(this.gallery.model.marquee.isPaused());
        this.frame(500_000_000);
        assertEquals(0, this.gallery.model.marquee.elapsedSeconds());
    }

    @Test
    void fractionalFramesReachActualRenderExtractionWithoutSnapping() {
        var ui = mock(IInternalCompoundUI.class);
        var graphics = mock(GuiGraphicsExtractor.class);
        var minecraft = Minecraft.getInstance();
        when(ui.getMc()).thenReturn(minecraft);
        when(ui.getActiveGuiGraphics()).thenReturn(graphics);
        when(ui.getActiveStack()).thenReturn(this.pose);
        when(graphics.pose()).thenReturn(this.pose);
        var actual = spy(ScreenContextFixture.create(ui));
        var positions = new ArrayList<Float>();
        var canvasPositions = new ArrayList<Float>();
        doAnswer(invocation -> {
            if ("DVD".equals(((Component) invocation.getArgument(0)).getString())) {
                canvasPositions.add(this.pose.m20());
            }
            return invocation.callRealMethod();
        }).when(actual).drawText(any(), anyFloat(), anyFloat());
        doAnswer(invocation -> {
            int x = invocation.getArgument(2);
            int y = invocation.getArgument(3);
            positions.add(this.pose.transformPosition(x, y, new Vector2f()).x);
            return null;
        }).when(graphics).text(any(Font.class), any(FormattedCharSequence.class),
                anyInt(), anyInt(), anyInt(), anyBoolean());
        for (int frame = 0; frame < 12; frame++) {
            long nanos = frame * 8_333_333L;
            doAnswer(invocation -> nanos).when(actual).frameNanos();
            this.tree.layoutAndRender(380, 210, actual);
        }
        assertEquals(12, canvasPositions.size());
        for (int frame = 1; frame < canvasPositions.size(); frame++) {
            assertEquals(65 * 8_333_333 / 1_000_000_000.0,
                    canvasPositions.get(frame) - canvasPositions.get(frame - 1), .00001,
                    "DVD draw pose must advance on every 120 Hz frame");
        }
        verify(actual, times(12)).frameNanos();
        assertTrue(positions.stream().anyMatch(x -> Math.abs(x - Math.round(x)) > .01),
                "actual extraction must retain fractional animated positions");
        positions.clear();
        doAnswer(invocation -> {
            int x = invocation.getArgument(0);
            int y = invocation.getArgument(1);
            positions.add(this.pose.transformPosition(x, y, new Vector2f()).x);
            return null;
        }).when(graphics).fillGradient(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
        for (int frame = 0; frame < 12; frame++) {
            actual.drawRect(frame * .25F, 0, 10, 10, 0xFFFFFFFF);
        }
        for (int frame = 1; frame < positions.size(); frame++) {
            assertEquals(.25F, positions.get(frame) - positions.get(frame - 1), .00001F,
                    "fractional render steps must not stall or jump at integer boundaries");
        }
        positions.clear();
        for (int frame = 0; frame < 12; frame++) {
            actual.drawFormattedCharSequence(FormattedCharSequence.EMPTY,
                    frame * .25F, 0);
        }
        for (int frame = 1; frame < positions.size(); frame++) {
            assertEquals(.25F, positions.get(frame) - positions.get(frame - 1), .00001F,
                    "fractional text steps must not stall or jump at integer boundaries");
        }
        assertEquals(new Matrix3x2f(), new Matrix3x2f(this.pose));
    }

    @Test
    void actualPrimitiveNodesAreSelectedAndDebugUsesMovingRotatedGeometry() {
        var spins = this.tree.findNodesByElementType(GradientRect.class);
        assertEquals(2, spins.size());
        var spin = spins.stream()
                .filter(node -> node.getBounds().width() == AnimationGeometry.SPIN_HALF_SIZE * 2)
                .findFirst().orElseThrow();
        var layout = spin.getBounds();
        var first = spin.getFrameGeometry();
        this.frame(375_000_000);
        var rotated = spin.getFrameGeometry();
        assertEquals(layout, spin.getBounds());
        assertNotEquals(first.corners(), rotated.corners());
        int x = (int) ((rotated.left() + rotated.right()) / 2);
        int y = (int) ((rotated.top() + rotated.bottom()) / 2);
        assertSame(spin, this.tree.findNodeAt(x, y));
        var renderer = new LayoutDebugRenderer();
        clearInvocations(this.context);
        var debugCorners = new ArrayList<Vector2f>();
        doAnswer(invocation -> {
            int color = invocation.getArgument(4);
            if (color == 0xFFFFEB3B) {
                float left = invocation.getArgument(0);
                float top = invocation.getArgument(1);
                float width = invocation.getArgument(2);
                float height = invocation.getArgument(3);
                debugCorners.add(this.pose.transformPosition(left, top, new Vector2f()));
                debugCorners.add(this.pose.transformPosition(left + width, top, new Vector2f()));
                debugCorners.add(this.pose.transformPosition(left + width, top + height,
                        new Vector2f()));
                debugCorners.add(this.pose.transformPosition(left, top + height, new Vector2f()));
            }
            return null;
        }).when(this.context).drawRectOutline(anyFloat(), anyFloat(), anyFloat(), anyFloat(),
                anyInt(), anyInt());
        renderer.render(this.context, spin);
        for (int corner = 0; corner < 4; corner++) {
            var actual = debugCorners.get(debugCorners.size() - 4 + corner);
            var expected = rotated.corners().get(corner);
            assertEquals(expected.x(), actual.x, .0001);
            assertEquals(expected.y(), actual.y, .0001);
        }
        assertEquals(new Matrix3x2f(), new Matrix3x2f(this.pose));
        assertTrue(this.tree.findNodesByElementType(Text.class).size() >= 5);
        assertTrue(this.tree.findNodesByElementType(Rect.class).size() >= 2);
        var dvd = this.tree.findNodesByElementType(Text.class).stream()
                .filter(node -> node.getElement().getNarrationMessage().getString().equals("DVD"))
                .findFirst().orElseThrow();
        var before = dvd.getFrameGeometry();
        this.frame(500_000_000);
        assertTrue(dvd.getFrameGeometry().left() > before.left());
        this.gallery.canvas.toggleBlock();
        this.frame(500_000_000);
        assertSame(spin, this.tree.getNodeForElement(spin.getElement()));
        assertFalse(this.tree.hasNode(dvd.getElement()));
    }

    @Test
    void inverseRotationAndReflectionsHandleLargeStepsAndDegenerateBounds() {
        assertTrue(ReflectedMotion.containsRotated(0, 15, 0, 0, Math.PI / 4, 12));
        assertFalse(ReflectedMotion.containsRotated(12, 12, 0, 0, Math.PI / 4, 12));
        var axis = ReflectedMotion.advance(2, 10, 10, 8);
        assertEquals(6, axis.position());
        assertEquals(12, axis.collisions());
        assertEquals(10, axis.velocity());
        assertEquals(0, ReflectedMotion.advance(2, -10, 100, 0).position());
        var model = this.gallery.model;
        long collisions = model.dvd.collisions();
        model.advance(1, 1, 100, 100);
        assertEquals(0, model.dvd.x());
        assertEquals(0, model.dvd.y());
        assertEquals(collisions, model.dvd.collisions());
    }
}
