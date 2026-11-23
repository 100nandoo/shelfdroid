import com.android.build.api.dsl.CommonExtension

listOf("com.android.library", "com.android.test").forEach { pluginId ->
  pluginManager.withPlugin(pluginId) {
    extensions.getByType<CommonExtension>().defaultConfig.testInstrumentationRunner =
      "androidx.test.runner.AndroidJUnitRunner"
  }
}
