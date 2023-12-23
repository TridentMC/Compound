package com.tridevmc.compound.ui.sprite;

import com.tridevmc.compound.ui.screen.IScreenContext;

/**
 * Implementation of {@link IScreenSpriteWriter} that tiles the sprite within the given dimensions.
 */
public class ScreenSpriteWriterTile implements IScreenSpriteWriter {

    public static final ScreenSpriteWriterTile INSTANCE = new ScreenSpriteWriterTile();

    @Override
    public void drawSprite(IScreenContext screen, IScreenSprite sprite, float x, float y, float width, float height, int zLevel) {
        screen.bindTexture(sprite);
        var spriteWidth = sprite.getWidth();
        var spriteHeight = sprite.getHeight();
        int xTiles = (int) Math.ceil(width / spriteWidth);
        int yTiles = (int) Math.ceil(height / spriteHeight);

        for (int xTile = 0; xTile < xTiles; xTile++) {
            for (int yTile = 0; yTile < yTiles; yTile++) {
                var tileX = x + (xTile * spriteWidth);
                var tileY = y + (yTile * spriteHeight);
                var tileWidth = Math.min(width - (xTile * spriteWidth), spriteWidth);
                var tileHeight = Math.min(height - (yTile * spriteHeight), spriteHeight);
                screen.drawRectUsingSprite(sprite, tileX, tileY, tileWidth, tileHeight, 0, 0, zLevel);
            }
        }
    }
}
