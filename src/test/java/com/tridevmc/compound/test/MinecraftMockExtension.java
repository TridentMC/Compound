package com.tridevmc.compound.test;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.gui.GuiMetadataSection;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.server.Bootstrap;

import java.util.Optional;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.MockedStatic;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.RETURNS_SMART_NULLS;

/**
 * JUnit extension that properly mocks Minecraft for tests.
 * The static initializer sets up mocks BEFORE test classes load.
 */
public class MinecraftMockExtension implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {
    private static MockedStatic<Minecraft> minecraftMock;

    // Static initializer runs when THIS class loads, which is before test classes load
    static {
        try {
            // Try to initialize Minecraft's Bootstrap to allow registries to work
            // This may fail with FML errors, but might initialize enough for static fields
            try {
                Bootstrap.bootStrap();
            } catch (Throwable bootstrapError) {
                // Bootstrap might fail due to missing FML, but some initialization may have succeeded
                // Log but continue - we'll mock what we can
                System.err.println("Bootstrap initialization failed (expected in test environment): " + bootstrapError.getMessage());
            }

            // Create a mock Minecraft instance with deep stubs for method chaining
            // Use lenient mode to avoid strict stubbing issues
            Minecraft minecraft = mock(Minecraft.class, withSettings()
                    .defaultAnswer(RETURNS_DEEP_STUBS)
                    .lenient());

            // Create a mock sprite to return from the chain
            TextureAtlasSprite mockSprite = mock(TextureAtlasSprite.class);

            // Create a mock GUI metadata section with proper scaling
            GuiSpriteScaling mockScaling = mock(GuiSpriteScaling.class, RETURNS_DEEP_STUBS);
            lenient().when(mockScaling.type()).thenReturn(GuiSpriteScaling.Type.STRETCH);

            GuiMetadataSection mockMetadata = mock(GuiMetadataSection.class);
            lenient().when(mockMetadata.scaling()).thenReturn(mockScaling);

            // Create a mock sprite contents for the sprite
            SpriteContents mockContents = mock(SpriteContents.class);
            lenient().when(mockContents.getAdditionalMetadata(any())).thenReturn(Optional.of(mockMetadata));
            lenient().when(mockSprite.contents()).thenReturn(mockContents);

            // Pre-stub the entire chain that ComposedSlot needs
            // This must be done before ComposedSlot loads
            lenient().when(minecraft.getAtlasManager().getAtlasOrThrow(any()).getSprite(any()))
                    .thenReturn(mockSprite);

            // Create a mock Font for Text.measure()
            net.minecraft.client.gui.Font mockFont = mock(net.minecraft.client.gui.Font.class, withSettings().lenient());
            lenient().when(mockFont.width((net.minecraft.network.chat.FormattedText) any())).thenAnswer(invocation -> {
                net.minecraft.network.chat.FormattedText text = invocation.getArgument(0);
                // Return 6 pixels per character as a reasonable approximation
                return text.getString().length() * 6;
            });
            lenient().when(mockFont.width((String) any())).thenAnswer(invocation -> {
                String text = invocation.getArgument(0);
                return text.length() * 6;
            });
            // Set lineHeight to 9 (vanilla font height) using reflection
            try {
                var lineHeightField = net.minecraft.client.gui.Font.class.getDeclaredField("lineHeight");
                lineHeightField.setAccessible(true);
                lineHeightField.set(mockFont, 9);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                // lineHeight is public final, may fail - font mock will still work for width()
            }
            // Assign the font mock to the minecraft.font field using reflection (it's final)
            try {
                var fontField = Minecraft.class.getDeclaredField("font");
                fontField.setAccessible(true);
                fontField.set(minecraft, mockFont);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException("Failed to set minecraft.font via reflection", e);
            }

            // Create static mock
            minecraftMock = mockStatic(Minecraft.class);
            minecraftMock.when(Minecraft::getInstance).thenReturn(minecraft);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Minecraft mocks", e);
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        // Register a callback to close the mock when tests complete
        context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL)
                .put("MinecraftMockExtension", this);
    }

    @Override
    public void close() {
        if (minecraftMock != null) {
            minecraftMock.close();
        }
    }
}
