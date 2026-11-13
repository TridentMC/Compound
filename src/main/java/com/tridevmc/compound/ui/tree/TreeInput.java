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

import javax.annotation.Nullable;
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
    @Nullable private FocusRequest pendingFocus;
    private boolean changingFocus;
    private long focusResetVersion;
    private ITreeNode activeInputRoot;
    private boolean scopeOrderDirty = true;

    private record InputScope(ITreeNode node, ITreeNode previousFocus) { }
    private record FocusRequest(@Nullable ITreeNode target) { }


    TreeInput(UITree tree) {
        this.tree = tree;
    }

    void reset() {
        var wasChangingFocus = this.changingFocus;
        this.changingFocus = true;
        this.focusResetVersion++;
        this.pendingFocus = null;
        try {
            this.loseFocus();
            this.inputScopes.clear();
            this.activeInputRoot = null;
            this.scopeOrderDirty = true;
            this.tree.releaseWindowPin(this.capturedNode);
            this.capturedNode = null;
            this.capturedButton = -1;
            this.lastHoveredPath.clear();
            this.requestedCursor = CursorTypes.ARROW;
        } finally {
            this.pendingFocus = null;
            this.changingFocus = wasChangingFocus;
        }
    }

    void detach(ITreeNode node) {
        this.scopeOrderDirty = true;
        this.clearInputRoot(node);
        this.lastHoveredPath.remove(node);
        if (this.capturedNode == node) {
            this.tree.releaseWindowPin(this.capturedNode);
            this.capturedNode = null;
            this.capturedButton = -1;
        }
        if (this.focusedNode == node) {
            // Detachment must notify loss before this node's owned resources are disposed.
            if (this.changingFocus) this.loseFocus();
            else this.clearFocus();
        }
    }

    Position mousePosition() {
        return new Position(this.lastMouseX, this.lastMouseY);
    }

    ITreeNode hoveredNode() {
        return this.tree.findNodeAt(this.lastMouseX, this.lastMouseY);
    }

    public void setInputRoot(ITreeNode node) {
        if (!this.tree.isAttached(node) || this.inputRoot() == node
                || this.inputScopes.stream().anyMatch(scope -> scope.node() == node)) return;
        var previousRoot = this.inputRoot();
        this.inputScopes.add(new InputScope(node, this.focusedNode));
        this.scopeOrderDirty = true;
        if (this.inputRoot() != previousRoot) {
            if (!this.acceptsInput(this.focusedNode)) this.clearFocus();
            this.tree.releaseWindowPin(this.capturedNode);
            this.capturedNode = null;
            this.capturedButton = -1;
        }
        // The scope's overlay descendants may be composed after this registration.
        this.scopeOrderDirty = true;
        this.tree.requestHoverUpdate();
    }

    public void clearInputRoot(ITreeNode node) {
        for (int i = this.inputScopes.size() - 1; i >= 0; i--) {
            var entry = this.inputScopes.get(i);
            if (entry.node() == node) {
                var wasActive = this.inputRoot() == node;
                this.inputScopes.remove(i);
                if (i < this.inputScopes.size()) {
                    var following = this.inputScopes.get(i);
                    var previous = following.previousFocus();
                    if (previous == null || previous == node || node.isAncestorOf(previous)) {
                        this.inputScopes.set(i, new InputScope(following.node(), entry.previousFocus()));
                    }
                }
                this.scopeOrderDirty = true;
                if (wasActive) {
                    var previous = entry.previousFocus();
                    this.requestFocus(this.acceptsInput(previous) ? previous : null);
                }
                this.tree.requestHoverUpdate();
                return;
            }
        }
    }

    ITreeNode inputRoot() {
        if (this.inputScopes.isEmpty()) return this.tree.getRoot();
        if (this.scopeOrderDirty) {
            this.activeInputRoot = InputScopeOrder.highest(this.tree,
                    this.inputScopes.stream().map(InputScope::node).toList());
            this.scopeOrderDirty = false;
        }
        return this.activeInputRoot;
    }

    private boolean hasActiveScope() {
        var root = this.inputRoot();
        return this.acceptsInput(root)
                && this.inputScopes.stream().anyMatch(scope -> scope.node() == root);
    }

    private boolean acceptsInput(ITreeNode node) {
        if (!this.tree.isAttached(node)) return false;
        var inputRoot = this.inputRoot();
        for (var current = node; current != null; current = current.getParent()) {
            if (!current.getElement().isVisible()) return false;
            if (current == inputRoot) {
                for (var parent = current.getParent(); parent != null; parent = parent.getParent()) {
                    if (!parent.getElement().isVisible()) return false;
                }
                return true;
            }
        }
        return false;
    }

    private void validateTargets() {
        this.scopeOrderDirty = true;
        if (this.focusedNode != null && !this.acceptsInput(this.focusedNode)) this.clearFocus();
        if (this.capturedNode != null && !this.acceptsInput(this.capturedNode)) {
            this.tree.releaseWindowPin(this.capturedNode);
            this.capturedNode = null;
            this.capturedButton = -1;
        }
    }

    public ITreeNode getFocusedNode() {
        return this.focusedNode;
    }

    ITreeNode getCapturedNode() {
        return this.capturedNode;
    }

    public void requestFocus(ITreeNode node) {
        if (node != null && !this.acceptsInput(node)) return;
        this.pendingFocus = new FocusRequest(node);
        if (this.changingFocus) return;
        this.changingFocus = true;
        try {
            for (var pass = 0; this.pendingFocus != null; pass++) {
                if (pass == 64) throw new IllegalStateException("Focus requests did not settle in 64 transitions");
                var request = this.pendingFocus;
                this.pendingFocus = null;
                this.changeFocus(request.target());
            }
        } finally {
            this.changingFocus = false;
            this.pendingFocus = null;
        }
    }

    private void changeFocus(ITreeNode node) {
        if (this.focusedNode == node) return;
        var resetVersion = this.focusResetVersion;
        this.loseFocus();
        // A lost callback can redirect or clear focus; finish its notification batch first.
        if (resetVersion != this.focusResetVersion || this.pendingFocus != null) return;
        if (node == null || !this.acceptsInput(node)) return;

        this.focusedNode = node;
        if (UITree.DEBUG_EVENTS) {
            System.out.println("[UITree] focus=" + node.getElement().getClass().getSimpleName());
        }
        for (var handler : node.getFocusGainedHandlers()) {
            if (resetVersion != this.focusResetVersion || this.focusedNode != node
                    || !this.acceptsInput(node)) return;
            handler.run();
        }
        if (resetVersion != this.focusResetVersion || this.focusedNode != node
                || !this.acceptsInput(node)
                || this.pendingFocus != null && this.pendingFocus.target() != node) return;
        var minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.getNarrator() != null) {
            minecraft.getNarrator().saySystemNow(this.narrationMessage(node));
        }
    }

    private void loseFocus() {
        var previousFocused = this.focusedNode;
        this.tree.releaseWindowPin(previousFocused);
        this.focusedNode = null;
        if (previousFocused != null) {
            for (var handler : previousFocused.getFocusLostHandlers()) {
                handler.run();
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
        this.requestFocus(null);
    }

    public boolean hasFocus(ITreeNode node) {
        return this.focusedNode == node;
    }

    public boolean dispatchClick(int x, int y, MouseClickEvent event) {
        this.validateTargets();
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
                        if (this.acceptsInput(current)) {
                            this.capturedNode = current;
                            this.capturedButton = event.button();
                        }
                        consumed = true;
                        break;
                    }
                }
                current = current.getParent();
            }
            return consumed || this.hasActiveScope();
        }
        return this.hasActiveScope();
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
        this.validateTargets();
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
        this.validateTargets();
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
        return this.hasActiveScope();
    }

    public boolean dispatchKeyPress(KeyInputEvent event) {
        this.validateTargets();
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
        return this.hasActiveScope();
    }

    private boolean moveFocus(boolean backwards) {
        var candidates = new ArrayList<ITreeNode>();
        this.tree.walkDepthFirst(this.inputRoot(), node -> {
            if (this.acceptsInput(node) && node.getElement().isFocusable()
                    && this.isVisibleForFocus(node)) candidates.add(node);
        });
        if (candidates.isEmpty()) return this.hasActiveScope();
        var index = candidates.indexOf(this.focusedNode);
        var next = index < 0 ? (backwards ? candidates.size() - 1 : 0)
                : Math.floorMod(index + (backwards ? -1 : 1), candidates.size());
        this.requestFocus(candidates.get(next));
        return true;
    }

    private boolean isVisibleForFocus(ITreeNode node) {
        var bounds = node.getBounds();
        if (bounds == null || bounds.width() <= 0 || bounds.height() <= 0) return false;
        var geometry = node.getFrameGeometry();
        if (geometry != null) {
            double determinant = geometry.a() * geometry.d() - geometry.b() * geometry.c();
            if (!Double.isFinite(determinant) || Math.abs(determinant) < 1e-12) return false;
            return this.intersectsViewport(geometry.left(), geometry.top(), geometry.right(),
                    geometry.bottom(), geometry.clip());
        }
        for (var parent = node.getParent(); parent != null; parent = parent.getParent()) {
            if (parent.getLayoutProperties().isClip() && parent.getBounds() != null) {
                bounds = bounds.intersection(parent.getBounds());
            }
            if (parent == this.inputRoot() || parent.getLayoutProperties().getLayer() > 0) break;
        }
        return this.intersectsViewport(bounds.left(), bounds.top(), bounds.right(), bounds.bottom(), null);
    }

    private boolean intersectsViewport(double left, double top, double right, double bottom,
                                       @Nullable Bounds clip) {
        if (clip != null) {
            left = Math.max(left, clip.left());
            top = Math.max(top, clip.top());
            right = Math.min(right, clip.right());
            bottom = Math.min(bottom, clip.bottom());
        }
        var viewport = this.tree.getViewportSize();
        if (viewport.width() >= 0 && viewport.height() >= 0) {
            left = Math.max(left, 0);
            top = Math.max(top, 0);
            right = Math.min(right, viewport.width());
            bottom = Math.min(bottom, viewport.height());
        }
        return right > left && bottom > top;
    }

    public boolean dispatchKeyRelease(KeyInputEvent event) {
        this.validateTargets();
        var current = this.focusedNode;
        while (current != null && this.acceptsInput(current)) {
            for (var handler : current.getKeyReleaseHandlers()) {
                if (handler.apply(event)) return true;
            }
            current = current.getParent();
        }
        return this.hasActiveScope();
    }

    public boolean dispatchCharTyped(CharEvent event) {
        this.validateTargets();
        if (this.focusedNode != null && this.acceptsInput(this.focusedNode)) {
            for (var handler : this.focusedNode.getCharTypedHandlers()) {
                if (handler.apply(event)) return true;
            }
        }
        return this.hasActiveScope();
    }

    public boolean dispatchMouseRelease(int x, int y, MouseReleaseEvent event) {
        this.validateTargets();
        ITreeNode node = this.capturedNode != null && this.capturedButton == event.button()
                ? this.capturedNode : this.tree.findNodeAt(x, y);
        if (this.capturedButton == event.button()) {
            this.tree.releaseWindowPin(this.capturedNode);
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
            return consumed || this.hasActiveScope();
        }
        return this.hasActiveScope();
    }

    public boolean dispatchMouseDrag(int x, int y, MouseDragEvent event) {
        this.validateTargets();
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
            return consumed || this.hasActiveScope();
        }
        return this.hasActiveScope();
    }
}
