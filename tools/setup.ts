/**
 * clone 後のセットアップ。エージェント用スキルを apm で配置する（何度実行しても安全）。
 * apm 自体はインストールしない。先に各自で入れておくこと（README の「セットアップ」）。
 */
import { ROOT } from "./lib/util";

const apm = Bun.which("apm");
if (!apm) {
  console.error(
    "apm が見つかりません。README の「セットアップ」に沿って apm をインストールしてから再実行してください。",
  );
  process.exit(1);
}
const result = Bun.spawnSync([apm, "install"], { cwd: ROOT, stdout: "inherit", stderr: "inherit" });
process.exit(result.exitCode ?? 1);
