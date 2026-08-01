# Ravengard Extras — v1.0

Client-side only Fabric mod for Minecraft 26.2. A small QoL toolkit — more
features slot into the same menu over time. Nothing here touches the server:
each feature only reads client-visible data (like item lore) and draws to
your own screen. No packets sent, no NBT/item mutation, no automation —
should be within Hypixel's rules for cosmetic/client-only QoL mods, but
you're the one playing there, so use your judgement.

Run `/ravengardextras` in-game to open the menu.

## Features

### Gear Highlighter

Outlines item slots whose lore contains a "N Crowns" line above a threshold
you set, in one of three colors (three tiers), plus an animated rainbow
option.

- Hooks `AbstractContainerScreen.extractSlot` (client rendering only) and
  checks each visible item's lore for a line matching `N Crowns` (handles
  commas and k/m suffixes).
- Highest tier whose threshold is met wins: tier 3 > tier 2 > tier 1.
- Below all three thresholds → no outline.

**GUI:** `/ravengardextras` → "Gear Highlighter" — toggle, three threshold
fields, three color-swatch pickers (10 presets + rainbow), Save/Back. You
can also bind a key to jump straight to the menu: Options → Controls → Key
Binds → "Open Ravengard Extras menu" (unbound by default).

Config is saved to `.minecraft/config/ravengardextras.json` after every
change. Defaults: tier1 = 50 crowns (green), tier2 = 200 crowns (red),
tier3 = 1000 crowns (gold).

## Install

Drop `dist/ravengardextras-1.0.0.jar` into your Fabric profile's `mods`
folder (e.g. your Modrinth "raven" profile's `mods` directory), alongside
Fabric API 0.155.2+26.2 or newer. If you had the old standalone
`crownoutline-1.0.0.jar` installed, delete it first — this replaces it.
Requires:

- Minecraft 26.2
- Fabric Loader ≥ 0.19.3
- Fabric API (any 26.2-compatible build)
- Java ≥ 25

## Rebuild from source

```
./gradlew build
```

Output lands in `build/libs/ravengardextras-1.0.0.jar`.

The `.gradle/` and `build/` folders in this directory are the real Fabric
dev environment (downloaded Minecraft jars, decompiled sources, etc.) — safe
to delete to reclaim space; `./gradlew build` regenerates them.

## Adding a new feature

Each feature gets its own package under `com.ravengardextras.<feature>`
(see `gearhighlighter/` for the pattern: a config class, a screen, and
whatever mixin/logic it needs). Add a button for it in
`RavengardExtrasMenuScreen.init()`.
