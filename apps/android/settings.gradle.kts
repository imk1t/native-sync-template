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
module(":core:extensions", "core/extensions")
module(":core:utils", "core/utils")
module(":core:font", "core/font")
module(":core:navigation", "core/navigation")
module(":core:network", "core/network")
module(":core:storage", "core/storage")
module(":shared:designsystem", "shared/designsystem")
module(":shared:bookmarks", "shared/bookmarks")
module(":shared:github-rest", "shared/github-rest")
module(":features:bookmarks", "features/bookmarks")
module(":features:search", "features/search")
module(":features:settings", "features/settings")

module(":core:localization", "core/localization")
