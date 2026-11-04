import CoreStorage
import Observation

@MainActor
@Observable
final class SettingsViewModel {
    let settings: DisplaySettings
    var showDescriptions = true
    private(set) var isLoaded = false
    private(set) var isSaving = false
    private(set) var error: String?

    init(settings: DisplaySettings) { self.settings = settings }
    func load() async {
        do {
            try await settings.load()
            showDescriptions = settings.showDescriptions
            isLoaded = true
            error = nil
        } catch { self.error = "設定を読み込めませんでした。再試行してください。" }
    }
    func save() async {
        guard isLoaded, !isSaving, showDescriptions != settings.showDescriptions else { return }
        isSaving = true
        defer { isSaving = false }
        do {
            try await settings.setShowDescriptions(showDescriptions)
            error = nil
        } catch {
            showDescriptions = settings.showDescriptions
            self.error = "設定を保存できませんでした。再試行してください。"
        }
    }
}
