# Contributing

## Requirements

- An Audiobookshelf server you can sign in to
- Android 10 or newer
- JDK 25 for local builds

## Workflow

1. Fork the repository.
2. Create a feature branch with `git checkout -b feature/YourFeatureName`.
3. Make your changes, run formatting, and test the affected code.
4. Commit your changes. If you use Commitizen, run `cz c`.
5. Push your branch and open a pull request.

## Code style and formatting

ShelfDroid uses [ktfmt 0.65](https://github.com/Kotlin/ktfmt) with Google's Kotlin style and the
project's `.editorconfig` settings. Run this command from the repository root:

```bash
find . -type d \( -name .git -o -name .idea -o -name build \) -prune -o \
  -type f \( -name '*.kt' -o -name '*.kts' \) \
  -exec ktfmt --google-style --enable-editorconfig {} +
```

The command formats Kotlin source files and Gradle Kotlin scripts, skipping `.git/`, `.idea/`,
and `build/` directories at every level.

To check formatting without changing files, add `--dry-run --set-exit-if-changed` after
`--enable-editorconfig`.
