package com.tridevmc.compound.ui.element.button;

import com.google.common.collect.Lists;
import com.tridevmc.compound.ui.ICompoundUI;
import com.tridevmc.compound.ui.Rect2D;
import com.tridevmc.compound.ui.UVData;
import com.tridevmc.compound.ui.element.Element;
import com.tridevmc.compound.ui.layout.ILayout;
import com.tridevmc.compound.ui.listeners.IMousePressListener;
import com.tridevmc.compound.ui.screen.IScreenContext;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;

import java.util.List;

/**
 * A resizable button element to add to UIs, add hover and press listeners to add functionality.
 */
public class ElementButton extends Element {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/widgets.png");
    private boolean isEnabled;
    private boolean isVisible;
    private boolean isHovered;
    private List<IButtonPressListener> pressListeners;
    private List<IButtonHoverListener> hoverListeners;

    public ElementButton(Rect2D dimensions, ILayout layout) {
        super(dimensions, layout);
        this.isEnabled = true;
        this.isVisible = true;
        this.isHovered = false;
        this.pressListeners = Lists.newArrayList();
        this.hoverListeners = Lists.newArrayList();
    }

    @Override
    public void drawBackground(ICompoundUI ui) {
        IScreenContext screen = ui.getScreenContext();
        Rect2D dimensions = this.getTransformedDimensions(screen);
        double mouseX = screen.getMouseX();
        double mouseY = screen.getMouseY();
        if (dimensions.isPointInRect(mouseX, mouseY) && this.canPress()) {
            if (!this.isHovered) {
                this.hoverListeners.forEach((l) -> l.onButtonHover(mouseX, mouseY, true));
                this.isHovered = true;
            }
        } else {
            if (this.isHovered) {
                this.hoverListeners.forEach((l) -> l.onButtonHover(mouseX, mouseY, false));
                this.isHovered = false;
            }
        }

        if (this.isVisible()) {
            screen.bindTexture(TEXTURE);
            this.drawCorners(ui);
            this.drawConnectingLines(ui);
            this.drawMiddle(ui);
        }
    }

    @Override
    public void initElement(ICompoundUI ui) {
        ui.addListener((IMousePressListener) this::onMousePress);
    }

    private UVData getUvData() {
        int uvX = 0, uvY = 46;

        if (this.isEnabled()) {
            uvY += 20;
            if (this.isHovered) {
                uvY += 20;
            }
        }

        return new UVData(uvX, uvY);
    }

    private void drawCorners(ICompoundUI ui) {
        IScreenContext screen = ui.getScreenContext();
        Rect2D rect = this.getTransformedDimensions(screen);
        UVData uvData = this.getUvData();
        double xOff = rect.getX();
        double yOff = rect.getY();
        double width = rect.getWidth();
        double height = rect.getHeight();

        // top-left -> top-right -> bottom-left -> bottom-right
        screen.drawTexturedRect(new Rect2D(xOff, yOff, 3, 3), uvData);
        screen.drawTexturedRect(new Rect2D(xOff + width - 3, yOff, 3, 3),
                new UVData(uvData.getU() + 197, uvData.getV()));
        screen.drawTexturedRect(new Rect2D(xOff, yOff + height - 3, 3, 3),
                new UVData(uvData.getU(), uvData.getV() + 17));
        screen.drawTexturedRect(new Rect2D(xOff + width - 3, yOff + height - 3, 3, 3),
                new UVData(uvData.getU() + 197, uvData.getV() + 17));
    }

    private void drawConnectingLines(ICompoundUI ui) {
        IScreenContext screen = ui.getScreenContext();
        Rect2D rect = this.getTransformedDimensions(screen);
        UVData uvData = this.getUvData();
        double xOff = rect.getX();
        double yOff = rect.getY();
        double width = rect.getWidth();
        double height = rect.getHeight();

        // left -> right -> top -> bottom
        screen.drawTiledTexturedRect(new Rect2D(xOff, yOff + 3, 3, height - 6),
                new UVData(uvData.getU(), uvData.getV() + 3),
                new UVData(uvData.getU() + 3, uvData.getV() + 17));

        screen.drawTiledTexturedRect(new Rect2D(xOff + width - 3, yOff + 3, 3, height - 6),
                new UVData(uvData.getU() + 197, uvData.getV() + 3),
                new UVData(uvData.getU() + 200, uvData.getV() + 17));

        screen.drawTiledTexturedRect(new Rect2D(xOff + 3, yOff, width - 6, 3),
                new UVData(uvData.getU() + 3, uvData.getV()),
                new UVData(uvData.getU() + 197, uvData.getV() + 3));

        screen.drawTiledTexturedRect(new Rect2D(xOff + 3, yOff + height - 3, width - 6, 3),
                new UVData(uvData.getU() + 3, uvData.getV() + 17),
                new UVData(uvData.getU() + 197, uvData.getV() + 20));

    }

    private void drawMiddle(ICompoundUI ui) {
        IScreenContext screen = ui.getScreenContext();
        Rect2D rect = this.getTransformedDimensions(screen);
        UVData uvData = this.getUvData();
        double xOff = rect.getX();
        double yOff = rect.getY();
        double width = rect.getWidth();
        double height = rect.getHeight();

        screen.drawTiledTexturedRect(new Rect2D(xOff + 3, yOff + 3, width - 6, height - 6),
                new UVData(uvData.getU() + 3, uvData.getV() + 3),
                new UVData(uvData.getU() + 197, uvData.getV() + 17));
    }

    public void addPressListener(IButtonPressListener listener) {
        this.pressListeners.add(listener);
    }

    public void addHoverListener(IButtonHoverListener listener) {
        this.hoverListeners.add(listener);
    }

    public void disable() {
        this.isEnabled = false;
    }

    public void enable() {
        this.isEnabled = true;
    }

    public void hide() {
        this.isVisible = false;
    }

    public void show() {
        this.isVisible = true;
    }

    public boolean isVisible() {
        return this.isVisible;
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }

    private void onMousePress(IScreenContext screen, double x, double y, int button) {
        if (button != 0)
            return;

        Rect2D dimensions = this.getTransformedDimensions(screen);
        if (dimensions.isPointInRect(x, y) && this.canPress()) {
            if (!this.isHovered) {
                this.hoverListeners.forEach((l) -> l.onButtonHover(x, y, true));
                this.isHovered = true;
            }
            screen.getMc().getSoundHandler().play(SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            this.pressListeners.forEach((l) -> l.onButtonPress(x, y));
        }
    }

    private boolean canPress() {
        return this.isVisible && this.isEnabled;
    }
}
