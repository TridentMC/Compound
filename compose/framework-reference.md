# Compound UI Framework Reference

## Overview

Compound is a declarative, composition-based UI system for Minecraft mods. You describe the
structure of a UI once with nested `e(...)` calls; the structure is then "baked" and only rebuilds
where you explicitly opt into state binding. Rendering happens every frame, so ordinary property
reads (text, color) need no binding at all.

Current integration: Minecraft 26.3, NeoForge 26.3.0.26-beta, ModDevGradle 2.0.147.

> This is the broad framework reference: architecture, layout, state, events, authoring custom
> elements, patterns, and testing. For the exhaustive **consumer** API — every scope method, layout
> property, slot key, and built-in widget — see
> [`api-surface.md`](api-surface.md).

---

## Architecture Concepts

### Element types

| Kind | Interface | Purpose | Examples |
|------|-----------|---------|----------|
| Primitive | `IPrimitiveElement` | Draws pixels; no children | `Rect`, `GradientRect`, `Sprite`, `Text`, `Spacer`, `ItemDisplay` |
| Container | `IContainer` | Arranges children by a layout algorithm | `Stack`, `Column`, `Row`, `Box`, `Grid` |
| Composable | `IComposableElement` | Encapsulates state/behavior with internal composition; customized via slots | `Panel`, `Button`, `Label`, `ScrollArea`, `TextInput`, pickers |

`IElement` is the common base; `IGenericElement` / `IGenericContainer` are the generic roots that
`measure`/`place` are defined on. The tree itself is owned centrally by `UITree` — elements do not
hold their own child lists, which is what makes the "baked structure + explicit binding" model
enforceable.

### Base classes

- **`BaseElement`** — implements `IElementInternal`; stores the node reference, supplies `getBounds()`,
  `getNode()`, `setNode()`, and default no-op lifecycle. Concrete elements implement `measure` and
  `place`.
- **`BasePrimitiveElement`** — adds a final `draw(IScreenContext)` that wraps your
  `drawElement(IScreenContext, Bounds)` with visibility handling, and an empty `place`.
- **`BaseContainer`** — minimal base for container elements.

---

## Entry points

```java
public class MyScreen extends ComposedUI {
    @Override
    protected void compose(ICompositionScope scope) {
        scope.e(new Stack(), stack -> {
            stack.layout().fillMax().contentAlignment(Alignment.CENTER);
        });
    }
}
```

For inventory screens, extend `ComposedUIContainer<T extends CompoundContainerMenu>` and construct it
with `(menu, inventory, title)`. It discovers `InventorySlot` elements automatically and manages
hover/quick-craft/carried-item visuals.

Both classes handle measure/place/render and dispatch mouse, keyboard, scroll, drag, and focus events
into the tree. You only implement `compose(ICompositionScope scope)`. Screen-context access uses
`IScreenContext`. The scope still exposes concrete `UITree` through `getTree()` for advanced
overlay, viewport, focus, and hit-testing work; ordinary composition does not require it.

---

## Composition

Elements are added with `scope.e(element, configurator)`; the configurator type is chosen from the
element's static type:

- primitive → `IElementScope<T>` (`getElement()`, `layout()`)
- container → `IContainerScope<T>` (`+ e(...)`, events, `bind`/`bindLayout`, animations)
- composable → `IComposableElementScope<T>` (`getElement()`, `layout()`, `fillSlot(...)`)

```java
scope.e(new Column(), column -> {
    column.layout()
        .fillMaxWidth()
        .spacing(10)
        .padding(20)
        .horizontalAlignment(Alignment.CENTER);

    column.e(new Button(), button -> {
        button.layout().fixedSize(100, 40);
        button.fillSlot(Button.CONTENT_SLOT, content ->
            content.e(new Label(Component.literal("Click me"))));
    });
});
```

`e(element)` with no configurator is shorthand for "add, no configuration". Events and
`bind`/`bindLayout` exist on container/root scopes and inside a composable's own `compose` — not on
the configurator handed to a composable's user.

