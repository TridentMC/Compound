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
                var border = scale.border();
                if (border.left() == border.right() && border.top() == border.bottom() && border.left() == border.top()) {
                    yield new ScreenSpriteWriterBorderedBox(border.left());
                } else {
                    yield new ScreenSpriteWriterNineSlice(border.left(), border.right(), border.top(), border.bottom());
                }
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
