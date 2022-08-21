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
import com.mojang.blaze3d.vertex.PoseStack;
import com.tridevmc.compound.ui.EnumUILayer;
import com.tridevmc.compound.ui.Rect2F;
import com.tridevmc.compound.ui.UVData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.client.ForgeHooksClient;

import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * Provides useful methods and variables for drawing a UI and it's elements.
 * This should be used in place of any direct calls to minecraft GUI methods.
 */
public interface IScreenContext {

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
    void bindTexture(ResourceLocation texture);

    /**
     * Draws a solid single colour rect on the screen matching the provided rect data.
     *
     * @param rect   the position and dimensions of the rect to draw.
     * @param colour the colour of the rect to draw.
     */
    void drawRect(Rect2F rect, int colour);

    /**
     * Draws a solid gradient rect on the screen matching the provided rect data.
     *
     * @param rect        the position and dimensions of the rect to draw.
     * @param startColour the colour at the beginning of the gradient.
     * @param endColour   the colour at the end of the gradient.
     */
    void drawGradientRect(Rect2F rect, int startColour, int endColour);

    /**
     * Draws the given string on the screen at the given position.
     *
     * @param matrix the matrix stack.
     * @param text   the string to draw.
     * @param x      the x position to draw the string at.
     * @param y      the y position to draw the string at.
     * @param colour the colour to draw the string in.
     */
    default void drawString(PoseStack matrix, String text, float x, float y, int colour) {
        this.drawText(matrix, Component.translatable(text).withStyle(s -> s.withColor(colour)), x, y);
    }

    /**
     * Draws the given string on the screen with the middle of the string centered on the given position.
     *
     * @param matrix the matrix stack.
     * @param text   the string to draw.
     * @param x      the x position to draw the string at.
     * @param y      the y position to draw the string at.
     * @param colour the colour to draw the string in.
     */
    default void drawCenteredString(PoseStack matrix, String text, float x, float y, int colour) {
        this.drawCenteredText(matrix, Component.translatable(text).withStyle(s -> s.withColor(colour)), x, y);
    }

    /**
     * Draws the given string on the screen with the middle of the string centered on the given position, with a drop shadow applied.
     *
     * @param matrix the matrix stack.
     * @param text   the string to draw.
     * @param x      the x position to draw the string at.
     * @param y      the y position to draw the string at.
     * @param colour the colour to draw the string in.
     */
    default void drawStringWithShadow(PoseStack matrix, String text, float x, float y, int colour) {
        this.drawTextWithShadow(matrix, Component.translatable(text).withStyle(s -> s.withColor(colour)), x, y);
    }

    /**
     * Draws the given string on the screen at the given position, with a drop shadow applied.
     *
     * @param matrix the matrix stack.
     * @param text   the string to draw.
     * @param x      the x position to draw the string at.
     * @param y      the y position to draw the string at.
     * @param colour the colour to draw the string in.
     */
    default void drawCenteredStringWithShadow(PoseStack matrix, String text, float x, float y, int colour) {
        this.drawCenteredTextWithShadow(matrix, Component.translatable(text).withStyle(style -> style.withColor(colour)), x, y);
    }

    /**
     * Draws the given text component on the screen at the given position.
     *
     * @param matrix the matrix stack.
     * @param text   the component to draw.
     * @param x      the x position to draw the string at.
     * @param y      the y position to draw the string at.
     */
    default void drawText(PoseStack matrix, Component text, float x, float y) {
        this.drawFormattedCharSequence(matrix, text.getVisualOrderText(), x, y);
    }

    /**
     * Draws the given text component on the screen with the middle of the string centered on the given position.
     *
     * @param matrix the matrix stack.
     * @param text   the text to draw.
     * @param x      the x position to draw the string at.
     * @param y      the y position to draw the string at.
     */
    default void drawCenteredText(PoseStack matrix, Component text, float x, float y) {
        this.drawCenteredFormattedCharSequence(matrix, text.getVisualOrderText(), x, y);
    }

    /**
     * Draws the given text component on the screen with the middle of the string centered on the given position, with a drop shadow applied.
     *
     * @param matrix the matrix stack.
     * @param text   the text to draw.
     * @param x      the x position to draw the string at.
     * @param y      the y position to draw the string at.
     */
    default void drawTextWithShadow(PoseStack matrix, Component text, float x, float y) {
        this.drawFormattedCharSequenceWithShadow(matrix, text.getVisualOrderText(), x, y);
    }

    /**
     * Draws the given text component on the screen at the given position, with a drop shadow applied.
     *
     * @param matrix the matrix stack.
     * @param text   the text to draw.
     * @param x      the x position to draw the string at.
     * @param y      the y position to draw the string at.
     */
    default void drawCenteredTextWithShadow(PoseStack matrix, Component text, float x, float y) {
        this.drawCenteredFormattedCharSequenceWithShadow(matrix, text.getVisualOrderText(), x, y);
    }

