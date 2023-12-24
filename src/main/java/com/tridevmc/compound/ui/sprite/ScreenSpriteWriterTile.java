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

/**
 * Implementation of {@link IScreenSpriteWriter} that tiles the sprite within the given dimensions.
 */
public class ScreenSpriteWriterTile implements IScreenSpriteWriter {

    public static final ScreenSpriteWriterTile INSTANCE = new ScreenSpriteWriterTile();

    @Override
    public void drawSprite(IScreenContext screen, IScreenSprite sprite, float x, float y, float width, float height, int zLevel) {
        var spriteWidth = sprite.getWidthInPixels();
        var spriteHeight = sprite.getHeightInPixels();
        var horizontalTiles = (int) Math.ceil(width / spriteWidth);
        var verticalTiles = (int) Math.ceil(height / spriteHeight);

        for (int h = 0; h < horizontalTiles; h++) {
            for (int v = 0; v < verticalTiles; v++) {
                var tileX = x + (h * spriteWidth);
                var tileY = y + (v * spriteHeight);
                var tileWidth = Math.min(width - (h * spriteWidth), spriteWidth);
                var tileHeight = Math.min(height - (v * spriteHeight), spriteHeight);
                screen.drawRectUsingSprite(sprite,
                        tileX, tileY,
                        0, 0,
                        tileWidth, tileHeight,
                        zLevel
                );
            }
        }
    }
}
