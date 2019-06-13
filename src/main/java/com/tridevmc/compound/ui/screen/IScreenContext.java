package com.tridevmc.compound.ui.screen;

import com.tridevmc.compound.ui.EnumUILayer;
import com.tridevmc.compound.ui.Rect2D;
import com.tridevmc.compound.ui.UVData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.net.URI;
import java.util.List;

/**
 * Provides useful methods and variables for drawing a UI and it's elements.
 * This should be used in place of any direct calls to minecraft GUI methods.
 */
public interface IScreenContext {

    /**
     * Gets the width of the screen.
     *
     * @return the width of the screen.
     */
    int getWidth();

    /**
     * Gets the height of the screen.
     *
     * @return the height of the screen.
     */
    int getHeight();

    /**
     * Gets the current x coordinate of the mouse on the screen.
     *
     * @return the current x coordinate of the mouse.
     */
    float getMouseX();

    /**
     * Gets the current y coordinate of the mouse on the screen.
     *
     * @return the current y coordinate of the mouse.
     */
    float getMouseY();

    /**
     * Gets the partial tick value for the current frame.
     *
     * @return the current partial tick value.
     */
    float getPartialTicks();

    /**
     * Gets the amount of ticks that have passed since the ui was opened.
     *
     * @return the amount of ticks that have passed since the ui was opened.
     */
    long getTicks();

    /**
     * Gets the active gui that's currently being displayed.
     *
     * @return the active gui currently being displayed.
     */
    Screen getActiveGui();

    /**
     * Gets the current Minecraft game instance.
     *
     * @return the game instance.
     */
    Minecraft getMc();

    /**
     * Gets the font renderer used by the game.
     *
     * @return the font renderer used by the game.
     */
    FontRenderer getFontRenderer();

    /**
     * Binds the given texture to the texture manager.
     *
     * @param texture the texture to bind.
     */
    void bindTexture(ResourceLocation texture);

    /**
     * Draws a solid single colour rect on the screen matching the provided rect data.
     *
     * @param rect   the position and dimensions of the rect to draw.
     * @param colour the colour of the rect to draw.
     */
    void drawRect(Rect2D rect, int colour);

    /**
     * Draws a solid gradient rect on the screen matching the provided rect data.
     *
     * @param rect        the position and dimensions of the rect to draw.
     * @param startColour the colour at the beginning of the gradient.
     * @param endColour   the colour at the end of the gradient.
     */
    void drawGradientRect(Rect2D rect, int startColour, int endColour);

    /**
     * Draws the given string on the screen at the given position.
     *
     * @param text   the string to draw.
     * @param x      the x position to draw the string at.
     * @param y      the y position to draw the string at.
     * @param colour the colour to draw the string in.
     */
    void drawString(String text, double x, double y, int colour);

    /**
     * Draws the given string on the screen with the middle of the string centered on the given position.
     *
     * @param text   the string to draw.
     * @param x      the x position to draw the string at.
     * @param y      the y position to draw the string at.
     * @param colour the colour to draw the string in.
     */
    void drawCenteredString(String text, double x, double y, int colour);

    /**
     * Draws the given string on the screen with the middle of the string centered on the given position, with a drop shadow applied.
     *
     * @param text   the string to draw.
     * @param x      the x position to draw the string at.
     * @param y      the y position to draw the string at.
     * @param colour the colour to draw the string in.
     */
    void drawStringWithShadow(String text, double x, double y, int colour);

    /**
     * Draws the given string on the screen at the given position, with a drop shadow applied.
     *
     * @param text   the string to draw.
     * @param x      the x position to draw the string at.
     * @param y      the y position to draw the string at.
     * @param colour the colour to draw the string in.
     */
    void drawCenteredStringWithShadow(String text, double x, double y, int colour);

    /**
     * Draws a textured rect on the screen matching the provided rect data.
     *
     * @param rect the position and dimensions of the rect to draw.
     * @param uvs  the uv data for the rect.
     */
    void drawTexturedRect(Rect2D rect, UVData uvs);

    /**
     * Draws a textured rect on the screen matching the provided rect data.
     *
     * @param x      the x coordinate to draw the rect at.
     * @param y      the y coordinate to draw the rect at.
     * @param minUvs the minimum uvs for the rect.
     * @param maxUvs the maximum uvs for the rect.
     */
    void drawTexturedRect(float x, float y, UVData minUvs, UVData maxUvs);

    /**
     * Draws a textured rect on the screen matching the provided rect data.
     *
     * @param rect   the position and dimensions of the rect to draw.
     * @param minUvs the minimum uvs for the rect.
     * @param maxUvs the maximum uvs for the rect.
     */
    void drawTexturedRect(Rect2D rect, UVData minUvs, UVData maxUvs);

    /**
     * Draws a textured rect on the screen matching the provided rect data.
     *
     * @param rect   the position and dimensions of the rect to draw.
     * @param sprite the sprite to draw on the screen, used for gathering uv data.
     */
    void drawTexturedRect(Rect2D rect, TextureAtlasSprite sprite);

