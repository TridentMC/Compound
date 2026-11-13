# Composable UI pre-merge review

Review target: [Compound PR #4](https://github.com/TridentMC/Compound/pull/4),
base `878ff1f5cdc4af692fc5c073fa2a0e09ecc65fa8`, original head
`937f25e70aa52d1141662dc50da724a6c521eed4`. Standards and spec reviews ran in
separate adversarial agents. The repository migration has a separate PR.

## Standards

The review checked the documented visibility, geometry, input, and lifetime contracts,
alongside the local code-style and behaviour-composition skills.

| Priority | Finding | Repair |
| --- | --- | --- |
| P1 | Built-in controls compared transformed pointer events with untransformed bounds. | Buttons, sliders, text editors, and scrollbars convert through sampled geometry. Public event and listener coordinates remain in screen space. |
| P2 | Hidden descendants retained focus and keyboard input despite the visibility contract. | Input targets validate attachment and ancestor visibility; focus and capture are released. |
| P2 | A click handler could detach itself and then acquire stale capture. | Capture requires the consuming node to remain attached and visible. |
| P2 | Tooltip drawing helpers disappeared at screen edges and could intercept content after gaining drawable bounds. | Helpers use anchor bounds, transformed hover, and explicit pointer exclusion. Children remain eligible for hit testing. |

Four findings repaired. The most severe was incorrect interaction with transformed controls.

## Spec

The user required multi-selection, headless interaction checks, and useful screenshots
before merging the rewrite. The PR description supplied the existing feature scope.

| Priority | Finding | Repair |
| --- | --- | --- |
| P1 | Lists and trees could select only one row. | Opt-in `EnumSelectionMode.MULTIPLE`, Ctrl toggles, Shift ranges, Ctrl+A, keyboard navigation, immutable snapshots, and callbacks. Single selection remains the default. |
| P2 | Modifier selection also expanded or collapsed tree branches. | Modifier clicks change selection without toggling expansion. |
| P2 | Removed tree subtrees could remain selected in retained node objects. | Removal prunes selection; detached and foreign nodes are rejected. Collapsed descendants retain selection. |
| P2 | Tab headers overflowed narrow viewports. | Headers scroll horizontally and reveal selected or focused tabs; navigation skips disabled tabs and retains header nodes. |

Four findings repaired. The most severe was missing multiple selection.

## Headless verification

Tests exercise production composition, layout, input routing, transforms, clipping, and
collection mutation. Java2D raster checks verify pointer geometry, selection and focus
outlines, text extraction, and overlay clipping without opening a client window.

`BufferedImageScreenContext` supports a fixed frame clock, matrix transforms, nested
scissors, styled text, and resource disposal. Pixel assertions avoid hashes that depend
on an operating system font. These are diagnostic images with approximate fonts and
sprite/item placeholders. They do not establish native Minecraft texture, font, GPU,
or audio fidelity.

The Molecule demo harness renders the production gallery, every tab, a compact viewport,
click selection, multiple selected rows, and selection retention after resize. Generated
images remain in ignored build outputs. Jenkins archives them for review.

Run the library checks with `gradle :test`. Once the separate migration is applied, run
`gradle :test :Molecule:test` and inspect `Molecule/build/screenshots/gallery/`.
Both builds limit Gradle and test workers and run with `java.awt.headless=true`.

Validation on 2026-11-12: 89 library tests and 32 demo tests passed, with no skipped tests.
Both projects built successfully. Publication assertions found exactly six Compound
publications and no demo publish tasks; generated config/network/UI POMs require
`compound-core` at the same version.

Local validation reused the existing Minecraft 26.3 artifacts with
`-x :createMinecraftArtifacts -x :Molecule:createMinecraftArtifacts`: NeoForge's normal
artifact refresh failed when replacing its cached launcher manifest on Windows.
The fresh-artifact path and the Jenkins job have not been executed in this review.

## Follow-up review

A fresh parallel adversarial review at `4d3de4d` reproduced seven additional issues.
The fixes stay in UI PR #4. The Molecule repository migration stays in PR #6.

| Issue | Repair and regression coverage |
| --- | --- |
| Padded or aligned containers used unbounded maxima as available space. | Box and Stack expand only along bounded axes; small content fits in either scroll direction. |
| Root placement ignored fixed and maximum dimensions. | Placement respects measured size and margins; explicit fill and minimum constraints retain their behaviour. |
| Container shortcuts escaped focused text editors. | Text editing retains keyboard ownership while preserving Escape handling. |
| Different slots with identical menu coordinates resolved to the same native slot. | Native dispatch preserves slot identity and restores original coordinates afterwards. |
| Transformed slots failed native hit testing. | Slot and inventory-background containment use the sampled drawing geometry. |
| Reentrant source changes during lazy-cell disposal corrupted mounted indices. | Pending source changes settle before cells mount from newer data. |
| Nested overlays ignored global numeric layer order. | Rendering and hit testing share ordered overlays; nested overlays draw once and lazy-cell clips remain effective. |

Multi-selection also covers opt-in lazy grids and lists. Selection is retained by stable
key across virtual unmounts, Ctrl-click toggles independent cells, Shift selects a
row-major range, and Ctrl+Shift adds a range to existing selections. Existing lists and
trees keep their single-selection default and opt-in multiple-selection behaviour.

For an existing list or tree, call `setSelectionMode(EnumSelectionMode.MULTIPLE)`.
For a lazy grid or list, retain the model outside composition and attach it:

```java
var selection = ISelection.<Integer>create(EnumSelectionMode.MULTIPLE);
var grid = ILazyCollection.grid(source, 4, 20, 20, 2, composer).selection(selection);
selection.setOnSelectionChanged(keys -> updateActions(keys));
```

An ordinary `Grid` composition can use the same model by supplying its ordered stable
keys through `setKeys`, passing mouse modifiers to `select`, and drawing from
`isSelected`. The lazy collection wires those behaviours automatically. Interactive
children retain their own consumed input.

The screen adapters use the mouse event's captured Shift and platform editing modifiers
for selection, including Ctrl+Shift ranges, instead of polling the live keyboard state.
Regression checks exercise both ordinary and container screen adapters.

Follow-up validation: both builds pass with 129 Compound tests and 32 Molecule tests,
zero failures and zero skips. The same cached-artifact limitation above still applies.
The grid interaction regression generates `build/screenshots/grid-multiple-selection.png`.

Three independent agents reviewed the fixes at `0b867b4` in parallel. They found three
additional edge cases, now covered by permanent regression tests:

- Built-in dropdowns inside modals needed layers above their containing overlays.
- Input scopes needed to preserve the effective global paint group during hit testing.
- Queued keyboard and wheel events needed reveal calculations based on both live and
  last-placed scroll offsets, including nested horizontal and vertical viewports.

The layer and selection reviewers rechecked those repairs and reported no remaining
actionable findings. The native-container reviewer also reported no actionable findings.
Final validation passes all 161 tests, both builds, and Javadoc generation. Javadoc
retains existing documentation warnings. All validation was headless; native graphics
and audio fidelity were not exercised.
