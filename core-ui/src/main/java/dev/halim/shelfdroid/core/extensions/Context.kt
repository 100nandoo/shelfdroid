package dev.halim.shelfdroid.core.extensions

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

fun Context.findActivity(): Activity =
  when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("Can't find activity from context")
  }
