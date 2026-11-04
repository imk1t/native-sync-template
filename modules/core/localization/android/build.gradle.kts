plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}
android {
    buildFeatures { compose = true }
    namespace = "com.example.core.localization"
    compileSdk = 36
    sourceSets["main"].res.srcDir("src/main/generated/res")
    sourceSets["main"].java.srcDir("src/main/generated/kotlin")
    defaultConfig {
        minSdk = 28
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    // Resource resolution needs Android. Keep test sources at the repository's standard path.
    sourceSets["test"].java.setSrcDirs(emptyList<String>())
    sourceSets["androidTest"].java.srcDir("src/test/kotlin")
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin {
    jvmToolchain(17)
    sourceSets["test"].kotlin.setSrcDirs(emptyList<String>())
}
dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
}
