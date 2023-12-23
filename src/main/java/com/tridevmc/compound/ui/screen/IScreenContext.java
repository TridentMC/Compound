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

import com.tridevmc.compound.ui.Rect2F;
import com.tridevmc.compound.ui.UVData;
import com.tridevmc.compound.ui.sprite.IScreenSprite;
import net.minecraft.world.item.ItemStack;

/**
 * Provides useful methods and variables for drawing a UI and it's elements.
 * This should be used in place of any direct calls to minecraft GUI methods.
 */
public interface IScreenContext extends IPrimitiveScreenContext {

    /**
     * Draws a solid single colour rect on the screen matching the provided rect data.
     *
     * @param rect   the position and dimensions of the rect to draw.
     * @param colour the colour of the rect to draw.
     */
    default void drawRect(Rect2F rect, int colour) {
        this.drawRect(rect, colour, 0);
    }

    /**
     * Draws a solid single colour rect on the screen matching the provided rect data.
     *
     * @param rect   the position and dimensions of the rect to draw.
     * @param colour the colour of the rect to draw.
     * @param zLevel the z level to draw the rect at.
     */
    default void drawRect(Rect2F rect, int colour, int zLevel) {
        this.drawGradientRect(rect, colour, colour, zLevel);
    }

    /**
     * Draws a solid gradient rect on the screen matching the provided rect data.
     *
     * @param rect        the position and dimensions of the rect to draw.
     * @param startColour the colour at the beginning of the gradient.
     * @param endColour   the colour at the end of the gradient.
     */
    default void drawGradientRect(Rect2F rect, int startColour, int endColour) {
        this.drawGradientRect(rect, startColour, endColour, 0);
    }

    /**
     * Draws a solid gradient rect on the screen matching the provided rect data.
     *
     * @param rect        the position and dimensions of the rect to draw.
     * @param startColour the colour at the beginning of the gradient.
     * @param endColour   the colour at the end of the gradient.
     * @param zLevel      the z level to draw the rect at.
     */
    default void drawGradientRect(Rect2F rect, int startColour, int endColour, int zLevel) {
        this.drawGradientRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(), startColour, endColour, zLevel);
    }

    /**
     * Draws a textured rect on the screen matching the provided rect data.
     *
     * @param rect   the position and dimensions of the rect to draw.
     * @param minUvs the minimum uvs for the rect.
     * @param maxUvs the maximum uvs for the rect.
     */
    default void drawTexturedRect(Rect2F rect, UVData minUvs, UVData maxUvs) {
        this.drawTexturedRect(rect, minUvs, maxUvs, 0);
    }

    /**
     * Draws a textured rect on the screen matching the provided rect data using the given sprite.
     *
     * @param rect   the position and dimensions of the rect to draw.
     * @param minUvs the minimum uvs for the rect.
     * @param maxUvs the maximum uvs for the rect.
     */
    default void drawTexturedRect(Rect2F rect, UVData minUvs, UVData maxUvs, int zLevel) {
        this.drawTexturedRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(), minUvs.getU(), minUvs.getV(), maxUvs.getU(), maxUvs.getV(), zLevel);
    }

    /**
     * Draws a textured rect on the screen matching the provided rect data using the given sprite.
     *
     * @param sprite the sprite to draw on the screen, used for gathering uv data and determining tiling behaviour.
     * @param rect   the position and dimensions of the rect to draw.
     */
    default void drawSprite(IScreenSprite sprite, Rect2F rect) {
        this.drawSprite(sprite, rect, 0);
    }

    /**
     * Draws a textured rect on the screen matching the provided rect data using the given sprite.
     *
     * @param sprite the sprite to draw on the screen, used for gathering uv data and determining tiling behaviour.
     * @param rect   the position and dimensions of the rect to draw.
     */
    default void drawSprite(IScreenSprite sprite, Rect2F rect, int zLevel) {
        this.drawSprite(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(), sprite, zLevel);
    }

    /**
     * Draws a textured rect on the screen matching the provided rect data using the given sprite for calculating uv data.
     *
     * @param sprite the sprite to draw on the screen, used for gathering uv data and determining tiling behaviour.
     * @param rect   the position and dimensions of the rect to draw.
     * @param uv     the uv data to use for drawing the sprite.
     */
    default void drawRectUsingSprite(IScreenSprite sprite, Rect2F rect, UVData uv) {
        this.drawRectUsingSprite(sprite, rect, uv, 0);
    }

    /**
     * Draws a textured rect on the screen matching the provided rect data using the given sprite for calculating uv data.
     *
     * @param sprite the sprite to draw on the screen, used for gathering uv data and determining tiling behaviour.
     * @param rect   the position and dimensions of the rect to draw.
     * @param uv     the uv data to use for drawing the sprite.
     */
    default void drawRectUsingSprite(IScreenSprite sprite, Rect2F rect, UVData uv, int zLevel) {
        this.drawRectUsingSprite(sprite, rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(), uv.getU(), uv.getV(), zLevel);
    }

    /**
     * Draws the given itemstack on the screen within the given dimensions.
     *
     * @param stack      the stack to draw.
     * @param dimensions the dimensions to draw the stack within.
     * @param altText    the text to draw if no stack count will be drawn.
     */
    default void drawItemStack(ItemStack stack, Rect2F dimensions, String altText) {
        this.drawItemStack(stack, dimensions.getX(), dimensions.getY(), dimensions.getWidth(), dimensions.getHeight(), altText, 0);
    }

    /**
     * Draws the given itemstack on the screen within the given dimensions.
     *
     * @param stack      the stack to draw.
     * @param dimensions the dimensions to draw the stack within.
     * @param altText    the text to draw if no stack count will be drawn.
     * @param zLevel     the blit offset to draw at.
     */
    default void drawItemStack(ItemStack stack, Rect2F dimensions, String altText, int zLevel) {
        this.drawItemStack(stack, dimensions.getX(), dimensions.getY(), dimensions.getWidth(), dimensions.getHeight(), altText, zLevel);
    }

}
