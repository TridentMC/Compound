# Trident Compound

Libraries for developing Minecraft mods with NeoForge. Compound is a dependency, not a standalone mod.

This branch targets Minecraft **26.3**, NeoForge **26.3.0.26-beta**, and Java **25**. Build with `gradle build` (ModDevGradle 2.0.147). The library version is set in `properties.gradle`.

## Modules

Use `compound-all`, or select `compound-core`, `compound-config`, `compound-network`, and `compound-ui`. Config, network, and UI depend on the matching version of core; their Maven POMs declare that dependency. The legacy inventory artifact is retained for compatibility.

```groovy
repositories {
    maven { url = 'https://repo.tridevmc.com/' }
}

dependencies {
    implementation "com.tridevmc.compound:compound-ui:${compoundVersion}"
}
```

Choose a version published for your Minecraft target. A branch version does not imply that its artifacts have been published. Include the required library modules in your mod's distribution; if shading and relocating, keep all Compound modules under the same relocated package.

Molecule is the companion demo mod used for in-game validation. Report game failures to the mod using Compound first; its developer can determine whether the library is responsible.

## License

Apache License 2.0. See [LICENSE](LICENSE).

## Composable UI

Version 2.0.0 replaces the legacy UI API. See the [consumer API](compose/api-surface.md), [framework reference](compose/framework-reference.md), and [migration notes](compose/migration-2.0.md).
