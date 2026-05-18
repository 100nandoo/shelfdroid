package dev.halim.shelfdroid.core.data.download

interface ManagedDownloadEnqueuer {
  fun enqueue(request: ManagedDownloadRequest): Long
}
