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

package com.tridevmc.compound.ui.compose.element;

import com.tridevmc.compound.ui.compose.layout.Bounds;
import com.tridevmc.compound.ui.compose.layout.Constraints;
import com.tridevmc.compound.ui.compose.layout.LayoutHelper;
import com.tridevmc.compound.ui.compose.layout.Size;
import com.tridevmc.compound.ui.compose.scope.ICompositionScope;
import com.tridevmc.compound.ui.compose.slot.SlotKey;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.sprite.IScreenSprite;
import com.tridevmc.compound.ui.sprite.IScreenSpriteWriter;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Supplier;

/**
 * A composable box element that renders a nineslice background with content on top.
 * By default uses the inventory container sprite.
 */
public class ElementBox extends BaseElement implements IComposableElement {

    public static final SlotKey CONTENT_SLOT = new SlotKey("content");

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
                    256, 256
            );
            // Custom writer that matches old ElementBox behavior: 4px corners/edges, 1px gray fill
            DEFAULT_SPRITE = wrapWithWriter(baseSprite, new IScreenSpriteWriter() {
                @Override
                public void drawSprite(IScreenContext screen, IScreenSprite sprite, float x, float y, float width, float height) {
                    // Corners: top-left, top-right, bottom-left, bottom-right
                    screen.drawRectUsingSprite(sprite, x, y, 4, 4, 0, 0, 4, 4);
                    screen.drawRectUsingSprite(sprite, x + width - 4, y, 4, 4, 172, 0, 176, 4);
                    screen.drawRectUsingSprite(sprite, x, y + height - 4, 4, 4, 0, 162, 4, 166);
                    screen.drawRectUsingSprite(sprite, x + width - 4, y + height - 4, 4, 4, 172, 162, 176, 166);

                    // Edges: left, right, top, bottom (stretch 1px strips)
                    screen.drawRectUsingSprite(sprite, x, y + 4, 4, height - 8, 0, 4, 4, 5);
                    screen.drawRectUsingSprite(sprite, x + width - 4, y + 4, 4, height - 8, 172, 4, 176, 5);
                    screen.drawRectUsingSprite(sprite, x + 4, y, width - 8, 4, 4, 0, 5, 4);
                    screen.drawRectUsingSprite(sprite, x + 4, y + height - 4, width - 8, 4, 4, 162, 5, 166);

                    // Middle: stretch single gray pixel at (4,4)
                    screen.drawRectUsingSprite(sprite, x + 4, y + 4, width - 8, height - 8, 4, 4, 5, 5);
                }
            });
        }
        return DEFAULT_SPRITE;
    }

    private static IScreenSprite wrapWithWriter(IScreenSprite base, IScreenSpriteWriter writer) {
        return new IScreenSprite() {
            public IScreenSpriteWriter getWriter() { return writer; }
            public ResourceLocation getTextureLocation() { return base.getTextureLocation(); }
            public float getMinU() { return 0F; }
            public float getMinV() { return 0F; }
            public float getMaxU() { return 176F / 256F; }
            public float getMaxV() { return 166F / 256F; }
            public float getWidth() { return 176F / 256F; }
            public float getHeight() { return 166F / 256F; }
            public int getWidthInPixels() { return 176; }
            public int getHeightInPixels() { return 166; }
        };
    }

    @Override
    public void compose(ICompositionScope scope) {
        // Create a Stack to layer the background sprite and slot content
        scope.e(new Stack(), stack -> {
            // Make the Stack fill the entire ElementBox bounds
            stack.layout().fillMax();

            // Background: compose sprite if available, otherwise fallback to colored rectangle
            IScreenSprite sprite = this.spriteSupplier.get();
            if (sprite != null) {
                // Background nineslice sprite (ElementSprite already supports Supplier)
                stack.e(new ElementSprite(this.spriteSupplier));
            } else {
                // Fallback: gray background similar to Minecraft's inventory
                stack.e(new ElementRect(0xFFC6C6C6));
            }

            // Content slot - render slot content as a child of the Stack
            scope.slotInto(CONTENT_SLOT, stack);
        });
    }

    @Override
    public Size measure(Constraints constraints, List<IElement> children) {
        System.out.println("=== ElementBox MEASURE DEBUG ===");
        System.out.println("Input constraints: " + constraints);

        // Step 1: Calculate final size using the layout system helper
        // Use large intrinsic size since we don't have a natural size
        var finalSize = LayoutHelper.calculateSizeWithProperties(
            Integer.MAX_VALUE, Integer.MAX_VALUE,  // No intrinsic size preference
            this.getLayoutProperties(),
            constraints
        );

        // Step 2: Measure children with constraints matching our decided size
        if (!children.isEmpty()) {
            // Give children exact constraints matching our final size
            var childConstraints = Constraints.fixed(finalSize.width(), finalSize.height());
            var childSize = LayoutHelper.measureChild(children.getFirst(), childConstraints);
        }

        // Step 3: Return our final size to parent
        return finalSize;
    }

    @Override
    public void place(Bounds bounds, List<IElement> children) {
        for (int i = 0; i < children.size(); i++) {
            System.out.println("Child " + i + ": " + children.get(i).getClass().getSimpleName());
        }

        this.setBounds(bounds);

        if (!children.isEmpty()) {
            LayoutHelper.placeChild(children.getFirst(), bounds);
        }
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
}
