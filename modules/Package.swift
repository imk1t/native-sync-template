// swift-tools-version: 6.0
// 全 iOS モジュールを 1 つのパッケージにまとめる。
// 同名ディレクトリ（各モジュールの ios/）のローカルパッケージは SwiftPM で同一 ID として衝突するため。
// 依存ルールは tools/check-deps.ts が、このファイルの target 行（1 行 1 target）を読んで検査する。
import PackageDescription
import Foundation

let localizationURL = URL(fileURLWithPath: #filePath).deletingLastPathComponent().appendingPathComponent("Generated/Localization/config.json")
let localization = try JSONDecoder().decode(LocalizationConfig.self, from: Data(contentsOf: localizationURL))
struct LocalizationConfig: Decodable { let defaultLocale: String }

let package = Package(
    name: "Modules",
    defaultLocalization: .init(stringLiteral: localization.defaultLocale),
    platforms: [.iOS(.v18)],
    products: [
        .library(name: "CoreExtensions", targets: ["CoreExtensions"]),
        .library(name: "CoreFont", targets: ["CoreFont"]),
        .library(name: "CoreLocalization", targets: ["CoreLocalization"]),
        .library(name: "CoreUtils", targets: ["CoreUtils"]),
        .library(name: "CoreNavigation", targets: ["CoreNavigation"]),
        .library(name: "CoreNetwork", targets: ["CoreNetwork"]),
        .library(name: "CoreStorage", targets: ["CoreStorage"]),
        .library(name: "DesignSystem", targets: ["DesignSystem"]),
        .library(name: "SharedBookmarks", targets: ["SharedBookmarks"]),
        .library(name: "SharedGitHubREST", targets: ["SharedGitHubREST"]),
        .library(name: "FeatureBookmarks", targets: ["FeatureBookmarks"]),
        .library(name: "FeatureSearch", targets: ["FeatureSearch"]),
        .library(name: "FeatureSettings", targets: ["FeatureSettings"]),
    ],
    targets: [
        .target(name: "CoreExtensions", path: "core/extensions/ios/Sources/Core/Extensions"),
        .target(name: "CoreFont", path: "core/font/ios/Sources/Core/Font", resources: [.process("Resources")]),
        .target(name: "CoreLocalization", path: "core/localization/ios/Sources/Core/Localization", resources: [.process("Generated/Resources")]),
        .target(name: "CoreUtils", path: "core/utils/ios/Sources/Core/Utils"),
        .target(name: "CoreNavigation", path: "core/navigation/ios/Sources/Core/Navigation"),
        .target(name: "CoreNetwork", dependencies: ["CoreUtils"], path: "core/network/ios/Sources/Core/Network"),
        .target(name: "CoreStorage", path: "core/storage/ios/Sources/Core/Storage"),
        .target(name: "DesignSystem", dependencies: ["CoreFont"], path: "shared/designsystem/ios/Sources/Shared/DesignSystem"),
        .target(name: "SharedBookmarks", dependencies: ["CoreExtensions", "CoreStorage"], path: "shared/bookmarks/ios/Sources/Shared/Bookmarks"),
        .target(name: "SharedGitHubREST", dependencies: ["CoreNetwork"], path: "shared/github-rest/ios/Sources/Shared/GitHubREST"),
        .target(name: "FeatureBookmarks", dependencies: ["CoreLocalization", "CoreNavigation", "CoreNetwork", "CoreStorage", "SharedBookmarks", "DesignSystem"], path: "features/bookmarks/ios/Sources/Features/Bookmarks"),
        .target(name: "FeatureSearch", dependencies: ["CoreExtensions", "CoreUtils", "CoreLocalization", "CoreNavigation", "CoreNetwork", "CoreStorage", "SharedBookmarks", "SharedGitHubREST", "DesignSystem"], path: "features/search/ios/Sources/Features/Search"),
        .target(name: "FeatureSettings", dependencies: ["CoreLocalization", "CoreStorage", "CoreNavigation", "DesignSystem"], path: "features/settings/ios/Sources/Features/Settings"),
        .testTarget(name: "CoreExtensionsTests", dependencies: ["CoreExtensions"], path: "core/extensions/ios/Tests/Core/Extensions"),
        .testTarget(name: "CoreFontTests", dependencies: ["CoreFont"], path: "core/font/ios/Tests/Core/Font"),
        .testTarget(name: "CoreUtilsTests", dependencies: ["CoreUtils"], path: "core/utils/ios/Tests/Core/Utils"),
        .testTarget(name: "CoreLocalizationTests", dependencies: ["CoreLocalization"], path: "core/localization/ios/Tests/Core/Localization"),
        .testTarget(name: "FeatureSettingsTests", dependencies: ["FeatureSettings", "CoreStorage", "CoreNavigation"], path: "features/settings/ios/Tests/Features/Settings"),
        .testTarget(name: "CoreNavigationTests", dependencies: ["CoreNavigation"], path: "core/navigation/ios/Tests/Core/Navigation"),
        .testTarget(name: "SharedGitHubRESTTests", dependencies: ["SharedGitHubREST", "CoreNetwork"], path: "shared/github-rest/ios/Tests/Shared/GitHubREST"),
        .testTarget(name: "CoreStorageTests", dependencies: ["CoreStorage"], path: "core/storage/ios/Tests/Core/Storage"),
        .testTarget(name: "SharedBookmarksTests", dependencies: ["SharedBookmarks", "CoreStorage"], path: "shared/bookmarks/ios/Tests/Shared/Bookmarks"),
        .testTarget(name: "FeatureBookmarksTests", dependencies: ["FeatureBookmarks", "SharedBookmarks", "CoreStorage", "CoreNetwork", "CoreNavigation"], path: "features/bookmarks/ios/Tests/Features/Bookmarks"),
        .testTarget(name: "FeatureSearchTests", dependencies: ["FeatureSearch", "SharedBookmarks", "SharedGitHubREST", "CoreNavigation", "CoreStorage", "CoreNetwork"], path: "features/search/ios/Tests/Features/Search"),
    ]
)
