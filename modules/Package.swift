// swift-tools-version: 6.0
// 全 iOS モジュールを 1 つのパッケージにまとめる。
// 同名ディレクトリ（各モジュールの ios/）のローカルパッケージは SwiftPM で同一 ID として衝突するため。
// 依存ルールは tools/check-deps.ts が、このファイルの target 行（1 行 1 target）を読んで検査する。
import PackageDescription

let package = Package(
    name: "Modules",
    platforms: [.iOS(.v18)],
    products: [
        .library(name: "CoreUtils", targets: ["CoreUtils"]),
        .library(name: "CoreNavigation", targets: ["CoreNavigation"]),
        .library(name: "CoreNetwork", targets: ["CoreNetwork"]),
        .library(name: "CoreStorage", targets: ["CoreStorage"]),
        .library(name: "DesignSystem", targets: ["DesignSystem"]),
        .library(name: "SharedBookmarks", targets: ["SharedBookmarks"]),
        .library(name: "FeatureBookmarks", targets: ["FeatureBookmarks"]),
        .library(name: "FeatureSearch", targets: ["FeatureSearch"]),
        .library(name: "FeatureSettings", targets: ["FeatureSettings"]),
    ],
    targets: [
        .target(name: "CoreUtils", path: "core/utils/ios/Sources/CoreUtils"),
        .target(name: "CoreNavigation", path: "core/navigation/ios/Sources/CoreNavigation"),
        .target(name: "CoreNetwork", dependencies: ["CoreUtils"], path: "core/network/ios/Sources/CoreNetwork"),
        .target(name: "CoreStorage", path: "core/storage/ios/Sources/CoreStorage"),
        .target(name: "DesignSystem", path: "designsystem/ios/Sources/DesignSystem"),
        .target(name: "SharedBookmarks", dependencies: ["CoreNetwork", "CoreStorage"], path: "shared/bookmarks/ios/Sources/SharedBookmarks"),
        .target(name: "FeatureBookmarks", dependencies: ["CoreNavigation", "CoreNetwork", "CoreStorage", "SharedBookmarks", "DesignSystem"], path: "features/bookmarks/ios/Sources/FeatureBookmarks"),
        .target(name: "FeatureSearch", dependencies: ["CoreNavigation", "CoreNetwork", "CoreStorage", "SharedBookmarks", "DesignSystem"], path: "features/search/ios/Sources/FeatureSearch"),
        .target(name: "FeatureSettings", dependencies: ["CoreStorage", "CoreNavigation", "DesignSystem"], path: "features/settings/ios/Sources/FeatureSettings"),
        .testTarget(name: "FeatureSettingsTests", dependencies: ["FeatureSettings", "CoreStorage", "CoreNavigation"], path: "features/settings/ios/Tests/FeatureSettingsTests"),
        .testTarget(name: "CoreNavigationTests", dependencies: ["CoreNavigation"], path: "core/navigation/ios/Tests/CoreNavigationTests"),
        .testTarget(name: "CoreNetworkTests", dependencies: ["CoreNetwork"], path: "core/network/ios/Tests/CoreNetworkTests"),
        .testTarget(name: "CoreStorageTests", dependencies: ["CoreStorage"], path: "core/storage/ios/Tests/CoreStorageTests"),
        .testTarget(name: "SharedBookmarksTests", dependencies: ["SharedBookmarks", "CoreStorage", "CoreNetwork"], path: "shared/bookmarks/ios/Tests/SharedBookmarksTests"),
        .testTarget(name: "FeatureBookmarksTests", dependencies: ["FeatureBookmarks", "SharedBookmarks", "CoreStorage", "CoreNetwork", "CoreNavigation"], path: "features/bookmarks/ios/Tests/FeatureBookmarksTests"),
        .testTarget(name: "FeatureSearchTests", dependencies: ["FeatureSearch", "SharedBookmarks", "CoreNavigation", "CoreStorage", "CoreNetwork"], path: "features/search/ios/Tests/FeatureSearchTests"),
    ]
)
