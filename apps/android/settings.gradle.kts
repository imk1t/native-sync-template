pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "native-sync-template"

include(":app")

// モジュールは modules/ に置き、プロジェクト名を iOS のモジュール名と対応させる
fun module(name: String, dir: String) {
    include(name)
    project(name).projectDir = file("../../modules/$dir/android")
}
module(":core:utils", "core/utils")
module(":core:navigation", "core/navigation")
module(":core:network", "core/network")
module(":core:storage", "core/storage")
module(":designsystem", "designsystem")
module(":features:items", "features/items")
