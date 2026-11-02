"""ビルド定義・spec・外部 SDK の境界を一時コピーで検証する。"""
import contextlib
import io
import json
import pathlib
import shutil
import sys
import tempfile
import unittest
from unittest.mock import patch

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
import check_deps
import check_spec_sync


class RepoChecksTests(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = pathlib.Path(temp.name)
        shutil.copytree(ROOT / "modules", self.root / "modules",
                        ignore=shutil.ignore_patterns(".build", "build", ".gradle"))
        for name in ("apps/android/settings.gradle.kts", "apps/android/gradle/libs.versions.toml", "shared/third-party.yaml"):
            dest = self.root / name
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(ROOT / name, dest)
        for module in (check_deps, check_spec_sync):
            patches = patch.multiple(module, ROOT=self.root, MODULES=self.root / "modules")
            patches.start()
            self.addCleanup(patches.stop)

    def replace(self, name, before, after):
        path = self.root / name
        text = path.read_text()
        self.assertIn(before, text)
        path.write_text(text.replace(before, after))

    def deps_result(self):
        output = io.StringIO()
        with contextlib.redirect_stdout(output):
            result = check_deps.main()
        return result, output.getvalue()

    def add_sdk(self, mod="core/network", register=True):
        target = {"core/network": "CoreNetwork", "core/utils": "CoreUtils", "features/items": "FeatureItems"}[mod]
        manifest = self.root / "modules/Package.swift"
        rows = manifest.read_text().splitlines()
        for index, row in enumerate(rows):
            if f'.target(name: "{target}",' in row:
                if "dependencies: [" in row:
                    row = row.replace("dependencies: [", 'dependencies: [.product(name: "ExampleSDK", package: "example-sdk"), ')
                else:
                    row = row.replace('path:', 'dependencies: [.product(name: "ExampleSDK", package: "example-sdk")], path:')
                rows[index] = row
        manifest.write_text("\n".join(rows) + "\n")
        catalog = self.root / "apps/android/gradle/libs.versions.toml"
        catalog.write_text(catalog.read_text().replace("[libraries]", '[libraries]\nexample-sdk = { module = "com.example:sdk", version = "1.0" }'))
        build = self.root / "modules" / mod / "android/build.gradle.kts"
        build.write_text(build.read_text() + '\ndependencies { implementation(libs.example.sdk) }\n')
        if register:
            entry = {"role": "example", "module": mod, "owner": "platform",
                     "ios": {"name": "example-sdk", "license": "Apache-2.0"},
                     "android": {"name": "example-sdk", "license": "Apache-2.0"}}
            (self.root / "shared/third-party.yaml").write_text(json.dumps({"libraries": [entry]}))

    def test_current_repository_passes(self):
        self.assertEqual(self.deps_result()[0], 0)
        self.assertEqual(check_spec_sync.check(), [])

    def test_registered_sdk_product_is_separate_from_internal_graph(self):
        self.add_sdk()
        result, output = self.deps_result()
        self.assertEqual(result, 0, output)
        graph, external = check_deps.ios_dependencies()
        self.assertEqual(graph["core/network"], {"core/utils"})
        self.assertEqual(external, {("core/network", "example-sdk")})

    def test_sdk_requires_both_platforms_in_registry(self):
        self.add_sdk(register=False)
        result, output = self.deps_result()
        self.assertEqual(result, 1)
        self.assertIn("ios: core/network", output)
        self.assertIn("android: core/network", output)

    def test_sdk_cannot_be_added_directly_to_feature_or_utils(self):
        for mod in ("features/items", "core/utils"):
            with self.subTest(mod=mod):
                # 各ケースで外部依存だけを追加・検査する。
                errors = check_deps.check_external({"ios": {(mod, "example-sdk")}, "android": set()})
                self.assertTrue(any("直接依存できません" in error for error in errors))

    def test_registry_entry_with_missing_android_dependency_fails(self):
        self.add_sdk()
        self.replace("modules/core/network/android/build.gradle.kts", "implementation(libs.example.sdk)", "")
        result, output = self.deps_result()
        self.assertEqual(result, 1)
        self.assertIn("実際の依存にありません", output)

    def test_unknown_internal_target_fails_with_actionable_error(self):
        self.replace("modules/Package.swift", 'dependencies: ["CoreUtils"]', 'dependencies: ["Missing"]')
        result, output = self.deps_result()
        self.assertEqual(result, 1)
        self.assertIn("不明な内部 target Missing", output)

    def test_named_internal_target_dependency_passes(self):
        self.replace("modules/Package.swift", 'dependencies: ["CoreUtils"]', 'dependencies: [.target(name: "CoreUtils")]')
        self.assertEqual(self.deps_result()[0], 0)

    def test_unsupported_swift_dependency_is_not_silently_ignored(self):
        self.replace("modules/Package.swift", 'dependencies: ["CoreUtils"]', 'dependencies: externalDependencies')
        result, output = self.deps_result()
        self.assertEqual(result, 1)
        self.assertIn("1行で記述", output)

    def test_android_project_names_are_resolved_through_settings(self):
        self.replace("apps/android/settings.gradle.kts", '":core:utils"', '":utilities"')
        self.replace("modules/core/network/android/build.gradle.kts", 'project(":core:utils")', 'project(path = ":utilities")')
        self.assertEqual(self.deps_result()[0], 0)

    def test_android_unregistered_build_script_fails(self):
        self.replace("apps/android/settings.gradle.kts", 'module(":core:utils", "core/utils")', "")
        self.assertEqual(self.deps_result()[0], 1)

    def test_utils_cannot_depend_on_official_android_libraries(self):
        build = self.root / "modules/core/utils/android/build.gradle.kts"
        build.write_text(build.read_text() + '\ndependencies { implementation(libs.coroutines.core) }\n')
        result, output = self.deps_result()
        self.assertEqual(result, 1)
        self.assertIn("直接依存できません", output)

    def test_deleted_spec_is_detected_for_every_module(self):
        for spec in (self.root / "modules").rglob("spec.md"):
            original = spec.read_text()
            spec.unlink()
            with self.subTest(module=str(spec.parent)):
                self.assertTrue(any("spec.md がありません" in error for error in check_spec_sync.check()))
            spec.write_text(original)

    def test_build_registration_detects_module_even_if_implementations_are_deleted(self):
        shutil.rmtree(self.root / "modules/features/items")
        self.assertIn("modules/features/items: spec.md がありません", check_spec_sync.check())

    def test_unregistered_implementation_requires_spec(self):
        (self.root / "modules/features/orphan/ios").mkdir(parents=True)
        self.assertIn("modules/features/orphan: spec.md がありません", check_spec_sync.check())

    def test_spec_changes_still_require_both_markers(self):
        spec = self.root / "modules/features/items/spec.md"
        spec.write_text(spec.read_text() + "\n変更\n")
        errors = check_spec_sync.check()
        self.assertEqual(sum("追従していません" in error for error in errors), 2)


if __name__ == "__main__":
    unittest.main()
