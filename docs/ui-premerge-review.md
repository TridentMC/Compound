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
