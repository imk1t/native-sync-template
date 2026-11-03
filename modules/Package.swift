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
        .library(name: "FeatureGitHub", targets: ["FeatureGitHub"]),
        .library(name: "FeatureSettings", targets: ["FeatureSettings"]),
    ],
    targets: [
        .target(name: "CoreUtils", path: "core/utils/ios/Sources/CoreUtils"),
        .target(name: "CoreNavigation", path: "core/navigation/ios/Sources/CoreNavigation"),
        .target(name: "CoreNetwork", dependencies: ["CoreUtils"], path: "core/network/ios/Sources/CoreNetwork"),
        .target(name: "CoreStorage", path: "core/storage/ios/Sources/CoreStorage"),
        .target(name: "DesignSystem", path: "designsystem/ios/Sources/DesignSystem"),
        .target(name: "FeatureGitHub", dependencies: ["CoreNavigation", "CoreNetwork", "CoreStorage", "DesignSystem"], path: "features/github/ios/Sources/FeatureGitHub"),
        .target(name: "FeatureSettings", dependencies: ["CoreStorage", "CoreNavigation", "DesignSystem"], path: "features/settings/ios/Sources/FeatureSettings"),
        .testTarget(name: "FeatureSettingsTests", dependencies: ["FeatureSettings", "CoreStorage", "CoreNavigation"], path: "features/settings/ios/Tests/FeatureSettingsTests"),
        .testTarget(name: "CoreNavigationTests", dependencies: ["CoreNavigation"], path: "core/navigation/ios/Tests/CoreNavigationTests"),
        .testTarget(name: "CoreNetworkTests", dependencies: ["CoreNetwork"], path: "core/network/ios/Tests/CoreNetworkTests"),
        .testTarget(name: "CoreStorageTests", dependencies: ["CoreStorage"], path: "core/storage/ios/Tests/CoreStorageTests"),
        .testTarget(name: "FeatureGitHubTests", dependencies: ["FeatureGitHub", "CoreNavigation", "CoreStorage", "CoreNetwork"], path: "features/github/ios/Tests/FeatureGitHubTests"),
    ]
)
