package com.tridevmc.compound.ui.sprite;

import com.tridevmc.compound.ui.screen.IScreenContext;

/**
 * Implementation of {@link IScreenSpriteWriter} that stretches the sprite to fit the given dimensions.
 */
public class ScreenSpriteWriterStretch implements IScreenSpriteWriter {

    public static final ScreenSpriteWriterStretch INSTANCE = new ScreenSpriteWriterStretch();

    @Override
    public void drawSprite(IScreenContext screen, IScreenSprite sprite, float x, float y, float width, float height, int zLevel) {
        screen.bindTexture(sprite);
        screen.drawTexturedRect(
                x, y,
                width, height,
                sprite.getMinU(), sprite.getMinV(),
                sprite.getMaxU(), sprite.getMaxV(),
                zLevel
        );
    }
}
