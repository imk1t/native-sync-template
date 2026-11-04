import CoreNavigation
import SwiftUI

@main
struct AppMain: App {
    @State private var navigator = Navigator()
    @State private var dependencies = AppDependencies()

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
