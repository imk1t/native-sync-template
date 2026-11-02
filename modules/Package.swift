// swift-tools-version: 6.0
// 全 iOS モジュールを 1 つのパッケージにまとめる。
// 同名ディレクトリ（各モジュールの ios/）のローカルパッケージは SwiftPM で同一 ID として衝突するため。
// 依存ルールは tools/check_deps.py が、このファイルの target 行（1 行 1 target）を読んで検査する。
import PackageDescription

let package = Package(
    name: "Modules",
    platforms: [.iOS(.v18)],
    products: [
        .library(name: "CoreUtils", targets: ["CoreUtils"]),
        .library(name: "CoreNetwork", targets: ["CoreNetwork"]),
        .library(name: "DesignSystem", targets: ["DesignSystem"]),
        .library(name: "FeatureItems", targets: ["FeatureItems"]),
    ],
    targets: [
        .target(name: "CoreUtils", path: "core/utils/ios/Sources/CoreUtils"),
        .target(name: "CoreNetwork", dependencies: ["CoreUtils"], path: "core/network/ios/Sources/CoreNetwork"),
        .target(name: "DesignSystem", path: "designsystem/ios/Sources/DesignSystem"),
        .target(name: "FeatureItems", dependencies: ["CoreNetwork", "DesignSystem"], path: "features/items/ios/Sources/FeatureItems"),
        .testTarget(name: "CoreNetworkTests", dependencies: ["CoreNetwork"], path: "core/network/ios/Tests/CoreNetworkTests"),
        .testTarget(name: "FeatureItemsTests", dependencies: ["FeatureItems"], path: "features/items/ios/Tests/FeatureItemsTests"),
    ]
)
