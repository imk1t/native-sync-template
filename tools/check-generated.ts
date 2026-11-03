/** 再生成して差分が出ないことを確認する。 */
import { dirname } from "node:path";
import { generate, OUTPUTS } from "./generate";
import { ROOT, sorted } from "./lib/util";

export function main(root: string = ROOT): number {
  generate(root);
  // diff だけでは、削除後に再生成された未追跡ファイルを検出できない。
  const dirs = sorted(new Set(Object.values(OUTPUTS).map((path) => dirname(path))));
  const result = Bun.spawnSync(["git", "status", "--porcelain", "--untracked-files=all", "--", ...dirs], {
    cwd: root,
    stderr: "inherit",
  });
  if (result.exitCode !== 0) return 1;
  const changes = result.stdout.toString().trimEnd();
  if (changes) {
    console.log(changes);
    console.log("生成コードが最新ではありません。bun tools/generate.ts を実行してコミットしてください");
    return 1;
  }
  return 0;
}

if (import.meta.main) process.exit(main());
