# Compound Compositional UI — Consumer API Surface

A standalone reference for **using** the Compound composable UI framework: what you write in a
screen, what you get back from each scope, and which elements exist. It documents the current
public API (`com.tridevmc.compound.ui.*`), not the internal layout engine.

For architecture and custom element authoring, see the
[framework reference](../../Compound-UI-Framework-Reference.md).

---

## 1. The mental model

You describe a UI once, as a tree built by nested `e(...)` calls during **composition**. After that
initial pass the structure is "baked"; it only rebuilds where you explicitly opt in with a state
binding. Draw suppliers are read when rendering. Changes affecting geometry or child structure request layout or composition updates.

There are three kinds of node:

| Kind | Interface | Examples | Can have children? |
|------|-----------|----------|--------------------|
| Primitive | `IPrimitiveElement` | `Rect`, `GradientRect`, `Sprite`, `Text`, `Spacer`, `ItemDisplay` | No — draws pixels |
| Container | `IContainer` | `Stack`, `Column`, `Row`, `Box`, `Grid` | Yes — arranges children |
| Composable | `IComposableElement` | `Panel`, `Button`, `Label`, `ScrollArea`, inputs, pickers | Internally yes, externally no — customize via **slots** |

Containers are "dumb": they only arrange the children you hand them. Composables are "smart": they
own internal state/behavior and expose a small API plus named slots for customization.

---

## 2. Entry points

Extend one of two screen classes and override `compose(ICompositionScope scope)`.

**Plain screen:**

```java
public class MyScreen extends ComposedUI {
    @Override
    protected void compose(ICompositionScope scope) {
        scope.e(new Stack(), stack -> {
            stack.layout().fillMax().contentAlignment(Alignment.CENTER);
            // ... build here
        });
    }
}
```

**Container-menu screen (inventories):**

```java
public class MyCrateScreen extends ComposedUIContainer<MyCrateMenu> {
    public MyCrateScreen(MyCrateMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void compose(ICompositionScope scope) {
        // use InventorySlot to bind menu slots
    }
}
```

`ComposedUIContainer` auto-discovers every `InventorySlot` in the tree, keeps its hover/quick-craft
visual state in sync, and handles carried/snapback items. You do not wire slot interaction yourself.

Both classes run measure/place/render and route mouse, keyboard, scroll, drag and focus events to
the tree for you.

---

## 3. The scope API

The scope is the only object you build through. It comes in three flavours, and **the static type of
the element decides which overload and which configurator type you get**:

| You call `e(new X(), scope -> ...)` where X is... | Configurator type you receive | What it can do |
|---|---|---|
| `IPrimitiveElement` (`Rect`, `Sprite`, ...) | `IElementScope<T>` | `getElement()`, `layout()` |
| `IContainer` (`Stack`, `Column`, ...) | `IContainerScope<T>` | above + `e(...)`, events, `bind`/`bindLayout`, animations |
| `IComposableElement` (`Panel`, `Button`, ...) | `IComposableElementScope<T>` | above minus `e(...)` and events, plus `fillSlot(...)` |

