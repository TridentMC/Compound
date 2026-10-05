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

package com.tridevmc.compound.ui.tree;

import com.tridevmc.compound.ui.cursor.UICursor;
import com.tridevmc.compound.ui.event.CharEvent;
import com.tridevmc.compound.ui.event.KeyInputEvent;
import com.tridevmc.compound.ui.event.MouseClickEvent;
import com.tridevmc.compound.ui.event.MouseDragEvent;
import com.tridevmc.compound.ui.event.MouseMoveEvent;
import com.tridevmc.compound.ui.event.MouseReleaseEvent;
import com.tridevmc.compound.ui.event.MouseScrollEvent;
import com.tridevmc.compound.ui.geometry.api.LocalPoint;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Position;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class TreeInput {
    private final UITree tree;
    private ITreeNode focusedNode;
    private ITreeNode capturedNode;
    private int capturedButton = -1;
    private final List<InputScope> inputScopes = new ArrayList<>();
    private int lastMouseX;
    private int lastMouseY;
    private Set<ITreeNode> lastHoveredPath = new HashSet<>();
    private CursorType requestedCursor = CursorTypes.ARROW;

    private record InputScope(ITreeNode node, ITreeNode previousFocus) { }


    TreeInput(UITree tree) {
        this.tree = tree;
    }

    void reset() {
        this.clearFocus();
        this.inputScopes.clear();
        this.capturedNode = null;
        this.capturedButton = -1;
        this.lastHoveredPath.clear();
        this.requestedCursor = CursorTypes.ARROW;
    }

    void detach(ITreeNode node) {
        this.clearInputRoot(node);
        this.lastHoveredPath.remove(node);
        if (this.capturedNode == node) {
            this.capturedNode = null;
            this.capturedButton = -1;
        }
        if (this.focusedNode == node) this.clearFocus();
    }

    Position mousePosition() {
        return new Position(this.lastMouseX, this.lastMouseY);
    }

    ITreeNode hoveredNode() {
        return this.tree.findNodeAt(this.lastMouseX, this.lastMouseY);
    }

    public void setInputRoot(ITreeNode node) {
        if (node == null || this.inputRoot() == node) return;
        this.inputScopes.add(new InputScope(node, this.focusedNode));
        if (this.focusedNode != node && (this.focusedNode == null || !node.isAncestorOf(this.focusedNode))) {
            this.clearFocus();
        }
        this.capturedNode = null;
        this.tree.requestHoverUpdate();
    }

    public void clearInputRoot(ITreeNode node) {
        for (int i = this.inputScopes.size() - 1; i >= 0; i--) {
            var entry = this.inputScopes.get(i);
            if (entry.node() == node) {
                var wasActive = i == this.inputScopes.size() - 1;
                this.inputScopes.remove(i);
                if (wasActive) {
                    var previous = entry.previousFocus();
                    this.requestFocus(this.tree.isAttached(previous) ? previous : null);
                }
                this.tree.requestHoverUpdate();
                return;
            }
        }
    }

    ITreeNode inputRoot() {
        return this.inputScopes.isEmpty() ? this.tree.getRoot() : this.inputScopes.getLast().node();
    }

    private boolean acceptsInput(ITreeNode node) {
        var inputRoot = this.inputRoot();
        return node != null && (node == inputRoot || (inputRoot != null && inputRoot.isAncestorOf(node)));
    }

    public ITreeNode getFocusedNode() {
        return this.focusedNode;
    }

    public void requestFocus(ITreeNode node) {
        if (node != null && (!this.tree.isAttached(node) || !this.acceptsInput(node))) return;
        if (this.focusedNode == node) return;

        var previousFocused = this.focusedNode;
        this.focusedNode = null;

        if (previousFocused != null) {
            for (var handler : previousFocused.getFocusLostHandlers()) {
                handler.run();
            }
        }

        this.focusedNode = node;
        if (UITree.DEBUG_EVENTS) {
            System.out.println("[UITree] focus=" + (node == null ? "none" : node.getElement().getClass().getSimpleName()));
        }

        if (node != null) {
            for (var handler : node.getFocusGainedHandlers()) {
                handler.run();
            }
            var minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.getNarrator() != null) {
                minecraft.getNarrator().saySystemNow(this.narrationMessage(node));
            }
        }
    }

    private Component narrationMessage(ITreeNode node) {
        if (!node.getElement().isVisible()) return Component.empty();
        var message = node.getElement().getNarrationMessage();
        if (!message.getString().isBlank()) return message;
        var combined = Component.empty();
        for (var child : node.getChildren()) {
            var childMessage = this.narrationMessage(child);
            if (!childMessage.getString().isBlank()) {
                if (!combined.getString().isEmpty()) combined.append(", ");
                combined.append(childMessage);
            }
        }
        return combined;
    }

    public void clearFocus() {
        if (this.focusedNode != null) {
            var previousFocused = this.focusedNode;
            this.focusedNode = null;

            for (var handler : previousFocused.getFocusLostHandlers()) {
                handler.run();
            }
        }
    }

    public boolean hasFocus(ITreeNode node) {
        return this.focusedNode == node;
    }

    public boolean dispatchClick(int x, int y, MouseClickEvent event) {
        ITreeNode node = this.tree.findNodeAt(x, y);
        if (UITree.DEBUG_EVENTS) {
            System.out.println("[UITree] dispatchClick at (" + x + "," + y + ") node=" +
                    (node != null ? node.getElement().getClass().getSimpleName() : "null") +
                    " handlers=" + (node != null ? node.getClickHandlers().size() : 0));
        }

        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            var focusTarget = node;
            while (focusTarget != null && this.acceptsInput(focusTarget)
                    && !focusTarget.getElement().isFocusable()) {
                focusTarget = focusTarget.getParent();
            }
            this.requestFocus(this.acceptsInput(focusTarget) ? focusTarget : null);
        }

        if (node != null) {
            ITreeNode current = node;
            boolean consumed = false;
            while (current != null && this.acceptsInput(current) && !consumed) {
                for (var handler : current.getClickHandlers()) {
                    if (handler.apply(event)) {
                        if (UITree.DEBUG_EVENTS) {
                            System.out.println("[UITree] click consumed by " +
                                    current.getElement().getClass().getSimpleName());
                        }
                        this.capturedNode = current;
                        this.capturedButton = event.button();
                        consumed = true;
                        break;
                    }
                }
                current = current.getParent();
            }
            return consumed || !this.inputScopes.isEmpty();
        }
        return !this.inputScopes.isEmpty();
    }

    public CursorType getRequestedCursor() {
        return this.requestedCursor;
    }

    private static CursorType toPlatformCursor(UICursor cursor) {
        if (cursor == null) return CursorTypes.ARROW;
        return switch (cursor) {
            case HAND -> CursorTypes.POINTING_HAND;
            case IBEAM -> CursorTypes.IBEAM;
            case CROSSHAIR -> CursorTypes.CROSSHAIR;
            case HRESIZE -> CursorTypes.RESIZE_EW;
            case VRESIZE -> CursorTypes.RESIZE_NS;
            default -> CursorTypes.ARROW;
        };
    }

    void updateHoverState() {
        int x = this.lastMouseX;
        int y = this.lastMouseY;
        ITreeNode node = this.tree.findNodeAt(x, y);

        Set<ITreeNode> currentHoveredPath = new HashSet<>();
        if (node != null) {
            ITreeNode current = node;
            while (current != null && this.acceptsInput(current)) {
                currentHoveredPath.add(current);
                current = current.getParent();
            }
        }

        Set<ITreeNode> exitedNodes = new HashSet<>(lastHoveredPath);
        exitedNodes.removeAll(currentHoveredPath);

        Set<ITreeNode> enteredNodes = new HashSet<>(currentHoveredPath);
        enteredNodes.removeAll(lastHoveredPath);

        if (!exitedNodes.isEmpty()) {
            for (ITreeNode exitedNode : exitedNodes) {
                for (var handler : exitedNode.getMouseExitHandlers()) {
                    handler.run();
                }
            }
        }

        if (!enteredNodes.isEmpty()) {
            for (ITreeNode enteredNode : enteredNodes) {
                for (var handler : enteredNode.getMouseEnterHandlers()) {
                    handler.run();
                }
            }
        }

        lastHoveredPath = currentHoveredPath;

        UICursor cursor = null;
        if (node != null) {
            ITreeNode current = node;
            while (current != null && this.acceptsInput(current)) {
                Bounds bounds = current.getElement().getBounds();
                if (bounds != null) {
                    var geometry = current.getFrameGeometry();
                    var local = geometry == null
                            ? Optional.of(new LocalPoint(x - bounds.x(), y - bounds.y()))
                            : geometry.toLocal(x, y);
                    if (local.isEmpty()) {
                        current = current.getParent();
                        continue;
                    }
                    int localX = (int) Math.floor(local.get().x());
                    int localY = (int) Math.floor(local.get().y());
                    var nodeCursor = current.getElement().getCursor(localX, localY);
                    if (nodeCursor != null) {
                        cursor = nodeCursor;
                        break;
                    }
                }
                current = current.getParent();
            }
        }
        this.requestedCursor = toPlatformCursor(cursor);
    }

    public void dispatchMouseMove(int x, int y, MouseMoveEvent event) {
        this.lastMouseX = x;
        this.lastMouseY = y;
        this.updateHoverState();

        ITreeNode node = this.tree.findNodeAt(x, y);
        if (node != null) {
            ITreeNode current = node;
            boolean consumed = false;
            while (current != null && this.acceptsInput(current) && !consumed) {
                for (var handler : current.getMouseMoveHandlers()) {
                    if (handler.apply(event)) {
                        consumed = true;
                        break;
                    }
                }
                current = current.getParent();
            }
        }
    }

    public boolean dispatchScroll(int x, int y, MouseScrollEvent event) {
        ITreeNode node = this.tree.findNodeAt(x, y);
        if (UITree.DEBUG_EVENTS) {
            System.out.println("[UITree] dispatchScroll at (" + x + "," + y + ") node=" +
                    (node != null ? node.getElement().getClass().getSimpleName() : "null"));
        }
        if (node != null) {
            ITreeNode current = node;
            while (current != null && this.acceptsInput(current)) {
                for (var handler : current.getScrollHandlers()) {
                    boolean handled = handler.apply(event);
                    if (handled) {
                        if (UITree.DEBUG_EVENTS) {
                            System.out.println("[UITree] scroll consumed by " +
                                    current.getElement().getClass().getSimpleName());
                        }
                        return true;
                    }
                }
                current = current.getParent();
            }
        }
        return !this.inputScopes.isEmpty();
    }

    public boolean dispatchKeyPress(KeyInputEvent event) {
        if (UITree.DEBUG_EVENTS) {
            System.out.println("[UITree] key=" + event.keyCode() + " focus="
                    + (this.focusedNode == null ? "none" : this.focusedNode.getElement().getClass().getSimpleName()));
        }
        if (event.keyCode() == InputConstants.KEY_TAB && !event.ctrlDown() && !event.altDown()) {
            return this.moveFocus(event.shiftDown());
        }
        var current = this.focusedNode != null ? this.focusedNode : this.inputRoot();
        while (current != null && this.acceptsInput(current)) {
            for (var handler : current.getKeyPressHandlers()) {
                if (handler.apply(event)) return true;
            }
            current = current.getParent();
        }
        return !this.inputScopes.isEmpty();
    }

    private boolean moveFocus(boolean backwards) {
        var candidates = new ArrayList<ITreeNode>();
        this.tree.walkDepthFirst(this.inputRoot(), node -> {
            var bounds = node.getBounds();
            if (!node.getElement().isVisible() || !node.getElement().isFocusable() || bounds == null || bounds.width() <= 0 || bounds.height() <= 0) return;
            for (var parent = node.getParent(); parent != null; parent = parent.getParent()) {
                if (!parent.getElement().isVisible()) return;
                if (parent.getLayoutProperties().isClip() && parent.getBounds() != null
                        && !parent.getBounds().intersects(bounds)) return;
                if (parent == this.inputRoot() || parent.getLayoutProperties().getLayer() > 0) break;
            }
            candidates.add(node);
        });
        if (candidates.isEmpty()) return !this.inputScopes.isEmpty();
        var index = candidates.indexOf(this.focusedNode);
        var next = index < 0 ? (backwards ? candidates.size() - 1 : 0)
                : Math.floorMod(index + (backwards ? -1 : 1), candidates.size());
        this.requestFocus(candidates.get(next));
        return true;
    }

    public boolean dispatchKeyRelease(KeyInputEvent event) {
        var current = this.focusedNode;
        while (current != null && this.acceptsInput(current)) {
            for (var handler : current.getKeyReleaseHandlers()) {
                if (handler.apply(event)) return true;
            }
            current = current.getParent();
        }
        return !this.inputScopes.isEmpty();
    }

    public boolean dispatchCharTyped(CharEvent event) {
        if (this.focusedNode != null && this.acceptsInput(this.focusedNode)) {
            for (var handler : this.focusedNode.getCharTypedHandlers()) {
                if (handler.apply(event)) return true;
            }
        }
        return !this.inputScopes.isEmpty();
    }

    public boolean dispatchMouseRelease(int x, int y, MouseReleaseEvent event) {
        ITreeNode node = this.capturedNode != null && this.capturedButton == event.button()
                ? this.capturedNode : this.tree.findNodeAt(x, y);
        if (this.capturedButton == event.button()) {
            this.capturedNode = null;
            this.capturedButton = -1;
        }
        if (node != null) {
            ITreeNode current = node;
            boolean consumed = false;
            while (current != null && this.acceptsInput(current) && !consumed) {
                for (var handler : current.getMouseReleaseHandlers()) {
                    if (handler.apply(event)) {
                        consumed = true;
                        break;
                    }
                }
                current = current.getParent();
            }
            return consumed || !this.inputScopes.isEmpty();
        }
        return !this.inputScopes.isEmpty();
    }

    public boolean dispatchMouseDrag(int x, int y, MouseDragEvent event) {
        ITreeNode node = this.capturedNode != null && this.capturedButton == event.button()
                ? this.capturedNode : this.tree.findNodeAt(x, y);
        if (node != null) {
            ITreeNode current = node;
            boolean consumed = false;
            while (current != null && this.acceptsInput(current) && !consumed) {
                for (var handler : current.getMouseDragHandlers()) {
                    if (handler.apply(event)) {
                        consumed = true;
                        break;
                    }
                }
                current = current.getParent();
            }
            return consumed || !this.inputScopes.isEmpty();
        }
        return !this.inputScopes.isEmpty();
    }
}
