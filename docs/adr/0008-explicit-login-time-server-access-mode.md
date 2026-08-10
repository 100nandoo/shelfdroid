# Explicit login-time server access mode

ShelfDroid will treat login-time local-network access as an explicit user choice on `LoginScreen` instead of inferring it from DNS or IP heuristics. We are choosing this because the current bug needs a narrow fix on the login path, Android 17 local-network permission applies to the route to the current server rather than to a distinct auth API, and a visible `Server access` choice (`Internet` or `Local network`) is simpler, more predictable, and easier to reason about than automatic LAN detection for this first pass.
