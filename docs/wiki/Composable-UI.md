# Composable UI

This guide describes Compound 2.0 on Minecraft 26.3, NeoForge 26.3.0.26-beta, and Java 25. The API is introduced by [PR #4](https://github.com/TridentMC/Compound/pull/4); check that PR for merge status. Existing 1.x consumers should use the [legacy guide](Legacy-UI) until migrating.

Use `compound-ui` with the matching `compound-core`, or `compound-all`. The UI module's Maven POM declares the core dependency. Compound remains a library included by your mod; it has no mod entry point of its own.

## Create a screen

Extend `ComposedUI` and override `compose`. Add elements with `scope.e(element, configuration)`. The configuration scope exposes `layout()` and `getElement()`. Composable controls accept children through named slots.

```java
import com.tridevmc.compound.ui.element.Box;
import com.tridevmc.compound.ui.element.Button;
import com.tridevmc.compound.ui.element.Column;
import com.tridevmc.compound.ui.element.Label;
import com.tridevmc.compound.ui.element.Panel;
import com.tridevmc.compound.ui.layout.Alignment;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.ComposedUI;
import com.tridevmc.compound.ui.state.State;
import net.minecraft.network.chat.Component;

public class CounterScreen extends ComposedUI {
    private final State<Integer> count = State.of(0);

    @Override
    protected void compose(ICompositionScope scope) {
        scope.e(new Box(), root -> {
            root.layout().fillMax().contentAlignment(Alignment.CENTER);
            root.e(new Panel(), panel -> {
                panel.layout().fixedSize(220, 90);
                panel.fillSlot(Panel.CONTENT_SLOT, content -> {
                    content.e(new Column(), column -> {
                        column.layout().fillMax().padding(8).spacing(6);
                        column.e(new Label(() -> Component.literal("Count: " + count.get()), () -> 0xFF404040));
                        column.e(new Button(), button -> {
                            button.layout().fillMaxWidth().fixedHeight(20);
                            button.fillSlot(Button.CONTENT_SLOT, label ->
                                    label.e(new Label(Component.literal("Add one"))));
                            button.getElement().addPressListener((x, y) ->
                                    count.update(value -> value + 1));
                        });
                    });
                });
            });
        });
    }
}
```

Open it from client-side code using `Minecraft.getInstance().gui.setScreen(new CounterScreen())`. Keep this screen class and its registration away from dedicated-server class loading.

## State and lifetime

Create observable values with `State.of(initialValue)`. Keep values that should survive a rebuild in fields on the screen or on retained controls. `StateImpl` is internal.

Suppliers such as the counter label are read during rendering. Use `scope.bind(state)` on the owning composition scope when a change affects which children exist. For example, a conditional branch that adds an advanced-settings panel needs a binding. Avoid binding a whole screen solely to update a colour or a fixed-size label.

Elements are mounted instances. Do not mount the same element instance in two places at once. A screen resize can rebuild the tree, while state kept in screen fields survives. Removing the screen disposes its tree bindings and registered animation ownership.

## Layout and slots

Layout dimensions use GUI pixels, so Minecraft's GUI scale affects their on-screen size. `Row` and `Column` arrange children sequentially; `Box` aligns one child; `Stack` overlays them; `Grid` arranges cells.

Use `fixedSize`, `fillMaxWidth`, `fillMaxHeight`, `padding`, `margin`, `spacing`, and `weight` on an element's `layout()`. Weights divide available bounded space. Maximum-size settings remain constrained by the parent; explicit unbounded layout operations are for content that must measure beyond its viewport.

`Panel.CONTENT_SLOT`, `Button.CONTENT_SLOT`, and `ScrollArea.CONTENT_SLOT` describe where content belongs inside those controls. Put a container inside a slot when it needs several children. The slot map is unrelated to Minecraft inventory slots.

## Scrolling and controls

Give a scroll area a bounded viewport and place its content in the content slot:

```java
scope.e(new ScrollArea(), scroll -> {
    scroll.layout().fixedSize(200, 120);
    scroll.getElement().scrollbarStyle(ScrollArea.ScrollbarStyle.GRIPPY);
    scroll.fillSlot(ScrollArea.CONTENT_SLOT, content -> {
        content.e(new Column(), rows -> {
            rows.layout().fillMaxWidth().spacing(2);
            for (int i = 1; i <= 30; i++) {
                rows.e(new Label(Component.literal("Entry " + i)));
            }
        });
    });
});
```

Import `ScrollArea` from `com.tridevmc.compound.ui.element` for this fragment. `LIST` uses a proportional selection-list scrollbar; `GRIPPY` uses the textured fixed-size thumb. The horizontal direction accepts horizontal wheel input and falls back to the vertical wheel. At an edge, an unhandled scroll can reach an enclosing scroll area.

Available controls include buttons, checkboxes, switches, sliders, progress bars, single-line and multiline text inputs, number inputs, cycle buttons, dropdowns, radio groups, tabs, accordions, lists, trees, context menus, modals, and tooltips. Read each control's Javadocs for its callbacks, state accessors, and content slots. Retain editable controls when their text or selection must survive recomposition.

## Input and animation

Register input handlers on a composition scope. Return `true` from a handler only when it consumes the event. Built-in controls handle their own focus and editing; custom focusable elements participate in tree focus routing.

Mouse wheel events expose `scrollX()` and `scrollY()`. Navigation uses physical key codes; editing shortcuts use `KeyInputEvent.isShortcut(...)` with logical key codes. `CharEvent` carries a Unicode code point, which may occupy two Java `char` values.

Use scope animation factories such as `animateFloat`, `animateColor`, and the looping variants. Durations are milliseconds. Choose `Easing.STEP` for abrupt changes or an easing curve for interpolation. The scope registers animation ownership with the tree. For an animation cached in an element field, call `scope.retainAnimation(animation)` during its owning composition, including the initial pass. This keeps it alive across owner recompositions. Detaching disposes it; clear the cached field in `onDetached()` and create a fresh animation on remount. Use the tree scheduler shared by the scope.

Carets and selection highlights compose rectangles. `CursorBlink` lets the text controls use step blinking or an easing curve through the same animation system. A frame colour change does not require rebuilding the control.

## Inventory screens

Extend `ComposedUIContainer<T>` with a menu type extending `CompoundContainerMenu`. Pass the menu, player inventory, and title to `super`. Register the screen through NeoForge's `RegisterMenuScreensEvent`, as with native menu screens.

Within `compose`, add an `InventorySlot(menu, index)` for each menu slot that should be displayed. The index refers to the menu's slot order; it is not a pixel position. A grid can position a row of menu slots:

```java
scope.e(new Grid(9, 0, 0), grid -> {
    for (int index = 0; index < 9; index++) {
        grid.e(new InventorySlot(this.getMenu(), index));
    }
});
```

Import `Grid` and `InventorySlot` from `com.tridevmc.compound.ui.element`. This fragment assumes the menu has at least nine slots. Keep server-side inventory rules and item movement in the menu. The screen integrates native pickup, placement, tooltips, and carried-stack rendering with the element bounds.

For a complete menu and screen, see [Molecule's crate example](https://github.com/TridentMC/Molecule/tree/codex/composable-ui-demo-26.3/src/main/java/com/tridevmc/molecule/ui).

## Further reference

- [Consumer API and available controls](https://github.com/TridentMC/Compound/blob/codex/composable-ui-26.3/compose/api-surface.md)
- [Layout, custom elements, and architecture](https://github.com/TridentMC/Compound/blob/codex/composable-ui-26.3/compose/framework-reference.md)
- [Migration from the old UI API](https://github.com/TridentMC/Compound/blob/codex/composable-ui-26.3/compose/migration-2.0.md)

Custom controls normally implement `IComposableElement` and combine existing primitives. Add a drawing primitive only when an existing one cannot express the rendering. Lists currently construct rows eagerly; large data sets may need application-level paging.
