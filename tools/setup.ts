/**
 * clone 後のセットアップ。エージェント用スキルを apm で配置する（何度実行しても安全）。
 * apm は mise.toml で版を固定している。先に `mise install` で入れておくこと（README の「セットアップ」）。
 */
import { ROOT } from "./lib/util";

const apm = Bun.which("apm");
if (!apm) {
  console.error("apm が見つかりません。`mise install` で apm を入れてから、`mise run setup` で再実行してください。");
  process.exit(1);
}
const result = Bun.spawnSync([apm, "install"], { cwd: ROOT, stdout: "inherit", stderr: "inherit" });
process.exit(result.exitCode ?? 1);
