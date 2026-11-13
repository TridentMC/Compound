# Composable UI validation

[UI PR #4](https://github.com/TridentMC/Compound/pull/4) contains the framework rewrite.
The Molecule demo migration stays in [PR #6](https://github.com/TridentMC/Compound/pull/6).

## UI coverage

The coverage baseline is the public widget contracts, the previous UI's capabilities,
and the Molecule gallery. All 38 concrete public elements are accounted for, and every
legacy widget category has a replacement. Tests focus
on observable behaviour; passive drawing elements also receive gallery and pixel coverage.

| Group | Elements | Coverage |
| --- | --- | --- |
| Drawing | Rect, GradientRect, Text, Label, Sprite, Divider, Surface, Panel | Production layout/rendering, colours, text extraction, clipping, transforms, and gallery screenshots. |
| Layout | Box, Column, Row, Stack, Grid, Spacer | Bounded/unbounded constraints, padding, margins, weights, alignment, placement, and inventory grids. |
| Forms | Button, Checkbox, ToggleSwitch, RadioButton, RadioButtonGroup, Slider, CycleButton, TextInput, TextArea, NumberInput | Routed pointer/keyboard actions, toggles and exclusive choices, disabled controls, editing/validation, selection, and styled glyph geometry. RadioButton itself is a passive indicator. |
| Collections | ListItem, ListView, TreeView, ISelection, lazy lists/grids | Single/multiple selection, Ctrl toggles, Shift/Ctrl+Shift ranges, navigation, stable keys, source changes, virtual mounting, and focus/capture retention. |
| Overlays | Dropdown, ContextMenu, Modal, Tooltip | Viewport placement, current transforms, scrolling, dismissal, queued input, global paint/input order, and focus restoration. |
| Navigation | Tabs, Accordion, ScrollArea | Tab overflow/reveal, accordion expansion, nested scrolling, both axes and scrollbar styles. |
| Inventory | InventorySlot, ItemDisplay, composed container screens | Slot positions and identity, transformed input, native hover/drag/release, carried-item boundaries, rendering, and culling. |
| Feedback/animation | ProgressBar, cursors, animated state, timelines | Progress rendering/range behaviour, pointer cursors, timing, cancellation, recomposition, and disposal. |

The combined demo checks render every gallery tab, a compact viewport, multiple
selections, and resize retention. Rendering-only coverage is appropriate for passive
decorations; interactive controls have direct behaviour checks.

## Test scope

Tests exercise production composition, layout, input routing, and rendering while mocking
Minecraft services. They retain regressions for real failures in geometry, lifecycle,
selection, native inventory integration, and callback reentrancy.

The inventory integration test checks public slot/grid bounds and submitted rendering.
It does not freeze private Stack/Sprite child order. Text editing checks exercise the
public adapter and editing behaviour instead of repeating defaults and getter values.
Related control checks share small grouped scenarios; isolated tests for every class
or setter are unnecessary.

Multi-selection is opt-in:

```java
list.setSelectionMode(EnumSelectionMode.MULTIPLE);
var selection = ISelection.<Integer>create(EnumSelectionMode.MULTIPLE);
var grid = ILazyCollection.grid(source, 4, 20, 20, 2, composer).selection(selection);
```

Ctrl-click toggles independent items, Shift selects an anchored range, and Ctrl+Shift
adds a range. Lazy collections retain selection by stable key across virtual unmounts.
They use fixed cells and require identity transforms on their ancestors. ListView and
TreeView remain eager. Axis-aligned clipping supports translation/scaling; rotated
clipping requires a stencil backend.

## Validation

Validated on 2026-11-13: 195 library tests and 32 demo tests pass, with no failures or
skips. Both builds and Javadoc generation pass; existing Javadoc warnings remain.

Run `gradle :build :javadoc`. With the separate migration applied, also run
`gradle :Molecule:build`. Gradle and test workers are limited in the build configuration;
tests use `java.awt.headless=true`.

Diagnostic images are generated under `build/screenshots/` and
`Molecule/build/screenshots/gallery/`. Border/selection pixels and input geometry are
asserted without depending on operating-system font hashes. The shared Java2D context
handles transforms, scissors, styled text, a fixed clock, and resource disposal.

Publication checks verify six Compound publications and matching-version core
dependencies. The separate migration gives Molecule no publishing plugin or tasks.
Release jars contain the current UI classes and no Molecule classes.

Local validation reuses existing Minecraft 26.3 artifacts with
`-x :createMinecraftArtifacts -x :Molecule:createMinecraftArtifacts`, because NeoForge's
launcher-manifest cache replacement fails in this Windows environment. Fresh artifact
generation and Jenkins execution remain unverified. Java2D fonts, textures, and items
are diagnostic approximations; native GPU/audio fidelity is outside these headless checks.
