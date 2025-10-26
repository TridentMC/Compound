# Declarative UI Framework - Core Interface Definitions

This document defines the core interfaces needed for the declarative UI system. These are signatures only - no implementations.

---

## 1. Core Element Interfaces

### IElement
The base interface for all UI elements.

```java
public interface IElement {
    // Layout Phase 1: Measure (bottom-up)
    // Returns the desired size given the constraints
    Size measure(Constraints constraints);

    // Layout Phase 2: Place (top-down)
    // Sets the final bounds for this element
    void place(Bounds bounds);

    // Rendering
    // Draw this element (called every frame)
    void draw(PoseStack poseStack, int mouseX, int mouseY, float partialTick);

    // Lifecycle
    // Called when element is attached to the tree
    void onAttached();

    // Called when element is detached from the tree
    void onDetached();

    // Properties
    LayoutProperties getLayoutProperties();
    void setLayoutProperties(LayoutProperties properties);

    // Current bounds after placement
    Bounds getBounds();

    // Visibility
    boolean isVisible();
    void setVisible(boolean visible);
}
```

### IPrimitiveElement
Elements that cannot have children - they draw pixels.

```java
public interface IPrimitiveElement extends IElement {
    // No additional methods - this is a marker interface
    // Primitives are "terminators" in the tree
}
```

### IContainer
Elements that can have children.

```java
public interface IContainer extends IElement {
    // Get children (queries the internal tree)
    List<IElement> getChildren();

    // Check if this container has any children
    boolean hasChildren();

    // Get child count
    int getChildCount();
}
```

### IComposableElement
"Smart" components that encapsulate behavior and have internal composition.

```java
public interface IComposableElement extends IContainer {
    // Internal composition method
    // Called once during tree construction to build this element's internal UI
    void compose(ICompositionScope scope);

    // Exposed slots that users can fill
    List<SlotKey> getAvailableSlots();
}
```

---

## 2. Tree Structure (Internal)

### ITreeNode
Wraps an element and stores tree metadata (like DOM nodes wrap elements).
**NOTE: This is internal to the framework and not exposed to elements or composition code.**

```java
public interface ITreeNode {
    // Element reference
    IElement getElement();

    // Parent-child relationships (stored in the node, not the element)
    ITreeNode getParent();
    List<ITreeNode> getChildren();
    void addChild(ITreeNode child);
    void removeChild(ITreeNode child);
    void clearChildren();
    boolean hasChildren();
    int getChildCount();

    // State bindings (for re-composition)
    List<State<?>> getBoundStates();
    void bindState(State<?> state);
    void unbindState(State<?> state);
    boolean isBoundToState(State<?> state);

    // Composition function (stored for re-composition)
    Runnable getCompositionFunction();
    void setCompositionFunction(Runnable compositionFn);
    boolean hasCompositionFunction();

    // Event handlers (tied to this node, cleaned up when node is removed)
    void registerClickHandler(Consumer<MouseClickEvent> handler);
    void registerMouseEnterHandler(Runnable handler);
    void registerMouseExitHandler(Runnable handler);
    void registerMouseMoveHandler(Consumer<MouseMoveEvent> handler);
    void registerScrollHandler(Consumer<MouseScrollEvent> handler);
    void registerKeyPressHandler(Consumer<KeyEvent> handler);
    void registerKeyReleaseHandler(Consumer<KeyEvent> handler);
    Consumer<MouseClickEvent> getClickHandler();
    Runnable getMouseEnterHandler();
    Runnable getMouseExitHandler();
    Consumer<MouseMoveEvent> getMouseMoveHandler();
    Consumer<MouseScrollEvent> getScrollHandler();
    Consumer<KeyEvent> getKeyPressHandler();
    Consumer<KeyEvent> getKeyReleaseHandler();
    void clearHandlers();

    // Slot map (for composable elements)
    SlotMap getSlotMap();
    void setSlotMap(SlotMap slotMap);

    // Tree queries
    int getDepth();
    boolean isAncestorOf(ITreeNode other);
    boolean isDescendantOf(ITreeNode other);
}
```

