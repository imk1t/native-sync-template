---
name: Return Native
colors:
  background: "#F3F1EC"
  background-dark: "#0F1115"
  line: "#E3DFD6"
  line-dark: "#2B2F37"
  muted: "#5E636B"
  muted-dark: "#A9AEB7"
  onBackground: "#14161A"
  onBackground-dark: "#F3F1EC"
  primary: "#3D4DFF"
  primary-dark: "#8C98FF"
  surface: "#FFFFFF"
  surface-dark: "#1B1E24"
typography:
  h1:
    fontFamily: Inter Tight
    fontSize: 40px
    fontWeight: 700
  h2:
    fontFamily: Inter Tight
    fontSize: 34px
    fontWeight: 700
  h3:
    fontFamily: Inter Tight
    fontSize: 28px
    fontWeight: 700
  body:
    fontFamily: Inter Tight
    fontSize: 15px
    fontWeight: 400
  caption:
    fontFamily: Inter Tight
    fontSize: 13px
    fontWeight: 500
  mono:
    fontFamily: JetBrains Mono
    fontSize: 14px
    fontWeight: 500
rounded:
  avatar: 14px
  card: 22px
  chip: 22px
  hero: 28px
  pill: 34px
spacing:
  small: 8px
  medium: 16px
  large: 24px
---

# Return Native design system

This file is the source of truth for design tokens. `bun tools/generate.ts` turns the front matter into `Tokens` for iOS (Swift) and Android (Kotlin) in `shared/designsystem`; never edit the generated files. The format follows [DESIGN.md](https://stitch.withgoogle.com/docs/design-md/specification); the spec has no theme modes, so each color's dark value is a second entry named `<name>-dark` (for example `primary` and `primary-dark`), and every color needs both.

## Overview

A paper-colored background, dark ink, and one indigo accent. Content sits on white cards with a thin line, so rows read as separate objects. The same names and values are used on both platforms; OS-specific differences are documented in `modules/shared/designsystem/spec.md`.

## Colors

- `background` is the screen. `surface` is a card on it. `line` is the thin border of a card.
- `onBackground` is body text and `muted` is secondary text (counts, dates, captions).
- `primary` marks the selected tab, buttons, links, and a saved repository.
- Dark values follow the OS appearance. Do not hard-code a color in a screen; use `Tokens`.

## Typography

`h1` 40, `h2` 34, `h3` 28, `body` 15, and `caption` 13. Inter Tight with Noto Sans JP for Japanese text, and a monospaced face (`mono`, JetBrains Mono) for identifiers and numbers. iOS and Android may substitute the system face (SF, Roboto). Typography is documented here but not generated into `Tokens` yet.

## Layout

Spacing is `small` 8, `medium` 16, and `large` 24. Tappable elements are at least 44pt/dp.

## Elevation & Depth

Cards are flat: a `line` border, no shadow. The floating tab bar is the only translucent surface (see the OS differences in the designsystem spec).

## Shapes

`avatar` 14, `card` 22, `chip` 22, `hero` 28, `pill` 34.

## Components

`RepositoryRow`, `DesignCard`, `StatusBadge`, and the loading, empty, and error states live in `shared/designsystem`; see its `spec.md`.

## Do's and Don'ts

- Do add every color as a pair, `<name>` and `<name>-dark`; generation fails otherwise.
- Do change a token here and regenerate, then run `mise run check`.
- Don't hard-code colors, radii, or spacing in a feature module.
