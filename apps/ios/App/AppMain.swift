import CoreFont
import CoreNavigation
import SwiftUI

@main
struct AppMain: App {
    @State private var navigator = Navigator()
    @State private var dependencies = AppDependencies()

    init() {
        // 同梱の書体を、最初の画面を作る前に登録する。
        FontRegistry.registerAll()
    }

    var body: some Scene {
        WindowGroup {
            AppRoot(navigator: navigator) { route in
                AppRouter(route: route, dependencies: dependencies, navigator: navigator)
            }
            .onOpenURL { url in
                if let route = Route(url: url) {
                    navigator.reset(.opening(route, in: navigator.state.selectedTab))
                }
            }
        }
    }
}