This is compile-time safety: you cannot add children to a `Rect`, and you cannot add arbitrary
children to a `Button` — only fill its declared slots. Events and `bind`/`bindLayout` are available
when configuring a **container**, and to a composable's **author** inside its internal `compose`
method — not to the user configuring a composable (who uses the element's simple API and slots).

### `e(...)`

```java
// With a configurator
scope.e(new Column(), col -> {
    col.layout().spacing(4).padding(8);
    col.e(new Label(Component.literal("Hello")));   // no config needed
});

// Without a configurator
scope.e(new Spacer(0, 8));
```

There are overloads for primitive / container / composable so the lambda's parameter type is
inferred. `e(element)` with no lambda is the shorthand for "add it, no configuration".

### Element access

```java
scope.getElement()   // the element instance being configured
scope.layout()       // this element's LayoutProperties (fluent; see §4)
```

`getElement()` is how you drive a composable's simple API from inside its configurator:

```java
scope.e(new TextInput(), input -> {
    input.layout().fillMaxWidth().fixedHeight(20);
    input.getElement().setHint(Component.literal("Name"));
    input.getElement().setResponder(text -> System.out.println(text));
});
```

### Reactivity

These are available when configuring a **container** (`IContainerScope`) or at the **root**
(`RootScope`) — both implement `ICompositionScope`.

```java
scope.bind(state);        // state change => rebuild this scope's subtree (structure)
scope.bindLayout(state);  // state change => re-measure/re-place only (size/position)
```

- **`bind`** — structural. Use for lists, `if`/`else` branches, swapping children.
- **`bindLayout`** — layout only, no recomposition. Use for values that change size/position
  frequently (animations, scroll offsets).
- **No binding** — just read a `Supplier` inside a draw/measure. Free redraws happen every frame.
  This is the default for text, color, etc.

### Events

Handlers registered on the current node. Boolean handlers return `true` to consume the event and
stop bubbling; `Runnable` handlers (`onMouseEnter`/`onMouseExit`/focus) have no return.

```java
scope.onClick(event -> { ...; return true; });
scope.onScroll(event -> { ...; return true; });
scope.onScrollWhenFocused(event -> { ...; return true; }); // only when focused
scope.onMouseMove(event -> ...);
scope.onMouseDrag(event -> ...);
scope.onMouseRelease(event -> ...);
scope.onKeyPress(event -> ...);
scope.onKeyRelease(event -> ...);
scope.onCharTyped(event -> ...);
scope.onMouseEnter(() -> ...);   // Runnable
scope.onMouseExit(() -> ...);
scope.onFocusGained(() -> ...);
scope.onFocusLost(() -> ...);

scope.requestFocus();   // claim keyboard focus for this node
scope.isFocused();      // is this node focused?
```

Event records (all in `com.tridevmc.compound.ui.event`):

| Record | Fields |
|---|---|
| `MouseClickEvent` | `x, y, button, shiftDown, ctrlDown, altDown` |
| `MouseReleaseEvent` | `x, y, button` |
| `MouseDragEvent` | `button, x, y, deltaX, deltaY` |
| `MouseMoveEvent` | `x, y, prevX, prevY` |
| `MouseScrollEvent` | `x, y, scrollX, scrollY` |
| `KeyInputEvent` | `keyCode, logicalKeyCode, shiftDown, ctrlDown, altDown` |
| `CharEvent` | `codePoint, modifiers` |

### Animations

Animations created during composition belong to that composition and are disposed before it replays. Cache an animation across replays with `scope.retainAnimation(animation)`; it is then disposed when the node detaches. Clear a cached field in `onDetached()` if the element can be mounted again.

```java
var fade   = scope.animateFloat(0f, 300);                       // EASE_IN_OUT default
var count  = scope.animateInt(0, 500);
var color  = scope.animateColor(0xFF000000, 250);
var pulse  = scope.animateFloatLooping(0.4f, 1.0f, 800);        // STEP easing
var blink  = scope.animateIntLooping(0, 1, 500, Easing.EASE_IN_OUT);

fade.set(1f);            // animates to target
fade.get();              // current value (this tick)
fade.get(partialTicks);  // interpolated value for smooth drawing
```

Explicit-easing overloads exist for all six (`animateFloat/Int/Color` and their `...Looping`
variants). `AnimatedState<T>` also offers `setImmediate(value)`, `isAnimating()` and
`stopLooping()`.

### Deferred layout (advanced)

Mutate layout properties in response to a bound state, re-run right before each layout pass:

```java
var width = scope.animateInt(0, 300);

box.layout()
   .fillMaxHeight()
   .deferred(deferred -> {
       deferred.bind(width);
       deferred.layout().fixedWidth(width.get());
   });
```

### Advanced / author-only

Available on `ICompositionScope`; intended for a composable's internal `compose` method. `slot` and
`slotInto` throw `UnsupportedOperationException` in container/root scopes.

```java
scope.getTree();                                  // UITree — e.g. the animation scheduler
scope.getSlotMap();                               // this composable's slot map
scope.slot(key, defaultContent);                  // render user content, else default
scope.slotInto(key, defaultContent, targetScope); // render into a nested scope
```

---

## 4. Layout properties

Every element is laid out by its parent according to its `LayoutProperties`, reached with
`scope.layout()` (fluent, chainable). Not every property applies to every container — the notes
below say which.

### Sizing

| Method | Effect |
|---|---|
| `fixedWidth(int)` / `fixedHeight(int)` | Force a dimension |
| `fixedSize(int w, int h)` | Force both |
| `fillMaxWidth()` / `fillMaxHeight()` / `fillMax()` | Expand to available space |
| `minWidth(int)` / `minHeight(int)` / `maxWidth(int)` / `maxHeight(int)` | Clamp |
| `weight(float)` | Reserved for future use |

### Spacing

| Method | Effect | Used by |
|---|---|---|
| `padding(int all)` | Space **inside** bounds | Box, Column, Row, Stack, Grid |
| `padding(int horizontal, int vertical)` | Same, split axes | As above |
| `padding(int l, int t, int r, int b)` | Per-side | As above |
| `margin(int all)` | Space **outside** bounds (parent-reserved) | All elements |
| `margin(int horizontal, int vertical)` | Same, split axes | All |
| `margin(int l, int t, int r, int b)` | Per-side | All |
| `spacing(int)` | Gap between children | Column (vertical), Row (horizontal) |

### Alignment

| Method | Effect | Used by |
|---|---|---|
| `contentAlignment(Alignment)` | Align child(ren) in the content area | Box, Stack |
| `horizontalAlignment(Alignment)` | Align children across a column | Column |
| `verticalAlignment(Alignment)` | Align children across a row | Row |

`Alignment` values: `TOP_LEFT`, `TOP_CENTER`, `TOP_RIGHT`, `CENTER_LEFT`, `CENTER`, `CENTER_RIGHT`,
`BOTTOM_LEFT`, `BOTTOM_CENTER`, `BOTTOM_RIGHT`.

### Other

| Method | Effect |
|---|---|
| `clip()` | Clip children to this element's bounds (GPU scissor) |
| `layer(int)` | Render/hit-test above normal nodes (popups, dropdowns, tooltips); `0` = default |
| `gridSize(int columns, int rows)` | Reserved for future Grid enhancements |
| `deferred(Consumer<DeferredScope>)` | Reactive layout mutation (see §3) |

`Grid` spacing is set through its **constructor**, not `spacing()`. `Panel`/`Button` have no
intrinsic size — give them a size or `fillMax` and let their content drive it.

---

## 5. Element catalog

### Containers (layout primitives)

| Element | Constructors | Notes |
|---|---|---|
| `Stack` | `Stack()` | Z-order layering; `contentAlignment` positions children and makes the stack expand to fill constraints |
| `Column` | `Column()` | Vertical; `spacing`, `horizontalAlignment` |
| `Row` | `Row()` | Horizontal; `spacing`, `verticalAlignment` |
| `Box` | `Box()` | Single child + padding/alignment. Multiple children → only first renders (logs a warning). With padding it fills available space; without, it wraps content |
| `Grid` | `Grid(int columns)`, `Grid(int columns, int spacing)`, `Grid(int columns, int hSpacing, int vSpacing)` | Rows inferred from child count; column widths / row heights sized to content |

```java
scope.e(new Grid(9, 0, 0), grid -> {
    for (int i = 0; i < 27; i++) grid.e(new InventorySlot(menu.getSlot(i)));
});
```

### Visual primitives

| Element | Constructors | Notes |
|---|---|---|
| `Rect` | `Rect(int color)`, `Rect(Supplier<Integer> color)` | Solid fill (ARGB) |
| `GradientRect` | `GradientRect(int top, int bottom)`, `GradientRect(Supplier<Integer> top, Supplier<Integer> bottom)` | Vertical gradient |
| `Sprite` | `Sprite(IScreenSprite)`, `Sprite(Supplier<IScreenSprite>)` | `setSprite` / `setSpriteSupplier`; sprite carries its own stretch/tile/nine-slice writer |
| `Text` | `Text(Component)`, `Text(Component, int color)`, `Text(Supplier<Component>, Supplier<Integer>, Supplier<Boolean> shadow)` | `setColor`, `setShadow`, `setWrap`; compose a `Rect` behind text for selection highlighting |
| `Spacer` | `Spacer()` (flexible), `Spacer(int w, int h)` | Occupies space, draws nothing |
| `ItemDisplay` | `ItemDisplay(ItemStack)`, `ItemDisplay(Supplier<ItemStack>)`, `ItemDisplay(Supplier<ItemStack>, Supplier<String> countOverride)` | Renders an item stack |

### Composable widgets

Each exposes a simple setter API via `scope.getElement()` and, where noted, `SlotKey` constants for
`fillSlot`.

| Element | Constructors | Slots | Selected API |
|---|---|---|---|
| `Label` | `Label(Component)`, `(Component, int color)`, `(Component, int color, boolean shadow)`, plus `Supplier` overloads | — | `setText`, `setColor`, `setShadow`, and supplier variants |
| `Panel` | `Panel()`, `Panel(IScreenSprite)`, `Panel(Supplier<IScreenSprite>)` | `CONTENT_SLOT` | `setSprite` |
| `Button` | `Button()`, `Button(boolean enabled)`, `Button(boolean enabled, boolean visible)` | `CONTENT_SLOT` | `addPressListener`, `addHoverListener`, `setEnabled`, `setVisible`, state getters |
| `Surface` | `Surface(int bg)`, `Surface(int bg, int border, int borderWidth)`, `Supplier` variants | `CONTENT_SLOT` | — |
| `Divider` | `Divider()`, `Divider(int color)`, `Divider(int color, int thickness)` | — | `setColor`, `setThickness` |
| `ScrollArea` | `ScrollArea()`, `ScrollArea(Direction)` | `CONTENT_SLOT` | `direction`, `scrollbarStyle`, `scrollSpeed`, `showScrollbar`, `scrollTo`, `getScrollXState`/`getScrollYState`, `getMaxScrollX`/`getMaxScrollY` |
| `Accordion` | `Accordion(Component title[, boolean expanded])` | `CONTENT_SLOT` | `setExpanded`, `isExpanded`, `setOnExpandedChanged` |
| `CycleButton<T>` | `CycleButton(Component title, List<T> options, Function<T, Component> formatter)` | `Button.CONTENT_SLOT` | `getValue`, `setValue`, `setOnValueChanged`; custom slot content replaces the default label |
| `TextInput` | `TextInput()`, `TextInput(String initial)` | — | `setHint`, `setResponder`, `setValue`, `setFilter`, `setMaxLength`, `setEditable`, `setCentered`, `setBordered`, `setSuggestion`, `addFormatter`; `getTextState` |
| `TextArea` | `TextArea()` | — | `setHint`, `setResponder`, `setValue`, `setFilter`, `setMaxLength`, `setEditable`, `setCursorAnimationMode` |
| `NumberInput` | `NumberInput()`, `NumberInput(double initial)` | — | `setRange`, `setValue`, `setOnValueChanged`, `setAllowDecimals`, `setHint`, `getValue` |
| `Checkbox` | `Checkbox()`, `Checkbox(Component label)`, `Checkbox(Component label, boolean checked)` | — | `setChecked`, `setOnCheckedChanged`, `setLabel`, `setLabelRight`, `setSpacing`, `addCheckedChangeListener` |
| `ToggleSwitch` | `ToggleSwitch()`, `ToggleSwitch(boolean on)` | — | `setOn`, `setOnChanged`, `addOnChangedListener`, `setUseTextures`, `setOnColor`/`setOffColor`, `setOnLabel`/`setOffLabel` |
| `Slider` | `Slider()`, `Slider(double min, double max, double step)` | — | `setRange`, `setStep`, `setValue`, `setOnValueChanged`, `setOnDragStart`, `setOnDragEnd`, `setShowValue`, `setFormatter` |
| `ProgressBar` | `ProgressBar()` | — | `setProgress`, `setProgress(current, max)`, `setIndeterminate`, `setShowFraction`, `setShowPercentage`, `setLabel`, `setLabelFormatter`, `setOnCompleted` |
| `Dropdown<T>` | `Dropdown(List<T> options)`, `Dropdown(List<T> options, T selected)` | — | `setSelected`, `setOnSelectionChanged`, `setDisplayTextProvider`, `setPlaceholder`, `setMaxVisibleItems`, `setOnOpen`/`setOnClose`, `getSelected` |
| `ListView<T>` | `ListView()` | — | `setItems`, `addItem`, `removeItem`, `setSelectedIndex`, `setOnSelectionChanged`, `setDisplayTextProvider`, `setItemHeight`, `setMaxVisibleItems`, `getSelected` |
| `ListItem` | `ListItem(Component label, Supplier<Boolean> selected, Supplier<Boolean> hovered, Supplier<Integer> selBg, Supplier<Integer> hovBg, Supplier<Integer> selText, Supplier<Integer> text)` | — | `setClickHandler`, `setHoverHandlers` |
| `RadioButton` | `RadioButton(boolean selected, int outer, int inner)`, `Supplier` variant | — | — |
| `RadioButtonGroup` | `RadioButtonGroup()` | — | `addOption`, `removeOption`, `setSelectedIndex`, `setOnSelectionChanged`, `setOptionSpacing` |
| `Tabs` | `Tabs()` | — | `addTab(label, content[, enabled])`, `removeTab`, `setSelectedTab`, `setTabEnabled`, `setOnTabChanged` |
| `Modal` | `Modal(Component title)`, `Modal(Component title, Component message)` | — | `addButton`, `clearButtons`, `show`, `close`, `setSize`, `setTitle`, `setMessage`, `setOnClose` |
| `ContextMenu` | `ContextMenu()` | — | `addItem`, `addSeparator`, `clearItems`, `show(x, y)`, `hide` |
| `Tooltip` | `Tooltip(Component)`, `Tooltip(Supplier<Component>)` | — | `setDelay`, `setMaxWidth`, `setOnShow`, `setOnHide` |
| `TreeView<T>` | `TreeView()` | — | `getRoot().addChild(value[, config])`, `setDisplayTextProvider`, `setOnSelectionChanged`, `setItemHeight`, `setEnabled` |
| `InventorySlot` | `InventorySlot(AbstractContainerMenu menu, int slotIndex)`, `InventorySlot(Slot vanillaSlot)` | — | Managed automatically; `setDisplayStack`, `setDrawOverlay`, `setDrawUnderlay`, `reset` |

```java
scope.e(new Panel(), panel -> {
    panel.layout().fixedSize(178, 190);
    panel.fillSlot(Panel.CONTENT_SLOT, content -> {
        content.e(new Column(), col -> {
            col.layout().spacing(4).padding(8);
            col.e(new Label(Component.literal("Crate"), 0x404040, false));
            col.e(new Grid(9, 0, 0), grid -> {
                for (int i = 0; i < 27; i++) grid.e(new InventorySlot(menu.getSlot(i)));
            });
        });
    });
});
```

```java
scope.e(new Button(), button -> {
    button.layout().fillMaxWidth().fixedHeight(20);
    button.fillSlot(Button.CONTENT_SLOT, content ->
        content.e(new Label(Component.literal("Click me"))));
    button.getElement().addPressListener((x, y) -> System.out.println("pressed"));
});
```

---

### Scrollbar styles

```java
scope.e(new ScrollArea().scrollbarStyle(ScrollArea.ScrollbarStyle.GRIPPY), scroll -> {
    scroll.layout().fixedSize(180, 120);
    scroll.fillSlot(ScrollArea.CONTENT_SLOT, content -> {
        // Compose the scrollable content here.
    });
});
```

`LIST` is the default six-pixel selection-list scrollbar with a proportional thumb.
`GRIPPY` uses a fourteen-pixel recessed track with the creative inventory's twelve-pixel-wide,
fifteen-pixel-long thumb, including its disabled sprite when all content fits. Both leave a four-pixel gutter beside
content and support wheel scrolling and dragging. The grippy thumb stays twelve pixels wide and fifteen pixels high in either direction; its horizontal track is seventeen pixels high.
The gallery's Scrollbars tab shows both styles with overflowing and fitting content.

Both text inputs accept `setCursorAnimation(milliseconds, easing)` for blink or fade timing.
Use `Easing.STEP` for on/off blinking or `Easing.EASE_IN_OUT` for a fade. Carets and selection
backgrounds are composed rectangles.

`ProgressBar` follows vanilla's loading-screen appearance: a one-pixel outline with a solid
fill inset by two pixels. The default fill and outline are white with a transparent interior.
`setFillColor` changes the fill and outline; `setBackgroundColor` colours the interior.
Determinate progress changes the fill width, while indeterminate mode animates a clipped segment.

## 6. Reactivity in practice

There are two ways data reaches the UI.

**Binding (structure/layout).** Call `bind`/`bindLayout` in a scope; the framework remembers which
node the call belongs to and invalidates only what the state can affect.

```java
private final State<List<String>> names = State.of(generate());

scope.e(new Column(), col -> {
    col.bind(names);                       // rebuild this column when names changes
    for (var name : names.get()) {
        col.e(new Label(Component.literal(name)));
    }
});
```

`State<T>` (`State.of(initial)`) has `get()`, `set(value)`, `update(fn)`, and `dispose()`. A `set`
synchronously notifies bound nodes.

**Providers (draw-time reads).** Many elements accept a `Supplier<...>` for a property. Because
drawing happens every frame, the supplier is simply re-read — no binding, no recomposition.

```java
private final State<Integer> tint = State.of(0xFFFFFFFF);

scope.e(new Rect(() -> tint.get()), r -> r.layout().fillMax());
```

> `State<T>` is **not** a `Supplier<T>` (it does not extend it). When an element wants a supplier,
> wrap the state: `() -> state.get()`, or use the element's `setXxxSupplier` setter.

**Animations = bound state.** Create with `scope.animateFloat/Int/Color(...)`, then either read at
draw time (draw-only, cheapest) or `bindLayout` them when they change size/position. A bound
animated value re-measures the affected subtree once per game tick; smoothness comes from
`get(partialTicks)` at draw time.

**Deferred scope.** For layout properties driven by a bound state without recomposition, use
`layout().deferred(...)` (§3).

---

## 7. Slots (customizing composables)

A composable declares `public static final SlotKey` constants for the regions it allows you to
override. To customize, call `fillSlot` in its configurator; to provide a default, the author calls
`scope.slot(key, defaultContent)`. If the user filled the slot, their content wins; otherwise the
default runs.

`SlotKey` is a named key; keys compare by name, so `Panel.CONTENT_SLOT` and `Button.CONTENT_SLOT`
are equal by value but live in separate per-element slot maps and never collide.

Built-in slot keys: `Panel.CONTENT_SLOT`, `Button.CONTENT_SLOT`, `ScrollArea.CONTENT_SLOT`,
`Surface.CONTENT_SLOT`.

---

## 8. Drawing and custom elements

Custom **primitives** extend `BasePrimitiveElement` and implement `measure(...)` plus
`drawElement(IScreenContext, Bounds)`; the base wraps drawing with visibility/lifecycle and provides
an empty `place`. `draw` is called every frame.

`IScreenContext` / `IPrimitiveScreenContext` is the only drawing surface you should use. Highlights:

```java
context.drawRect(x, y, w, h, argb);
context.drawRectOutline(x, y, w, h, argb, thickness);
context.drawGradientRect(x, y, w, h, topArgb, bottomArgb);
context.drawSprite(sprite, x, y, w, h);
context.drawText(Component, x, y);
context.drawTextWithShadow(Component, x, y);
context.drawCenteredText(Component, x, y);
context.drawItemStack(stack, x, y, altText);
context.drawTooltip(Component, x, y);
context.enableScissor(x, y, right, bottom);
context.disableScissor();
context.getMouseX(); context.getMouseY();
context.getPartialTicks(); context.getTicks();
context.getFont(); context.getMc();
```

Custom **composables** extend `BaseElement` and implement `IComposableElement.compose(ICompositionScope
scope)` — build internal structure with `scope.e(...)`, bind internal state, register handlers, and
expose your own `SlotKey`s plus a small setter API. Custom **containers** extend `BaseContainer` and
implement `measure`/`place`. In all cases, `getBounds()` returns the last placed bounds.

---

## 9. Gotchas that save time

- **Scope type is your permission system.** Primitives can't have children; composables can't have
  arbitrary children (slots only); events are only available on container/composable/root scopes.
- **`bind` vs `bindLayout`.** Structure changes need `bind`; size/position-only changes should use
  `bindLayout` to skip recomposition.
- **`Grid` spacing is constructor-level**, not `layout().spacing(...)`.
- **`Panel` and `Button` are flexible by default.** They measure to their content; with no content,
  set an explicit size (or `fillMax`).
- **`Box` is single-child.** Extra children are ignored (with a debug log).
- **Use `layer(...)` for overlays** (dropdowns, modals, tooltips) so they render on top and
  hit-test first.
- **`ScrollArea` clips itself** and reserves the scrollbar width plus a four-pixel gutter; put your content in
  its `CONTENT_SLOT`, and give the area an explicit size or fill constraint.
- **Wrap states for supplier properties**: `() -> state.get()`.
- **No manual animation cleanup** — animations created through a scope are tied to the node
  lifetime.
