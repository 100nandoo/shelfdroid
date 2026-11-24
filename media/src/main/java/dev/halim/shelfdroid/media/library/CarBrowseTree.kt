package dev.halim.shelfdroid.media.library

import java.time.Instant
import java.time.ZoneId
import java.util.UUID

internal data class CarBrowseRow(
  val id: CarMediaId,
  val title: String,
  val publishedAt: Long? = null,
)

internal class CarBrowseTree(
  private val parent: CarMediaId,
  entries: List<CarBrowseRow>,
  order: Order,
  unknownDate: String,
) {
  enum class Order {
    LIBRARY,
    TITLE,
    EPISODE_DATE,
  }

  private val groups = mutableMapOf<CarMediaId.Group, Pair<CarBrowseRow, List<CarBrowseRow>>>()
  val rows: List<CarBrowseRow>

  init {
    val titleOrder =
      compareBy<CarBrowseRow, String>(String.CASE_INSENSITIVE_ORDER) { it.title }
        .thenBy { it.id.value }
    val sorted =
      when (order) {
        Order.LIBRARY -> entries
        Order.TITLE -> entries.sortedWith(titleOrder)
        Order.EPISODE_DATE ->
          entries.sortedWith(compareByDescending<CarBrowseRow> { it.publishedAt }.then(titleOrder))
      }
    rows =
      if (order == Order.EPISODE_DATE && sorted.size > LIMIT) {
        val years = sorted.groupBy { row ->
          row.publishedAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).year }
        }
        val yearRows = years.map { (year, episodes) ->
          val key = "year-${year ?: "unknown"}"
          addGroup(key, year?.toString() ?: unknownDate, split(episodes, key))
        }
        split(yearRows, "years")
      } else {
        split(sorted, "rows")
      }
  }

  fun item(id: CarMediaId.Group): CarBrowseRow? = groups[id]?.first

  fun children(id: CarMediaId.Group): List<CarBrowseRow>? = groups[id]?.second

  private fun split(rows: List<CarBrowseRow>, key: String): List<CarBrowseRow> {
    if (rows.size <= LIMIT) return rows

    var groupSize = LIMIT.toLong()
    while (groupSize * LIMIT < rows.size) groupSize *= LIMIT
    return rows.chunked(groupSize.toInt()).mapIndexed { index, chunk ->
      val first = shortTitle(chunk.first().title)
      val last = shortTitle(chunk.last().title)
      val label =
        if (first.equals(last, ignoreCase = true)) {
          val from = index * groupSize + 1
          "$first · $from–${from + chunk.size - 1}"
        } else {
          "$first – $last"
        }
      val boundary = "$key:${chunk.first().id.value}"
      val groupKey = UUID.nameUUIDFromBytes(boundary.encodeToByteArray()).toString()
      addGroup(groupKey, label, split(chunk, groupKey))
    }
  }

  private fun addGroup(key: String, title: String, children: List<CarBrowseRow>): CarBrowseRow {
    val id = CarMediaId.Group(parent, key)
    val row = CarBrowseRow(id, title)
    groups[id] = row to children
    return row
  }

  private fun shortTitle(title: String): String {
    val value = title.trim().ifBlank { "…" }
    return if (value.length > RANGE_TITLE_LENGTH) value.take(RANGE_TITLE_LENGTH) + "…" else value
  }

  companion object {
    const val LIMIT = 50
    private const val RANGE_TITLE_LENGTH = 20
  }
}
