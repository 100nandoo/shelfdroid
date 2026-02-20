package dev.halim.shelfdroid.core

data class Permissions(
  val download: Boolean = false,
  val update: Boolean = false,
  val delete: Boolean = false,
  val upload: Boolean = false,
  val createEReader: Boolean = false,
  val accessExplicit: Boolean = false,
  val accessAllLibraries: Boolean = false,
  val accessAllTags: Boolean = false,
) {
  companion object {
    val UserPermissions =
      Permissions(download = true, accessAllLibraries = true, accessAllTags = true)
    val AdminPermissions = Permissions(true, true, true, true, true, true, true, true)
    val GuestPermissions = Permissions(accessAllLibraries = true, accessAllTags = true)
  }
}
