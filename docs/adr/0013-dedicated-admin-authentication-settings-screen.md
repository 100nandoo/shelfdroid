# Dedicated Admin Authentication Settings Screen

ShelfDroid will implement **Authentication settings** as a dedicated admin/root screen under the
`Misc` Server group, backed by Audiobookshelf's `/api/auth-settings` boundary instead of folding
the feature into broader **Server settings** or User management. Updates will use changed-fields
partial patches, and ShelfDroid will reduce the loaded OpenID client secret to
configured/not-configured rather than retaining or redisplaying it; this preserves the upstream
security boundary and reduces accidental lockout or credential overwrite risk.
