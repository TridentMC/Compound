/*
 * Copyright 2018 - 2022 TridentMC
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

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.tridevmc.compound.ui.EnumUILayer;
import com.tridevmc.compound.ui.sprite.IScreenSprite;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Provides useful methods and variables for drawing a UI and it's elements.
 * This should be used in place of any direct calls to minecraft GUI methods.
 */
public interface IPrimitiveScreenContext {

    default IScreenContext asScreenContext() {
        return (IScreenContext) this;
    }

    /**
     * Gets the active matrix stack in use for the current draw.
     *
     * @return the active matrix stack for the current draw.
     */
    PoseStack getActiveStack();

    /**
     * Gets the width of the screen.
     *
     * @return the width of the screen.
     */
    int getWidth();

    /**
     * Gets the height of the screen.
     *
     * @return the height of the screen.
     */
    int getHeight();

    /**
     * Gets the current x coordinate of the mouse on the screen.
     *
     * @return the current x coordinate of the mouse.
     */
    double getMouseX();

    /**
     * Gets the current y coordinate of the mouse on the screen.
     *
     * @return the current y coordinate of the mouse.
     */
    double getMouseY();

    /**
     * Gets the partial tick value for the current frame.
     *
     * @return the current partial tick value.
     */
    float getPartialTicks();

    /**
     * Gets the amount of ticks that have passed since the ui was opened.
     *
     * @return the amount of ticks that have passed since the ui was opened.
     */
    long getTicks();

    /**
     * Gets the active gui that's currently being displayed.
     *
     * @return the active gui currently being displayed.
     */
    Screen getActiveGui();

    /**
     * Gets the current Minecraft game instance.
     *
     * @return the game instance.
     */
    Minecraft getMc();

    /**
     * Gets the font renderer used by the game.
     *
     * @return the font renderer used by the game.
     */
    Font getFont();

    /**
     * Binds the given texture to the texture manager.
     *
     * @param texture the texture to bind.
     */
    default void bindTexture(ResourceLocation texture) {
        RenderSystem.setShaderTexture(0, texture);
    }

    /**
     * Binds the given texture to the texture manager.
     *
     * @param sprite the sprite to bind.
     */
    default void bindTexture(IScreenSprite sprite) {
        this.bindTexture(sprite.getTextureLocation());
    }

    /**
     * Draws a solid single colour rect on the screen matching the provided rect data.
     *
     * @param colour the colour of the rect to draw.
     */
    default void drawRect(float x, float y, float width, float height, int colour) {
        this.drawRect(x, y, width, height, colour, 0);
    }

    /**
     * Draws a solid single colour rect on the screen matching the provided rect data.
     *
     * @param colour the colour of the rect to draw.
     * @param zLevel the z level to draw the rect at.
     */
    default void drawRect(float x, float y, float width, float height, int colour, int zLevel) {
        this.drawGradientRect(x, y, width, height, colour, colour, zLevel);
    }

    /**
     * Draws a solid gradient rect on the screen matching the provided rect data.
     *
     * @param startColour the colour at the beginning of the gradient.
     * @param endColour   the colour at the end of the gradient.
     */
    default void drawGradientRect(float x, float y, float width, float height, int startColour, int endColour) {
        this.drawGradientRect(x, y, width, height, startColour, endColour, 0);
    }

    /**
     * Draws a solid gradient rect on the screen matching the provided rect data.
     *
     * @param startColour the colour at the beginning of the gradient.
     * @param endColour   the colour at the end of the gradient.
     * @param zLevel      the z level to draw the rect at.
     */
    default void drawGradientRect(float x, float y, float width, float height, int startColour, int endColour, int zLevel) {
        float[] startColourUnpacked = this.getRGBA(startColour);
        float r1 = startColourUnpacked[0];
        float g1 = startColourUnpacked[1];
        float b1 = startColourUnpacked[2];
        float a1 = startColourUnpacked[3];

        float[] endColourUnpacked = this.getRGBA(endColour);
        float r2 = endColourUnpacked[0];
        float g2 = endColourUnpacked[1];
        float b2 = endColourUnpacked[2];
        float a2 = endColourUnpacked[3];

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        var pose = this.getActiveStack().last().pose();
        var tesselator = Tesselator.getInstance();
        var bufferbuilder = tesselator.getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferbuilder.vertex(pose, x + width, y, zLevel)
                .color(r1, g1, b1, a1)
                .endVertex();
        bufferbuilder.vertex(pose, x, y, zLevel)
                .color(r1, g1, b1, a1)
                .endVertex();
        bufferbuilder.vertex(pose, x, y + height, zLevel)
                .color(r2, g2, b2, a2)
                .endVertex();
        bufferbuilder.vertex(pose, x + width, y + height, zLevel)
                .color(r2, g2, b2, a2)
                .endVertex();
        tesselator.end();
        RenderSystem.disableBlend();
    }

