# Molecule

Demo mod for Compound. Build and test from the repository root:

```
gradle :Molecule:build
gradle :Molecule:runClient
```

`UIGalleryVisualTest` renders the production gallery with mocked game services and item
data. Its Java2D previews appear in `Molecule/build/screenshots/gallery/` after
`gradle :Molecule:test`. Fonts, textures, and items are diagnostic approximations.

The demo depends on the root Compound project. It has no Maven publishing plugin;
Jenkins publishes Compound libraries only.

## License

MIT. See [LICENSE](LICENSE).