### UITree
Manages the tree of nodes (like the DOM).
**NOTE: This is internal to the framework and not exposed to elements or composition code.**

```java
class UITree {
    // Root node
    void setRoot(ITreeNode root);
    ITreeNode getRoot();
    boolean hasRoot();

    // Node lookup
    ITreeNode getNodeForElement(IElement element);
    boolean hasNode(IElement element);

    // Tree operations
    ITreeNode createNode(IElement element);
    void attachNode(ITreeNode parent, ITreeNode child);
    void detachNode(ITreeNode node);

    // Re-composition (kill node and rebuild)
    void recomposeNode(ITreeNode node);

    // Tree traversal
    void walkDepthFirst(ITreeNode start, Consumer<ITreeNode> visitor);
    void walkBreadthFirst(ITreeNode start, Consumer<ITreeNode> visitor);

    // Find operations
    <T extends IElement> List<ITreeNode> findNodesByElementType(Class<T> type);
    ITreeNode findNodeAt(int x, int y); // Hit test

    // Layout coordination
    void measureTree(Constraints rootConstraints);
    void placeTree(Position rootPosition);

    // Event dispatch
    void dispatchClick(int x, int y, MouseClickEvent event);
    void dispatchMouseMove(int x, int y, MouseMoveEvent event);
    void dispatchScroll(int x, int y, MouseScrollEvent event);
    void dispatchKeyPress(KeyEvent event);
    void dispatchKeyRelease(KeyEvent event);

    // State change notification
    void onStateChanged(State<?> state);
}
```

---

## 3. Layout System

### Size
Represents a width and height.

```java
public class Size {
    Size(int width, int height);

    int getWidth();
    int getHeight();

    // Utility
    static Size ZERO;
    static Size of(int width, int height);
    Size withWidth(int width);
    Size withHeight(int height);
}
```

### Position
Represents an x, y coordinate.

```java
public class Position {
    Position(int x, int y);

    int getX();
    int getY();

    // Utility
    static Position ORIGIN;
    static Position of(int x, int y);
    Position offset(int dx, int dy);
}
```

### Bounds
Represents position and size together.

```java
public class Bounds {
    Bounds(int x, int y, int width, int height);
    Bounds(Position position, Size size);

    Position getPosition();
    Size getSize();

    int getX();
    int getY();
    int getWidth();
    int getHeight();

    int getLeft();
    int getTop();
    int getRight();
    int getBottom();

    // Hit testing
    boolean contains(int x, int y);
    boolean contains(Position position);
    boolean intersects(Bounds other);

    // Utility
    Bounds offset(int dx, int dy);
    Bounds shrink(int amount); // Inset by amount on all sides
    Bounds shrink(int horizontal, int vertical);
    Bounds expand(int amount);
}
```

### Constraints
Measurement constraints passed down during the measure phase.

```java
public class Constraints {
    Constraints(int minWidth, int maxWidth, int minHeight, int maxHeight);

    int getMinWidth();
    int getMaxWidth();
    int getMinHeight();
    int getMaxHeight();

    boolean hasFixedWidth();
    boolean hasFixedHeight();
    boolean hasBoundedWidth();
    boolean hasBoundedHeight();
    boolean isUnbounded();

    // Constrain a size to fit within these constraints
    Size constrain(Size size);

    // Create variations
    Constraints withMaxWidth(int maxWidth);
    Constraints withMaxHeight(int maxHeight);
    Constraints withMinWidth(int minWidth);
    Constraints withMinHeight(int minHeight);
    Constraints withFixedWidth(int width);
    Constraints withFixedHeight(int height);
    Constraints withFixedSize(int width, int height);

    // Shrink available space (for padding, etc.)
    Constraints deflate(int horizontal, int vertical);

    // Common presets
    static Constraints unbounded();
    static Constraints fixed(int width, int height);
    static Constraints loose(int maxWidth, int maxHeight);
    static Constraints tight(int width, int height);
}
```

### Alignment
How to align children within available space.

