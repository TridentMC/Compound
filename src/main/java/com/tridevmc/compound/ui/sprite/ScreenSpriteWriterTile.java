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
