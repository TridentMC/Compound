package com.tridevmc.compound.ui.sprite;

import com.tridevmc.compound.ui.screen.IScreenContext;

/**
 * Implementation of {@link IScreenSpriteWriter} that tiles the sprite within the given dimensions.
 */
public class ScreenSpriteWriterNineSlice implements IScreenSpriteWriter {

    public ScreenSpriteWriterNineSlice(float leftBorder, float rightBorder, float topBorder, float bottomBorder) {
        this.leftBorder = leftBorder;
        this.rightBorder = rightBorder;
        this.topBorder = topBorder;
        this.bottomBorder = bottomBorder;
    }

    private final float leftBorder, rightBorder, topBorder, bottomBorder;

    @Override
    public void drawSprite(IScreenContext screen, IScreenSprite sprite, float x, float y, float width, float height, int zLevel) {
        screen.bindTexture(sprite);
        var minU = sprite.getMinU();
        var minV = sprite.getMinV();
        var maxU = sprite.getMaxU();
        var maxV = sprite.getMaxV();
        var spriteWidth = sprite.getWidth();
        var spriteHeight = sprite.getHeight();
        var leftWidth = leftBorder;
        var rightWidth = rightBorder;
        var topHeight = topBorder;
        var bottomHeight = bottomBorder;
        var centerWidth = width - leftWidth - rightWidth;
        var centerHeight = height - topHeight - bottomHeight;
        var centerMinU = sprite.getU(leftWidth);
        var centerMinV = sprite.getV(topHeight);
        var centerMaxU = sprite.getU(leftWidth + centerWidth);
        var centerMaxV = sprite.getV(topHeight + centerHeight);

        // Top left
        screen.drawTexturedRect(
                x, y, leftWidth, topHeight,
                minU, minV,
                sprite.getU(leftWidth), sprite.getV(topHeight),
                zLevel
        );

        // Top right
        screen.drawTexturedRect(
                x + leftWidth + centerWidth, y, rightWidth, topHeight,
                sprite.getU(spriteWidth - rightWidth), minV,
                maxU, sprite.getV(topHeight),
                zLevel
        );

        // Bottom left
        screen.drawTexturedRect(
                x, y + topHeight + centerHeight, leftWidth, bottomHeight,
                minU, sprite.getV(spriteHeight - bottomHeight),
                sprite.getU(leftWidth), maxV,
                zLevel
        );

        // Bottom right
        screen.drawTexturedRect(
                x + leftWidth + centerWidth, y + topHeight + centerHeight, rightWidth, bottomHeight,
                sprite.getU(spriteWidth - rightWidth), sprite.getV(spriteHeight - bottomHeight),
                maxU, maxV,
                zLevel
        );

        // Top
        for (int i = 0; i < centerWidth; i++) {
            screen.drawTexturedRect(
                    x + leftWidth + i, y, 1, topHeight,
                    centerMinU + i / centerWidth * (centerMaxU - centerMinU), minV,
                    centerMinU + (i + 1) / centerWidth * (centerMaxU - centerMinU), sprite.getV(topHeight),
                    zLevel
            );
        }

        // Bottom
        for (int i = 0; i < centerWidth; i++) {
            screen.drawTexturedRect(
                    x + leftWidth + i, y + topHeight + centerHeight, 1, bottomHeight,
                    centerMinU + i / centerWidth * (centerMaxU - centerMinU), sprite.getV(spriteHeight - bottomHeight),
                    centerMinU + (i + 1) / centerWidth * (centerMaxU - centerMinU), maxV,
                    zLevel
            );
        }

        // Left
        for (int i = 0; i < centerHeight; i++) {
            screen.drawTexturedRect(
                    x, y + topHeight + i, leftWidth, 1,
                    minU, centerMinV + i / centerHeight * (centerMaxV - centerMinV),
                    sprite.getU(leftWidth), centerMinV + (i + 1) / centerHeight * (centerMaxV - centerMinV),
                    zLevel
            );
        }

        // Right
        for (int i = 0; i < centerHeight; i++) {
            screen.drawTexturedRect(
                    x + leftWidth + centerWidth, y + topHeight + i, rightWidth, 1,
                    sprite.getU(spriteWidth - rightWidth), centerMinV + i / centerHeight * (centerMaxV - centerMinV),
                    maxU, centerMinV + (i + 1) / centerHeight * (centerMaxV - centerMinV),
                    zLevel
            );
        }

        // Center (finally)
        for (int i = 0; i < centerWidth; i++) {
            for (int j = 0; j < centerHeight; j++) {
                screen.drawTexturedRect(
                        x + leftWidth + i, y + topHeight + j, 1, 1,
                        centerMinU + i / centerWidth * (centerMaxU - centerMinU), centerMinV + j / centerHeight * (centerMaxV - centerMinV),
                        centerMinU + (i + 1) / centerWidth * (centerMaxU - centerMinU), centerMinV + (j + 1) / centerHeight * (centerMaxV - centerMinV),
                        zLevel
                );
            }
        }
    }
}
