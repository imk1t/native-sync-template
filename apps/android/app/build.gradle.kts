plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}
android {
    namespace = "com.example.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.app"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }
}
dependencies {
    implementation(project(":core:network"))
    implementation(project(":features:items"))
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
}
