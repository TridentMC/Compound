/*
 * Copyright 2018 - 2026 TridentMC
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


import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Defines a screen sprite, used for interpolating texture file coordinates to their UV equivalents.
 * <p>
 * Atlas sprites (created via {@link #of(Identifier)}) store their original sprite identifier
 * and are rendered using {@code blitSprite()}, which resolves current atlas data and stretch/tile/nine-slice
 * scaling based on sprite metadata.
 * <p>
 * Raw texture sprites (created via {@link #ofAssetLocation}) do not have a sprite identifier
 * and are rendered using manual UV-based blitting.
 */
public interface IScreenSprite {

    /**
     * Creates a new screen sprite from the given sprite and writer.
     *
     * @param sprite the sprite to create a screen sprite from.
     * @param writer the writer to use for the screen sprite.
     * @return a new screen sprite.
     */
    static IScreenSprite of(TextureAtlasSprite sprite, IScreenSpriteWriter writer) {
        return of(sprite, writer, null);
    }

    /**
     * Creates a sprite with an explicit writer and optional identifier. The writer controls drawing.
     *
     * @param sprite           the sprite to create a screen sprite from.
     * @param writer           the writer to use for the screen sprite.
     * @param spriteIdentifier the original sprite identifier for atlas lookups via blitSprite.
     * @return a new screen sprite.
     */
    static IScreenSprite of(TextureAtlasSprite sprite, IScreenSpriteWriter writer, @Nullable Identifier spriteIdentifier) {
        return of(sprite, writer, spriteIdentifier, false);
    }

    private static IScreenSprite of(TextureAtlasSprite sprite, IScreenSpriteWriter writer,
                                   @Nullable Identifier spriteIdentifier, boolean nativeScaling) {
        var location = sprite.atlasLocation();
        var minU = sprite.getU0();
        var minV = sprite.getV0();
        var maxU = sprite.getU1();
        var maxV = sprite.getV1();
        return new IScreenSprite() {
            @Override
            public boolean usesNativeScaling() {
                return nativeScaling;
            }

            @Override
            public @Nullable Identifier getSpriteIdentifier() {
                return spriteIdentifier;
            }

            @Override
            public IScreenSpriteWriter getWriter() {
                return writer;
            }

            @Override
            public Identifier getTextureLocation() {
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
    static IScreenSprite of(TextureAtlasSprite sprite) {
        return of(sprite, IScreenSpriteWriter.forTextureAtlasSprite(sprite), null);
    }

    /**
     * Creates a new screen sprite from the given resource location.
     * <p>
     * The sprite is resolved from the GUI atlas and stores its original identifier
     * so it can be rendered using {@code blitSprite()}, which automatically handles
     * stretch/tile/nine-slice scaling based on sprite metadata.
     *
     * @param location the sprite identifier to create a screen sprite from (e.g. {@code minecraft:widget/button}).
     * @return a new screen sprite.
     */
    static IScreenSprite of(Identifier location) {
        var sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.GUI).getSprite(location);
        var writer = IScreenSpriteWriter.forTextureAtlasSprite(sprite);
        return of(sprite, writer, location, true);
    }

    /**
     * Creates a new screen sprite representing a raw asset texture (not from an atlas).
     * <p>
     * These sprites have no sprite identifier and must be rendered using manual UV-based blitting
     * via {@link com.tridevmc.compound.ui.screen.IPrimitiveScreenContext#drawTexturedRect}.
     *
     * @param location the raw texture path (e.g. {@code minecraft:textures/gui/container/inventory.png}).
     * @param width    the width of the texture in pixels.
     * @param height   the height of the texture in pixels.
     * @return a new screen sprite.
     */
    static IScreenSprite ofAssetLocation(Identifier location, int width, int height) {
        return new IScreenSprite() {
            @Override
            public @Nullable Identifier getSpriteIdentifier() {
                return null;
            }

            @Override
            public IScreenSpriteWriter getWriter() {
                return IScreenSpriteWriter.stretch();
            }

            @Override
            public Identifier getTextureLocation() {
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

    /**
     * Gets the original sprite identifier for atlas-based sprites, or null for raw texture sprites.
     * <p>
     * When non-null, this identifier can be used with {@code GuiGraphicsExtractor.blitSprite()}
     * to render the sprite with automatic scaling (stretch/tile/nine-slice) based on its metadata.
     *
     * @return the sprite identifier, or null if this is a raw texture sprite.
     */
    @Nullable Identifier getSpriteIdentifier();

    /** Whether drawing resolves the current atlas sprite and its scaling metadata. */
    default boolean usesNativeScaling() {
        return false;
    }

    IScreenSpriteWriter getWriter();

    /**
     * Gets the atlas the sprite is located in.
     *
     * @return the atlas the sprite is located in.
     */
    Identifier getTextureLocation();

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
     * Gets the width of the sprite in UV space.
     *
     * @return the width of the sprite.
     */
    default float getWidth() {
        return this.getMaxU() - this.getMinU();
    }

    /**
     * Gets the height of the sprite in UV space.
     *
     * @return the height of the sprite.
     */
    default float getHeight() {
        return this.getMaxV() - this.getMinV();
    }

    /**
     * Gets the U coordinate of the sprite at the given pixel coordinate.
     *
     * @param u the pixel U coordinate to get the sprite UV coordinate at.
     * @return the sprite U coordinate at the given pixel coordinate.
     */
    default float getU(float u) {
        var scale = this.getWidth() / this.getWidthInPixels();
        return this.getMinU() + (u * scale);
    }

    /**
     * Gets the V coordinate of the sprite at the given pixel coordinate.
     *
     * @param v the pixel V coordinate to get the sprite UV coordinate at.
     * @return the sprite V coordinate at the given pixel coordinate.
     */
    default float getV(float v) {
        var scale = this.getHeight() / this.getHeightInPixels();
        return this.getMinV() + (v * scale);
    }

    /**
     * Returns true if this sprite is an atlas sprite that should be rendered via blitSprite().
     *
     * @return true if this is an atlas sprite.
     */
    default boolean isAtlasSprite() {
        return this.getSpriteIdentifier() != null;
    }
}
