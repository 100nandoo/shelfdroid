package dev.halim.shelfdroid.store

fun libraryError(id: String = ""): String {
    return if (id.isEmpty()) "No libraries found for LibraryKey.All!" else "No library found for id = ${id}!"
}

fun itemError(id: String = ""): String {
    return if (id.isEmpty()) "No items found for ids = $id!" else "No item found for id = $id!"
}
