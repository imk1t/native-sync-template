plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}
android {
    namespace = "com.example.designsystem"
    compileSdk = 36
    defaultConfig { minSdk = 28 }
}
dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
}
