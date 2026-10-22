# Large player widget cover growth

Status: Implemented.

This updates the large presentation in [Responsive player widget layouts](../widget-responsive-layouts/spec.md) and keeps the single control row from [Player widget chapter controls in the playback row](../widget-unified-control-row/spec.md).

## Agreed presentation

- Apply to every size that selects the large Player widget layout, using available dp dimensions rather than a fixed 4×3 grid span.
- Preserve the responsive cover placement: beside metadata at shorter heights or larger font scales, and above metadata when the current layout selects the stacked arrangement.
- Keep MainPlaybackControls in one 48dp-tall row at the bottom. Preserve its current button order and responsive button widths.
- Grow the square cover into the height remaining after widget padding, spacing, the control row, and readable metadata. In the side arrangement, leave room for text beside the cover. Keep the second headline line when it fits.
- Let available width limit cover growth. Keep the full artwork visible within its 1:1 frame rather than cropping it.

## Implementation outcome

- The large layout calculates cover size from available width and height instead of the former 96dp cap.
- The control row has a fixed 48dp height, with flexible space above it to keep it bottom aligned.
- The cover image request uses the displayed cover size so enlarged artwork is not decoded from a fixed 48dp request.
- Layout checks cover the side-to-stacked threshold, a short large widget, a tall stacked widget, and a narrow widget.
- The focused widget layout and responsive layout unit tests pass.

This reversible presentation choice uses the existing **Player widget** term in `CONTEXT.md` and does not require an ADR.