    /**
     * Draws a textured rect on the screen matching the provided rect data.
     *
     * @param rect          the position and dimensions of the rect to draw.
     * @param uvs           the uv data for the rect.
     * @param textureWidth  the width of the texture that is being drawn.
     * @param textureHeight the height of the texture that is being drawn.
     */
    void drawTexturedRect(Rect2D rect, UVData uvs, float textureWidth, float textureHeight);

    /**
     * Draws a scaled, tiled, and textured rect on the screen matching the provided rect data.
     *
     * @param rect       the position and dimensions of the rect to draw.
     * @param uvs        the uv data for the rect.
     * @param uWidth     the width of the uv map.
     * @param vHeight    the height of the uv map.
     * @param tileWidth  the width of the tile to draw.
     * @param tileHeight the height of the tile to draw.
     */
    void drawTexturedRect(Rect2D rect, UVData uvs, int uWidth, int vHeight, float tileWidth, float tileHeight);

    /**
     * Draws a tiled textured rect on the screen matching the provided rect data.
     *
     * @param rect  the position and dimensions of the rect to draw.
     * @param uvMin the minimum uv data of the texture to tile.
     * @param uvMax the maximum uv data of the texture to tile.
     */
    void drawTiledTexturedRect(Rect2D rect, UVData uvMin, UVData uvMax);

    /**
     * Draws the tooltip for the given itemstack at the given coordinates.
     *
     * @param stack the itemstack to draw the tooltip of.
     * @param x     the x position to draw the tooltip at.
     * @param y     the y position to draw the tooltip at.
     */
    void drawTooltip(ItemStack stack, int x, int y);

    /**
     * Draws the given text as a tooltip at the given coordinates.
     *
     * @param text the text to draw.
     * @param x    the x position to draw the tooltip at.
     * @param y    the y position to draw the tooltip at.
     */
    void drawTooltip(String text, int x, int y);

    /**
     * Draws the given text as a tooltip at the given coordinates.
     *
     * @param text         the text to draw.
     * @param x            the x position to draw the tooltip at.
     * @param y            the y position to draw the tooltip at.
     * @param fontRenderer the font renderer to use when drawing the text.
     */
    void drawTooltip(String text, int x, int y, FontRenderer fontRenderer);

    /**
     * Draws a multi-line tooltip from the given list of lines at the given coordinates.
     *
     * @param lines the text to draw, each list entry representing a new line.
     * @param x     the x position to draw the tooltip at.
     * @param y     the y position to draw the tooltip at.
     */
    void drawTooltip(List<String> lines, int x, int y);

    /**
     * Draws a multi-line tooltip from the given list of lines at the given coordinates.
     *
     * @param lines        the text to draw, each list entry representing a new line.
     * @param x            the x position to draw the tooltip at.
     * @param y            the y position to draw the tooltip at.
     * @param fontRenderer the font renderer to use when drawing the text.
     */
    void drawTooltip(List<String> lines, int x, int y, FontRenderer fontRenderer);

    /**
     * Draws the appropriate tooltip for the given text component.
     *
     * @param component the text component to draw the tooltip for.
     * @param x         the x position to draw the tooltip at.
     * @param y         the y position to draw the tooltip at.
     */
    void drawTooltip(ITextComponent component, int x, int y);

    /**
     * Draws the given itemstack on the screen within the given dimensions.
     *
     * @param stack      the stack to draw.
     * @param dimensions the dimensions to draw the stack within.
     * @param altText    the text to draw if no stack count will be drawn.
     */
    void drawItemStack(ItemStack stack, Rect2D dimensions, String altText);

    /**
     * Draws the given itemstack on the screen at the given coordinates.
     *
     * @param stack   the stack to draw.
     * @param x       the x position to draw the stack at.
     * @param y       the y position to draw the stack at.
     * @param altText the text to draw if no stack count will be drawn.
     */
    void drawItemStack(ItemStack stack, int x, int y, String altText);

    /**
     * Sends a chat message to the server and adds it to the local chat log.
     *
     * @param message the message to send.
     */
    void sendChatMessage(String message);

    /**
     * Sends a chat message to the server and adds it to the local chat log if specified.
     *
     * @param message   the message to send.
     * @param addToChat determines whether the message should be added to the local chat.
     */
    void sendChatMessage(String message, boolean addToChat);

    /**
     * Directly opens the given URI without prompting the user.
     *
     * @param url the url to open.
     */
    void openWebLink(URI url);

    /**
     * Determines if the shift key is currently being held down.
     *
     * @return true if the key is held down, false otherwise.
     */
    boolean isShiftDown();

    /**
     * Determines if the alt key is currently being held down.
     *
     * @return true if the key is held down, false otherwise.
     */
    boolean isAltDown();

    /**
     * Converts a packed colour into an array of floats matching [R, G, B, A]
     *
     * @param colour the colour to unpack.
     * @return the array of values.
     */
    float[] getRGBA(int colour);

    /**
     * Gets the layer currently being drawn on the screen.
     *
     * @return the current layer being drawn.
     */
    EnumUILayer getCurrentLayer();

}
