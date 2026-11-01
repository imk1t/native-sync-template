"""一時 Git リポジトリで生成差分チェックの見逃しを検証する。"""
import json
import pathlib
import shutil
import subprocess
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
OUTPUTS = (
    "modules/designsystem/ios/Sources/DesignSystem/Generated/Tokens.swift",
    "modules/designsystem/android/src/main/kotlin/com/example/designsystem/generated/Tokens.kt",
)


class CheckGeneratedTests(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.repo = pathlib.Path(temp.name)
        for name in ("tools/generate.py", "tools/check_generated.sh", "shared/tokens/tokens.json", *OUTPUTS):
            dest = self.repo / name
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(ROOT / name, dest)
        self.git("init")
        self.git("add", ".")
        self.commit()

    def git(self, *args):
        return subprocess.run(
            ["git", "-c", "user.name=Test", "-c", "user.email=test@example.invalid",
             "-c", "commit.gpgsign=false", "-c", "core.hooksPath=/dev/null", *args],
            cwd=self.repo, check=True, capture_output=True, text=True,
        )

    def commit(self):
        self.git("commit", "-m", "Test fixture")

    def check_generated(self):
        return subprocess.run(
            ["bash", "tools/check_generated.sh"], cwd=self.repo,
            capture_output=True, text=True,
        )

    def test_current_outputs_pass(self):
        result = self.check_generated()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_committed_token_change_without_regeneration_fails(self):
        path = self.repo / "shared/tokens/tokens.json"
        tokens = json.loads(path.read_text())
        tokens["spacing"]["medium"]["$value"] = "20"
        path.write_text(json.dumps(tokens))
        self.git("add", ".")
        self.commit()
        self.assertEqual(self.check_generated().returncode, 1)

    def test_committed_deletion_of_either_output_fails(self):
        for output in OUTPUTS:
            with self.subTest(output=output):
                baseline = self.git("rev-parse", "HEAD").stdout.strip()
                self.git("rm", output)
                self.commit()
                result = self.check_generated()
                self.assertEqual(result.returncode, 1, result.stdout + result.stderr)
                self.assertIn("?? " + output, result.stdout)
                self.git("reset", "--hard", baseline)

    def test_staged_corruption_fails_even_when_generator_repairs_worktree(self):
        path = self.repo / OUTPUTS[0]
        path.write_text("// stale output\n")
        self.git("add", OUTPUTS[0])
        self.assertEqual(self.check_generated().returncode, 1)


if __name__ == "__main__":
    unittest.main()
