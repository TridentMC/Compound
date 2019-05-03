package com.tridevmc.compound.ui.element;

import com.tridevmc.compound.ui.ICompoundUI;
import com.tridevmc.compound.ui.Rect2D;
import com.tridevmc.compound.ui.UVData;
import com.tridevmc.compound.ui.layout.ILayout;
import com.tridevmc.compound.ui.layout.LayoutNone;
import com.tridevmc.compound.ui.screen.IScreenContext;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

public class ElementSlot implements IElement {

    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/container/inventory.png");
    private Rect2D dimensions;
    private ILayout layout;
    private Slot slot;
    private boolean drawOverlay;
    private boolean drawUnderlay;
    private ItemStack displayStack;
    private String displayString;

    public ElementSlot(@Nonnull Rect2D dimensions, @Nonnull Slot slot) {
        this(dimensions, new LayoutNone(), slot);
    }

    public ElementSlot(@Nonnull Rect2D dimensions, @Nonnull ILayout layout, @Nonnull Slot slot) {
        this.dimensions = dimensions;
        this.layout = layout;
        this.slot = slot;
    }

    @Override
    public void drawBackground(ICompoundUI ui) {
        IScreenContext screen = ui.getScreenContext();
        Rect2D rect = this.getTransformedDimensions(screen);
        screen.bindTexture(TEXTURE);
        screen.drawTexturedRect(rect, new UVData(8, 8), new UVData(8, 8));
        screen.drawTexturedRect(new Rect2D(rect.getX(), rect.getY(), rect.getWidth() - 1, 1),
                new UVData(7, 7), new UVData(7, 7));
        screen.drawTexturedRect(new Rect2D(rect.getX(), rect.getY(), 1, rect.getHeight() - 1),
                new UVData(7, 7), new UVData(7, 7));

        screen.drawTexturedRect(new Rect2D(rect.getX() + rect.getWidth() - 1, rect.getY() + 1, 1, rect.getHeight() - 1),
                new UVData(24, 8), new UVData(24, 8));
        screen.drawTexturedRect(new Rect2D(rect.getX() + 1, rect.getY() + rect.getHeight() - 1, rect.getWidth() - 1, 1),
                new UVData(24, 8), new UVData(24, 8));
    }

    @Override
    public void drawForeground(ICompoundUI ui) {
        if (this.drawUnderlay) {
            this.drawHighlight(ui.getScreenContext());
        }

        Rect2D rect = this.getTransformedDimensions(ui.getScreenContext());
        rect = new Rect2D(rect.getX() + 1, rect.getY() + 1, rect.getWidth() - 2, rect.getHeight() - 2);
        ui.getScreenContext().drawItemStack(this.displayStack, rect, this.displayString);
    }

    @Override
    public void drawOverlay(ICompoundUI ui) {
        if (this.drawOverlay) {
            this.drawHighlight(ui.getScreenContext());
            if (ui.getScreenContext().getMc().player.inventory.getItemStack().isEmpty() && this.slot.getHasStack()) {
                this.drawTooltip(ui.getScreenContext());
            }
        }

        this.reset();
    }

    private void drawHighlight(IScreenContext screen) {
        Rect2D highlightArea = this.getTransformedDimensions(screen).offset(new Rect2D(1, 1, -1, -1));
        GlStateManager.disableLighting();
        GlStateManager.disableDepthTest();
        GlStateManager.colorMask(true, true, true, false);
        int slotColor = -2130706433;
        screen.drawRect(highlightArea, slotColor);
        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.enableLighting();
        GlStateManager.enableDepthTest();
    }

    private void drawTooltip(IScreenContext screen) {
        screen.drawTooltip(this.displayStack, (int) screen.getMouseX(), (int) screen.getMouseY());
    }

    private void reset() {
        this.drawOverlay = false;
        this.drawUnderlay = false;
        this.displayStack = this.slot.getStack();
        this.displayString = null;
    }

    @Nonnull
    @Override
    public Rect2D getDimensions() {
        return this.dimensions;
    }

    @Override
    public void setDimensions(@Nonnull Rect2D dimensions) {
        this.dimensions = dimensions;
    }

    @Nonnull
    @Override
    public ILayout getLayout() {
        return this.layout;
    }

    @Override
    public void setLayout(@Nonnull ILayout layout) {
        this.layout = layout;
    }

    public boolean isMouseOverSlot(IScreenContext screen) {
        Rect2D rect = this.getTransformedDimensions(screen).offsetSize(-1, -1);
        return rect.isPointInRect(screen.getMouseX(), screen.getMouseY());
    }

    public void setDrawOverlay(boolean drawOverlay) {
        this.drawOverlay = drawOverlay;
    }

    public void setDisplayStack(ItemStack displayStack) {
        this.displayStack = displayStack;
    }

    public void setDisplayString(String displayString) {
        this.displayString = displayString;
    }

    public void setDrawUnderlay(boolean drawUnderlay) {
        this.drawUnderlay = drawUnderlay;
    }
}
