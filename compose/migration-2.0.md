# Migrating to Compound 2.0

Compound 1.18.0 ports the existing libraries to Minecraft 26.3. Compound 2.0.0 replaces the legacy UI API with the composable framework on that same platform. This is a major-version change; existing UI consumers need migration.

- Replace `CompoundUI` with `ComposedUI`, and `CompoundUIContainer` with `ComposedUIContainer`. Describe the screen with composition scopes instead of registering legacy elements and layouts.
- Replace legacy `Element*` widgets and `Layout*` classes with composable controls, containers, and layout properties. Use `InventorySlot` and the container's slot mapping for inventory screens.
- Construct observable values with `State.of(initialValue)`. `StateImpl` is package-private and its duplicate factory has been removed.
- Use typed tree events and state bindings instead of legacy listener registration. MouseScrollEvent exposes `scrollX()` and `scrollY()`; key events distinguish navigation keys from logical editing shortcuts. CharEvent carries a Unicode code point.
- Carets and highlights compose existing rectangles and text; cursor blinking uses the animation system. Keep retained state and animation ownership tied to the screen's lifecycle.

For consumers of earlier framework prototypes, replace `scrollDelta()` with `scrollY()`. TextInput no longer exposes `setCanLoseFocus`/`canLoseFocus`: tree focus is authoritative. Maximum layout constraints stay within the parent; use the explicit unbounded operations only when intended.

Start with the [consumer API](api-surface.md), then the [framework reference](framework-reference.md). The companion Molecule framework-demo branch provides a widget gallery and a working inventory screen.

Validation includes standalone Compound and combined Molecule builds, 11 Compound checks and 4 demo checks, plus in-game checks at 1440×900. Lists construct rows eagerly and dirty layout traverses the tree; no large-list performance guarantee is made. Non-US keyboard layouts and intermediate quick-craft drag previews were source-reviewed, not fully exercised by the UI automation.
