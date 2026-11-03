import CoreNetwork
import Foundation

/// 初回だけキャンセルする。lateResponse はキャンセルを無視して応答する API を再現する。
actor CancelledOnceAPI: ItemsAPI {
    enum Mode: CaseIterable, Sendable { case cancellationError, urlCancellation, lateResponse }
    private let mode: Mode
    private(set) var calls = 0

    init(mode: Mode) { self.mode = mode }

    func listItems() async throws -> [Item] { [try next()] }
    func getItem(id: String) async throws -> Item { try next() }

    private func next() throws -> Item {
        calls += 1
        if calls == 1 {
            switch mode {
            case .cancellationError: throw CancellationError()
            case .urlCancellation: throw URLError(.cancelled)
            case .lateResponse: withUnsafeCurrentTask { $0?.cancel() }
            }
        }
        return Item(id: "1", title: "First")
    }
}
