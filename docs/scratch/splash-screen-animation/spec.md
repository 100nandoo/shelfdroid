# Splash screen waveform animation

Status: Implemented; debug build passes. Visual and runtime verification remains outstanding.

## Goal

Add a brief animation to ShelfDroid's existing waveform logo during app startup, similar in scope to Harmonic's animated splash icon. Preserve normal startup timing and finish on the current logo.

## Current setup

- `gradle/libs.versions.toml` declares `androidx.core:core-splashscreen:1.2.0`, already used by the app module.
- `app/src/main/res/values/splash.xml` defines `Theme.ShelfDroid.MySplash`, inheriting from `Theme.SplashScreen.IconBackground`.
- The theme uses the static `@drawable/ic_launcher_foreground`, the peach `@color/launcher` icon background (`#FFC981`), and `Theme.ShelfDroid` as its post-splash theme.
- `app/src/main/res/drawable/ic_launcher_foreground.xml` contains seven separate black waveform bars inside a scaled and translated group. There are no animation targets yet.
- `app/src/main/AndroidManifest.xml` applies the splash theme to `MainActivity` and `OpenIdCallbackActivity`.
- `OpenIdCallbackActivity` does not call `installSplashScreen()`. It processes the authentication callback, starts `MainActivity` with `NEW_TASK | CLEAR_TASK`, and finishes.
- `MainActivity.onCreate()` calls `installSplashScreen()` after `super.onCreate()`. Move installation before the superclass call to follow the documented API order.
- No custom splash retention condition or exit animation is currently configured.

## Proposed motion

- Animate the seven bars' vertical scale around their vertical centers, with a small stagger across the waveform.
- Begin with visible, shortened bars so the logo is recognizable immediately.
- Expand smoothly into the existing bar heights without bounce or overshoot, staggering from left to right. Target approximately 500 ms for the complete sequence, including the stagger.
- Preserve the current final geometry, black foreground, peach icon background, and splash icon placement.
- Keep the animation finite; do not loop or add a mandatory splash delay.
- Treat the exact stagger and easing as visual tuning during implementation.

## Implementation plan

1. Create a splash-specific vector with named groups around each bar, retaining the existing outer transform. Set each group's pivot to the bar's vertical center in its local coordinates.
2. Create an `AnimatedVectorDrawable` targeting those groups, using vertical scale animators with staggered start offsets. Ensure every bar ends at scale 1.
3. Keep the current static icon as the default splash drawable for older Android versions. Supply the animated version through an Android 12+ (`drawable-v31`) resource with the same splash-specific resource name.
4. Update `windowSplashScreenAnimatedIcon` in the existing splash theme to that resource. Declare animation-duration metadata matching the total sequence duration where appropriate for the splash API.
5. Move `installSplashScreen()` before `super.onCreate()` in `MainActivity`.
6. Give `OpenIdCallbackActivity` a separate static splash theme while `MainActivity` uses the animated splash theme. Preserve authentication processing and activity routing. Verify the return flow for repeated splash presentation.

The existing splash library is sufficient. Keep the launcher icon resource unchanged; the animation belongs to the splash-specific resources.

## Startup and compatibility

- Animate the system splash icon on Android 12 and later; retain a static icon on earlier Android versions.
- Let the splash dismiss when the app is ready. The animation duration is not a minimum display time, so quick launches may show only part of the sequence.
- Immediate dismissal at any frame is acceptable, even if the animation is barely visible. Do not continue the animation inside the app.
- Preserve the system's normal cold/warm launch behavior and hot-start behavior.
- Ensure the icon remains sensible when animations are disabled or interrupted.
- Preserve the current inherited splash window background; background changes are outside this task. The window theme is light, while the app's saved dark-mode preference is applied separately. Check matching and conflicting system/app theme settings and record visible transition problems.

## Validation and acceptance criteria

- The Android debug build compiles and packages successfully.
- Android 12+ cold and warm launches show a short waveform animation when startup timing permits; older Android versions show the static fallback.
- The final frame matches the current logo's geometry, position, and colors, with no clipping or layout jump.
- Fast launches dismiss normally without waiting for the animation to finish.
- Matching and conflicting system/app light/dark settings and disabled animations produce a readable, stable splash. Record any existing background transition problem separately.
- OpenID authentication returns still reach the expected screen; the callback uses a static icon. Check the return flow for repeated splash presentation.
- Visual device/emulator verification is performed only when explicitly authorized. Record any verification that remains outstanding.
- A successful build alone does not complete validation. Keep the feature marked as awaiting visual/runtime verification until appearance, interrupted launches, disabled animations, and OpenID return behavior have been checked.

## Reference

- [Android splash screen migration guide](https://developer.android.com/develop/ui/views/launch/splash-screen/migrate): theme configuration and `installSplashScreen()` call order.
- [AndroidX SplashScreen reference](https://developer.android.com/reference/androidx/core/splashscreen/SplashScreen): animated icon compatibility and duration semantics.

These API details were checked through Android CLI documentation during the initial setup review.
