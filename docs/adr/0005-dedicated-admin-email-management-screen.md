# Dedicated Admin Email Management Screen

ShelfDroid will implement Audiobookshelf email management as a dedicated admin screen under the `Misc` admin route cluster instead of folding it into `Settings` or `Server settings`. The upstream product and API already treat SMTP settings and shared e-reader devices as a separate `/api/emails/*` surface, and keeping the feature isolated preserves the upstream save-versus-test semantics and keeps the replace-all device-management flow away from unrelated server settings.
