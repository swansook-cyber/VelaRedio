plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
android { namespace = "net.velalab.radio"; compileSdk = 35
 defaultConfig { applicationId = "net.velalab.radio"; minSdk = 26; targetSdk = 35; versionCode = 21; versionName = "1.3.7"; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
 signingConfigs {
  create("velaRelease") {
   val keyFile = System.getenv("VELA_RELEASE_KEYSTORE_FILE")
   if (!keyFile.isNullOrBlank()) storeFile = file(keyFile)
   storePassword = System.getenv("VELA_RELEASE_STORE_PASSWORD")
   keyAlias = "velalab-release"
   keyPassword = System.getenv("VELA_RELEASE_KEY_PASSWORD")
  }
 }
 buildTypes {
  getByName("release") {
   if (gradle.startParameter.taskNames.any { it.endsWith("assembleRelease") }) {
    require(!System.getenv("VELA_RELEASE_KEYSTORE_FILE").isNullOrBlank()) { "V2 release keystore is required" }
    require(!System.getenv("VELA_RELEASE_STORE_PASSWORD").isNullOrBlank()) { "V2 release store password is required" }
    require(!System.getenv("VELA_RELEASE_KEY_PASSWORD").isNullOrBlank()) { "V2 release key password is required" }
   }
   signingConfig = signingConfigs.getByName("velaRelease")
   isMinifyEnabled = false
  }
 }
 buildFeatures { compose = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies {
 implementation(platform("androidx.compose:compose-bom:2024.10.01"))
 implementation("androidx.activity:activity-compose:1.9.3")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.foundation:foundation")
 implementation("androidx.compose.ui:ui")
 implementation("androidx.media3:media3-exoplayer:1.4.1")
 implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
 implementation("androidx.media3:media3-session:1.4.1")
 implementation("androidx.media3:media3-common:1.4.1")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
 implementation("com.google.guava:guava:33.3.1-android")
 implementation("io.coil-kt:coil-compose:2.7.0")
 androidTestImplementation(platform("androidx.compose:compose-bom:2024.10.01"))
 androidTestImplementation("androidx.compose.ui:ui-test-junit4")
 androidTestImplementation("androidx.test.ext:junit:1.2.1")
 androidTestImplementation("androidx.test:runner:1.6.2")
 debugImplementation("androidx.compose.ui:ui-test-manifest")
}