    /**
     * Draws the given text component on the screen at the given position.
     *
     * @param matrix    the matrix stack.
     * @param processor the processor to draw.
     * @param x         the x position to draw the string at.
     * @param y         the y position to draw the string at.
     */
    void drawFormattedCharSequence(PoseStack matrix, FormattedCharSequence processor, float x, float y);

    /**
     * Draws the given text component on the screen with the middle of the string centered on the given position.
     *
     * @param matrix    the matrix stack.
     * @param processor the processor to draw.
     * @param x         the x position to draw the string at.
     * @param y         the y position to draw the string at.
     */
    void drawCenteredFormattedCharSequence(PoseStack matrix, FormattedCharSequence processor, float x, float y);

    /**
     * Draws the given text component on the screen with the middle of the string centered on the given position, with a drop shadow applied.
     *
     * @param matrix    the matrix stack.
     * @param processor the processor to draw.
     * @param x         the x position to draw the string at.
     * @param y         the y position to draw the string at.
     */
    void drawFormattedCharSequenceWithShadow(PoseStack matrix, FormattedCharSequence processor, float x, float y);

    /**
     * Draws the given text component on the screen at the given position, with a drop shadow applied.
     *
     * @param matrix    the matrix stack.
     * @param processor the processor to draw.
     * @param x         the x position to draw the string at.
     * @param y         the y position to draw the string at.
     */
    void drawCenteredFormattedCharSequenceWithShadow(PoseStack matrix, FormattedCharSequence processor, float x, float y);

    /**
     * Draws a textured rect on the screen matching the provided rect data.
     *
     * @param rect the position and dimensions of the rect to draw.
     * @param uvs  the uv data for the rect.
     */
    void drawTexturedRect(Rect2F rect, UVData uvs);

    /**
     * Draws a textured rect on the screen matching the provided rect data.
     *
     * @param x      the x coordinate to draw the rect at.
     * @param y      the y coordinate to draw the rect at.
     * @param minUvs the minimum uvs for the rect.
     * @param maxUvs the maximum uvs for the rect.
     */
    void drawTexturedRect(float x, float y, UVData minUvs, UVData maxUvs);

    /**
     * Draws a textured rect on the screen matching the provided rect data.
     *
     * @param rect   the position and dimensions of the rect to draw.
     * @param minUvs the minimum uvs for the rect.
     * @param maxUvs the maximum uvs for the rect.
     */
    void drawTexturedRect(Rect2F rect, UVData minUvs, UVData maxUvs);

    /**
     * Draws a textured rect on the screen matching the provided rect data.
     *
     * @param rect   the position and dimensions of the rect to draw.
     * @param sprite the sprite to draw on the screen, used for gathering uv data.
     */
    void drawTexturedRect(Rect2F rect, TextureAtlasSprite sprite);

    /**
     * Draws a textured rect on the screen matching the provided rect data.
     *
     * @param rect          the position and dimensions of the rect to draw.
     * @param uvs           the uv data for the rect.
     * @param textureWidth  the width of the texture that is being drawn.
     * @param textureHeight the height of the texture that is being drawn.
     */
    void drawTexturedRect(Rect2F rect, UVData uvs, float textureWidth, float textureHeight);

    /**
     * Draws a scaled, tiled, and textured rect on the screen matching the provided rect data.
     *
     * @param rect       the position and dimensions of the rect to draw.
     * @param uvs        the uv data for the rect.
     * @param uWidth     the width of the uv map.
     * @param vHeight    the height of the uv map.
     * @param tileWidth  the width of the tile to draw.
     * @param tileHeight the height of the tile to draw.
     */
    void drawTexturedRect(Rect2F rect, UVData uvs, int uWidth, int vHeight, float tileWidth, float tileHeight);

    /**
     * Draws a tiled textured rect on the screen matching the provided rect data.
     *
     * @param rect  the position and dimensions of the rect to draw.
     * @param uvMin the minimum uv data of the texture to tile.
     * @param uvMax the maximum uv data of the texture to tile.
     */
    void drawTiledTexturedRect(Rect2F rect, UVData uvMin, UVData uvMax);

    /**
     * Draws the tooltip for the given itemstack.
     *
     * @param poseStack the matrix stack.
     * @param stack     the itemstack to draw the tooltip for.
     * @param x         the x position to draw the tooltip at.
     * @param y         the y position to draw the tooltip at.
     */
    @SuppressWarnings("removal") // They're not gone yet and there's no suitable replacement.
    default void drawTooltip(PoseStack poseStack, ItemStack stack, int x, int y) {
        Font font = ForgeHooksClient.getTooltipFont(null, stack, getFont());
        font = font == null ? this.getFont() : font;
        List<Component> tooltip = stack.getTooltipLines(this.getMc().player, this.getMc().options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL);
        this.drawTooltip(poseStack, tooltip, x, y, font);
    }

    /**
     * Draws the given string as a tooltip on the screen.
     *
     * @param poseStack the matrix stack.
     * @param text      the text to draw .
     * @param x         the x position to draw the tooltip at.
     * @param y         the y position to draw the tooltip at.
     */
    default void drawTooltip(PoseStack poseStack, String text, int x, int y) {
        this.drawTooltip(poseStack, Component.translatable(text), x, y, getFont());
    }

