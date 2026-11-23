import com.android.build.api.dsl.CommonExtension

listOf("com.android.application", "com.android.library", "com.android.test").forEach { pluginId ->
  pluginManager.withPlugin(pluginId) {
    extensions.getByType<CommonExtension>().compileOptions.apply {
      sourceCompatibility = JavaVersion.VERSION_17
      targetCompatibility = JavaVersion.VERSION_17
    }
  }
}
