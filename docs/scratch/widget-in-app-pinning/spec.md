# In-app Player widget pinning

Status: Implemented.

## Confirmed decisions

- Offer an in-app action to add the existing **Player widget** to the home screen with its current behavior.
- Do not introduce widget customization in this flow.
- If the launcher cannot accept the pin request, show an error message. Do not hide the action or show manual setup instructions.
- Add a new **Widget** entry in Settings that opens a dedicated Widget screen containing the **Add widget** action.
- Allow adding when no media is selected and when another Player widget already exists.
- Show **Current playback** in the pin confirmation preview when media is selected, including paused media; otherwise use the existing sample **Player widget preview**.
- Present request errors in a snackbar. Do not show an additional success message; cancellation is silent.

## Existing behavior

- **Player widget** and **Player widget preview** use the definitions in the root `CONTEXT.md`.
- The widget follows **Current playback** and has an empty state when no media is selected.
- The widget has no configuration activity; its preview uses curated sample content.

## Platform constraints

- Sending a pin request does not establish that the widget was added.
- The success callback runs only when the widget is successfully pinned; cancellation has no callback.

Reference: [Pin Glance widgets in-app](https://developer.android.com/develop/ui/compose/glance/pin-in-app).