    /**
     * Draws the given string on the screen at the given position.
     *
     * @param text   the string to draw.
     * @param x      the x position to draw the string at.
     * @param y      the y position to draw the string at.
     * @param colour the colour to draw the string in.
     */
    default void drawString(String text, float x, float y, int colour) {
        this.drawText(Component.translatable(text).withStyle(s -> s.withColor(colour)), x, y);
    }

    /**
     * Draws the given string on the screen with the middle of the string centered on the given position.
     *
     * @param text   the string to draw.
     * @param x      the x position to draw the string at.
     * @param y      the y position to draw the string at.
     * @param colour the colour to draw the string in.
     */
    default void drawCenteredString(String text, float x, float y, int colour) {
        this.drawCenteredText(Component.translatable(text).withStyle(s -> s.withColor(colour)), x, y);
    }

    /**
     * Draws the given string on the screen with the middle of the string centered on the given position, with a drop shadow applied.
     *
     * @param text   the string to draw.
     * @param x      the x position to draw the string at.
     * @param y      the y position to draw the string at.
     * @param colour the colour to draw the string in.
     */
    default void drawStringWithShadow(String text, float x, float y, int colour) {
        this.drawTextWithShadow(Component.translatable(text).withStyle(s -> s.withColor(colour)), x, y);
    }

    /**
     * Draws the given string on the screen at the given position, with a drop shadow applied.
     *
     * @param text   the string to draw.
     * @param x      the x position to draw the string at.
     * @param y      the y position to draw the string at.
     * @param colour the colour to draw the string in.
     */
    default void drawCenteredStringWithShadow(String text, float x, float y, int colour) {
        this.drawCenteredTextWithShadow(Component.translatable(text).withStyle(style -> style.withColor(colour)), x, y);
    }

    /**
     * Draws the given text component on the screen at the given position.
     *
     * @param text the component to draw.
     * @param x    the x position to draw the string at.
     * @param y    the y position to draw the string at.
     */
    default void drawText(Component text, float x, float y) {
        this.drawFormattedCharSequence(text.getVisualOrderText(), x, y);
    }

    /**
     * Draws the given text component on the screen with the middle of the string centered on the given position.
     *
     * @param text the text to draw.
     * @param x    the x position to draw the string at.
     * @param y    the y position to draw the string at.
     */
    default void drawCenteredText(Component text, float x, float y) {
        this.drawCenteredFormattedCharSequence(text.getVisualOrderText(), x, y);
    }

    /**
     * Draws the given text component on the screen with the middle of the string centered on the given position, with a drop shadow applied.
     *
     * @param text the text to draw.
     * @param x    the x position to draw the string at.
     * @param y    the y position to draw the string at.
     */
    default void drawTextWithShadow(Component text, float x, float y) {
        this.drawFormattedCharSequenceWithShadow(text.getVisualOrderText(), x, y);
    }

    /**
     * Draws the given text component on the screen at the given position, with a drop shadow applied.
     *
     * @param text the text to draw.
     * @param x    the x position to draw the string at.
     * @param y    the y position to draw the string at.
     */
    default void drawCenteredTextWithShadow(Component text, float x, float y) {
        this.drawCenteredFormattedCharSequenceWithShadow(text.getVisualOrderText(), x, y);
    }

