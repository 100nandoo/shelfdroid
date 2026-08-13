# Library items response OOM

## Status

Open. Investigation completed; implementation intentionally deferred.

## Finding

ShelfDroid can crash with `java.lang.OutOfMemoryError` while loading a library.

The failing call is `GET /api/libraries/{libraryId}/items` from
`LibraryItemRepository.idsByLibraryId()`. The request does not specify pagination,
so Audiobookshelf returns the complete library in one response. The captured
response was approximately 52 MB.

Retrofit's Kotlin serialization converter calls `ResponseBody.string()`. Okio
therefore attempts to materialize the complete response as a `String` before
deserialization. At the time of the crash, Android reported approximately 25 MB
available for a required 52 MB allocation and a 192 MB heap growth limit.

The crash occurs on the `OkHttp Dispatcher` thread during response conversion,
before ShelfDroid can process the returned library items. Network Inspector also
reported payloads larger than 10 MB.

## Reproduction evidence

Captured stack trace path:

```text
okio.Buffer.readByteArray
okio.Buffer.readString
okhttp3.ResponseBody.string
retrofit2.converter.kotlinx.serialization.Serializer$FromString.fromResponseBody
retrofit2.OkHttpCall.parseResponse
java.lang.OutOfMemoryError
```

## Scope

The confirmed issue is response size and whole-body buffering, not API model
nullability. No fix is included in this investigation document.
