# Responsive player widget layouts

Status: Implemented.

## Agreed presentation

- 4×1: retain the compact cover, title, author, and rewind / play-pause / forward controls.
- 4×2: larger cover beside the text, a title of up to two lines, author, and current chapter when available. Place rewind / play-pause / forward on a separate row.
- 4×3 and taller: cover above the text and controls. Add previous chapter / next chapter below the main playback controls. Do not add playback speed or sleep timer controls.
- Do not show a progress bar or time remaining in the taller layouts.
- Podcast episodes show episode title and podcast name; show a chapter row only when chapter information is available.
- Wider widgets give title and chapter text more space rather than adding controls.
- Beyond the large layout, increase cover size up to a sensible limit and add breathing room; do not reveal further information or controls.
- The empty state says “Choose something to play” with an “Open ShelfDroid” action, centered in the available space. Taller empty widgets add spacing without placeholder metadata or chapter controls.
- Keep the existing colors and typography, with play-pause visually dominant.
- Select compact, medium, and large layouts from actual available width and height; grid spans describe the intended presentations, not universal device-independent dimensions.
- Require sufficient width and height for each layout; fall back to a smaller layout when space is constrained to preserve readable text and comfortable button targets.
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

- Verify compact, medium, and large layouts, intermediate sizes, and tall narrow sizes.
- Check long titles and chapter names, missing metadata, and larger font settings.
- Check books and podcast episodes with zero, one, and multiple chapters, including first and last chapter states.
- Verify that chapter navigation preserves paused or playing state and cover/title taps open the expanded player.
- Verify the empty state and existing compact playback actions.

## Implementation outcome

- Use compact when available height is below 104dp or width is below 220dp; use medium from 104dp through 175dp; use large from 176dp upward when width is at least 220dp.
- Glance unit tests verify content and click actions at 320×72dp, 320×128dp, and 320×180dp, including first/last chapter states and the empty state. Player data tests cover podcast chapter metadata and navigation visibility.
- The Glance tests inspect the emitted content/action tree; they do not render pixels for screenshot-based clipping checks.
- `./gradlew test` passes.

## Existing behavior relevant to the design

- The current widget is one horizontal row, with a cover capped at 48dp and a second title line allowed from 80dp height.
- The expanded player can restart the current chapter when previous is pressed; media notification chapter actions navigate directly to adjacent chapters.
- Notification chapter controls currently exclude podcasts and books with fewer than two chapters.
- Current active widget artwork and metadata do not launch the player; the empty widget launches ShelfDroid.

No glossary change is needed so far: Player widget, Current playback, Chapter, and Seek interval already have definitions in CONTEXT.md. These reversible presentation choices do not require an ADR.