```java
public enum Alignment {
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    CENTER_LEFT,
    CENTER,
    CENTER_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT;

    // Calculate position for a child with given size in a parent with given size
    Position align(Size childSize, Size parentSize);
}
```

### LayoutProperties
Configuration for how an element should be laid out by its parent.

```java
public class LayoutProperties {
    // Factory
    static LayoutProperties create();

    // Fixed size
    LayoutProperties withFixedWidth(int width);
    LayoutProperties withFixedHeight(int height);
    LayoutProperties withFixedSize(int width, int height);
    Integer getFixedWidth();
    Integer getFixedHeight();

    // Weight (for flex layouts)
    LayoutProperties withWeight(float weight);
    Float getWeight();

    // Min/Max size
    LayoutProperties withMinWidth(int minWidth);
    LayoutProperties withMinHeight(int minHeight);
    LayoutProperties withMaxWidth(int maxWidth);
    LayoutProperties withMaxHeight(int maxHeight);
    Integer getMinWidth();
    Integer getMinHeight();
    Integer getMaxWidth();
    Integer getMaxHeight();

    // Padding (space inside the element's bounds)
    LayoutProperties withPadding(int padding);
    LayoutProperties withPadding(int horizontal, int vertical);
    LayoutProperties withPadding(int left, int top, int right, int bottom);
    int getPaddingLeft();
    int getPaddingTop();
    int getPaddingRight();
    int getPaddingBottom();

    // Margin (space outside the element's bounds)
    LayoutProperties withMargin(int margin);
    LayoutProperties withMargin(int horizontal, int vertical);
    LayoutProperties withMargin(int left, int top, int right, int bottom);
    int getMarginLeft();
    int getMarginTop();
    int getMarginRight();
    int getMarginBottom();

    // Alignment (for containers)
    LayoutProperties withAlignment(Alignment alignment);
    Alignment getAlignment();

    // Spacing (between children in containers)
    LayoutProperties withSpacing(int spacing);
    Integer getSpacing();

    // Grid-specific
    LayoutProperties withGridSize(int columns, int rows);
    Integer getGridColumns();
    Integer getGridRows();
}
```

---

## 4. Composition System

### ICompositionContext
The root context given to screens to begin composition.

```java
public interface ICompositionContext extends IContainerScope {
    // This is the entry point for building the UI
    // Inherits e() from IContainerScope

    // State creation helper
    <T> State<T> useState(T initialValue);
}
```

### ICompositionScope
The scope given to composable elements for their internal composition.

```java
public interface ICompositionScope extends IContainerScope {
    // State creation helper (ties state lifecycle to this element)
    <T> State<T> useState(T initialValue);

    // Render a slot with fallback
    void slot(SlotKey key, Consumer<IContainerScope> defaultContent);
}
```

### IElementScope
Scope for configuring a primitive element (no children).

```java
public interface IElementScope<T extends IPrimitiveElement> {
    // Access to the element being configured
    T active;

    // No e() method - primitives cannot have children
}
```

### IContainerScope
Scope for configuring a container element (can add children).

```java
public interface IContainerScope<T extends IContainer> {
    // Access to the element being configured
    T active;

    // Add a child element
    <E extends IElement> void e(E child, Consumer<? extends IElementScope<E>> configure);

    // Mark this scope for re-composition when state changes
    <S> void bind(State<S> state);
}
```

### IComposableElementScope
Scope for configuring a composable element (can fill slots).

```java
public interface IComposableElementScope<T extends IComposableElement>
        extends IContainerScope<T> {
    // Fill a slot with custom content
    void fillSlot(SlotKey key, Consumer<IContainerScope<?>> content);
}
```

---

## 5. State & Reactivity

### State<T>
Observable state container that triggers re-composition.

```java
public interface State<T> {
    // Get current value
    T get();

    // Set new value (triggers observers)
    void set(T value);

    // Update value using current value
    void update(Function<T, T> updater);

    // Internal: Register observer (used by framework)
    void addObserver(StateObserver observer);
    void removeObserver(StateObserver observer);

    // Cleanup
    void dispose();
}
```

