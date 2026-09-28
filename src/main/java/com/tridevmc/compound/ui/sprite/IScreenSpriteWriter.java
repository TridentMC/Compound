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

package com.tridevmc.compound.ui.sprite;

import com.tridevmc.compound.ui.screen.IScreenContext;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.gui.GuiMetadataSection;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;

/**
 * Responsible for drawing/interpolating sprites to the screen.
 */
public interface IScreenSpriteWriter {

    /**
     * Returns a shared writer that stretches the sprite to fill the destination rectangle.
     *
     * @return the stateless stretch writer.
     */
    static IScreenSpriteWriter stretch() {
        return ScreenSpriteWriterStretch.INSTANCE;
    }

    /**
     * Returns a shared writer that repeats the sprite at its original pixel dimensions.
     * Partial tiles are cropped at the destination edges.
     *
     * @return the stateless tile writer.
     */
    static IScreenSpriteWriter tiled() {
        return ScreenSpriteWriterTile.INSTANCE;
    }

    /**
     * Creates a writer that preserves the corners and tiles the edges and center.
     * Borders are measured in source pixels and kept at that size in GUI pixels.
     * The source must have a positive center size, and the destination must be large
     * enough to contain the borders.
     *
     * @param left the nonnegative left border width.
     * @param right the nonnegative right border width.
     * @param top the nonnegative top border height.
     * @param bottom the nonnegative bottom border height.
     * @return a new writer retaining the border dimensions.
     */
    static IScreenSpriteWriter nineSlice(int left, int right, int top, int bottom) {
        return new ScreenSpriteWriterNineSlice(left, right, top, bottom);
    }

    /**
     * Selects a writer from the sprite's GUI scaling metadata, defaulting to stretching.
     *
     * @param sprite the atlas sprite whose metadata supplies the scaling rules.
     * @return a writer configured for the sprite.
     */
    static IScreenSpriteWriter forTextureAtlasSprite(TextureAtlasSprite sprite) {
        var scaling = sprite.contents().getAdditionalMetadata(GuiMetadataSection.TYPE).orElse(GuiMetadataSection.DEFAULT).scaling();
        return switch (scaling.type()) {
            case STRETCH -> stretch();
            case TILE -> tiled();
            case NINE_SLICE -> {
                var scale = (GuiSpriteScaling.NineSlice) scaling;
                yield nineSlice(scale.border().left(), scale.border().right(), scale.border().top(), scale.border().bottom());
            }
        };
    }

    /**
     * Draws a sprite to the screen, following any scaling rules defined by the implementation.
     *
     * @param screen the screen to draw the sprite to.
     * @param sprite the sprite to draw.
     * @param x      the x coordinate to draw the sprite at.
     * @param y      the y coordinate to draw the sprite at.
     * @param width  the width of the rectangle to draw the sprite in.
     * @param height the height of the rectangle to draw the sprite in.
     */
    void drawSprite(IScreenContext screen, IScreenSprite sprite, float x, float y, float width, float height);

}
