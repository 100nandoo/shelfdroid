package dev.halim.shelfdroid.widget.playback

import android.content.Context
import android.content.Intent

internal fun openAppIntent(context: Context): Intent =
  Intent(Intent.ACTION_MAIN)
    .addCategory(Intent.CATEGORY_LAUNCHER)
    .setPackage(context.packageName)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
