package com.tridevmc.compound.ui.sprite;

import com.tridevmc.compound.ui.screen.IScreenContext;

/**
 * Implementation of {@link IScreenSpriteWriter} that tiles the sprite within the given dimensions.
 */
public class ScreenSpriteWriterBorderedBox implements IScreenSpriteWriter {

    public ScreenSpriteWriterBorderedBox(float borderSize) {
        this.borderSize = borderSize;
    }

    private final float borderSize;

    @Override
    public void drawSprite(IScreenContext screen, IScreenSprite sprite, float x, float y, float width, float height, int zLevel) {
        var minU = sprite.getMinU();
        var minV = sprite.getMinV();
        var maxU = sprite.getMaxU();
        var maxV = sprite.getMaxV();
        var spriteWidth = sprite.getWidth();
        var spriteHeight = sprite.getHeight();
        int xTiles = (int) Math.ceil((width - borderSize * 2) / spriteWidth);
        int yTiles = (int) Math.ceil((height - borderSize * 2) / spriteHeight);

        screen.bindTexture(sprite);
        // Top left
        screen.drawTexturedRect(
                x, y,
                borderSize, borderSize,
                minU, minV,
                minU + borderSize, minV + borderSize,
                zLevel
        );
        // Top right
        screen.drawTexturedRect(x + width - borderSize, y,
                borderSize, borderSize,
                maxU - borderSize, minV,
                maxU, minV + borderSize,
                zLevel
        );
        // Bottom left
        screen.drawTexturedRect(
                x, y + height - borderSize,
                borderSize, borderSize,
                minU, maxV - borderSize,
                minU + borderSize, maxV,
                zLevel
        );
        // Bottom right
        screen.drawTexturedRect(
                x + width - borderSize, y + height - borderSize,
                borderSize, borderSize,
                maxU - borderSize, maxV - borderSize,
                maxU, maxV,
                zLevel
        );

        // Top
        for (int i = 0; i < xTiles; i++) {
            screen.drawTexturedRect(
                    x + borderSize + i * spriteWidth, y,
                    spriteWidth, borderSize, minU + borderSize, minV,
                    maxU - borderSize, minV + borderSize,
                    zLevel
            );
        }
        // Bottom
        for (int i = 0; i < xTiles; i++) {
            screen.drawTexturedRect(
                    x + borderSize + i * spriteWidth, y + height - borderSize,
                    spriteWidth, borderSize, minU + borderSize,
                    maxV - borderSize, maxU - borderSize, maxV,
                    zLevel
            );
        }
        // Left
        for (int i = 0; i < yTiles; i++) {
            screen.drawTexturedRect(
                    x, y + borderSize + i * spriteHeight,
                    borderSize, spriteHeight,
                    minU, minV + borderSize,
                    minU + borderSize, maxV - borderSize,
                    zLevel
            );
        }
        // Right
        for (int i = 0; i < yTiles; i++) {
            screen.drawTexturedRect(
                    x + width - borderSize, y + borderSize + i * spriteHeight,
                    borderSize, spriteHeight,
                    maxU - borderSize, minV + borderSize,
                    maxU, maxV - borderSize,
                    zLevel
            );
        }

        // Center
        for (int i = 0; i < xTiles; i++) {
            for (int j = 0; j < yTiles; j++) {
                screen.drawTexturedRect(
                        x + borderSize + i * spriteWidth, y + borderSize + j * spriteHeight,
                        spriteWidth, spriteHeight,
                        minU + borderSize, minV + borderSize,
                        maxU - borderSize, maxV - borderSize,
                        zLevel
                );
            }
        }

    }
}
