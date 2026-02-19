package dev.halim.shelfdroid.core

enum class UserType {
  Admin,
  User,
  Root,
  Guest,
  Unknown;

  companion object {
    fun toUserType(value: String): UserType {
      return when (value.lowercase()) {
        "admin" -> Admin
        "user" -> User
        "root" -> Root
        "guest" -> Guest
        else -> Unknown
      }
    }

    val editTypes = listOf(Admin, User, Guest)
  }

  fun isAdmin(): Boolean = this == Admin

  fun isUser(): Boolean = this == User

  fun isRoot(): Boolean = this == Root

  fun isGuest(): Boolean = this == Guest

  fun isUnknown(): Boolean = this == Unknown
}
