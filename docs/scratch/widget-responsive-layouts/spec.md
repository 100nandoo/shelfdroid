# Responsive player widget layouts

Status: Implemented.

## Agreed presentation

- 4×1: retain the compact cover, title, author, and rewind / play-pause / forward controls.
- 4×2: larger cover beside the text, a title of up to two lines, author, and current chapter when available. Place rewind / play-pause / forward on a separate row.
- 4×3 and taller: cover above the text at standard font scale when there is enough height, with playback controls below. Add previous chapter / next chapter below the main playback controls. At constrained large heights and larger system font scales, place the cover beside metadata to preserve vertical room for both control rows. Do not add playback speed or sleep timer controls.
- Do not show a progress bar or time remaining in the taller layouts.
- Podcast episodes show episode title and podcast name; show a chapter row only when chapter information is available.
- Wider widgets give title and chapter text more space rather than adding controls.
- Beyond the large layout, increase cover size up to a sensible limit and add breathing room; do not reveal further information or controls.
- The empty state says “Choose something to play” with an “Open ShelfDroid” action, centered in the available space. Taller empty widgets add spacing without placeholder metadata or chapter controls.
- Keep the existing colors and typography, with play-pause visually dominant.
- Select compact, medium, and large layouts from actual available width and height; grid spans describe the intended presentations, not universal device-independent dimensions.
- Use available vertical space and the configured font scale to decide when title and chapter text can use a second line; keep the single-line fallback when space is tight.
- At the largest font scales, keep the large layout's chapter controls and arrange author and chapter metadata together to preserve vertical room; increase the medium minimum height when its metadata and controls would not fit.
- Require sufficient width and height for each layout; fall back to a smaller layout when space is constrained to preserve readable text and comfortable 48dp button targets.
- Use three distinct layouts with flexible artwork and spacing inside each. Keep text and button sizes stable across resizing.
- Expanded layouts allow up to two lines each for title and chapter, and one line for author or podcast name. Ellipsize overflow and omit missing metadata without placeholder text.
- Determine exact layout breakpoints by verifying that the content fits without clipping, including long text and larger font settings.

## Agreed interactions

- Previous chapter jumps directly to the adjacent previous chapter; it does not restart the current chapter first.
- Chapter navigation preserves whether playback is paused or playing.
- Disable previous chapter at the first chapter and next chapter at the last chapter.
- Hide both chapter buttons when fewer than two chapters are available. Support chapter navigation for both books and podcast episodes with chapters.
- Tapping the cover or title opens the expanded player for Current playback.
- Playback buttons perform their actions without opening ShelfDroid.

## Implementation validation

- Verify compact, medium, and large layouts at 320×72dp, 320×116dp, and 320×176dp, intermediate sizes, and tall narrow sizes. At 2× font scale, check medium just below and at its adjusted minimum height.
- Check long titles and chapter names, missing metadata, and larger font settings.
- Check books and podcast episodes with zero, one, and multiple chapters, including first and last chapter states.
- Verify that chapter navigation preserves paused or playing state and cover/title taps open the expanded player.
- Verify the empty state and existing compact playback actions.

## Implementation outcome

- At standard font scale, use compact below 116dp or below 220dp width; use medium from 116dp through 175dp; use large from 176dp upward when width is at least 220dp. Raise the medium minimum by 48dp per 1× increase above standard font scale.
- Large widgets use a cover beside metadata below 302dp height or above standard font scale; at standard font scale and 302dp or taller, the cover sits above metadata. This keeps art size and available text lines stable as the widget grows. Playback and chapter controls use 48dp targets.
- Glance unit tests verify content and click actions below and at the 116dp and 176dp layout boundaries, plus 320×128dp and 320×180dp, with larger-font boundary cases and first/last chapter states. Player data tests cover podcast chapter metadata and navigation visibility.
- Playback state keeps the library item name available alongside the current chapter or episode title. Podcast audio-file chapters populate widget chapter metadata, and widget navigation seeks to an adjacent episode chapter while preserving playback state.
- Media tests verify podcast chapter seek targets, first-chapter boundaries, and that chapter actions do not toggle play or pause.
- The Glance tests inspect the emitted content/action tree; they do not render pixels for screenshot-based clipping checks.
- `./gradlew test` passes.

## Existing behavior relevant to the design

- The current widget is one horizontal row, with a cover capped at 48dp and a second title line allowed from 80dp height.
- The expanded player can restart the current chapter when previous is pressed; media notification chapter actions navigate directly to adjacent chapters.
- Notification chapter controls currently exclude podcasts and books with fewer than two chapters.
- Current active widget artwork and metadata do not launch the player; the empty widget launches ShelfDroid.

The Chapter definition in CONTEXT.md covers both books and podcast episodes. These reversible presentation choices do not require an ADR.
