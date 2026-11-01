"""ビルド定義からモジュール一覧を読む。未対応の構文は黙って無視せず失敗する。"""
import pathlib
import re


def module_path(value):
    path = pathlib.PurePosixPath(value)
    if path.is_absolute() or ".." in path.parts or not path.parts:
        raise ValueError(f"不正なモジュールパス: {value}")
    return str(path)


def ios_targets(root):
    targets = {}
    text = (root / "modules/Package.swift").read_text(encoding="utf-8")
    for line in text.splitlines():
        if not line.strip().startswith(".target("):
            continue
        row = re.fullmatch(
            r'\s*\.target\(\s*name:\s*"([^"]+)"\s*,\s*'
            r'(?:dependencies:\s*\[(.*)\]\s*,\s*)?path:\s*"([^"]+)"\s*\)\s*,?\s*', line,
        )
        if not row:
            raise ValueError(f"iOS target は name・dependencies・path の順に1行で記述してください: {line.strip()}")
        name, deps, path = row.groups()
        mod, separator, _ = path.partition("/ios/")
        if not separator:
            raise ValueError(f"iOS target の path に /ios/ がありません: {path}")
        if name in targets:
            raise ValueError(f"重複した iOS target: {name}")
        targets[name] = (module_path(mod), deps or "")
    if not targets:
        raise ValueError("iOS target がありません")
    return targets


def android_modules(root):
    text = (root / "apps/android/settings.gradle.kts").read_text(encoding="utf-8")
    modules = {}
    for line in text.splitlines():
        if not line.strip().startswith("module("):
            continue
        row = re.fullmatch(r'\s*module\("(:[\w:]+)",\s*"([^"]+)"\)\s*', line)
        if not row:
            raise ValueError(f"Android module は1行で記述してください: {line.strip()}")
        name, path = row.groups()
        if name in modules or path in modules.values():
            raise ValueError(f"重複した Android module: {name}, {path}")
        modules[name] = module_path(path)
    if not modules:
        raise ValueError("Android module がありません")
    return modules
