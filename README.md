# 📚 ShelfDroid

ShelfDroid is a third-party Android client
for [Audiobookshelf](https://github.com/advplyr/audiobookshelf), designed to
provide seamless access to your audiobook collection. Stream, organize, and enjoy your audiobooks
with an intuitive
interface built specifically for Android.

---

## 📸 Screenshots

<!-- Add screenshots of your app here -->

---

## 🛠 Getting Started

### Prerequisites

- An active [Audiobookshelf](https://github.com/advplyr/audiobookshelf) server instance.
- Android device running version 7.0 (Nougat) or higher.

## 🎨 Code Style & Formatting

This project uses [ktfmt](https://github.com/facebook/ktfmt) for Kotlin code formatting, applying
Google’s Kotlin style
guide.

To format all Kotlin files in the project, run:

```bash
find . \( -path './.idea' -o -path './build' \) -prune -o \
  -name '*.kt' -print | xargs ktfmt --google-style
```

**Why .idea and build are ignored**

* **.idea/**

  Contains Android Studio / IntelliJ project metadata. These files are IDE-generated, frequently
  modified automatically, and should not be manually formatted.
* **build/**

  Contains generated build outputs and intermediate files. Formatting these files is unnecessary and
  may introduce unwanted changes or slow down the formatting process.

By excluding these directories, ktfmt only formats actual source code, keeping formatting fast,
safe, and focused on files that matter.

Maintaining a consistent style across the codebase helps improve readability and reduce noise in
pull requests.

---

## 🏗 Architecture

ShelfDroid follows
the [Android Architecture Templates (Multi-Module)](https://github.com/android/architecture-templates/tree/multimodule)
to ensure a scalable and maintainable codebase.

---

## 📅 Roadmap

- [x] Implement core audiobook streaming functionality.
- [x] Add offline downloading and playback.
- [ ] Improve search and filtering features.
- [ ] Introduce custom themes for personalization.
- [ ] Add in-app settings for customization.
- [ ] Integrate Google Assistant for voice control.
- [x] Enhance playback controls with bookmarks and sleep timers.
- [x] Develop a modern and user-friendly UI.
- [x] Support audiobook chapters for easy navigation.

Feel free to check out the [Issues](https://github.com/100nandoo/shelfdroid/issues) section to see
what’s in progress or
suggest new features!

---

## 🤝 Contributing

Contributions are welcome! If you'd like to contribute, please follow these steps:

1. Fork the repository.
2. Create a new branch:
    ```bash
    git checkout -b feature/YourFeatureName
    ```
3. Make your changes and commit them using [Commitizen](https://github.com/commitizen/cz-cli):
    ```bash
    cz c
    ```
   This will guide you through creating a compliant commit message.
4. Push your branch:
    ```bash
    git push origin feature/YourFeatureName
    ```
5. Open a Pull Request.

## 📖 Documentation

For more details on using and extending this project, check out the [Documentation](docs/DOCS.md).

---

## 🙌 Acknowledgements

- [Audiobookshelf](https://github.com/advplyr/audiobookshelf) – The powerful server that ShelfDroid
  connects to.

---

## 📬 Contact

**Fernando Fransisco Halim**  
GitHub: [100nandoo](https://github.com/100nandoo)  
Project Link: [ShelfDroid Repository](https://github.com/100nandoo/shelfdroid)

