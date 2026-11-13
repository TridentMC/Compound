package com.tridevmc.compound.ui.visual;

import com.tridevmc.compound.ui.element.Rect;
import com.tridevmc.compound.ui.element.Stack;
import com.tridevmc.compound.ui.geometry.api.ITransform2D;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.tree.UITree;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BufferedImageScreenContextTest {
    @Test
    void productionTreePixelsMatchTransformedHitTestingAndOverlayClipping() {
        var tree = new UITree();
        var overlay = new Rect(0xFF4060FF);
        ICompositionScope.root(tree).e(new Stack(), screen -> screen.e(new Stack(), clipped -> {
            clipped.layout().fixedSize(20, 20).clip();
            clipped.transform(() -> ITransform2D.translation(10, 10));
            clipped.e(new Rect(0xFFFF4040), rect -> {
                rect.layout().fixedSize(10, 10);
                rect.transform(() -> ITransform2D.translation(25, 0));
            });
            clipped.e(overlay, rect -> {
                rect.layout().fixedSize(10, 10).layer(1);
                rect.transform(() -> ITransform2D.translation(25, 0));
            });
        }));
        try (var context = new BufferedImageScreenContext(80, 60)) {
            tree.layoutAndRender(80, 60, context);
            assertSame(tree.getNodeForElement(overlay), tree.findNodeAt(36, 11));
            assertEquals(0xFF4060FF, context.getImage().getRGB(36, 11));
            assertEquals(0, context.getImage().getRGB(11, 11));
            assertEquals(0, context.getImage().getRGB(46, 11));
            context.saveTo("build/screenshots/geometry-overlay.png");
        } finally {
            tree.reset();
        }
    }

    @Test
    void transformedDrawingUsesScreenSpaceClipAndRestoresIt() {
        try (var context = new BufferedImageScreenContext(40, 30)) {
            var pose = context.getActiveStack();
            pose.pushMatrix();
            pose.translate(10, 5);
            context.enableScissor(12, 7, 18, 13);
            context.drawRect(0, 0, 20, 20, 0xFFFF0000);
            context.enableScissor(15, 10, 25, 20);
            context.drawRect(0, 0, 20, 20, 0xFF0000FF);
            context.disableScissor();
            context.disableScissor();
            pose.popMatrix();
            context.drawRect(0, 0, 2, 2, 0xFF00FF00);

            var image = context.getImage();
            assertEquals(0xFF00FF00, image.getRGB(0, 0));
            assertEquals(0, image.getRGB(10, 5));
            assertEquals(0xFFFF0000, image.getRGB(12, 7));
            assertEquals(0xFF0000FF, image.getRGB(15, 10));
            assertEquals(0, image.getRGB(18, 13));
        }
    }

    @Test
    void formattedTextDrawsGlyphsAndRunColours() {
        try (var context = new BufferedImageScreenContext(120, 20)) {
            var red = FormattedCharSequence.forward("UI", Style.EMPTY.withColor(0xFF0000));
            context.drawFormattedCharSequence(red, 0, 0);
            int redPixels = 0;
            for (int y = 0; y < 20; y++) {
                for (int x = 0; x < 120; x++) {
                    if (context.getImage().getRGB(x, y) == 0xFFFF0000) {
                        assertTrue(x < 12, "Only the supplied two glyphs may be rendered");
                        redPixels++;
                    }
                }
            }
            assertTrue(redPixels > 0, "FormattedCharSequence must be visited to obtain its text");
        }
    }
}
