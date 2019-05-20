package com.tridevmc.compound.ui.element;

import com.tridevmc.compound.ui.ICompoundUI;
import com.tridevmc.compound.ui.Rect2D;
import com.tridevmc.compound.ui.UVData;
import com.tridevmc.compound.ui.layout.ILayout;
import com.tridevmc.compound.ui.layout.LayoutNone;
import com.tridevmc.compound.ui.screen.IScreenContext;
import net.minecraft.util.ResourceLocation;

/**
 * A resizable box element to add to UIs, useful for backgrounds to place elements on top of.
 */
public class ElementBox extends Element {

    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/container/inventory.png");

    public ElementBox(Rect2D dimensions) {
        this(dimensions, new LayoutNone());
    }

    public ElementBox(Rect2D dimensions, ILayout layout) {
        super(dimensions, layout);
    }

    @Override
    public void drawBackground(ICompoundUI ui) {
        IScreenContext screen = ui.getScreenContext();
        screen.bindTexture(TEXTURE);
        this.drawCorners(ui);
        this.drawConnectingLines(ui);
        this.drawMiddle(ui);
    }


    private void drawCorners(ICompoundUI ui) {
        IScreenContext screen = ui.getScreenContext();
        Rect2D rect = this.getTransformedDimensions(screen);
        double xOff = rect.getX();
        double yOff = rect.getY();
        double width = rect.getWidth();
        double height = rect.getHeight();

        // top-left -> top-right -> bottom-left -> bottom-right
        screen.drawTexturedRect(new Rect2D(xOff, yOff, 4, 4), new UVData(0, 0));
        screen.drawTexturedRect(new Rect2D(xOff + width - 4, yOff, 4, 4),
                new UVData(172, 0));
        screen.drawTexturedRect(new Rect2D(xOff, yOff + height - 4, 4, 4),
                new UVData(0, 162));
        screen.drawTexturedRect(new Rect2D(xOff + width - 4, yOff + height - 4, 4, 4),
                new UVData(172, 162));
    }

    private void drawConnectingLines(ICompoundUI ui) {
        IScreenContext screen = ui.getScreenContext();
        Rect2D rect = this.getTransformedDimensions(screen);
        double xOff = rect.getX();
        double yOff = rect.getY();
        double width = rect.getWidth();
        double height = rect.getHeight();

        // left -> right -> up -> down
        screen.drawTexturedRect(new Rect2D(xOff, yOff + 4, 4, height - 8),
                new UVData(0, 4), new UVData(4, 5));
        screen.drawTexturedRect(new Rect2D(xOff + width - 4, yOff + 4, 4, height - 8),
                new UVData(172, 4), new UVData(176, 5));
        screen.drawTexturedRect(new Rect2D(xOff + 4, yOff, width - 8, 4),
                new UVData(4, 0), new UVData(5, 4));
        screen.drawTexturedRect(new Rect2D(xOff + 4, yOff + height - 4, width - 8, 4),
                new UVData(4, 162), new UVData(5, 166));

    }

    private void drawMiddle(ICompoundUI ui) {
        IScreenContext screen = ui.getScreenContext();
        Rect2D rect = this.getTransformedDimensions(screen);
        double xOff = rect.getX();
        double yOff = rect.getY();
        double width = rect.getWidth();
        double height = rect.getHeight();

        screen.drawTexturedRect(new Rect2D(xOff + 4, yOff + 4, width - 8, height - 8),
                new UVData(4, 4), new UVData(5, 5));
    }
}
