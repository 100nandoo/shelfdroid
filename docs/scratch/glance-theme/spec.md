# Player widget theme

Status: Design decisions settled; implementation awaits shared-understanding confirmation.

## Agreed behavior

- Scope includes the player widget's light/dark appearance and typography.
- Honor ShelfDroid's dark-mode and dynamic-color preferences, including when the app's dark mode differs from Android's.
- Use a neutral themed surface, themed text, and an accent-colored play/pause button.
- Refresh every placed player widget when either appearance preference changes, including while playback is idle.

## Agreed typography

ShelfDroid's mini player uses Lora for its title and Inter for its author. Native Glance text supports system font families but not bundled custom fonts.

Use a system serif title at 16 sp with medium weight, and a system sans-serif author at 12 sp with normal weight, following the mini player's text roles where supported. The user accepted these substitutes for the bundled fonts.

## References

- [Implement a Glance theme](https://developer.android.com/develop/ui/compose/glance/theme)
- [Glance text support](https://developer.android.com/develop/ui/compose/glance/build-ui#handle-text)

No new domain terms or architectural decisions requiring an ADR have been identified.
