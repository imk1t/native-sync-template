import Foundation

extension Route {
    /// エンコード済みパスを渡し、区切りごとのデコードを Route(path:) に一度だけ任せる。
    public init?(url: URL) {
        guard let path = URLComponents(url: url, resolvingAgainstBaseURL: false)?.percentEncodedPath else {
            return nil
        }
        self.init(path: path)
    }
}
