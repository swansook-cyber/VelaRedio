plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
android { namespace = "net.velalab.radio"; compileSdk = 35
 defaultConfig { applicationId = "net.velalab.radio"; minSdk = 26; targetSdk = 35; versionCode = 5; versionName = "0.5.0" }
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
}