/*
 * Copyright 2018 - 2024 TridentMC
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

package com.tridevmc.compound.ui.element;

import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.slot.SlotKey;
import com.tridevmc.compound.ui.sprite.IScreenSprite;
import com.tridevmc.compound.ui.sprite.IScreenSpriteWriter;
import net.minecraft.resources.Identifier;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Supplier;

/**
 * A vanilla inventory-style background with content supplied through {@link #CONTENT_SLOT}.
 * Add padding to the content when it must stay clear of the decorative border.
 */
public class Panel extends BaseElement implements IComposableElement {

    /** The slot containing this element's consumer-provided content. */
    public static final SlotKey CONTENT_SLOT = new SlotKey("content");

    private static final int TEXTURE_SIZE = 256;
    private static final int SPRITE_WIDTH = 176;
    private static final int SPRITE_HEIGHT = 166;
    private static final int BORDER_SIZE = 4;
    private static final int RIGHT_EDGE_X = 172;
    private static final int BOTTOM_EDGE_Y = 162;

    private static IScreenSprite DEFAULT_SPRITE = null;

    private Supplier<IScreenSprite> spriteSupplier;

    /**
     * Creates a panel. The no-argument form uses the vanilla inventory-style background.
     */
    public Panel() {
        this(Panel::getDefaultSprite);
    }

    /**
     * Creates a panel. The no-argument form uses the vanilla inventory-style background.
     *
     * @param sprite the sprite to draw, or null to suppress drawing
     */
    public Panel(IScreenSprite sprite) {
        this(() -> sprite);
    }

    /**
     * Creates a panel. The no-argument form uses the vanilla inventory-style background.
     *
     * @param spriteSupplier the non-null sprite supplier; a null result suppresses drawing
     */
    public Panel(Supplier<IScreenSprite> spriteSupplier) {
        this.spriteSupplier = spriteSupplier;
    }

    private static IScreenSprite getDefaultSprite() {
        if (DEFAULT_SPRITE == null) {
            var baseSprite = IScreenSprite.ofAssetLocation(
                    Identifier.withDefaultNamespace("textures/gui/container/inventory.png"),
                    TEXTURE_SIZE, TEXTURE_SIZE
            );
            // Preserve four-pixel corners and edges while stretching the center.
            DEFAULT_SPRITE = wrapWithWriter(baseSprite, new IScreenSpriteWriter() {
                @Override
                public void drawSprite(IScreenContext screen, IScreenSprite sprite, float x, float y, float width, float height) {
                    screen.drawRectUsingSprite(sprite, x, y, BORDER_SIZE, BORDER_SIZE, 0, 0, BORDER_SIZE, BORDER_SIZE);
                    screen.drawRectUsingSprite(sprite, x + width - BORDER_SIZE, y, BORDER_SIZE, BORDER_SIZE, RIGHT_EDGE_X, 0, SPRITE_WIDTH, BORDER_SIZE);
                    screen.drawRectUsingSprite(sprite, x, y + height - BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, 0, BOTTOM_EDGE_Y, BORDER_SIZE, SPRITE_HEIGHT);
                    screen.drawRectUsingSprite(sprite, x + width - BORDER_SIZE, y + height - BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, RIGHT_EDGE_X, BOTTOM_EDGE_Y, SPRITE_WIDTH, SPRITE_HEIGHT);

                    screen.drawRectUsingSprite(sprite, x, y + BORDER_SIZE, BORDER_SIZE, height - BORDER_SIZE * 2, 0, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE + 1);
                    screen.drawRectUsingSprite(sprite, x + width - BORDER_SIZE, y + BORDER_SIZE, BORDER_SIZE, height - BORDER_SIZE * 2, RIGHT_EDGE_X, BORDER_SIZE, SPRITE_WIDTH, BORDER_SIZE + 1);
                    screen.drawRectUsingSprite(sprite, x + BORDER_SIZE, y, width - BORDER_SIZE * 2, BORDER_SIZE, BORDER_SIZE, 0, BORDER_SIZE + 1, BORDER_SIZE);
                    screen.drawRectUsingSprite(sprite, x + BORDER_SIZE, y + height - BORDER_SIZE, width - BORDER_SIZE * 2, BORDER_SIZE, BORDER_SIZE, BOTTOM_EDGE_Y, BORDER_SIZE + 1, SPRITE_HEIGHT);

                    screen.drawRectUsingSprite(sprite, x + BORDER_SIZE, y + BORDER_SIZE, width - BORDER_SIZE * 2, height - BORDER_SIZE * 2, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE + 1, BORDER_SIZE + 1);
                }
            });
        }
        return DEFAULT_SPRITE;
    }

    private static IScreenSprite wrapWithWriter(IScreenSprite base, IScreenSpriteWriter writer) {
        return new IScreenSprite() {
            public Identifier getSpriteIdentifier() {
                return null;
            }

            public IScreenSpriteWriter getWriter() {
                return writer;
            }

            public Identifier getTextureLocation() {
                return base.getTextureLocation();
            }

            public float getMinU() {
                return 0F;
            }

            public float getMinV() {
                return 0F;
            }

            public float getMaxU() {
                return (float) SPRITE_WIDTH / TEXTURE_SIZE;
            }

            public float getMaxV() {
                return (float) SPRITE_HEIGHT / TEXTURE_SIZE;
            }

            public float getWidth() {
                return (float) SPRITE_WIDTH / TEXTURE_SIZE;
            }

            public float getHeight() {
                return (float) SPRITE_HEIGHT / TEXTURE_SIZE;
            }

            public int getWidthInPixels() {
                return SPRITE_WIDTH;
            }

            public int getHeightInPixels() {
                return SPRITE_HEIGHT;
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void compose(ICompositionScope scope) {
        scope.e(new Stack(), stack -> {
            IScreenSprite sprite = this.spriteSupplier.get();
            if (sprite != null) {
                // Background fills Panel but doesn't affect sizing (due to fillMax)
                stack.e(new Sprite(this.spriteSupplier), s -> s.layout().fillMax());
            } else {
                stack.e(new Rect(0xFFC6C6C6), r -> r.layout().fillMax());
            }

            scope.slotInto(CONTENT_SLOT, stack);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
        // Panel has no fixed intrinsic size - flexible by default
        // Return size of composed Stack child, or fill available space
        if (!measuredChildren.isEmpty()) {
            return measuredChildren.get(0);
        }
        return new Size(0, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
        if (measuredChildren.isEmpty()) {
            return List.of();
        }

        // Place Stack child at full bounds
        return List.of(bounds);
    }

    /**
     * Evaluates the current sprite supplier.
     *
     * @return the current sprite, or null
     */
    public IScreenSprite getSprite() {
        return this.spriteSupplier.get();
    }

    /**
     * Replaces the sprite supplier with a constant.
     *
     * @param sprite the sprite to draw, or null to suppress drawing
     */
    public void setSprite(IScreenSprite sprite) {
        this.spriteSupplier = () -> sprite;
        this.invalidateComposition();
    }

    /**
     * Returns the sprite supplier used for the background.
     *
     * @return the configured supplier
     */
    public Supplier<IScreenSprite> getSpriteSupplier() {
        return this.spriteSupplier;
    }

    /**
     * Replaces the sprite supplier sampled during drawing.
     *
     * @param spriteSupplier the non-null sprite supplier; a null result suppresses drawing
     */
    public void setSpriteSupplier(Supplier<IScreenSprite> spriteSupplier) {
        this.spriteSupplier = spriteSupplier;
        this.invalidateComposition();
    }
}
