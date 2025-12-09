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
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Supplier;

/**
 * A composable box element that renders a nineslice background with content on top.
 * By default uses the inventory container sprite.
 */
public class ElementBox extends BaseElement implements IComposableElement {

    public static final SlotKey CONTENT_SLOT = new SlotKey("content");

    private static final int TEXTURE_SIZE = 256;
    private static final int SPRITE_WIDTH = 176;
    private static final int SPRITE_HEIGHT = 166;
    private static final int BORDER_SIZE = 4;
    private static final int RIGHT_EDGE_X = 172;
    private static final int BOTTOM_EDGE_Y = 162;

    private static IScreenSprite DEFAULT_SPRITE = null;

    private Supplier<IScreenSprite> spriteSupplier;

    public ElementBox() {
        this(ElementBox::getDefaultSprite);
    }

    public ElementBox(IScreenSprite sprite) {
        this(() -> sprite);
    }

    public ElementBox(Supplier<IScreenSprite> spriteSupplier) {
        this.spriteSupplier = spriteSupplier;
    }

    private static IScreenSprite getDefaultSprite() {
        if (DEFAULT_SPRITE == null) {
            var baseSprite = IScreenSprite.ofAssetLocation(
                    ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png"),
                    TEXTURE_SIZE, TEXTURE_SIZE
            );
            // Custom writer that matches old ElementBox behavior: 4px corners/edges, 1px gray fill
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
            public IScreenSpriteWriter getWriter() {
                return writer;
            }

            public ResourceLocation getTextureLocation() {
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

    @Override
    public void compose(ICompositionScope scope) {
        scope.e(new WrappingStack(), stack -> {
            IScreenSprite sprite = this.spriteSupplier.get();
            if (sprite != null) {
                stack.e(new ElementSprite(this.spriteSupplier));
            } else {
                stack.e(new ElementRect(0xFFC6C6C6));
            }

            scope.slotInto(CONTENT_SLOT, stack);
        });
    }

    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
        // ElementBox has no fixed intrinsic size - flexible by default
        // Return size of composed Stack child, or fill available space
        if (!measuredChildren.isEmpty()) {
            return measuredChildren.get(0);
        }
        return new Size(0, 0);
    }

    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
        if (measuredChildren.isEmpty()) {
            return List.of();
        }

        // Place Stack child at full bounds
        return List.of(bounds);
    }

    public IScreenSprite getSprite() {
        return this.spriteSupplier.get();
    }

    public void setSprite(IScreenSprite sprite) {
        this.spriteSupplier = () -> sprite;
    }

    public Supplier<IScreenSprite> getSpriteSupplier() {
        return this.spriteSupplier;
    }

    public void setSpriteSupplier(Supplier<IScreenSprite> spriteSupplier) {
        this.spriteSupplier = spriteSupplier;
    }

    /**
     * A specialized Stack that ignores the first child (background) for measurement
     * and forces the first child to match the stack's bounds during placement.
     */
    private static class WrappingStack extends Stack {
        @Override
        public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
            if (measuredChildren.isEmpty()) {
                return new Size(0, 0);
            }

            // If only background, return its size (likely max)
            if (measuredChildren.size() == 1) {
                return measuredChildren.get(0);
            }

            int maxWidth = 0;
            int maxHeight = 0;

            // Skip first child (background) for size calculation
            for (int i = 1; i < measuredChildren.size(); i++) {
                Size childSize = measuredChildren.get(i);
                maxWidth = Math.max(maxWidth, childSize.width());
                maxHeight = Math.max(maxHeight, childSize.height());
            }

            return new Size(maxWidth, maxHeight);
        }

        @Override
        public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
            List<Bounds> childBounds = super.place(bounds, props, measuredChildren);

            // Force background (index 0) to match stack bounds
            if (!childBounds.isEmpty()) {
                childBounds.set(0, new Bounds(bounds.position(), bounds.size()));
            }

            return childBounds;
        }
    }
}
