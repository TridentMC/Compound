/*
 * Copyright 2018 - 2024 TridentMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tridevmc.compound.ui.screen;

import com.tridevmc.compound.ui.EnumUILayer;
import com.tridevmc.compound.ui.IInternalCompoundUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.joml.Matrix3x2fStack;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public class CompoundScreenContext implements IScreenContext {

    private final IInternalCompoundUI ui;

    public CompoundScreenContext(IInternalCompoundUI ui) {
        this.ui = ui;
    }

    @Override
    public Matrix3x2fStack getActiveStack() {
        return this.ui.getActiveStack();
    }

    @Override
    public int getWidth() {
        return this.ui.getWidth();
    }

    @Override
    public int getHeight() {
        return this.ui.getHeight();
    }

    @Override
    public double getMouseX() {
        return this.ui.getMouseX();
    }

    @Override
    public double getMouseY() {
        return this.ui.getMouseY();
    }

    @Override
    public Minecraft getMc() {
        return this.ui.getMc();
    }

    @Override
    public Font getFont() {
        return this.getMc().font;
    }

    @Override
    public float getPartialTicks() {
        return this.getMc().getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }

    @Override
    public long getTicks() {
        return this.ui.getTicks();
    }

    @Override
    public Screen getActiveGui() {
        return this.ui.asGuiScreen();
    }

    @Override
    public void drawFormattedCharSequence(FormattedCharSequence processor, float x, float y) {
        this.ui.getActiveGuiGraphics().text(this.getFont(), processor, (int) x, (int) y, 0xFF404040, false);
    }

    @Override
    public void drawCenteredFormattedCharSequence(FormattedCharSequence processor, float x, float y) {
        int stringWidth = this.getFont().width(processor);
        this.drawFormattedCharSequence(processor, x - (stringWidth / 2F), y);
    }

    @Override
    public void drawFormattedCharSequenceWithShadow(FormattedCharSequence processor, float x, float y) {
        this.ui.getActiveGuiGraphics().text(this.getFont(), processor, (int) x, (int) y, 0xFF404040, true);
    }

    @Override
    public void drawCenteredFormattedCharSequenceWithShadow(FormattedCharSequence processor, float x, float y) {
        int stringWidth = this.getFont().width(processor);
        this.drawFormattedCharSequenceWithShadow(processor, x - (stringWidth / 2F), y);
    }

    @Override
    public void drawTexturedRect(Identifier texture, float x, float y, float width, float height, float minU, float minV, float maxU, float maxV) {
        var gg = this.ui.getActiveGuiGraphics();
        if (gg == null) return;

        gg.blit(texture, (int) x, (int) y, (int) (x + width), (int) (y + height), minU, maxU, minV, maxV);
    }

    @Override
    public void drawTooltip(List<Component> tooltip, int x, int y, Optional<TooltipComponent> extraComponents, Font font) {
        var gg = this.ui.getActiveGuiGraphics();
        if (gg == null) return;

        List<ClientTooltipComponent> components = net.neoforged.neoforge.client.ClientHooks.gatherTooltipComponents(
                ItemStack.EMPTY, tooltip, x, gg.guiWidth(), gg.guiHeight(), font);
        extraComponents.ifPresent(tc -> components.add(components.isEmpty() ? 0 : 1, ClientTooltipComponent.create(tc)));
        gg.setComponentTooltipForNextFrame(font, tooltip, x, y);
    }

    @Override
    public void drawProcessorAsTooltip(List<FormattedCharSequence> processors, int x, int y, Font font) {
        var gg = this.ui.getActiveGuiGraphics();
        if (gg == null) return;

        gg.setTooltipForNextFrame(font, processors, x, y);
    }

    @Override
    public void drawItemStack(ItemStack stack, float x, float y, float width, float height, String altText) {
        var gg = this.ui.getActiveGuiGraphics();
        if (gg == null) return;

        var font = IClientItemExtensions.of(stack).getFont(stack, IClientItemExtensions.FontContext.TOOLTIP);
        if (font == null) font = this.getFont();

        var pose = gg.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(width / 16F, height / 16F);

        gg.item(stack, 0, 0);
        gg.itemDecorations(font, stack, 0, 0, altText);

        pose.popMatrix();
    }

    @Override
    public void drawGradientRect(float x, float y, float width, float height, int startColour, int endColour) {
        var gg = this.ui.getActiveGuiGraphics();
        if (gg == null) return;

        gg.fillGradient((int) x, (int) y, (int) (x + width), (int) (y + height), startColour, endColour);
    }

    @Override
    public void sendChatMessage(String message) {
        this.sendChatMessage(message, true);
    }

    @Override
    public void sendChatMessage(String message, boolean addToChat) {
        if (addToChat) {
            this.getMc().gui.getChat().addClientSystemMessage(Component.translatable(message));
        }
    }

    @Override
    public void openWebLink(URI url) {
        try {
            java.awt.Desktop.getDesktop().browse(url);
        } catch (Exception e) {
            // Ignore browse errors
        }
    }

    @Override
    public boolean isShiftDown() {
        return this.getMc().hasShiftDown();
    }

    @Override
    public boolean isAltDown() {
        return this.getMc().hasAltDown();
    }

    @Override
    public float[] getRGBA(int colour) {
        float r = (float) (colour >> 16 & 255) / 255.0F;
        float g = (float) (colour >> 8 & 255) / 255.0F;
        float b = (float) (colour & 255) / 255.0F;
        float a = (float) (colour >> 24 & 255) / 255.0F;
        return new float[]{r, g, b, a};
    }

    @Override
    public EnumUILayer getCurrentLayer() {
        return this.ui.getCurrentLayer();
    }

    @Override
    public void enableScissor(int x, int y, int right, int bottom) {
        var gg = this.ui.getActiveGuiGraphics();
        if (gg != null) {
            gg.enableScissor(x, y, right, bottom);
        }
    }

    @Override
    public void disableScissor() {
        var gg = this.ui.getActiveGuiGraphics();
        if (gg != null) {
            gg.disableScissor();
        }
    }

}
