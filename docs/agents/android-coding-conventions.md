# Android Coding Conventions

## Jetpack Compose

- Bottom-align composable screen layouts, especially primary actions and important interactive UI,
  to keep them close to the user's thumb.
- Name each item composable rendered by a `LazyRow` or `LazyColumn` `<Feature>Item`; for example,
  name a tag-list item `TagItem`. Place each item composable in its own Kotlin file with a preview.
- Place Compose preview functions in the final section of the file, after production declarations
  and helpers.

## Repositories

- Give each repository class its own file; that file contains no other classes.