### Slots

A composable publishes `public static final SlotKey` constants for the regions it allows callers to
override:

```java
public class MyPanel extends BaseElement implements IComposableElement {
    public static final SlotKey HEADER_SLOT = new SlotKey("header");
    public static final SlotKey CONTENT_SLOT = new SlotKey("content");

    @Override
    public void compose(ICompositionScope scope) {
        scope.e(new Column(), column -> {
            column.slot(HEADER_SLOT, fallback ->
                fallback.e(new Label(Component.literal("Default Header"))));
            column.slot(CONTENT_SLOT);
        });
    }
}

scope.e(new MyPanel(), panel -> {
    panel.fillSlot(MyPanel.HEADER_SLOT, header ->
        header.e(new Label(Component.literal("Custom Header"))));
});
```

If the caller filled a slot, their content wins; otherwise the default lambda runs. `SlotKey`
compares by name, but each composable has its own slot map, so identical names across components do
not collide.

Built-in slot keys: `Panel.CONTENT_SLOT`, `Button.CONTENT_SLOT`, `ScrollArea.CONTENT_SLOT`,
`Surface.CONTENT_SLOT`.

---

## Layout system

Layout is two-phase:

1. **Measure (bottom-up).** `measure(Constraints, LayoutProperties, List<Size> measuredChildren)`
   returns the element's intrinsic size. The tree measures children first and applies their margins.
2. **Place (top-down).** `place(Bounds, LayoutProperties, List<Size> measuredChildren)` returns a
   `Bounds` per child (including margin space). The tree offsets by each child's margin and recurses.

```java
@Override
public Size measure(Constraints constraints, LayoutProperties props, List<Size> children) {
    int w = Math.min(desiredWidth, constraints.maxWidth());
    int h = Math.min(desiredHeight, constraints.maxHeight());
    return new Size(w, h);
}

@Override
public List<Bounds> place(Bounds bounds, LayoutProperties props, List<Size> children) {
    List<Bounds> result = new ArrayList<>();
    int y = bounds.y();
    for (Size child : children) {
        result.add(new Bounds(bounds.x(), y, child.width(), child.height()));
        y += child.height() + props.getSpacing();
    }
    return result;
}
```

`Bounds` is a record `(Position position, Size size)` with an `(int x, int y, int w, int h)`
constructor and helpers: `x/y/width/height`, `left/top/right/bottom`, `contains`, `intersects`,
`intersection`, `offset`, `shrink`, `expand`.

### Constraints

`Constraints` is a record `(minWidth, maxWidth, minHeight, maxHeight)` with factories:

```java
Constraints.unbounded()           // no limits
Constraints.fixed(200, 100)       // exact
Constraints.loose(200, 100)       // up to
Constraints.tight(200, 100)       // alias of fixed
```

Helpers: `hasFixedWidth/Height`, `hasBoundedWidth/Height`, `isUnbounded`, `constrain(Size)`,
`constrainWidth/Height`, `withMaxWidth/Height`, `withMinWidth/Height`, `withFixedWidth/Height`,
`withFixedSize`, `deflate(horizontal, vertical)`.

### Layout properties