### StateObserver
Internal interface for objects that react to state changes.

```java
public interface StateObserver {
    // Called when any observed state changes
    void onStateChanged();
}
```

---

## 6. Slot System

### SlotKey
Identifier for a customization slot in a composable element.

```java
public class SlotKey {
    SlotKey(String name);

    String getName();

    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();
}
```

### SlotContent
Wrapper for user-provided slot content.

```java
public class SlotContent {
    SlotContent(Consumer<IContainerScope<?>> content);

    // Execute the content in the given scope
    void render(IContainerScope<?> scope);
}
```

### SlotMap
Storage for slot content (internal to framework).

```java
public class SlotMap {
    // Store content for a slot
    void put(SlotKey key, SlotContent content);

    // Get content for a slot
    SlotContent get(SlotKey key);

    // Check if slot is filled
    boolean has(SlotKey key);

    // Clear all slots
    void clear();
}
```

---

## 7. Event System

### MouseClickEvent
Event for mouse clicks.

```java
public class MouseClickEvent {
    int getX();
    int getY();
    int getButton(); // 0 = left, 1 = right, 2 = middle
    boolean isShiftDown();
    boolean isCtrlDown();
    boolean isAltDown();

    // Event control
    void consume();
    boolean isConsumed();
}
```

### MouseMoveEvent
Event for mouse movement.

```java
public class MouseMoveEvent {
    int getX();
    int getY();
    int getDeltaX();
    int getDeltaY();
}
```

### MouseScrollEvent
Event for mouse wheel scrolling.

```java
public class MouseScrollEvent {
    int getX();
    int getY();
    double getScrollDelta();

    void consume();
    boolean isConsumed();
}
```

### KeyEvent
Event for keyboard input.

```java
public class KeyEvent {
    int getKeyCode();
    char getCharacter();
    boolean isShiftDown();
    boolean isCtrlDown();
    boolean isAltDown();

    void consume();
    boolean isConsumed();
}
```

---

## 8. Screen Integration

### CompoundScreenWithDeclarativeUI
Base screen class that integrates with the framework.

```java
public abstract class CompoundScreenWithDeclarativeUI extends Screen {

    protected CompoundScreenWithDeclarativeUI(Component title);

    // Subclasses implement this to build their UI
    protected abstract void compose(ICompositionContext context);

    // State management helper
    protected <T> State<T> useState(T initialValue);

    // Screen lifecycle (internal - handled by framework)
    @Override protected void init();
    @Override public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick);
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button);
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta);
    @Override public void mouseMoved(double mouseX, double mouseY);
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers);
    @Override public boolean keyReleased(int keyCode, int scanCode, int modifiers);
    @Override public void removed();
}
```

---

## Summary

This interface definition covers the core framework components:

1. **Core Element Types**: IElement, IPrimitiveElement, IContainer, IComposableElement
2. **Tree Structure**: ITreeNode (wraps elements like DOM nodes), UITree (manages the node hierarchy)
3. **Layout System**: Size, Position, Bounds, Constraints, Alignment, LayoutProperties
4. **Composition System**: ICompositionContext, ICompositionScope, IElementScope, IContainerScope, IComposableElementScope
5. **State & Reactivity**: State<T>, StateObserver
6. **Slot System**: SlotKey, SlotContent, SlotMap
7. **Event System**: Event types (MouseClickEvent, MouseMoveEvent, MouseScrollEvent, KeyEvent)
8. **Screen Integration**: CompoundScreenWithDeclarativeUI base class

**Key Design Principles:**
- Elements are completely unaware of the tree structure (like DOM elements)
- TreeNodes wrap elements and store all tree metadata (parent, children, bindings, handlers)
- When state changes, the framework kills the bound TreeNode and re-runs composition to rebuild
- UITree manages the node hierarchy and coordinates layout/events
- Event handlers are stored in TreeNodes and automatically cleaned up when nodes are removed
- All structural changes go through composition scopes
- Uses standard Java features (Supplier<T>, Consumer<T>, Function<T, R>)
