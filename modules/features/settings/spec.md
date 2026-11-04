# features/settings

## 概要
一覧の説明文表示を切り替え、アプリ情報を表示する。

## 画面遷移
`settings` は「設定」タブのルート。画面遷移は core/navigation の Navigator を経由する。

## 画面と状態
初回読み込み中・表示可能・保存中・失敗を持つ。説明文表示の初期値は true。読み込み・保存失敗を表示し、再試行できる。

公開入口は両 OS とも SettingsScreen とし、core/storage の DisplaySettings だけを受け取る。ViewModel の生成と画面エントリ単位の保持、画面本体は feature 内に閉じる。アプリ層の AppDependencies が GitHub 画面と同じ DisplaySettings を渡し、AppRouter が入口を呼ぶ。タブを切り替えても状態を維持する。

## ユーザー操作と結果
スイッチ操作で core/storage の DisplaySettings に保存する。書き込み成功後に全画面へ反映し、再起動後も復元する。保存中は操作を無効にし、失敗時は保存済みの値を維持する。

## 使う API とトークン
外部 API は使わない。`core/storage`、`core/navigation`、`shared/designsystem` に依存し、他 feature には依存しない。

## OS 固有の差分
モジュール名は iOS が `FeatureSettings`、Android のパッケージは `com.example.features.settings`。

iOS は Observation と SwiftUI の Form / Toggle、Android は StateFlow と Compose の Switch。公開入口は SettingsScreen、内部の描画は SettingsView / SettingsContent、状態管理は SettingsViewModel。iOS は @State、Android は Navigation 3 のエントリの ViewModelStore で ViewModel を保持する。

## シナリオ
- `specs/scenarios/settings.yaml`

## ローカライズ
UI 文言は core/localization の Message を表示時の言語で解決する。ViewModel は翻訳前のキー・引数を保持する。通信・保存の失敗は意味に応じて表示し、未知のエラーの内部メッセージは表示しない。既存の状態遷移・再試行・キャンセル動作は維持する。API の本文・名前やユーザーのメモは翻訳しない。
