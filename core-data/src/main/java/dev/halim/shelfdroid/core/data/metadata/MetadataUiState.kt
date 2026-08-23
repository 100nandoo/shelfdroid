package dev.halim.shelfdroid.core.data.metadata

data class MetadataMutation(val updatedItemCount: Int, val merged: Boolean = false)

data class MetadataRenameCollision(val exact: Boolean, val caseOnly: Boolean)

fun sortedMetadataItems(items: Iterable<String>): List<String> =
  items.distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)

fun metadataRenameCollision(
  currentItem: String,
  newItem: String,
  items: Iterable<String>,
): MetadataRenameCollision {
  val current = currentItem.trim()
  val target = newItem.trim()
  val exact = items.any { it != current && it == target }
  val caseOnly = !exact && items.any { it != current && it.equals(target, ignoreCase = true) }
  return MetadataRenameCollision(exact = exact, caseOnly = caseOnly)
}
