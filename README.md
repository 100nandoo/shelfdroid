<h1 align="center">ShelfDroid</h1>
<p align="center">
  <img src=".idea/icon.svg" width="120" alt="ShelfDroid app icon" style="border-radius: 50%;">
</p>
<p align="center">
  <a href="https://github.com/100nandoo/shelfdroid/releases/latest">
    <img src="https://img.shields.io/github/v/release/100nandoo/shelfdroid?style=flat-square&color=blue" alt="latest release">
  </a>
  <a href="LICENSE">
    <img src="https://img.shields.io/github/license/100nandoo/shelfdroid?style=flat-square&color=blue" alt="license">
  </a>
  <a href="https://github.com/100nandoo/shelfdroid/releases">
    <img src="https://img.shields.io/github/downloads/100nandoo/shelfdroid/total?style=flat-square&color=brightgreen" alt="downloads">
  </a>
</p>

<p align="center">
  <a href="https://play.google.com/store/apps/details?id=dev.halim.shelfdroid">
    <img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" height="80" alt="Get it on Google Play">
  </a>
  <a href="https://f-droid.org/en/packages/dev.halim.shelfdroid/">
    <img src="https://fdroid.gitlab.io/artwork/badge/get-it-on-en.svg" height="80" alt="Get it on F-Droid">
  </a>
  <a href="https://github.com/100nandoo/shelfdroid/releases/latest">
    <img src="docs/images/badge_github.png" height="80" alt="Download latest APK from GitHub">
  </a>
</p>

ShelfDroid is a third-party Android client for self-hosted [Audiobookshelf](https://github.com/advplyr/audiobookshelf) servers. Use it to browse audiobook and podcast libraries, stream playback from your server, keep listening offline on your Android device, and handle common server management tasks from the same app.

## Features

- Browse audiobook and podcast libraries from your Audiobookshelf server
- Stream audiobooks and podcast episodes with synced progress
- Download books and episodes for durable offline playback
- Use chapters, bookmarks, sleep timer, playback speed, and player controls
- Manage backups, API keys, users, logs, and server settings when your account has permission
- Review listening sessions and other admin screens available to your account
- Customize sorting, display preferences, and playback settings

## Screenshots

  <p>
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/01-home-book.png" width="160" alt="Books home screen">
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/02-home-podcast.png" width="160" alt="Podcasts home screen">
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/03-book.png" width="160" alt="Book details screen">
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/04-podcast.png" width="160" alt="Podcast details screen">
  </p>
  <p>
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/05-episode.png" width="160" alt="Episode details screen">
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/06-player-book.png" width="160" alt="Book player screen">
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/07-player-podcast.png" width="160" alt="Podcast player screen">
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/08-settings.png" width="160" alt="Settings screen">
  </p>
  <p>
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/09-backups.png" width="160" alt="Backups screen">
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/10-server-settings.png" width="160" alt="Server settings screen">
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/11-edit-book.png" width="160" alt="Edit book screen">
  </p>

## Requirements

- An Audiobookshelf server you can sign in to
- Android 10 or newer
- JDK 17 for local builds

## Roadmap

- [x] Implement core audiobook streaming functionality
- [x] Add offline downloading and playback
- [ ] Improve search and filtering features
- [ ] Introduce custom themes for personalization
- [ ] Add in-app settings for customization
- [ ] Integrate Google Assistant for voice control
- [x] Enhance playback controls with bookmarks and sleep timers
- [x] Develop a modern and user-friendly UI
- [x] Support audiobook chapters for easy navigation

See the [issue tracker](https://github.com/100nandoo/shelfdroid/issues) for current work and feature requests.

## Documentation

Project documentation lives in [docs/DOCS.md](docs/DOCS.md), including code style and architecture notes. Recent release notes are tracked in [CHANGELOG.md](CHANGELOG.md).

## Acknowledgements

- [Audiobookshelf](https://github.com/advplyr/audiobookshelf) for the server platform ShelfDroid connects to

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=100nandoo/shelfdroid&type=Date)](https://star-history.com/#100nandoo/shelfdroid&Date)

## Contributing

1. Fork the repository.
2. Create a feature branch with `git checkout -b feature/YourFeatureName`.
3. Make your changes, run formatting, and test the affected code.
4. Commit your changes. If you use Commitizen, run `cz c`.
5. Push your branch and open a pull request.

## License

ShelfDroid is open source under the GNU Affero General Public License v3.0.

---

Copyright (c) 2026 100nandoo
