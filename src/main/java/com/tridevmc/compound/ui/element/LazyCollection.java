package com.tridevmc.compound.ui.element;

import com.tridevmc.compound.ui.element.internal.IWindowedElement;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.tree.ITreeNode;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

final class LazyCollection<T, K> extends Element implements ILazyCollection<T, K>, IWindowedElement {
    private final ILazySource<T, K> source;
    private final int columns;
    private final int cellWidth;
    private final int cellHeight;
    private final int spacing;
    private final int pitchX;
    private final int pitchY;
    private final BiConsumer<T, ICompositionScope> composer;
    private final Map<K, Cell> cells = new HashMap<>();
    private ICompositionScope scope;
    private ILazySource.Subscription subscription = ILazySource.Subscription.NONE;
    private final List<ILazySource.Change> pendingChanges = new ArrayList<>();
    private boolean initialized;
    private int count;
    private int width;
    private int height;
    private Bounds lastViewport = new Bounds(0, 0, 0, 0);
    private boolean viewportDirty = true;

    LazyCollection(ILazySource<T, K> source, int columns, int cellWidth, int cellHeight,
            int spacing, BiConsumer<T, ICompositionScope> composer) {
        if (columns <= 0 || cellWidth <= 0 || cellHeight <= 0 || spacing < 0) {
            throw new IllegalArgumentException("Positive columns/cell dimensions and nonnegative spacing required");
        }
        this.source = Objects.requireNonNull(source);
        this.composer = Objects.requireNonNull(composer);
        this.columns = columns;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.spacing = spacing;
        this.pitchX = coordinate((long) cellWidth + spacing);
        this.pitchY = coordinate((long) cellHeight + spacing);
        this.width = coordinate((long) columns * this.pitchX - spacing);
    }

    private static int coordinate(long value) {
        if (value < 0 || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Lazy collection extent exceeds integer coordinates");
        }
        return (int) value;
    }

    @Override
    public void compose(ICompositionScope scope) {
        this.scope = scope;
        this.cells.clear();
        this.initialized = false;
        this.viewportDirty = true;
        this.pendingChanges.clear();
        this.subscription.dispose();
        this.subscription = this.source.subscribe(change -> {
            this.pendingChanges.add(change);
            this.getNode().getTree().requestWindowSource(this.getNode());
        });
        this.getNode().getTree().requestWindowSource(this.getNode());
        this.getNode().getLayoutProperties().unboundedHeight();
    }

    @Override
    public boolean reconcileSource() {
        var changes = List.copyOf(this.pendingChanges);
        this.pendingChanges.clear();
        if (this.initialized && changes.isEmpty()) return false;
        var tree = this.getNode().getTree();
        for (var change : changes) {
            var iterator = this.cells.values().iterator();
            while (iterator.hasNext()) {
                var cell = iterator.next();
                boolean discard = switch (change.kind()) {
                    case RESET, REORDER -> true;
                    case UPDATE, REMOVE -> cell.index >= change.index()
                            && (long) cell.index < (long) change.index() + change.count();
                    case INSERT -> false;
                };
                if (discard) {
                    tree.detachNode(cell.node);
                    iterator.remove();
                } else if (cell.index >= change.index()) {
                    if (change.kind() == ILazySource.Kind.INSERT) cell.index += change.count();
                    if (change.kind() == ILazySource.Kind.REMOVE) cell.index -= change.count();
                }
            }
        }
        this.count = this.source.size();
        if (this.count < 0) throw new IllegalArgumentException("Negative source size");
        long rows = ((long) this.count + this.columns - 1) / this.columns;
        this.height = rows == 0 ? 0 : coordinate(rows * this.pitchY - this.spacing);
        this.initialized = true;
        this.viewportDirty = true;
        return true;
    }

