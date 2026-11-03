package com.example.corenetwork

/** プレビュー・手動テスト用。同じ生成クライアントのデコードと変換を通す。 */
class DemoGitHubTransport : HttpTransport {
    override suspend fun send(request: HttpRequest): HttpResponse {
        val body = DemoResponses.bodies[request.path] ?: return HttpResponse(404)
        return HttpResponse(200, body)
    }
}