    /**
     * Draws the given text component on the screen at the given position.
     *
     * @param processor the processor to draw.
     * @param x         the x position to draw the string at.
     * @param y         the y position to draw the string at.
     */
    void drawFormattedCharSequence(FormattedCharSequence processor, float x, float y);

    /**
     * Draws the given text component on the screen with the middle of the string centered on the given position.
     *
     * @param processor the processor to draw.
     * @param x         the x position to draw the string at.
     * @param y         the y position to draw the string at.
     */
    void drawCenteredFormattedCharSequence(FormattedCharSequence processor, float x, float y);

    /**
     * Draws the given text component on the screen with the middle of the string centered on the given position, with a drop shadow applied.
     *
     * @param processor the processor to draw.
     * @param x         the x position to draw the string at.
     * @param y         the y position to draw the string at.
     */
    void drawFormattedCharSequenceWithShadow(FormattedCharSequence processor, float x, float y);

    /**
     * Draws the given text component on the screen at the given position, with a drop shadow applied.
     *
     * @param processor the processor to draw.
     * @param x         the x position to draw the string at.
     * @param y         the y position to draw the string at.
     */
    void drawCenteredFormattedCharSequenceWithShadow(FormattedCharSequence processor, float x, float y);

    /**
     * Draws a textured rect on the screen matching the provided rect data.
     *
     * @param x the x coordinate to draw the rect at.
     * @param y the y coordinate to draw the rect at.
     */
    void drawTexturedRect(float x, float y, float width, float height, float minU, float minV, float maxU, float maxV, int zLevel);

    /**
     * Draws a textured rect on the screen matching the provided rect data using the given sprite, utilizing the writer to draw the sprite.
     *
     * @param x      the x coordinate to draw the rect at.
     * @param y      the y coordinate to draw the rect at.
     * @param width  the width of the rect to draw.
     * @param height the height of the rect to draw.
     * @param sprite the sprite to draw on the screen, used for gathering uv data and binding the texture.
     */
    default void drawSprite(float x, float y, float width, float height, IScreenSprite sprite) {
        sprite.getWriter().drawSprite(asScreenContext(), sprite, x, y, width, height, 0);
    }

    /**
     * Draws a textured rect on the screen matching the provided rect data using the given sprite, utilizing the writer to draw the sprite.
     *
     * @param x      the x coordinate to draw the rect at.
     * @param y      the y coordinate to draw the rect at.
     * @param width  the width of the rect to draw.
     * @param height the height of the rect to draw.
     * @param sprite the sprite to draw on the screen, used for gathering uv data and binding the texture.
     * @param zLevel the z level to draw the rect at.
     */
    default void drawSprite(float x, float y, float width, float height, IScreenSprite sprite, int zLevel) {
        sprite.getWriter().drawSprite(asScreenContext(), sprite, x, y, width, height, zLevel);
    }

    /**
     * Draws a textured rect on the screen matching the provided rect data using the given sprite, utilizing the sprite to gather UV data and bind the texture.
     *
     * @param sprite the sprite to draw on the screen, used for gathering uv data and binding the texture.
     * @param x      the x coordinate to draw the rect at.
     * @param y      the y coordinate to draw the rect at.
     * @param width  the width of the rect to draw.
     * @param height the height of the rect to draw.
     * @param u      the u coordinate of the sprite to draw.
     * @param v      the v coordinate of the sprite to draw.
     */
    default void drawRectUsingSprite(IScreenSprite sprite, float x, float y, float width, float height, float u, float v) {
        this.drawRectUsingSprite(sprite, x, y, width, height, u, v, 0);
    }

    /**
     * Draws a textured rect on the screen matching the provided rect data using the given sprite, utilizing the sprite to gather UV data and bind the texture.
     *
     * @param sprite the sprite to draw on the screen, used for gathering uv data and binding the texture.
     * @param x      the x coordinate to draw the rect at.
     * @param y      the y coordinate to draw the rect at.
     * @param width  the width of the rect to draw.
     * @param height the height of the rect to draw.
     * @param u      the u coordinate of the sprite to draw.
     * @param v      the v coordinate of the sprite to draw.
     * @param zLevel the z level to draw the rect at.
     */
    default void drawRectUsingSprite(IScreenSprite sprite, float x, float y, float width, float height, float u, float v, int zLevel) {
        this.bindTexture(sprite);
        this.drawTexturedRect(x, y, width, height, sprite.getU(u), sprite.getV(v), sprite.getU(u + width), sprite.getV(v + height), zLevel);
    }

