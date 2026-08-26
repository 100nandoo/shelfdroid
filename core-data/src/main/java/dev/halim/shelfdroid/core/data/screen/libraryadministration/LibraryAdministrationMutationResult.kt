package dev.halim.shelfdroid.core.data.screen.libraryadministration

/**
 * Outcome of a server-accepted Library administration mutation.
 *
 * A server rejection remains a failed [Result]. Once the server accepts a mutation, local
 * synchronization can still fail; in that case the accepted value must remain visible and the
 * caller can retry synchronization without sending the mutation again.
 */
sealed interface LibraryAdministrationMutationResult<out T> {
  data class Accepted<T>(val value: T) : LibraryAdministrationMutationResult<T>

  data class AcceptedButNotSynchronized<T>(
    val value: T,
    val error: Throwable,
  ) : LibraryAdministrationMutationResult<T>
}
