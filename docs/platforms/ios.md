# iOS 固有の設定と注意点

OS 固有の設定・環境・落とし穴をここに残す。機能ごとの意図した違いは各 `spec.md` の「OS 固有の差分」に書く。

## ビルド環境
- Xcode と XcodeGen が必要。`apps/ios` で `xcodegen generate` を実行して `App.xcodeproj` を作る(コミットしない)。
- デプロイメントターゲットは iOS 18.0(`apps/ios/project.yml`)。
- Package は `modules/Package.swift` に集約する(理由: `docs/adr/0001`)。

## 実行
```
cd apps/ios && xcodegen generate
xcodebuild -project App.xcodeproj -scheme App -destination 'platform=iOS Simulator,name=<端末名>' CODE_SIGNING_ALLOWED=NO build
```
ビルドした `App.app` を `xcrun simctl install` / `launch`(バンドル ID は `com.example.app`)で起動できる。

## 画面まわり
- セーフエリアは SwiftUI が既定で扱うため、アプリ層で inset を意識する必要はない。
