# features/settings

## 概要
一覧の説明文表示を切り替え、アプリ情報を表示する。

## 画面遷移
`settings` は「設定」タブのルート。画面遷移は core/navigation の Navigator を経由する。

## 画面と状態
初回読み込み中・表示可能・保存中・失敗を持つ。説明文表示の初期値は true。読み込み・保存失敗を表示し、再試行できる。

## ユーザー操作と結果
スイッチ操作で core/storage の DisplaySettings に保存する。書き込み成功後に全画面へ反映し、再起動後も復元する。保存中は操作を無効にし、失敗時は保存済みの値を維持する。

## 使う API とトークン
外部 API は使わない。core/storage、core/navigation、designsystem に依存し、features/github には依存しない。

## OS 固有の差分
iOS は Observation と SwiftUI の Form / Toggle、Android は StateFlow と Compose の Switch。画面は SettingsView / SettingsScreen、状態管理は SettingsViewModel。

## シナリオ
- `shared/scenarios/settings.yaml`
