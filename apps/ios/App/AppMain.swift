import CoreNavigation
import CoreNetwork
import CoreStorage
import FeatureGitHub
import SwiftUI

@main
struct AppMain: App {
    @State private var navigator = Navigator()
    @State private var dependencies = AppDependencies()

    var body: some Scene {
        WindowGroup {
            RootView(navigator: navigator) { route in
                Screen(route: route, dependencies: dependencies.github, navigator: navigator)
            }
            .onOpenURL { url in
                if let route = Route(url: url) {
                    navigator.reset(.opening(route, in: navigator.state.selectedTab))
                }
            }
        }
    }
}
