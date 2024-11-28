package dev.halim.shelfdroid.store

fun libraryError(id: String = ""): String {
    return if (id.isEmpty()) "No libraries found for at all!" else "No library found with id = ${id}!"
}

fun itemError(id: String = ""): String {
    return if (id.isEmpty()) "No items found for at all!" else "No item found with id = $id!"
}

fun progressError(id: String = ""): String {
    return if (id.isEmpty()) "No progress found at all!" else "No progress found with item id = ${id}!"
}
