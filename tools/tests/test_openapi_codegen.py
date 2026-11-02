"""OpenAPI 生成器の対応範囲と、未対応構文で失敗することを検証する。"""
import pathlib
import sys
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
import openapi_codegen as gen


def doc(paths=None, schemas=None):
    return {
        "openapi": "3.1.0",
        "paths": paths or {},
        "components": {"schemas": schemas if schemas is not None else {
            "Item": {"type": "object", "required": ["id", "created_at"], "properties": {
                "id": {"type": "string"},
                "created_at": {"type": "integer", "format": "int64"},
                "tags": {"type": "array", "items": {"type": "string"}},
                "default": {"type": "boolean"},
            }},
        }},
    }


GET_ITEM = {"/items/{item_id}": {"get": {
    "operationId": "getItem",
    "parameters": [
        {"name": "item_id", "in": "path", "required": True, "schema": {"type": "string"}},
        {"name": "page_size", "in": "query", "schema": {"type": "integer"}},
    ],
    "responses": {"200": {"description": "OK", "content": {"application/json": {"schema": {"$ref": "#/components/schemas/Item"}}}}},
}}}
DELETE_ITEM = {"/items/{id}": {"delete": {
    "operationId": "delete-item",
    "parameters": [{"name": "id", "in": "path", "required": True, "schema": {"type": "string"}}],
    "responses": {"204": {"description": "No Content"}},
}}}


class ModelTests(unittest.TestCase):
    def test_snake_case_keys_are_mapped_on_both_platforms(self):
        spec = gen.Spec(doc())
        swift = gen.swift_models(spec)
        kotlin = gen.kotlin_models(spec, "p")
        self.assertIn("var createdAt: Int64", swift)
        self.assertIn('case createdAt = "created_at"', swift)
        self.assertIn("var tags: [String]?", swift)
        self.assertIn("var `default`: Bool?", swift)
        self.assertIn('@SerialName("created_at") val createdAt: Long,', kotlin)
        self.assertIn("val tags: List<String>? = null,", kotlin)
        self.assertIn("internal data class ItemDTO(", kotlin)

    def test_unsupported_schema_fails(self):
        for prop in ({"type": "string", "enum": ["a"]}, {"oneOf": []}, {"type": "object"}, {"type": ["string", "null"]}):
            with self.subTest(prop=prop), self.assertRaises(ValueError):
                gen.Spec(doc(schemas={"X": {"type": "object", "properties": {"p": prop}}}))

    def test_unknown_ref_fails(self):
        with self.assertRaises(ValueError):
            gen.Spec(doc(schemas={"X": {"type": "object", "properties": {"p": {"$ref": "#/components/schemas/Missing"}}}}))


class ClientTests(unittest.TestCase):
    def test_path_and_query_parameters(self):
        spec = gen.Spec(doc(paths=GET_ITEM))
        swift = gen.swift_client(spec)
        kotlin = gen.kotlin_client(spec, "p")
        self.assertIn("func getItem(itemId: String, pageSize: Int? = nil) async throws -> ItemDTO", swift)
        self.assertIn('path: "/items/\\(pathSegment(itemId))"', swift)
        self.assertIn('if let pageSize { query.append(URLQueryItem(name: "page_size", value: String(pageSize))) }', swift)
        self.assertIn("suspend fun getItem(itemId: String, pageSize: Int? = null): ItemDTO", kotlin)
        self.assertIn('path = "/items/${pathSegment(itemId.toString())}"', kotlin)
        self.assertIn('if (pageSize != null) query.add("page_size" to pageSize.toString())', kotlin)

    def test_no_content_response_returns_nothing(self):
        spec = gen.Spec(doc(paths=DELETE_ITEM))
        self.assertIn("func deleteItem(id: String) async throws {", gen.swift_client(spec))
        self.assertIn("try await transport.call(request)", gen.swift_client(spec))
        self.assertIn("suspend fun deleteItem(id: String) {", gen.kotlin_client(spec, "p"))
        self.assertIn("let query: [URLQueryItem] = []", gen.swift_client(spec))

    def test_json_request_body(self):
        paths = {"/items": {"post": {
            "operationId": "createItem",
            "requestBody": {"required": True, "content": {"application/json": {"schema": {"$ref": "#/components/schemas/Item"}}}},
            "responses": {"201": {"description": "Created", "content": {"application/json": {"schema": {"$ref": "#/components/schemas/Item"}}}}},
        }}}
        spec = gen.Spec(doc(paths=paths))
        self.assertIn("body: try JSONEncoder().encode(body)", gen.swift_client(spec))
        kotlin = gen.kotlin_client(spec, "p")
        self.assertIn("import kotlinx.serialization.encodeToString", kotlin)
        self.assertIn("body = ApiJson.encodeToString(body)", kotlin)

    def test_unsupported_operations_fail(self):
        bad = [
            {"/a": {"get": {"responses": {"200": {"description": "OK"}}}}},  # operationId なし
            {"/a/{id}": {"get": {"operationId": "a", "responses": {"200": {"description": "OK"}}}}},  # path パラメータ未定義
            {"/a": {"get": {"operationId": "a", "parameters": [{"name": "h", "in": "header", "schema": {"type": "string"}}],
                            "responses": {"200": {"description": "OK"}}}}},
            {"/a": {"get": {"operationId": "a", "responses": {"404": {"description": "NG"}}}}},
            {"/a": {"get": {"operationId": "a", "responses": {"200": {"description": "OK", "content": {"text/plain": {}}}}}}},
            {"/a": {"get": {"operationId": "a", "responses": {"200": {"description": "OK"}}}},
             "/b": {"get": {"operationId": "a", "responses": {"200": {"description": "OK"}}}}},
        ]
        for paths in bad:
            with self.subTest(paths=paths), self.assertRaises(ValueError):
                gen.Spec(doc(paths=paths))


class ValidateTests(unittest.TestCase):
    def test_fixture_validation(self):
        spec = gen.Spec(doc())
        item = ("ref", "Item")
        gen.validate_value(spec, item, {"id": "1", "created_at": 1}, "x")
        for value in ({"id": "1"}, {"id": 1, "created_at": 1}, {"id": "1", "created_at": 1, "extra": 0},
                      {"id": "1", "created_at": True}, [], None):
            with self.subTest(value=value), self.assertRaises(ValueError):
                gen.validate_value(spec, item, value, "x")


if __name__ == "__main__":
    unittest.main()
