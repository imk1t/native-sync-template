package com.example.featuregithub

/** テストで使う最小限の API レスポンス。アプリには同梱しない。 */
internal object TestResponses {
    val repository = """
        {
          "id":5000000001,"name":"Hello-World","full_name":"octocat/Hello-World",
          "description":null,"owner":{"login":"octocat","avatar_url":""},"language":null,
          "stargazers_count":0,"forks_count":0,"html_url":"https://github.com/octocat/Hello-World"
        }
    """
    val search = """
        {"total_count":1,"incomplete_results":false,"items":[$repository]}
    """
    val issue = """
        {
          "id":5000000002,"number":1,"title":"Test issue","state":"open","body":"Test body",
          "user":{"login":"octocat","avatar_url":""},"updated_at":"2026-11-03T00:00:00Z"
        }
    """
}
