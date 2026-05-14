# Compose Preview Policy

This repo treats Compose previews as a first-class UI development tool.

## Scope

Preview coverage is required for:

- Public presentational composables
- Screen-content composables that render meaningful UI state
- Receiver-scoped composables that render reusable UI

Preview coverage is not required for:

- `ViewModel` entrypoints
- DI-bound composables
- Navigation glue
- Runtime orchestration composables with no unique presentation responsibility

Those excluded composables should still expose a previewable content seam beneath them when needed.

## Baseline Rule

Every eligible composable should have deterministic preview coverage for its meaningful UI states.

A single happy-path preview is not always enough. Add previews for materially different states when relevant, such as:

- Empty
- Error
- Loading
- Selected
- Disabled
- Expanded
- Long-text or overflow cases
- Dialog open vs closed
- Fallback rendering paths

## Preview Structure

- Keep previews in the same file as the composable by default
- Preview pure/content composables instead of root runtime wrappers
- Use sample state and no-op callbacks
- Prefer small local sample values for one-off cases
- Use shared fixtures from `core-ui/.../preview/Defaults.kt` when the sample data is reused across files

## Standard Wrappers

- Use `@ShelfDroidPreview` for the standard light/dark preview matrix
- Use `PreviewWrapper(dynamicColor = false)` as the default rendering wrapper
- Add dynamic-color-specific previews only when color behavior is the point of the preview
- Use `AnimatedPreviewWrapper` for composables that depend on shared transition locals or animated content locals
- Add small scoped harnesses for receiver-based composables such as `BoxScope` or `LazyGridItemScope` when needed

## Component-Specific Guidance

- Preview stateful widgets in each meaningful visual state
- Preview fallback branches explicitly when the component has alternate rendering paths
- For typography/helper files with many tiny wrappers, prefer one aggregate preview plus direct previews for stateful helpers
- Parent previews are enough for trivial child wrappers when the parent already renders the relevant state

## Enforcement

- New eligible composables should ship with preview coverage
- Review preview coverage at the composable level, not only the file level
- Treat missing previews as acceptable only for explicit exclusions listed above