    /**
     * Draws a textured rect on the screen matching the provided rect data.
     *
     * @param x      the x coordinate to draw the rect at.
     * @param y      the y coordinate to draw the rect at.
     * @param width  the width of the rect to draw.
     * @param height the height of the rect to draw.
     * @param minU   the minimum u coordinate of the texture to draw.
     * @param minV   the minimum v coordinate of the texture to draw.
     * @param maxU   the maximum u coordinate of the texture to draw.
     * @param maxV   the maximum v coordinate of the texture to draw.
     */
    default void drawTexturedRect(float x, float y, float width, float height, float minU, float minV, float maxU, float maxV) {
        this.drawTexturedRect(x, y, width, height, minU, minV, maxU, maxV, 0);
    }

    /**
     * Draws the tooltip for the given itemstack.
     *
     * @param stack the itemstack to draw the tooltip for.
     * @param x     the x position to draw the tooltip at.
     * @param y     the y position to draw the tooltip at.
     */
    default void drawTooltip(ItemStack stack, int x, int y) {
        List<Component> tooltip = Screen.getTooltipFromItem(getMc(), stack);
        this.drawTooltip(tooltip, x, y, stack.getTooltipImage(), this.getFont());
    }

    /**
     * Draws the tooltip for the given itemstack, along with any extra components.
     *
     * @param tooltip         the tooltip to draw.
     * @param x               the x position to draw the tooltip at.
     * @param y               the y position to draw the tooltip at.
     * @param extraComponents the extra components to draw.
     * @param font            the font to use when drawing.
     */
    void drawTooltip(List<Component> tooltip, int x, int y, @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<TooltipComponent> extraComponents, Font font);

    /**
     * Draws the given string as a tooltip on the screen.
     *
     * @param text the text to draw .
     * @param x    the x position to draw the tooltip at.
     * @param y    the y position to draw the tooltip at.
     */
    default void drawTooltip(String text, int x, int y) {
        this.drawTooltip(Component.translatable(text), x, y, getFont());
    }

    /**
     * Draws the given string as a tooltip on the screen.
     *
     * @param text the text to draw .
     * @param x    the x position to draw the tooltip at.
     * @param y    the y position to draw the tooltip at.
     * @param font the font to use when drawing.
     */
    default void drawTooltip(String text, int x, int y, Font font) {
        this.drawTooltip(Component.translatable(text), x, y, font);
    }

    /**
     * Draws a multi-line tooltip from the given list of lines at the given coordinates.
     *
     * @param text the text component to draw the tooltip for.
     * @param x    the x position to draw the tooltip at.
     * @param y    the y position to draw the tooltip at.
     */
    default void drawTooltip(Component text, int x, int y) {
        this.drawTooltip(text, x, y, this.getFont());
    }

    /**
     * Draws the appropriate tooltip for the given text component.
     *
     * @param text the text component to draw the tooltip for.
     * @param x    the x position to draw the tooltip at.
     * @param y    the y position to draw the tooltip at.
     * @param font the font to use when drawing.
     */
    default void drawTooltip(Component text, int x, int y, Font font) {
        this.drawTooltip(Collections.singletonList(text), x, y, font);
    }

    /**
     * Draws a multi-line tooltip from the given list of lines at the given coordinates.
     *
     * @param lines the text to draw, each list entry representing a new line.
     * @param x     the x position to draw the tooltip at.
     * @param y     the y position to draw the tooltip at.
     */
    default void drawTooltip(List<Component> lines, int x, int y) {
        this.drawTooltip(lines, x, y, this.getFont());
    }

    /**
     * Draws a multi-line tooltip from the given list of lines at the given coordinates.
     *
     * @param lines the text to draw, each list entry representing a new line.
     * @param x     the x position to draw the tooltip at.
     * @param y     the y position to draw the tooltip at.
     * @param font  the font to use when drawing.
     */
    default void drawTooltip(List<Component> lines, int x, int y, Font font) {
        this.drawProcessorAsTooltip(Lists.transform(lines, Component::getVisualOrderText), x, y, font);
    }

