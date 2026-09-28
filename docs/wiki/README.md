# Wiki sources

These pages are reviewed with the code. `Modules-UI.md` selects the API version, `Composable-UI.md` documents 2.0, and `Legacy-UI.md` preserves the previous guide.

GitHub stores wiki pages in a separate repository, so merging a code PR does not publish wiki changes automatically. Publish the reviewed pages using an authenticated Git checkout:

```powershell
git clone https://github.com/TridentMC/Compound.wiki.git ../Compound.wiki
./scripts/update-wiki.ps1 -WikiDirectory ../Compound.wiki
git -C ../Compound.wiki diff
git -C ../Compound.wiki add Modules-UI.md Composable-UI.md Legacy-UI.md
git -C ../Compound.wiki commit -m "Update the UI guides"
git -C ../Compound.wiki push
```

The script copies only these three pages and preserves the other modules. The 2.0 guide links to PR #4 for its merge status, so it can also be published while the framework is under review.
