package dev.halim.shelfdroid.media.service

class ChapterSessionCommandAccess(private val applicationPackageName: String) {
  fun isAllowed(controllerPackageName: String, isTrusted: Boolean): Boolean =
    isTrusted && controllerPackageName == applicationPackageName
}
