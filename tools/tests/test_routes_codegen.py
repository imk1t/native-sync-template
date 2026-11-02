"""shared/routes.yaml の検証と生成を確かめる。"""
import pathlib
import sys
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
import routes_codegen

BASE = {
    "tabs": [{"id": "home", "title": "ホーム", "root": "items"}],
    "routes": [
        {"name": "items", "feature": "features/items", "deepLink": "/items"},
        {"name": "itemDetail", "feature": "features/items", "params": {"id": "string"}, "deepLink": "/items/{id}"},
    ],
}


def doc(**changes):
    value = {"tabs": [dict(t) for t in BASE["tabs"]], "routes": [dict(r) for r in BASE["routes"]]}
    for key, update in changes.items():
        update(value[key])
    return value


class RoutesCodegenTests(unittest.TestCase):
    def test_generates_matching_swift_and_kotlin(self):
        model = routes_codegen.load(BASE)
        swift = routes_codegen.swift(model)
        kotlin = routes_codegen.kotlin(model, "com.example.corenavigation")
        self.assertIn("case itemDetail(id: String)", swift)
        self.assertIn("data class ItemDetail(val id: String) : Route", kotlin)
        self.assertIn('case .home: "ホーム"', swift)
        self.assertIn('Home(Route.Items, "ホーム"),', kotlin)

    def test_literal_paths_are_matched_before_parameters(self):
        model = routes_codegen.load(doc(routes=lambda r: r.insert(0, {
            "name": "itemEdit", "feature": "features/items", "params": {"id": "string"}, "deepLink": "/items/{id}/edit",
        }) or r.append({"name": "newItem", "feature": "features/items", "deepLink": "/items/new"})))
        swift = routes_codegen.swift(model)
        self.assertLess(swift.index('segments[1] == "new"'), swift.index("self = .itemDetail"))

    def test_integer_params(self):
        model = routes_codegen.load(doc(routes=lambda r: r[1].update(params={"id": "integer"})))
        self.assertIn("let id = Int(segments[1])", routes_codegen.swift(model))
        self.assertIn("ItemDetail(segments[1].toLong())", routes_codegen.kotlin(model, "p"))
        self.assertIn("val id: Long", routes_codegen.kotlin(model, "p"))

    def test_rejects_invalid_definitions(self):
        cases = {
            "重複": doc(routes=lambda r: r.append(dict(r[0]))),
            "引数の型": doc(routes=lambda r: r[1].update(params={"id": "object"})),
            "deepLink と params": doc(routes=lambda r: r[1].update(deepLink="/items/{other}")),
            "同じ形のパス": doc(routes=lambda r: r.append(
                {"name": "other", "feature": "features/items", "params": {"key": "string"}, "deepLink": "/items/{key}"})),
            "引数のあるタブの先頭": doc(tabs=lambda t: t[0].update(root="itemDetail")),
            "存在しないタブの先頭": doc(tabs=lambda t: t[0].update(root="missing")),
            "features 以外": doc(routes=lambda r: r[0].update(feature="core/network")),
            "名前の形": doc(routes=lambda r: r[0].update(name="Items")),
            "未知のキー": doc(routes=lambda r: r[0].update(presentation="sheet")),
        }
        for label, value in cases.items():
            with self.subTest(label), self.assertRaises(ValueError):
                routes_codegen.load(value)

    def test_feature_spec_must_reference_route(self):
        with tempfile.TemporaryDirectory() as temp:
            root = pathlib.Path(temp)
            spec = root / "modules/features/items/spec.md"
            spec.parent.mkdir(parents=True)
            spec.write_text("`items`\n")
            with self.assertRaisesRegex(ValueError, "itemDetail"):
                routes_codegen.load(BASE, root)
            spec.write_text("`items` `itemDetail`\n")
            routes_codegen.load(BASE, root)

    def test_repository_routes_are_valid(self):
        import yaml
        routes_codegen.load(yaml.safe_load((ROOT / "shared/routes.yaml").read_text(encoding="utf-8")), ROOT)


if __name__ == "__main__":
    unittest.main()
