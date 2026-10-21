package dev.halim.shelfdroid.widget.player.ui

import androidx.compose.ui.unit.dp

internal object SharedMetrics {
  val verticalPadding = 8.dp
  val controlButtonSize = 48.dp
}

internal object CompactMetrics {
  val verticalPadding = 4.dp
  val verticalPaddingBudget = verticalPadding * 2
  val minimumCoverWidth = 280.dp
  val minimumCoverSize = 1.dp
  val maximumCoverSize = 48.dp
  val twoLineTitleMinimumHeight = 80.dp
}

internal object MediumMetrics {
  val contentSpacing = 4.dp
  // Leave room below the cover for controls, their gap, and the widget's top/bottom padding.
  val coverHeightBudget =
    SharedMetrics.verticalPadding * 2 + SharedMetrics.controlButtonSize + contentSpacing
  // Tuned offset used to estimate space for medium metadata. It is independent of the cover
  // budget above and does not represent an exact sum of rendered padding and controls.
  val metadataHeightBudget = 56.dp
  val minimumCoverSize = 48.dp
  val maximumCoverSize = 72.dp
  val twoLineTitleMinimumHeight = 66.dp
  val twoLineChapterTitleMinimumHeight = 80.dp
}

internal object LargeMetrics {
  val stackedCoverMinimumHeight = 302.dp
  val spaciousLayoutMinimumHeight = 240.dp
  val tightContentSpacing = 2.dp
  val spaciousContentSpacing = 8.dp
  // Tuned offsets used to leave room outside the cover for the rest of the layout.
  // Stacked art needs a larger budget because metadata cannot share its row.
  val sideCoverHeightBudget = 108.dp
  val stackedCoverHeightBudget = 164.dp
  // Conservative metadata budget for padding and controls; spacing is subtracted separately.
  val metadataHeightBudget = 100.dp
  // Reserve three gaps as headroom for both cover arrangements.
  val metadataSpacingSlots = 3
  val minimumCoverSize = 48.dp
  val maximumCoverSize = 96.dp
  val bookHeadlineTwoLineHeight = 68.dp
  val chapterHeadlineTwoLineHeight = 82.dp
}
