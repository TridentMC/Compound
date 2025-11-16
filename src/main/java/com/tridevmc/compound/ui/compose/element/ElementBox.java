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
import com.tridevmc.compound.ui.compose.scope.IComposableElementScope;
import com.tridevmc.compound.ui.compose.scope.ICompositionScope;
import com.tridevmc.compound.ui.compose.slot.SlotContent;
import com.tridevmc.compound.ui.compose.slot.SlotKey;
import com.tridevmc.compound.ui.sprite.IScreenSprite;
import com.tridevmc.compound.ui.sprite.IScreenSpriteWriter;
import com.tridevmc.compound.ui.sprite.ScreenSpriteWriterNineSlice;
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
            // Wrap the base sprite with a nineslice writer
            var baseSprite = IScreenSprite.ofAssetLocation(
                    ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png"),
                    256, 256
            );
            DEFAULT_SPRITE = wrapWithWriter(baseSprite, new ScreenSpriteWriterNineSlice(4, 4, 4, 4));
        }
        return DEFAULT_SPRITE;
    }

    private static IScreenSprite wrapWithWriter(IScreenSprite base, IScreenSpriteWriter writer) {
        return new IScreenSprite() {
            public IScreenSpriteWriter getWriter() { return writer; }
            public ResourceLocation getTextureLocation() { return base.getTextureLocation(); }

            // Use only the inventory panel area: 176x166 pixels starting from 0,0
            public float getMinU() { return 0F; }
            public float getMinV() { return 0F; }
            public float getMaxU() { return 176F / 256F; }  // UV for the panel edge
            public float getMaxV() { return 166F / 256F; }  // UV for the panel edge
            public float getWidth() { return 176F / 256F; }
            public float getHeight() { return 166F / 256F; }
            public int getWidthInPixels() { return 176; }
            public int getHeightInPixels() { return 166; }
        };
    }

    @Override
    public void compose(ICompositionScope scope) {
        IComposableElementScope<?> elementScope = (IComposableElementScope<?>) scope;

        // Stack the background sprite and content
        scope.e(new Stack(), stack -> {
            // Background: use sprite if available, otherwise fallback to colored rectangle
            IScreenSprite sprite = this.spriteSupplier.get();
            if (sprite != null) {
                // Background nineslice sprite (ElementSprite already supports Supplier)
                // Sprite will fill whatever bounds are set during placement
                stack.e(new ElementSprite(this.spriteSupplier));
            } else {
                // Fallback: gray background similar to Minecraft's inventory
                stack.e(new ElementRect(0xFFC6C6C6));  // Light gray background
            }

            // Content slot - render slot content directly in the stack
            SlotContent slotContent = elementScope.getSlotMap().get(CONTENT_SLOT);
            if (slotContent != null) {
                slotContent.render(stack);
            }
        });
    }

    @Override
    public Size measure(Constraints constraints, List<IElement> children) {
        System.out.println("=== ElementBox MEASURE DEBUG ===");
        System.out.println("Input constraints: " + constraints);

        var props = this.getLayoutProperties();
        if (props != null) {
            System.out.println("Layout properties:");
            System.out.println("  Fixed size: " + props.getFixedWidth() + "x" + props.getFixedHeight());
            System.out.println("  Min size: " + props.getMinWidth() + "x" + props.getMinHeight());
            System.out.println("  Max size: " + props.getMaxWidth() + "x" + props.getMaxHeight());
        }

        // Step 1: Calculate final size using the layout system helper
        // Use large intrinsic size since we don't have a natural size
        var finalSize = LayoutHelper.calculateSizeWithProperties(
            Integer.MAX_VALUE, Integer.MAX_VALUE,  // No intrinsic size preference
            this.getLayoutProperties(),
            constraints
        );

        System.out.println("Calculated final size: " + finalSize.width() + "x" + finalSize.height());
        System.out.println("Children count: " + children.size());

        // Step 2: Measure children with constraints matching our decided size
        if (!children.isEmpty()) {
            // Give children exact constraints matching our final size
            var childConstraints = Constraints.fixed(finalSize.width(), finalSize.height());
            System.out.println("Child constraints: " + childConstraints);
            var childSize = LayoutHelper.measureChild(children.getFirst(), childConstraints);
            System.out.println("Child measured size: " + childSize.width() + "x" + childSize.height());
        }

        // Step 3: Return our final size to parent
        System.out.println("=== END ElementBox MEASURE DEBUG ===");
        return finalSize;
    }

    @Override
    public void place(Bounds bounds, List<IElement> children) {
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