    @Override
    public boolean prepareWindow(Bounds viewport) {
        boolean changed = false;
        var tree = this.getNode().getTree();
        var focused = tree.getFocusedNode();
        var captured = tree.getCapturedNode();
        boolean released = this.cells.values().stream().anyMatch(cell ->
                !cell.visible && !cell.contains(focused) && !cell.contains(captured));
        var bounds = this.getBounds();
        var relativeViewport = new Bounds(viewport.x() - bounds.x(), viewport.y() - bounds.y(),
                viewport.width(), viewport.height());
        if (!this.viewportDirty && relativeViewport.equals(this.lastViewport) && !released) return false;
        this.viewportDirty = false;
        this.lastViewport = relativeViewport;
        int firstRow = Math.max(0, (viewport.top() - bounds.top()) / this.pitchY);
        int endRow = Math.min((int) (((long) this.count + this.columns - 1) / this.columns),
                Math.max(0, (int) (((long) viewport.bottom() - bounds.top() + this.pitchY - 1) / this.pitchY)));
        int firstCol = Math.max(0, (viewport.left() - bounds.left()) / this.pitchX);
        int endCol = Math.min(this.columns, Math.max(0,
                (int) (((long) viewport.right() - bounds.left() + this.pitchX - 1) / this.pitchX)));
        var wanted = new HashSet<K>();
        for (var cell : this.cells.values()) cell.visible = false;
        if (viewport.width() > 0 && viewport.height() > 0) {
            for (int row = firstRow; row < endRow; row++) {
                for (int col = firstCol; col < endCol; col++) {
                    long candidate = (long) row * this.columns + col;
                    if (candidate >= this.count) break;
                    int index = (int) candidate;
                    int x = coordinate((long) col * this.pitchX);
                    int y = coordinate((long) row * this.pitchY);
                    if (!new Bounds(bounds.x() + x, bounds.y() + y, this.cellWidth, this.cellHeight)
                            .intersects(viewport)) continue;
                    K key = Objects.requireNonNull(this.source.key(index), "Null lazy key");
                    if (!wanted.add(key)) throw new IllegalArgumentException("Duplicate mounted lazy key: " + key);
                    var cell = this.cells.get(key);
                    if (cell != null && cell.index != index) {
                        throw new IllegalArgumentException("Stable key moved without a source change: " + key);
                    }
                    if (cell == null) {
                        cell = new Cell(index);
                        T item = Objects.requireNonNull(this.source.item(index), "Null lazy item");
                        var mounted = cell;
                        this.scope.e(new Stack(), cellScope -> {
                            cellScope.layout().fixedSize(this.cellWidth, this.cellHeight).clip();
                            this.composer.accept(item, cellScope);
                        });
                        mounted.node = this.getNode().getChildren().getLast();
                        this.cells.put(key, cell);
                        changed = true;
                        if (!this.pendingChanges.isEmpty()) {
                            cell.visible = true;
                            this.viewportDirty = true;
                            return true;
                        }
                    }
                    cell.visible = true;
                }
            }
        }
        var iterator = this.cells.values().iterator();
        while (iterator.hasNext()) {
            var cell = iterator.next();
            if (cell.visible || cell.contains(focused) || cell.contains(captured)) continue;
            tree.detachNode(cell.node);
            iterator.remove();
            changed = true;
        }
        if (changed) {
            var ordered = this.cells.values().stream()
                    .sorted(Comparator.comparingInt(cell -> cell.index))
                    .map(cell -> cell.node).toList();
            var host = this.getNode();
            for (var child : host.getChildren()) host.removeChild(child);
            for (var child : ordered) host.addChild(child);
        }
        return changed;
    }

    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> children) {
        return new Size(this.width, this.height);
    }

    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> children) {
        var positions = new HashMap<ITreeNode, Integer>();
        for (var cell : this.cells.values()) positions.put(cell.node, cell.index);
        var result = new ArrayList<Bounds>();
        for (var node : this.getNode().getChildren()) {
            int index = positions.get(node);
            result.add(new Bounds(bounds.x() + index % this.columns * this.pitchX,
                    bounds.y() + index / this.columns * this.pitchY, this.cellWidth, this.cellHeight));
        }
        return result;
    }

    @Override
    public void onDetached() {
        this.subscription.dispose();
        this.subscription = ILazySource.Subscription.NONE;
        this.pendingChanges.clear();
        this.cells.clear();
        this.initialized = false;
    }

    private final class Cell {
        private int index;
        private ITreeNode node;
        private boolean visible;

        Cell(int index) {
            this.index = index;
        }

        boolean contains(ITreeNode target) {
            return target != null && (target == this.node || this.node.isAncestorOf(target));
        }
    }
}
