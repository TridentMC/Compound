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

package com.tridevmc.compound.ui.sprite;

import com.tridevmc.compound.ui.screen.IScreenContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;

/**
 * Responsible for drawing/interpolating sprites to the screen.
 */
public interface IScreenSpriteWriter {

    static IScreenSpriteWriter forTextureAtlasSprite(TextureAtlasSprite sprite) {
        var scaling = Minecraft.getInstance().getGuiSprites().getSpriteScaling(sprite);
        return switch (scaling.type()) {
            case STRETCH -> new ScreenSpriteWriterStretch();
            case TILE -> new ScreenSpriteWriterTile();
            case NINE_SLICE -> {
                var scale = (GuiSpriteScaling.NineSlice) scaling;
                yield new ScreenSpriteWriterNineSlice(scale.border().left(), scale.border().right(), scale.border().top(), scale.border().bottom());
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
     * @param zLevel the z level to draw the sprite at.
     */
    void drawSprite(IScreenContext screen, IScreenSprite sprite, float x, float y, float width, float height, int zLevel);

}
