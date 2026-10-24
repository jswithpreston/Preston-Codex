---
name: Warm Editorial Scholar
colors:
  surface: '#fcf9f4'
  surface-dim: '#dcdad5'
  surface-bright: '#fcf9f4'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f6f3ee'
  surface-container: '#f0ede9'
  surface-container-high: '#ebe8e3'
  surface-container-highest: '#e5e2dd'
  on-surface: '#1c1c19'
  on-surface-variant: '#45464d'
  inverse-surface: '#31302d'
  inverse-on-surface: '#f3f0eb'
  outline: '#75777e'
  outline-variant: '#c5c6ce'
  surface-tint: '#525e7d'
  primary: '#000a24'
  on-primary: '#ffffff'
  primary-container: '#14213d'
  on-primary-container: '#7c89aa'
  inverse-primary: '#b9c6ea'
  secondary: '#835500'
  on-secondary: '#ffffff'
  secondary-container: '#feb956'
  on-secondary-container: '#734a00'
  tertiary: '#000f07'
  on-tertiary: '#ffffff'
  tertiary-container: '#002819'
  on-tertiary-container: '#599578'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d9e2ff'
  primary-fixed-dim: '#b9c6ea'
  on-primary-fixed: '#0d1b36'
  on-primary-fixed-variant: '#3a4664'
  secondary-fixed: '#ffddb4'
  secondary-fixed-dim: '#feb956'
  on-secondary-fixed: '#291800'
  on-secondary-fixed-variant: '#633f00'
  tertiary-fixed: '#b1f0ce'
  tertiary-fixed-dim: '#95d4b3'
  on-tertiary-fixed: '#002114'
  on-tertiary-fixed-variant: '#0e5138'
  background: '#fcf9f4'
  on-background: '#1c1c19'
  surface-variant: '#e5e2dd'
typography:
  display-lg:
    fontFamily: Source Serif 4
    fontSize: 34px
    fontWeight: '600'
    lineHeight: 42px
    letterSpacing: -0.02em
  display-lg-mobile:
    fontFamily: Source Serif 4
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
    letterSpacing: -0.01em
  headline-lg:
    fontFamily: Source Serif 4
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Source Serif 4
    fontSize: 20px
    fontWeight: '500'
    lineHeight: 28px
  headline-sm:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: 0.01em
  body-lg:
    fontFamily: Source Serif 4
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
    letterSpacing: 0.01em
  body-md:
    fontFamily: Source Serif 4
    fontSize: 15px
    fontWeight: '400'
    lineHeight: 24px
  body-sm:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.04em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-mobile: 0.75rem
  margin: 1.25rem
  margin-mobile: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system embodies the intimacy of an archival clothbound notebook merged with the disciplined precision of modern Android engineering. It is crafted for discerning thinkers, bibliophiles, and focused practitioners who value contemplative reading alongside intentional daily execution.

The aesthetic fuses **Editorial Minimalism** with **Material Design 3 tactile structure**:
- **Tactile Paper Quality:** Interfaces feel like tactile parchment, reducing digital eye strain and encouraging lingering reflection.
- **Academic Serenity:** Visual hierarchy mirrors classical book layout—prioritizing comfortable line measures, generous margins, and distinguished serif treatments for curated thoughts.
- **Purposeful Utility:** Functional tools, metadata chips, and task counters remain unobtrusive, executing actions with crisp micro-interactions and rigorous 48dp minimum touch ergonomics.

## Colors

The palette establishes an organic reading environment through warm, paper-derived foundations anchored by deep bibliographic ink.

- **Primary (`#14213D` - Archival Deep Ink):** Used for critical primary text, top-level headings, dominant button containers, and selected state indicators. Evokes vintage printing ink on heavy stock.
- **Secondary (`#E09F3E` - Warm Bookplate Amber):** Reserved for focal points: bookmark markers, spaced repetition reviews due today, floating action buttons, and active star ratings. Used with restraint to maintain its illuminating power.
- **Tertiary (`#2D6A4F` - Botanical Pine / `#40916C`):** Denotes quiet resolution, completed study loops, memory mastery milestones, and new harvest tags.
- **Error & Urgency (`#B83A24` - Terracotta Sanguine / `#D90429`):** Reserved for lapsed reviews, uncompleted critical tasks, or destructive actions, referencing editorial red-pencil notations.
- **Neutral Surface Palette:**
  - Base Canvas (`#FAF7F2`): Unbleached woven paper background.
  - Surface Raised (`#FFFFFF`): Crisp card and modal surfaces.
  - Surface Border / Hairline (`#EBE5DF`): 1px structural partitions.
  - Ink Subdued (`#7A7571` & `#4A4541`): Metadata, footnotes, timestamps, and secondary labels.

## Typography

The type system creates an intentional dual voice:
1. **The Literary Voice (`Source Serif 4`):** Governs quote extractions, synthesized reflections, book titles, and editorial headings. It brings typographic rhythm, classic bracketed serifs, and high legibility during sustained reading sessions.
2. **The Systematic Voice (`Inter`):** Governs navigational elements, dates, tags, intervals, input mechanics, and progress metrics. Neutral, mathematically consistent, and clean, it avoids distracting from literary content.