    /**
     * Draws the given string as a tooltip on the screen.
     *
     * @param poseStack the matrix stack.
     * @param text      the text to draw .
     * @param x         the x position to draw the tooltip at.
     * @param y         the y position to draw the tooltip at.
     * @param font      the font to use when drawing.
     */
    default void drawTooltip(PoseStack poseStack, String text, int x, int y, Font font) {
        this.drawTooltip(poseStack, Component.translatable(text), x, y, font);
    }

    /**
     * Draws a multi-line tooltip from the given list of lines at the given coordinates.
     *
     * @param poseStack the matrix stack.
     * @param text      the text component to draw the tooltip for.
     * @param x         the x position to draw the tooltip at.
     * @param y         the y position to draw the tooltip at.
     */
    default void drawTooltip(PoseStack poseStack, Component text, int x, int y) {
        this.drawTooltip(poseStack, text, x, y, this.getFont());
    }

    /**
     * Draws the appropriate tooltip for the given text component.
     *
     * @param poseStack the matrix stack.
     * @param text      the text component to draw the tooltip for.
     * @param x         the x position to draw the tooltip at.
     * @param y         the y position to draw the tooltip at.
     * @param font      the font to use when drawing.
     */
    default void drawTooltip(PoseStack poseStack, Component text, int x, int y, Font font) {
        this.drawTooltip(poseStack, Collections.singletonList(text), x, y, font);
    }

    /**
     * Draws a multi-line tooltip from the given list of lines at the given coordinates.
     *
     * @param poseStack the matrix stack.
     * @param lines     the text to draw, each list entry representing a new line.
     * @param x         the x position to draw the tooltip at.
     * @param y         the y position to draw the tooltip at.
     */
    default void drawTooltip(PoseStack poseStack, List<Component> lines, int x, int y) {
        this.drawTooltip(poseStack, lines, x, y, this.getFont());
    }

    /**
     * Draws a multi-line tooltip from the given list of lines at the given coordinates.
     *
     * @param poseStack the matrix stack.
     * @param lines     the text to draw, each list entry representing a new line.
     * @param x         the x position to draw the tooltip at.
     * @param y         the y position to draw the tooltip at.
     * @param font      the font to use when drawing.
     */
    default void drawTooltip(PoseStack poseStack, List<Component> lines, int x, int y, Font font) {
        this.drawProcessorAsTooltip(poseStack, Lists.transform(lines, Component::getVisualOrderText), x, y, font);
    }

    /**
     * Draws the appropriate tooltip for the given processor.
     *
     * @param poseStack the matrix stack.
     * @param processor the processor to draw the tooltip for.
     * @param x         the x position to draw the tooltip at.
     * @param y         the y position to draw the tooltip at.
     */
    default void drawProcessorAsTooltip(PoseStack poseStack, FormattedCharSequence processor, int x, int y) {
        this.drawProcessorAsTooltip(poseStack, processor, x, y, this.getFont());
    }

    /**
     * Draws the appropriate tooltip for the given processor.
     *
     * @param poseStack the matrix stack.
     * @param processor the processor to draw the tooltip for.
     * @param x         the x position to draw the tooltip at.
     * @param y         the y position to draw the tooltip at.
     * @param font      the font to use when drawing.
     */
    default void drawProcessorAsTooltip(PoseStack poseStack, FormattedCharSequence processor, int x, int y, Font font) {
        this.drawProcessorAsTooltip(poseStack, Collections.singletonList(processor), x, y, font);
    }

    /**
     * Draws a multi-line tooltip from the given list of processors at the given coordinates.
     *
     * @param poseStack  the matrix stack.
     * @param processors the processors to draw, each list entry representing a new line.
     * @param x          the x position to draw the tooltip at.
     * @param y          the y position to draw the tooltip at.
     * @param font       the font to use when drawing.
     */
    void drawProcessorAsTooltip(PoseStack poseStack, List<FormattedCharSequence> processors, int x, int y, Font font);

    /**
     * Draws the given itemstack on the screen within the given dimensions.
     *
     * @param stack      the stack to draw.
     * @param dimensions the dimensions to draw the stack within.
     * @param altText    the text to draw if no stack count will be drawn.
     */
    void drawItemStack(ItemStack stack, Rect2F dimensions, String altText);

    /**
     * Draws the given itemstack on the screenw ithin the given dimensions.
     *
     * @param stack      the stack to draw.
     * @param dimensions the dimensions to draw the stack within.
     * @param altText    the text to draw if no stack count will be drawn.
     * @param blitOffset the blit offset to draw at.
     */
    void drawItemStack(ItemStack stack, Rect2F dimensions, String altText, int blitOffset);

    /**
     * Draws the given itemstack on the screen at the given coordinates.
     *
     * @param stack   the stack to draw.
     * @param x       the x position to draw the stack at.
     * @param y       the y position to draw the stack at.
     * @param altText the text to draw if no stack count will be drawn.
     */
    void drawItemStack(ItemStack stack, int x, int y, String altText);

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
