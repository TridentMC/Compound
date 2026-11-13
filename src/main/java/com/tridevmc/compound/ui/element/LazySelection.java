package com.tridevmc.compound.ui.element;

import com.mojang.blaze3d.platform.InputConstants;
import com.tridevmc.compound.ui.event.KeyInputEvent;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.tree.ITreeNode;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntSupplier;

final class LazySelection<K> {
    private final int columns;
    private final int pitchX;
    private final int pitchY;
    private final int cellWidth;
    private final int cellHeight;
    private Optional<ISelection<K>> selection = Optional.empty();

    LazySelection(int columns, int pitchX, int pitchY, int cellWidth, int cellHeight) {
        this.columns = columns;
        this.pitchX = pitchX;
        this.pitchY = pitchY;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
    }

    void model(ISelection<K> model) {
        this.selection = Optional.of(Objects.requireNonNull(model));
    }

    boolean isEnabled() {
        return this.selection.isPresent();
    }

    void compose(ICompositionScope scope, ITreeNode owner) {
        if (this.isEnabled()) scope.onKeyPress(event -> scope.isFocused() && this.navigate(owner, event));
    }

    void reconcile(ILazySource<?, K> source) {
        this.selection.ifPresent(model -> {
            var keys = new ArrayList<K>(source.size());
            for (int index = 0; index < source.size(); index++) keys.add(source.key(index));
            model.setKeys(keys);
        });
    }

    void decorate(K key, ICompositionScope cell, ICompositionScope owner) {
        this.selection.ifPresent(model -> {
            cell.onClick(event -> {
                if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
                owner.requestFocus();
                model.select(key, event.shiftDown(), event.ctrlDown());
                return true;
            });
            cell.e(new Outline(() -> model.isSelected(key) ? 0xFFFFFFFF
                    : owner.isFocused() && model.isFocused(key) ? 0xFF808080 : 0),
                    outline -> outline.layout().fillMax());
        });
    }

    private boolean navigate(ITreeNode owner, KeyInputEvent event) {
        var model = this.selection.orElseThrow();
        var keys = model.getKeys();
        if (event.isShortcut(InputConstants.KEYCODE_A) && model.getMode() == EnumSelectionMode.MULTIPLE) {
            model.selectAll();
            return true;
        }
        if (event.keyCode() == InputConstants.KEY_ESCAPE && !model.getSelectedKeys().isEmpty()) {
            model.clear();
            return true;
        }
        if (keys.isEmpty()) return false;
        int current = model.getFocusedKey().map(keys::indexOf).orElse(-1);
        if (event.keyCode() == InputConstants.KEY_SPACE && current >= 0) {
            model.select(keys.get(current), event.shiftDown(), event.ctrlDown());
            return true;
        }
        int target = switch (event.keyCode()) {
            case InputConstants.KEY_LEFT -> Math.max(0, current - 1);
            case InputConstants.KEY_RIGHT -> Math.min(keys.size() - 1, current + 1);
            case InputConstants.KEY_UP -> Math.max(0, current - this.columns);
            case InputConstants.KEY_DOWN -> current < 0 ? 0 : Math.min(keys.size() - 1, current + this.columns);
            case InputConstants.KEY_HOME -> 0;
            case InputConstants.KEY_END -> keys.size() - 1;
            default -> -1;
        };
        if (target < 0) return false;
        if (model.getMode() == EnumSelectionMode.MULTIPLE && event.ctrlDown() && !event.shiftDown()) {
            model.focus(keys.get(target));
        } else {
            model.select(keys.get(target), event.shiftDown(), event.ctrlDown());
        }
        this.reveal(owner, target);
        return true;
    }

    private void reveal(ITreeNode node, int index) {
        long left = (long) node.getBounds().x() + (long) (index % this.columns) * this.pitchX;
        long top = (long) node.getBounds().y() + (long) (index / this.columns) * this.pitchY;
        for (var ancestor = node.getParent(); ancestor != null; ancestor = ancestor.getParent()) {
            if (!(ancestor.getElement() instanceof ScrollArea area)) continue;
            var viewport = area.getBounds();
            var children = ancestor.getChildren();
            if (children.isEmpty() || children.getFirst().getAllocatedBounds() == null) continue;
            // Allocated content bounds record the applied offsets, before margins. Live
            // offsets may already include wheel or keyboard events awaiting another layout.
            var placedContent = children.getFirst().getAllocatedBounds();
            long placedX = (long) viewport.x() - placedContent.x();
            long placedY = (long) viewport.y() - placedContent.y();
            int oldX = area.getScrollXState().get();
            int oldY = area.getScrollYState().get();
            left -= oldX - placedX;
            top -= oldY - placedY;
            long deltaX = left < viewport.left() ? left - viewport.left()
                    : left + this.cellWidth > viewport.right() ? left + this.cellWidth - viewport.right() : 0;
            long deltaY = top < viewport.top() ? top - viewport.top()
                    : top + this.cellHeight > viewport.bottom() ? top + this.cellHeight - viewport.bottom() : 0;
            int nextX = Math.clamp(oldX + deltaX, 0, area.getMaxScrollX());
            int nextY = Math.clamp(oldY + deltaY, 0, area.getMaxScrollY());
            area.scrollTo(nextX, nextY);
            left -= nextX - oldX;
            top -= nextY - oldY;
        }
    }

    private static final class Outline extends Rect {
        private final IntSupplier colour;

        Outline(IntSupplier colour) {
            super(0);
            this.colour = colour;
        }

        @Override public boolean isHitTestable() { return false; }

        @Override
        protected void drawElement(IScreenContext context, Bounds bounds) {
            int colour = this.colour.getAsInt();
            if (colour != 0) context.drawRectOutline(bounds.x(), bounds.y(), bounds.width(), bounds.height(), colour, 1);
        }
    }
}
