# core/font

## Overview

Bundle the typefaces named in `DESIGN.md` and make them usable on both platforms. The module holds the static font files and the code that registers them (iOS) or exposes them as `FontFamily` values (Android). It depends on no other module and does not use third-party libraries.

Only the Latin typefaces below are bundled. Japanese text has no glyphs in them and falls back to the system typeface (Hiragino Sans on iOS, Noto Sans CJK on Android). A family or weight that is not bundled also falls back to the system typeface.

## Public API

| Name | Behavior |
| --- | --- |
| `FontRegistry.registerAll()` (iOS) | Registers every bundled font file for the process. Idempotent: calling it again, or registering a file that is already registered, is not an error. Call it once at startup, before the first view is built |
| `FontRegistry.postScriptName(family:weight:)` (iOS) | Returns the PostScript name of the bundled face for a family name from `DESIGN.md` (for example `Inter Tight`) and a weight (100–900), or nil when that face is not bundled |
| `FontRegistry.isAvailable(postScriptName:)` (iOS) | True when the face is registered and `UIFont(name:size:)` can create it |
| `FontFamilies.interTight` / `FontFamilies.jetBrainsMono` (Android) | `FontFamily` values with the bundled weights |
| `FontFamilies.lookup(name)` (Android) | Returns the `FontFamily` for a family name from `DESIGN.md`, or null when the family is not bundled |

Callers pass the family name that `DESIGN.md` uses (`Inter Tight`, `JetBrains Mono`). Callers must use the system typeface when the lookup returns nil / null. Do not hard-code a PostScript name or resource name outside this module.

## Typefaces

| Family | Weights | Files | License |
| --- | --- | --- | --- |
| Inter Tight | 400, 500, 700 | `InterTight-Regular.ttf`, `InterTight-Medium.ttf`, `InterTight-Bold.ttf` | SIL OFL 1.1 |
| JetBrains Mono | 500 | `JetBrainsMono-Medium.ttf` | SIL OFL 1.1 |

The files are static instances of the variable fonts distributed by Google Fonts. The license text of each family is in `licenses/` (`OFL-InterTight.txt`, `OFL-JetBrainsMono.txt`) and must stay with the fonts.

### Source and checksums

- Upstream: `https://github.com/google/fonts` at commit `5bb28733cec313b63fbd415b574b5ae35c8aa8b8`, under `ofl/intertight/` and `ofl/jetbrainsmono/`.
- Variable sources (`InterTight[wght].ttf`, `JetBrainsMono[wght].ttf`):

| Source file | SHA-256 |
| --- | --- |
| `ofl/intertight/InterTight[wght].ttf` | `b81b73dcb64df3c230cabade7df6c5773bf863233f24c9ee51087519f1f88b6f` |
| `ofl/jetbrainsmono/JetBrainsMono[wght].ttf` | `48715a42ec242c21e9f02692891e147d022299a52e48d5e413e1a942193ffeda` |

- Static instances were cut with fonttools 4.68.0 (`fontTools.varLib.instancer.instantiateVariableFont(font, {"wght": <weight>}, updateFontNames=False)`), then the name table (family, subfamily, full name, PostScript name) and `OS/2.usWeightClass` were set to the static values.
- Bundled files. The iOS copy (`ios/Sources/Core/Font/Resources/`) and the Android copy (`android/src/main/res/font/`, lower-case names with underscores) must be byte-identical. `tools/tests/font-files.test.ts` checks both copies against these hashes:

| Bundled file | Android resource | SHA-256 |
| --- | --- | --- |
| `InterTight-Regular.ttf` | `inter_tight_regular` | `d5f985c4dfe74aac978f0ac5d6e38e2a283cfe0a1a9b046aa7ba184328d4aeea` |
| `InterTight-Medium.ttf` | `inter_tight_medium` | `b393b51a7af9f3aa08ae6e26ddd5caa2b5921e6cbda3d2b1aca94dd71553cffe` |
| `InterTight-Bold.ttf` | `inter_tight_bold` | `31c69413b41cb225a67c6d8b42901507d60ef7cad898a0bce2969e0ac39eb77f` |
| `JetBrainsMono-Medium.ttf` | `jetbrains_mono_medium` | `0c65faf82f548c72652e95a1db980e60f4c0777c70260b9a16dbaaf224974320` |

| Face | PostScript name |
| --- | --- |
| Inter Tight 400 / 500 / 700 | `InterTight-Regular` / `InterTight-Medium` / `InterTight-Bold` |
| JetBrains Mono 500 | `JetBrainsMono-Medium` |

## Platform differences

- iOS registers the files at startup with CoreText (`CTFontManagerRegisterFontsForURL`, process scope) from the SwiftPM resource bundle. The app layer calls `FontRegistry.registerAll()` before the first view. Without registration, `postScriptName` still returns the name, so callers should check `isAvailable` when they need to know.
- Android needs no registration. The files are `res/font` resources and `FontFamilies` builds the `FontFamily` values from them. A glyph that a bundled font lacks (such as Japanese) falls back to the system typeface automatically.
- Weights that are not bundled: iOS returns nil and callers use `Font.system`; Android's `FontFamily` selects the nearest bundled weight, so callers that need exact fallback should check `lookup` and the token's weight.

## Scenarios

None. The module has no user-visible behavior of its own; its tests check that the bundled faces are registered and that unknown families return nil / null.