### Formatting Rules
- **Quotations:** Always render in `body-lg` italic with an indented left gutter border tinted in `#E09F3E`.
- **Book Metadata (Author, Page, ISBN):** Render in `label-sm` or `label-md` uppercase with `0.04em` tracking to evoke catalog taxonomy.
- **Numbers & Metrics:** Use tabular numbers in `Inter` for spaced-repetition schedules and dates.

## Layout & Spacing

Designed for the standard portrait Android canvas (360x800dp), the layout relies on a 4-column mobile fluid grid adhering to an 8dp baseline grid (with 4dp sub-steps for precise typography alignment).

- **Screen Margins:** Fixed at `16dp` (`1rem`) on compact mobile displays to maximize horizontal reading room while maintaining clean, paper-like edges.
- **Editorial Breathing Room:** Cards containing insights or quote cards require `space-md` (16dp) to `space-lg` (24dp) internal padding to preserve an open, uncluttered cadence.
- **Vertical Rhythm:** Group related sections using `space-sm` (8dp), separate distinct card modules with `space-md` (16dp), and divide screen milestones or daily chapters using `space-xl` (32dp).

## Elevation & Depth

Visual hierarchy does not rely on heavy synthetic drop-shadows; instead, it uses **tonal paper layering** complemented by **hairline framing**:

- **Level 0 (Canvas):** The base background `#FAF7F2` sits flat and unshadowed.
- **Level 1 (Card & Section Surfaces):** Elevated on pure `#FFFFFF` paper stock, bounded by a crisp `1px solid #EBE5DF` border. Elevation is augmented by a subtle, warm-tinted ambient veil: `0px 2px 6px rgba(20, 33, 61, 0.04)`.
- **Level 2 (Modals, Active Cards & Bottom Sheets):** Rendered on `#FFFFFF` with `1px solid #EBE5DF` and an ambient reading shadow: `0px 8px 24px rgba(20, 33, 61, 0.08)`.
- **Floating Action Bar & Buttons:** Anchored via an Amber or Deep Ink surface with `0px 4px 16px rgba(20, 33, 61, 0.12)`.

## Shapes

The geometric signature balances tactile warmth with contemporary mobile architecture:

- **Primary Cards & Modals:** Standardized to a signature `16dp` radius, softening the viewport while establishing an approachable, book-like panel aesthetic.
- **Interactive Controls (Inputs, Buttons):** Styled with `8dp` to `12dp` corner curvature for tactile feedback and clear affordance.
- **Chips & Status Pills:** Full pill-shaped rounding (`999dp`) to contrast against the structured rectilinearity of journal passages and quote excerpts.

## Components

### 1. Cards (Quote, Thought, and Daily Agenda)
- **Geometry:** 16dp corner radius, white background (`#FFFFFF`), outlined with a 1px hairline border (`#EBE5DF`).
- **Quote Variant:** Incorporates a 3dp left vertical accent line in Bookplate Amber (`#E09F3E`) with `Source Serif 4` italic typography, citation footer in `Inter` label style, and an optional quick-spaced-repetition trigger badge.
- **Padding:** 16dp horizontal and vertical padding (expandable to 20dp on larger cards).

### 2. Buttons & Actions
- **Touch Target:** Absolute minimum bounding box of 48x48dp for all interactive elements.
- **Primary Action (New Thought / Quick Harvest):** Solid Deep Ink (`#14213D`) fill with crisp white text (`#FAF7F2`), 12dp border radius, 48dp height.
- **Accent Action (Review Due / Commit):** Solid Amber (`#E09F3E`) fill with `#14213D` ink text for prominent visibility.
- **Secondary / Ghost Action:** 1px hairline border (`#EBE5DF`), transparent background, `#14213D` text.

### 3. Chip Tags & Taxonomy Indicators
- **Dimensions:** 32dp height, fully pill-rounded (999dp).
- **Default / Topic Chip:** Warm surface `#FAF7F2` fill, 1px border `#EBE5DF`, `#4A4541` text in `Inter` `label-md`.
- **State Chips:**
  - *Mastered / Done:* Soft sage background (`rgba(45, 106, 79, 0.12)`), `#2D6A4F` text.
  - *Due for Review:* Soft amber background (`rgba(224, 159, 62, 0.15)`), `#9C630D` text.

### 4. Input Fields (Reflection & Quote Capture)
- **Geometry:** 12dp radius container, minimum 52dp height for single-line; expansive auto-grow field for journal entries.
- **Styling:** `#FFFFFF` background, inset padding of 16dp, 1px outline in `#EBE5DF`. Focus state transforms border to 1.5px `#14213D` without aggressive outer rings.

### 5. Checkboxes & Task Mechanics
- **Checkboxes:** 22x22dp square with 6dp corner radius; inactive state has 1.5px `#7A7571` border. Active state transitions to `#2D6A4F` fill with an ivory tick.
- **Strikethrough Treatment:** Completed tasks fade to `#7A7571` with subtle text-decoration line, keeping daily journals legible without visual clutter.

### 6. Material Design 3 Bottom Navigation Bar
- **Architecture:** Fixed 80dp height docking 5 key destinations (Today, Library, Review, Insights, Settings).
- **Surface:** `#FAF7F2` with a 1px border-top (`#EBE5DF`) and no heavy drop shadow.
- **Active State Indicator:** 32dp high pill highlight (`#EBE5DF` or subtle amber tint) framing the active vector icon, paired with a distinct `label-sm` in `#14213D`.