    /**
     * Draws the appropriate tooltip for the given processor.
     *
     * @param processor the processor to draw the tooltip for.
     * @param x         the x position to draw the tooltip at.
     * @param y         the y position to draw the tooltip at.
     */
    default void drawProcessorAsTooltip(FormattedCharSequence processor, int x, int y) {
        this.drawProcessorAsTooltip(processor, x, y, this.getFont());
    }

    /**
     * Draws the appropriate tooltip for the given processor.
     *
     * @param processor the processor to draw the tooltip for.
     * @param x         the x position to draw the tooltip at.
     * @param y         the y position to draw the tooltip at.
     * @param font      the font to use when drawing.
     */
    default void drawProcessorAsTooltip(FormattedCharSequence processor, int x, int y, Font font) {
        this.drawProcessorAsTooltip(Collections.singletonList(processor), x, y, font);
    }

    /**
     * Draws a multi-line tooltip from the given list of processors at the given coordinates.
     *
     * @param processors the processors to draw, each list entry representing a new line.
     * @param x          the x position to draw the tooltip at.
     * @param y          the y position to draw the tooltip at.
     * @param font       the font to use when drawing.
     */
    void drawProcessorAsTooltip(List<FormattedCharSequence> processors, int x, int y, Font font);

    /**
     * Draws the given itemstack on the screen within the given dimensions, at z level 232.
     *
     * @param stack   the stack to draw.
     * @param x       the x position to draw the stack at.
     * @param y       the y position to draw the stack at.
     * @param width   the width of the rectangle to draw the stack in.
     * @param height  the height of the rectangle to draw the stack in.
     * @param altText the text to draw if no stack count will be drawn.
     */
    default void drawItemStack(ItemStack stack, float x, float y, float width, float height, String altText) {
        this.drawItemStack(stack, x, y, width, height, altText, 232);
    }

    /**
     * Draws the given itemstack on the screen within the given dimensions.
     *
     * @param stack   the stack to draw.
     * @param x       the x position to draw the stack at.
     * @param y       the y position to draw the stack at.
     * @param width   the width of the rectangle to draw the stack in.
     * @param height  the height of the rectangle to draw the stack in.
     * @param altText the text to draw if no stack count will be drawn.
     * @param zLevel  the blit offset to draw at.
     */
    void drawItemStack(ItemStack stack, float x, float y, float width, float height, String altText, int zLevel);

    /**
     * Draws the given itemstack on the screen at the given coordinates.
     *
     * @param stack   the stack to draw.
     * @param x       the x position to draw the stack at.
     * @param y       the y position to draw the stack at.
     * @param altText the text to draw if no stack count will be drawn.
     */
    default void drawItemStack(ItemStack stack, int x, int y, String altText) {
        this.drawItemStack(stack, x, y, 16, 16, altText);
    }

    /**
     * Sends a chat message to the server and adds it to the local chat log.
     *
     * @param message the message to send.
     */
    void sendChatMessage(String message);

    /**
     * Sends a chat message to the server and adds it to the local chat log if specified.
     *
     * @param message   the message to send.
     * @param addToChat determines whether the message should be added to the local chat.
     */
    void sendChatMessage(String message, boolean addToChat);

    /**
     * Directly opens the given URI without prompting the user.
     *
     * @param url the url to open.
     */
    void openWebLink(URI url);

    /**
     * Determines if the shift key is currently being held down.
     *
     * @return true if the key is held down, false otherwise.
     */
    boolean isShiftDown();

    /**
     * Determines if the alt key is currently being held down.
     *
     * @return true if the key is held down, false otherwise.
     */
    boolean isAltDown();

    /**
     * Converts a packed colour into an array of floats matching [R, G, B, A]
     *
     * @param colour the colour to unpack.
     * @return the array of values.
     */
    float[] getRGBA(int colour);

    /**
     * Gets the layer currently being drawn on the screen.
     *
     * @return the current layer being drawn.
     */
    EnumUILayer getCurrentLayer();

}
