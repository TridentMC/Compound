package com.tridevmc.compound.ui.sprite;


import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

/**
 * Defines a screen sprite, used for interpolating texture file coordinates. To their UV equivalents.
 */
public interface IScreenSprite {

    /**
     * Creates a new screen sprite from the given sprite and writer.
     *
     * @param sprite the sprite to create a screen sprite from.
     * @param writer the writer to use for the screen sprite.
     * @return a new screen sprite.
     */
    public static IScreenSprite of(TextureAtlasSprite sprite, IScreenSpriteWriter writer) {
        var location = sprite.atlasLocation();
        var minU = sprite.getU0();
        var minV = sprite.getV0();
        var maxU = sprite.getU1();
        var maxV = sprite.getV1();
        return new IScreenSprite() {
            @Override
            public IScreenSpriteWriter getWriter() {
                return writer;
            }

            @Override
            public ResourceLocation getTextureLocation() {
                return location;
            }

            @Override
            public float getMinU() {
                return minU;
            }

            @Override
            public float getMinV() {
                return minV;
            }

            @Override
            public float getMaxU() {
                return maxU;
            }

            @Override
            public float getMaxV() {
                return maxV;
            }

            @Override
            public int getWidthInPixels() {
                return sprite.contents().width();
            }

            @Override
            public int getHeightInPixels() {
                return sprite.contents().height();
            }
        };
    }

    /**
     * Creates a new screen sprite from the given sprite.
     *
     * @param sprite the sprite to create a screen sprite from.
     * @return a new screen sprite.
     */
    public static IScreenSprite of(TextureAtlasSprite sprite) {
        return of(sprite, IScreenSpriteWriter.forTextureAtlasSprite(sprite));
    }

    /**
     * Creates a new screen sprite from the given resource location.
     *
     * @param location the location to create a screen sprite from.
     * @return a new screen sprite.
     */
    static IScreenSprite of(ResourceLocation location) {
        return of(Minecraft.getInstance().getGuiSprites().getSprite(location));
    }

    static IScreenSprite ofAssetLocation(ResourceLocation location, int width, int height) {
        return new IScreenSprite() {
            @Override
            public IScreenSpriteWriter getWriter() {
                return ScreenSpriteWriterStretch.INSTANCE;
            }

            @Override
            public ResourceLocation getTextureLocation() {
                return location;
            }

            @Override
            public float getMinU() {
                return 0F;
            }

            @Override
            public float getMinV() {
                return 0F;
            }

            @Override
            public float getMaxU() {
                return 1F;
            }

            @Override
            public float getMaxV() {
                return 1F;
            }

            @Override
            public int getWidthInPixels() {
                return width;
            }

            @Override
            public int getHeightInPixels() {
                return height;
            }
        };
    }

    IScreenSpriteWriter getWriter();

    /**
     * Gets the atlas the sprite is located in.
     *
     * @return the atlas the sprite is located in.
     */
    ResourceLocation getTextureLocation();

    /**
     * Gets the minimum U coordinate of the sprite.
     *
     * @return the minimum U coordinate.
     */
    float getMinU();

    /**
     * Gets the minimum V coordinate of the sprite.
     *
     * @return the minimum V coordinate.
     */
    float getMinV();

    /**
     * Gets the maximum U coordinate of the sprite.
     *
     * @return the maximum U coordinate.
     */
    float getMaxU();

    /**
     * Gets the maximum V coordinate of the sprite.
     *
     * @return the maximum V coordinate.
     */
    float getMaxV();

    /**
     * Gets the width of the sprite in pixels.
     *
     * @return the width of the sprite in pixels.
     */
    int getWidthInPixels();

    /**
     * Gets the height of the sprite in pixels.
     *
     * @return the height of the sprite in pixels.
     */
    int getHeightInPixels();

    /**
     * Gets the width of the sprite.
     *
     * @return the width of the sprite.
     */
    default float getWidth() {
        return this.getMaxU() - this.getMinU();
    }

    /**
     * Gets the height of the sprite.
     *
     * @return the height of the sprite.
     */
    default float getHeight() {
        return this.getMaxV() - this.getMinV();
    }

    /**
     * Gets the U coordinate of the sprite at the given U coordinate.
     *
     * @param u the U coordinate to get the sprite U coordinate at.
     * @return the sprite U coordinate at the given U coordinate.
     */
    default float getU(float u) {
        var scale = this.getWidth() / this.getWidthInPixels();
        return this.getMinU() + (u * scale);
    }

    /**
     * Gets the V coordinate of the sprite at the given V coordinate.
     *
     * @param v the V coordinate to get the sprite V coordinate at.
     * @return the sprite V coordinate at the given V coordinate.
     */
    default float getV(float v) {
        var scale = this.getHeight() / this.getHeightInPixels();
        return this.getMinV() + (v * scale);
    }


}
