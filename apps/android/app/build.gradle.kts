plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}
android {
    namespace = "com.example.app"
    compileSdk = 36
    androidResources { generateLocaleConfig = true }
    sourceSets["main"].res.srcDir("src/main/generated/localization/res")
    defaultConfig {
        resourceConfigurations +=
            file("src/main/generated/localization/locales.txt").readLines().filter { it.isNotBlank() }
        applicationId = "com.example.app"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin { jvmToolchain(17) }
dependencies {
    implementation(project(":core:localization"))
    implementation(project(":core:navigation"))
    implementation(project(":core:network"))
    implementation(project(":shared:bookmarks"))
    implementation(project(":shared:github"))
    implementation(project(":shared:designsystem"))
    implementation(project(":features:bookmarks"))
    implementation(project(":features:search"))
    implementation(project(":features:settings"))
    implementation(project(":core:storage"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.navigation.suite)
    implementation(libs.lifecycle.viewmodel.navigation3)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.activity.compose)
    implementation(libs.activity.ktx)
}
