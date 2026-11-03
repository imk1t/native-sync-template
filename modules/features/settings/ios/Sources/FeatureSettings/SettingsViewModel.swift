import CoreStorage
import Observation

@MainActor
@Observable
public final class SettingsViewModel {
    public let settings: DisplaySettings
    public var showDescriptions = true
    public private(set) var isLoaded = false
    public private(set) var isSaving = false
    public private(set) var error: String?

    public init(settings: DisplaySettings) { self.settings = settings }
    public func load() async {
        do {
            try await settings.load()
            showDescriptions = settings.showDescriptions
            isLoaded = true
            error = nil
        } catch { self.error = "設定を読み込めませんでした。再試行してください。" }
    }
    public func save() async {
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
