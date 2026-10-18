# Player widget chapter controls in the playback row

Status: Implemented; validation pending.

This revises the control placement in [Responsive player widget layouts](../widget-responsive-layouts/spec.md). Its existing implementation outcome remains the record of the prior layout.

## Agreed presentation

- Keep the widget picker's default size at 4×1.
- At 4×2 and larger, when Current playback has at least two chapters, show one centered control row in this order: previous chapter, rewind, play/pause, forward, next chapter.
- At 4×2 and larger with fewer than two chapters, hide both chapter buttons and center the remaining three playback controls.
- Remove the separate chapter control row from the larger widget presentation.
- Preserve the existing cover, title, author or podcast name, and chapter text for each available layout.
- Select layouts by available dimensions rather than assuming a fixed dp size for a 4×2 grid span.
- Fit the five controls on one row at the widget's supported narrow width by reducing their size only as much as needed. Keep larger controls where width allows and retain play/pause as the visually dominant action.
- Preserve the current compact fallback when large system text leaves insufficient height. Show chapter navigation again when enough height is available.

## Agreed interactions

- Preserve existing playback and chapter actions, including disabled previous and next buttons at chapter boundaries.
- Chapter navigation remains available for books and podcast episodes with at least two chapters.
- Playback buttons act without opening ShelfDroid; cover and title continue opening the expanded player.

## Acceptance checks

- Confirm the picker still suggests 4×1 and the 4×1 controls stay compact.
- At representative 4×2, 4×3, and taller sizes, confirm the five controls form one row in the agreed order.
- Confirm the row fits at the supported narrow width, with no clipped buttons.
- Confirm three centered controls when chapter navigation is unavailable.
- Check first and last chapter disabled states, podcast episodes with chapters, and larger system text.

## Implementation outcome

- Medium and large layouts now add previous and next chapter actions to the playback control row when chapter navigation is available. The compact layout keeps its existing three controls.
- Control buttons remain 48dp where space allows and shrink to fit five controls at the widget's 220dp minimum width.
- Responsive layout assertions now expect chapter actions in the medium layout, including its larger-font minimum height.
- Tests have not been run for this change.

These reversible layout choices use the existing **Player widget**, **Current playback**, and **Chapter** terms in `CONTEXT.md` and do not require an ADR.
