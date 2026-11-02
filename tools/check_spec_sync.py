#!/usr/bin/env python3
"""各モジュールの spec.md と、ios/・android/ の .spec-sync（追従済み spec のハッシュ）を照合する。

  check_spec_sync.py           ズレがあれば失敗（CI 用）
  check_spec_sync.py --update ios|android|both <module-path>...   指定モジュールの追従を記録する
"""
import hashlib
import pathlib
import sys
from module_manifest import android_modules, ios_targets

ROOT = pathlib.Path(__file__).resolve().parent.parent
MODULES = ROOT / "modules"
PLATFORMS = ("ios", "android")


def spec_hash(spec):
    text = spec.read_text(encoding="utf-8").replace("\r\n", "\n")
    return hashlib.sha256(text.encode("utf-8")).hexdigest()[:16]


def specs():
    # spec を消しても、ビルド定義や実装ディレクトリに残るモジュールを検査する。
    modules = {p.parent for p in MODULES.rglob("spec.md")}
    modules.update(MODULES / mod for mod, _ in ios_targets(ROOT).values())
    modules.update(MODULES / mod for mod in android_modules(ROOT).values())
    for plat in PLATFORMS:
        modules.update(p.parent for p in MODULES.rglob(plat)
                       if p.is_dir() and not any(part.startswith(".") for part in p.relative_to(MODULES).parts))
    return sorted(modules)


def check():
    errors = []
    for mod in specs():
        rel = mod.relative_to(ROOT)
        if not (mod / "spec.md").is_file():
            errors.append(f"{rel}: spec.md がありません")
            continue
        expected = spec_hash(mod / "spec.md")
        for plat in PLATFORMS:
            marker = mod / plat / ".spec-sync"
            if not (mod / plat).is_dir():
                errors.append(f"{rel}: {plat}/ がありません")
            elif not marker.exists():
                errors.append(f"{rel}: {plat}/.spec-sync がありません（追従後に --update で記録）")
            elif marker.read_text().strip() != expected:
                errors.append(f"{rel}: {plat} が spec.md の最新に追従していません")
    return errors


def update(plats, paths):
    for p in paths:
        mod = (ROOT / p).resolve()
        if not (mod / "spec.md").exists():
            sys.exit(f"spec.md がありません: {p}")
        for plat in plats:
            (mod / plat / ".spec-sync").write_text(spec_hash(mod / "spec.md") + "\n")
            print(f"updated {mod.relative_to(ROOT)}/{plat}/.spec-sync")


if __name__ == "__main__":
    args = sys.argv[1:]
    if args and args[0] == "--update":
        if len(args) < 3 or args[1] not in ("ios", "android", "both"):
            sys.exit("usage: --update ios|android|both <module-path>...")
        update(PLATFORMS if args[1] == "both" else (args[1],), args[2:])
        sys.exit(0)
    try:
        errs = check()
    except (ValueError, OSError) as error:
        sys.exit(f"ERROR {error}")
    for e in errs:
        print("ERROR", e)
    print("spec sync: OK" if not errs else f"spec sync: {len(errs)} problem(s)")
    sys.exit(1 if errs else 0)
