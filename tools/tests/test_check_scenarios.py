"""共通シナリオの検査を一時コピーで検証する。"""
import contextlib
import io
import pathlib
import shutil
import sys
import tempfile
import unittest
from unittest.mock import patch

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
import check_scenarios

SCENARIO = "shared/scenarios/items-list.yaml"
IOS_TEST = "modules/features/items/ios/Tests/FeatureItemsTests/ItemsViewModelTests.swift"
ANDROID_TEST = "modules/features/items/android/src/test/kotlin/com/example/featureitems/ItemsViewModelTest.kt"


class CheckScenariosTests(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = pathlib.Path(temp.name)
        shutil.copytree(ROOT / "shared", self.root / "shared")
        shutil.copytree(ROOT / "modules", self.root / "modules",
                        ignore=shutil.ignore_patterns(".build", "build", ".gradle"))
        patcher = patch.object(check_scenarios, "ROOT", self.root)
        patcher.start()
        self.addCleanup(patcher.stop)

    def replace(self, name, before, after):
        path = self.root / name
        text = path.read_text()
        self.assertIn(before, text)
        path.write_text(text.replace(before, after, 1))

    def run_check(self):
        output = io.StringIO()
        with contextlib.redirect_stdout(output):
            result = check_scenarios.main()
        return result, output.getvalue()

    def assertFails(self, message):
        result, output = self.run_check()
        self.assertEqual(result, 1, output)
        self.assertIn(message, output)

    def test_current_repository_passes(self):
        result, output = self.run_check()
        self.assertEqual(result, 0, output)

    def test_step_missing_on_one_platform_fails(self):
        for test, platform in ((IOS_TEST, "ios"), (ANDROID_TEST, "android")):
            with self.subTest(platform=platform):
                original = (self.root / test).read_text()
                self.replace(test, "// @scenario items-list/load-fails\n", "")
                self.assertFails(f"features/items/{platform}: `@scenario items-list/load-fails`")
                (self.root / test).write_text(original)

    def test_marker_for_unknown_step_fails(self):
        self.replace(IOS_TEST, "// @scenario items-list/load-fails", "// @scenario items-list/load-fails\n    // @scenario items-list/typo")
        self.assertFails("存在しないシナリオ step を参照しています: items-list/typo")

    def test_unknown_operation_fails(self):
        self.replace(SCENARIO, "listItems:", "listThings:")
        self.assertFails("shared/openapi に無い operationId です")

    def test_body_not_matching_schema_fails(self):
        self.replace(SCENARIO, '{ id: "1", title: "First" }', '{ id: 1, title: "First" }')
        self.assertFails("string ではありません")

    def test_duplicate_step_id_fails(self):
        self.replace(SCENARIO, "id: load-fails", "id: load-succeeds")
        self.assertFails("重複しています: load-succeeds")

    def test_spec_must_reference_scenario(self):
        self.replace("modules/features/items/spec.md", "`shared/scenarios/items-list.yaml`", "なし")
        self.assertFails("shared/scenarios/items-list.yaml を参照してください")


if __name__ == "__main__":
    unittest.main()
