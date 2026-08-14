# Bounded concurrent Library item refresh requests

ShelfDroid limits each `POST /api/items/batch/get` request to 50 Library item IDs. When a Library refresh has more IDs, it deduplicates and splits them into 50-ID chunks, fetches those chunks concurrently, and applies the refresh only after every chunk succeeds; this honors the Audiobookshelf server limit while avoiding partial catalog state.
