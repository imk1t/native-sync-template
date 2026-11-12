plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}
android {
    namespace = "com.example.shared.bookmarks"
    compileSdk = 36
    defaultConfig { minSdk = 28 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin { jvmToolchain(17) }
dependencies {
    implementation(project(":core:extensions"))
    implementation(project(":core:storage"))
    implementation(libs.serialization.json)
    implementation(libs.coroutines.core)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.coroutines.test)
}
