import Foundation
import Testing
@testable import CoreNetwork

private actor StubTransport: HTTPTransport {
    let response: HTTPResponse
    private(set) var requests: [HTTPRequest] = []

    init(_ response: HTTPResponse) { self.response = response }

    func send(_ request: HTTPRequest) async throws -> HTTPResponse {
        requests.append(request)
        return response
    }
}

struct DefaultItemsAPITests {
    @Test func listItemsDecodesResponseIntoDomainModels() async throws {
        let json = #"[{"id":"1","title":"First","extra":true},{"id":"2","title":"Second"}]"#
        let transport = StubTransport(HTTPResponse(status: 200, body: Data(json.utf8)))
        let items = try await DefaultItemsAPI(transport: transport).listItems()
        #expect(items == [Item(id: "1", title: "First"), Item(id: "2", title: "Second")])
        #expect(await transport.requests == [HTTPRequest(method: "GET", path: "/items")])
    }

    @Test func getItemRequestsEscapedPath() async throws {
        let transport = StubTransport(HTTPResponse(status: 200, body: Data(#"{"id":"a/1","title":"First"}"#.utf8)))
        let item = try await DefaultItemsAPI(transport: transport).getItem(id: "a/1")
        #expect(item == Item(id: "a/1", title: "First"))
        #expect(await transport.requests == [HTTPRequest(method: "GET", path: "/items/a%2F1")])
    }

    @Test func nonSuccessStatusThrowsAPIError() async {
        let transport = StubTransport(HTTPResponse(status: 500))
        await #expect(throws: APIError(status: 500)) {
            try await DefaultItemsAPI(transport: transport).listItems()
        }
    }

    @Test func pathSegmentEscapesSlash() {
        #expect(pathSegment("a/b c") == "a%2Fb%20c")
    }
}
