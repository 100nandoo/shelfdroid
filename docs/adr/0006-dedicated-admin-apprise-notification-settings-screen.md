# Dedicated Admin Apprise Notification Settings Screen

ShelfDroid will implement **Apprise notification settings** as a dedicated admin screen under the `Misc` admin route cluster instead of folding the feature into the current local notification settings screen. The upstream product and API already treat Apprise automation as a separate `/api/notifications` surface, and keeping it isolated avoids mixing server-side **Notification rules** with local Android sleep-timer preferences while preserving upstream admin-only behavior and rule-management workflows.
