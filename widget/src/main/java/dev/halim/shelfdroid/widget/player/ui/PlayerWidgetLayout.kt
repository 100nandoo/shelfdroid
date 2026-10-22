package dev.halim.shelfdroid.widget.player.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal sealed interface PlayerWidgetLayoutSpec {
  val verticalPadding: Dp
  val controlButtonSize: Dp

  data class Compact(
    val showCover: Boolean,
    val coverSize: Dp,
    val titleMaxLines: Int,
    override val verticalPadding: Dp,
    override val controlButtonSize: Dp,
  ) : PlayerWidgetLayoutSpec

  data class Medium(
    val coverSize: Dp,
    val titleMaxLines: Int,
    val chapterTitleMaxLines: Int,
    val showChapterNavigation: Boolean,
    val contentSpacing: Dp,
    override val verticalPadding: Dp,
    override val controlButtonSize: Dp,
  ) : PlayerWidgetLayoutSpec

  data class Large(
    val sideCover: Boolean,
    val coverSize: Dp,
    val headlineMaxLines: Int,
    val showChapterNavigation: Boolean,
    val contentSpacing: Dp,
    override val verticalPadding: Dp,
    override val controlButtonSize: Dp,
  ) : PlayerWidgetLayoutSpec
}

private val mediumMinimumHeight = 116.dp
private val mediumFontScaleHeightAllowance = 48.dp
private val largeMinimumHeight = 176.dp
private val mediumLayoutMinimumWidth = 220.dp

internal fun resolvePlayerWidgetLayout(
  width: Dp,
  height: Dp,
  fontScale: Float,
  hasChapterTitle: Boolean,
  showChapterNavigation: Boolean,
): PlayerWidgetLayoutSpec {
  val normalizedFontScale = fontScale.coerceAtLeast(1f)
  val mediumHeightForFontScale =
    mediumMinimumHeight + mediumFontScaleHeightAllowance * (normalizedFontScale - 1f)

  if (width < mediumLayoutMinimumWidth || height < mediumHeightForFontScale) {
    return compactLayout(width, height)
  }

  if (height < largeMinimumHeight) {
    return mediumLayout(width, height, normalizedFontScale, showChapterNavigation)
  }

  return largeLayout(width, height, normalizedFontScale, hasChapterTitle, showChapterNavigation)
}

private fun compactLayout(width: Dp, height: Dp): PlayerWidgetLayoutSpec.Compact =
  PlayerWidgetLayoutSpec.Compact(
    showCover = width >= CompactMetrics.minimumCoverWidth,
    coverSize =
      minOf(
        CompactMetrics.maximumCoverSize,
        (height - CompactMetrics.verticalPaddingBudget).coerceAtLeast(
          CompactMetrics.minimumCoverSize
        ),
      ),
    titleMaxLines = if (height < CompactMetrics.twoLineTitleMinimumHeight) 1 else 2,
    verticalPadding = CompactMetrics.verticalPadding,
    controlButtonSize = SharedMetrics.controlButtonSize,
  )

private fun mediumLayout(
  width: Dp,
  height: Dp,
  normalizedFontScale: Float,
  showChapterNavigation: Boolean,
): PlayerWidgetLayoutSpec.Medium {
  val metadataHeight = height - MediumMetrics.metadataHeightBudget
  return PlayerWidgetLayoutSpec.Medium(
    coverSize =
      minOf(
        MediumMetrics.maximumCoverSize,
        (height - MediumMetrics.coverHeightBudget).coerceAtLeast(MediumMetrics.minimumCoverSize),
      ),
    titleMaxLines =
      if (metadataHeight >= MediumMetrics.twoLineTitleMinimumHeight * normalizedFontScale) 2 else 1,
    chapterTitleMaxLines =
      if (metadataHeight >= MediumMetrics.twoLineChapterTitleMinimumHeight * normalizedFontScale) 2
      else 1,
    showChapterNavigation = showChapterNavigation,
    contentSpacing = MediumMetrics.contentSpacing,
    verticalPadding = SharedMetrics.verticalPadding,
    controlButtonSize = playerWidgetControlButtonSize(width, showChapterNavigation),
  )
}

private fun largeLayout(
  width: Dp,
  height: Dp,
  normalizedFontScale: Float,
  hasChapterTitle: Boolean,
  showChapterNavigation: Boolean,
): PlayerWidgetLayoutSpec.Large {
  val contentSpacing =
    if (height >= LargeMetrics.spaciousLayoutMinimumHeight) LargeMetrics.spaciousContentSpacing
    else LargeMetrics.tightContentSpacing
  val sideCover = height < LargeMetrics.stackedCoverMinimumHeight || normalizedFontScale > 1f
  val headlineTwoLineHeight =
    if (hasChapterTitle) LargeMetrics.chapterHeadlineTwoLineHeight
    else LargeMetrics.bookHeadlineTwoLineHeight
  val availableCoverHeight =
    height -
      LargeMetrics.verticalPaddingBudget -
      SharedMetrics.controlButtonSize -
      contentSpacing * (if (sideCover) 1 else 2) -
      (if (sideCover) 0.dp else headlineTwoLineHeight * normalizedFontScale)
  val availableCoverWidth =
    width -
      LargeMetrics.horizontalPaddingBudget -
      (if (sideCover) {
        LargeMetrics.sideCoverMetadataSpacing + LargeMetrics.minimumMetadataWidth
      } else {
        0.dp
      })
  val coverSize =
    minOf(availableCoverHeight, availableCoverWidth).coerceAtLeast(LargeMetrics.minimumCoverSize)
  val metadataHeightBudget =
    height -
      LargeMetrics.metadataHeightBudget -
      (contentSpacing * LargeMetrics.metadataSpacingSlots)

  return PlayerWidgetLayoutSpec.Large(
    sideCover = sideCover,
    coverSize = coverSize,
    headlineMaxLines =
      if (!sideCover || metadataHeightBudget >= headlineTwoLineHeight * normalizedFontScale) 2 else 1,
    showChapterNavigation = showChapterNavigation,
    contentSpacing = contentSpacing,
    verticalPadding = SharedMetrics.verticalPadding,
    controlButtonSize = playerWidgetControlButtonSize(width, showChapterNavigation),
  )
}
