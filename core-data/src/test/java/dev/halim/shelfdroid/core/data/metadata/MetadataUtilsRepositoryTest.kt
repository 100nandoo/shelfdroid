package dev.halim.shelfdroid.core.data.metadata

import android.content.ContextWrapper
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import dev.halim.shelfdroid.helper.Helper
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Test

class MetadataUtilsRepositoryTest {
  @Test
  fun encodeTagPath_usesUtf8StandardBase64ThenUriEscaping() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val file = Files.createTempFile("metadata-helper", ".preferences_pb").toFile()
    file.deleteOnExit()
    val dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    val helper = Helper(DataStoreManager(dataStore), ContextWrapper(null))

    try {
      // "science/fiction + 日本" encodes to a path-safe value with no raw Base64 delimiters.
      val encoded = helper.encodeTagPath("science/fiction + 日本")

      assertEquals("c2NpZW5jZS9maWN0aW9uICsg5pel5pys", encoded)
      org.junit.Assert.assertFalse(encoded.contains('/'))
      org.junit.Assert.assertFalse(encoded.contains('+'))

      assertEquals("AAA%2B", helper.encodeTagPath("\u0000\u0000>"))
      assertEquals("AAA%2F", helper.encodeTagPath("\u0000\u0000?"))
      assertEquals("Zg%3D%3D", helper.encodeTagPath("f"))
    } finally {
      scope.cancel()
      file.delete()
    }
  }
}
