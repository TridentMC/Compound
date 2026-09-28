param([Parameter(Mandatory)][string]$WikiDirectory)

$ErrorActionPreference = 'Stop'
$wikiRoot = (Resolve-Path -LiteralPath $WikiDirectory).Path
$remote = git -C $wikiRoot remote get-url origin
if ($LASTEXITCODE -ne 0 -or $remote -notmatch '^https://github\.com/TridentMC/Compound\.wiki(?:\.git)?/?$') {
    throw 'The destination must be a checkout of the Compound wiki.'
}

$sourceRoot = Join-Path $PSScriptRoot '../docs/wiki'
foreach ($page in @('Modules-UI.md', 'Composable-UI.md', 'Legacy-UI.md')) {
    Copy-Item -LiteralPath (Join-Path $sourceRoot $page) -Destination (Join-Path $wikiRoot $page)
}