Reached via `scope.layout()` (or `element.layout()` inside a composable's `compose`). Full details in
the consumer doc; summary:

- **Size:** `fixedWidth/Height`, `fixedSize`, `fillMaxWidth/Height`, `fillMax`, `minWidth/Height`,
  `maxWidth/Height`, `clearMaxWidth/Height`, `unboundedWidth/Height`, `weight`
- **Spacing:** `padding(...)`, `margin(...)` (1, 2, or 4 argument forms), `spacing(int)`
- **Alignment:** `contentAlignment` (Box/Stack), `horizontalAlignment` (Column), `verticalAlignment` (Row)
- **Other:** `clip()`, `layer(int)`, `gridSize(columns, rows)`, `deferred(Consumer<DeferredScope>)`

`maxWidth` and `maxHeight` cap within the parent constraints, including when passed
`Integer.MAX_VALUE`. Use `unboundedWidth()` or `unboundedHeight()` explicitly for scroll content;
`clearMaxWidth()` / `clearMaxHeight()` remove the explicit cap and restore the parent limit.

`Grid` spacing is constructor-level (`new Grid(columns, hSpacing, vSpacing)`), not `spacing()`.

---

## State management

`State<T>` is the observable container (`State.of(initial)`): `get()`, `set(value)`, `update(fn)`,
`dispose()`, plus framework-internal observer registration.

> `State<T>` does **not** extend `Supplier<T>`. Elements that accept a supplier need
> `() -> state.get()` (or the element's `setXxxSupplier` setter).

### Binding

```java
scope.bind(username);       // state change => recompose this subtree (structure)
scope.bindLayout(columns);  // state change => remeasure/re-place only
```

- `bind` / `bindComposition` — conditional rendering, list rebuilds, swapping children. A
  container binding replays its configurator and replaces its children and composition-owned resources.
- `bindLayout` — size/position-only changes; skips recomposition.
- No binding — draw suppliers are read on each render. Suppliers that change measured size still
  need layout invalidation; measurement and placement do not run every frame.

### Deferred layout

```java
var width = scope.animateInt(0, 300);

box.layout()
   .fillMaxHeight()
   .deferred(deferred -> {
       deferred.bind(width);
       deferred.layout().fixedWidth(width.get());
   });
```

`DeferredScope` re-runs its callback when a bound state changes and requests layout. Its
subscriptions are removed with the owning composition or node.

### Animated state

Create animated values from the scope. Values created inside a composition are disposed when
that composition runs again. Use `scope.retainAnimation(value)` for an animation cached in an
element field; it then survives recomposition and is disposed when the element detaches. Clear
the cached field in `onDetached()` so a later attachment can create a fresh animation.
Animations created by a composable element's outer configurator belong to that node's lifetime.

```java
var value  = scope.animateFloat(0f, 500);                     // EASE_IN_OUT default
var num    = scope.animateInt(0, 500);
var color  = scope.animateColor(0xFF000000, 500, Easing.EASE_OUT_QUART);

var pulse  = scope.animateFloatLooping(0.4f, 1.0f, 800);      // STEP default
var blink  = scope.animateIntLooping(0, 1, 500, Easing.EASE_IN_OUT);
var cycle  = scope.animateColorLooping(0xFF0000, 0x0000FF, 1000);

value.set(1f);               // animate toward target
value.get();                 // current tick value
value.get(partialTicks);     // interpolated for smooth drawing
value.setImmediate(0f);      // jump without animating
scope.retainAnimation(value); // only when keeping it across compositions
```

`Interpolators` provides `FLOAT`, `INT`, `COLOR`. `Easing` provides `LINEAR`, `EASE_IN`,
`EASE_OUT`, `EASE_IN_OUT`, `EASE_IN_CUBIC`, `EASE_OUT_CUBIC`, `EASE_IN_OUT_CUBIC`,
`EASE_IN_OUT_SINE`, `EASE_OUT_QUART`, `EASE_IN_OUT_QUART`, and `STEP`.

---

## Event system

Handlers are registered on a container/root scope, or inside a composable's `compose`. Boolean
handlers return `true` to consume (stop bubbling) and `false` to let the parent handle it.

```java
scope.onClick(event -> {
    int x = event.x(), y = event.y();
    return true;
});

scope.onScroll(event -> {
    double delta = event.scrollY();
    return false;
});

scope.onScrollWhenFocused(event -> true);  // only consumes when this node is focused

scope.onMouseEnter(() -> hovered.set(true));
scope.onMouseExit(() -> hovered.set(false));
```

Available handlers: `onClick`, `onScroll`, `onScrollWhenFocused`, `onKeyPress`, `onKeyRelease`,
`onCharTyped`, `onMouseRelease`, `onMouseDrag`, `onMouseMove`, `onMouseEnter`, `onMouseExit`,
`onFocusGained`, `onFocusLost`, plus `requestFocus()` / `isFocused()`.

Event records: `MouseClickEvent(x, y, button, shiftDown, ctrlDown, altDown)`,
`MouseReleaseEvent(x, y, button)`, `MouseDragEvent(button, x, y, deltaX, deltaY)`,
`MouseMoveEvent(x, y, prevX, prevY)`, `MouseScrollEvent(x, y, scrollX, scrollY)`,
`KeyInputEvent(keyCode, logicalKeyCode, shiftDown, ctrlDown, altDown)`, `CharEvent(codePoint, modifiers)`.
Character input uses an integer Unicode code point; insert it with `Character.toChars(codePoint)`
rather than casting to `char`. Use native `InputConstants` for key and mouse-button comparisons.

Events dispatch from the deepest hit node outward (target → parent → ...) until consumed.

---

## Implementing elements

### Primitive

```java
public class Circle extends BasePrimitiveElement {
    private final Supplier<Integer> color;
    private final Supplier<Float> radius;

    public Circle(Supplier<Integer> color, Supplier<Float> radius) {
        this.color = color;
        this.radius = radius;
    }

    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> children) {
        int diameter = (int) (radius.get() * 2);
        return new Size(
            Math.min(diameter, constraints.maxWidth()),
            Math.min(diameter, constraints.maxHeight()));
    }

    @Override
    protected void drawElement(IScreenContext context, Bounds bounds) {
        // draw using context.drawRect / drawSprite / drawText / ...
    }
}
```

### Container

Implement `measure`/`place` as shown in [Layout system](#layout-system). A flow layout, wrapping
example, etc. all follow the same contract: measure intrinsic size from measured children, then
return one `Bounds` per child.

### Composable

```java
public class Card extends BaseElement implements IComposableElement {
    public static final SlotKey CONTENT_SLOT = new SlotKey("content");
    public static final SlotKey ACTIONS_SLOT = new SlotKey("actions");

    private final State<Boolean> hovered = State.of(false);

    @Override
    public void compose(ICompositionScope scope) {
        scope.bind(hovered);
        scope.onMouseEnter(() -> hovered.set(true));
        scope.onMouseExit(() -> hovered.set(false));

        scope.e(new Stack(), stack -> {
            stack.layout().fillMax();

            stack.e(new Rect(hovered.get() ? 0xFF444444 : 0xFF222222),
                    bg -> bg.layout().fillMax());

            stack.e(new Column(), column -> {
                column.layout().fillMax().padding(16);
                column.slot(CONTENT_SLOT);
                column.e(new Spacer(0, 16));
                column.e(new Row(), row -> {
                    row.layout().fillMaxWidth();
                    row.slot(ACTIONS_SLOT);
                });
            });
        });
    }

    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> children) {
        return children.isEmpty() ? new Size(0, 0) : children.get(0);
    }

    @Override
    public List<Bounds> place(Bounds bounds, LayoutProperties props, List<Size> children) {
        return children.isEmpty() ? List.of() : List.of(bounds);
    }
}
```

Composables also typically expose a small setter API (e.g. `setText`, `setEnabled`), state getters
for external binding, and listener registration methods.

---

## Best practices

1. **Pick the right element type.** Primitive for pixels, container for layout only, composable for
   behavior + internal structure + slots.
2. **Use the narrowest binding.** `bindLayout` for size/position changes; `bind` only for structural
   changes; plain suppliers for per-frame value reads.
3. **Keep state local** to the component that owns it.
4. **Don't do work in `measure`/`place`.** They are pure functions of their inputs.
5. **Return the right boolean** from event handlers to control bubbling.
6. **Use `layer(...)` for overlays** (dropdowns, modals, tooltips).
7. **Use `clip()` for scroll/overflow containers.**

## Common patterns

### Stateful component

```java
public class Counter extends BaseElement implements IComposableElement {
    private final State<Integer> count = State.of(0);

    @Override
    public void compose(ICompositionScope scope) {
        scope.bind(count);

        scope.e(new Column(), column -> {
            column.layout().horizontalAlignment(Alignment.CENTER);

            column.e(new Label(() -> Component.literal("Count: " + count.get()),
                               () -> 0xFFFFFF, () -> true));

            column.e(new Row(), row -> {
                row.layout().spacing(10);

                row.e(new Button(), button -> {
                    button.fillSlot(Button.CONTENT_SLOT, content ->
                        content.e(new Label(Component.literal("-"))));
                    button.getElement().addPressListener((x, y) ->
                        count.update(c -> Math.max(0, c - 1)));
                });

                row.e(new Button(), button -> {
                    button.fillSlot(Button.CONTENT_SLOT, content ->
                        content.e(new Label(Component.literal("+"))));
                    button.getElement().addPressListener((x, y) ->
                        count.update(c -> c + 1));
                });
            });
        });
    }
}
```

### Conditional rendering

```java
scope.e(new Column(), column -> {
    column.bind(showDetails);   // recompose this column when the flag changes

    column.e(new Button(), button -> {
        button.fillSlot(Button.CONTENT_SLOT, content ->
            content.e(new Label(Component.literal(showDetails.get() ? "Hide" : "Show"))));
        button.getElement().addPressListener((x, y) ->
            showDetails.set(!showDetails.get()));
    });

    if (showDetails.get()) {
        column.e(new Label(Component.literal("Detailed information")));
    }
});
```

### List rendering

```java
scope.e(new Column(), column -> {
    column.bind(items);
    for (String item : items.get()) {
        column.e(new Label(Component.literal(item)), label ->
            label.layout().fillMaxWidth());
    }
});
```

### Animated layout

```java
public class AnimatedAccordion extends BaseElement implements IComposableElement {
    private final State<Boolean> expanded = State.of(false);
    private AnimatedState<Float> height;

    @Override
    public void compose(ICompositionScope scope) {
        if (height == null) {
            height = scope.animateFloat(0f, 300);
        }
        scope.retainAnimation(height);
        scope.bind(expanded);

        scope.onClick(event -> {
            expanded.set(!expanded.get());
            height.set(expanded.get() ? 200f : 0f);
            return true;
        });

        scope.e(new Column(), column -> {
            column.e(new Rect(0xFF333333), header ->
                header.layout().fillMaxWidth().fixedHeight(40));

            column.e(new Box(), box -> {
                box.layout().fillMaxWidth().fixedHeight((int) (float) height.get())
                   .deferred(deferred -> {
                       deferred.bind(height);
                       deferred.layout().fixedHeight((int) (float) height.get());
                   });
                box.e(new Label(Component.literal("Accordion Content")), label ->
                    label.layout().padding(20));
            });
        });
    }

    @Override
    public void onDetached() {
        height = null;
    }

    // measure() / place() as for any composable
}
```

---

## Integration with Minecraft

### Screen

```java
public class MyUIScreen extends ComposedUI {
    @Override
    protected void compose(ICompositionScope scope) {
        scope.e(new Column(), column -> {
            column.layout().fillMax().padding(20);

            column.e(new Label(
                Component.literal("My UI Screen").withStyle(ChatFormatting.BOLD)));

            column.e(new Spacer(0, 20));
            // ...
        });
    }
}
```

### Container menu screen

```java
public class MyCrateScreen extends ComposedUIContainer<MyCrateMenu> {
    public MyCrateScreen(MyCrateMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void compose(ICompositionScope scope) {
        scope.e(new Panel(), panel -> {
            panel.layout().fixedSize(178, 190);
            panel.fillSlot(Panel.CONTENT_SLOT, content ->
                content.e(new Grid(9, 0, 0), grid -> {
                    for (int i = 0; i < 27; i++) {
                        grid.e(new InventorySlot(menu.getSlot(i)));
                    }
                }));
        });
    }
}
```

Open it the same way you open any `Screen` / `AbstractContainerScreen`.

---

## Testing UI components

Composition is testable without a running client: build a `UITree`, compose through a `RootScope`,
then measure, place, and render against a mocked `IScreenContext`.

```java
@Test
void testLayout() {
    var tree = new UITree();
    var scope = new RootScope(tree);

    scope.e(new Stack(), stack -> {
        stack.layout().fillMax().contentAlignment(Alignment.CENTER);
        stack.e(new Panel(), panel -> panel.layout().fixedSize(178, 190));
    });

    tree.measureTree(new Constraints(0, 800, 0, 600));
    tree.placeTree(new Position(0, 0), new Constraints(0, 800, 0, 600));

    var panel = tree.getRoot().getChildren().getFirst().getElement();
    assertEquals(new Bounds(311, 205, 178, 190), panel.getBounds());
}
```

Useful `UITree` members for tests: `measureTree(Constraints)`, `placeTree(Position[, Constraints])`,
`renderTree(IScreenContext)`, `dispatchClick(x, y, event)`, `dispatchScroll(x, y, event)`,
`dispatchKeyPress/Release`, `dispatchCharTyped`, `dispatchMouseMove/Drag/Release`, `getRoot()`,
`walkDepthFirst(node, visitor)`, `findNodesByElementType(Class)`, `findNodeAt(x, y)`,
`getFocusedNode()`, `requestFocus(node)`, `bindNodeToState(node, state)`.

The project's own regression suites (`CrateUIIntegrationTest`, `TextInputIntegrationTest`) validate
exact bounds and draw calls this way.

---

## Performance considerations

1. **Minimize recomposition** — prefer `bindLayout`, keep state scoped, avoid over-broad `bind`.
2. **Cheap measurement** — respect constraints, avoid O(n²) work, cache expensive content sizes.
3. **Clipping** — use `clip()` to bound draw work for overflow/scroll containers.
4. **Animations** — prefer draw-only reads of animated values; `bindLayout` only when size/position
   actually change. Bound animated values update at tick rate, with smoothness from partial-tick reads.

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| UI not updating | State not bound (`bind`/`bindLayout`), or a state mutated without `set`/`update` |
| Layout not recalculating | Layout property changed without `bindLayout` or `deferred` |
| Wrong layout | Constraint misuse in `measure`, or margin/padding expectations in `place` |
| Events not firing | Handler not registered (or registered on a composable's *user* configurator, which has no events); event consumed by a child first |
| Compile error adding a child | The element is a primitive/composable, not a container — use slots for composables |
| Overlay hidden behind siblings | Give it `layout().layer(1)` or higher |

Debug options: `UITree.DEBUG_EVENTS` (`-Dcompound.ui.debugEvents=true`) logs event dispatch;
`DebugOverlayConfig` (toggled with `F3+B` in a composed screen) draws layout bounds.

---

## Migration from legacy UI code

1. **Classify** existing widgets: pixels → primitive, layout → container, behavior → composable.
2. **Replace field mutation** with `State<T>` and bind where structure or layout depends on it.
3. **Replace absolute positioning** with layout properties (`fillMax`, `padding`, `spacing`,
   alignment, `fixedSize`).
4. **Extract reusable widgets** as composables that publish `SlotKey`s instead of exposing internals.
5. **Migrate incrementally**, starting with leaf widgets.

Retired names you may encounter in older docs map as: `ElementLabel` → `Label`, `ElementBox` →
`Panel`/`Surface`, `ElementRect` → `Rect`, `ElementImage`/`ElementSprite` → `Sprite`,
`ElementItem` → `ItemDisplay`, `ElementSpacer` → `Spacer`, `ElementSlot` → `InventorySlot`,
`ICompositionContext` → `ICompositionScope`, `LayoutProperties.withFixedSize(...)` → `layout().fixedSize(...)`.

---

For the complete consumer-facing surface (every scope method, layout property, slot key, and widget
constructor), see [`api-surface.md`](api-surface.md).
