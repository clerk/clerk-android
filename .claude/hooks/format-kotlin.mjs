#!/usr/bin/env node
// Claude Code shows a PostToolUse hook's stderr to the agent only when the hook exits 2.
import { spawnSync } from "node:child_process";
import { existsSync, readFileSync } from "node:fs";
import { homedir } from "node:os";
import { delimiter, dirname, join } from "node:path";

const input = JSON.parse(readFileSync(0, "utf8"));
const file = input.tool_input?.file_path;
if (!file || !/\.kts?$/.test(file) || !existsSync(file)) process.exit(0);

const git = spawnSync("git", ["-C", dirname(file), "rev-parse", "--show-toplevel"], {
  encoding: "utf8",
});
if (git.status !== 0) process.exit(0);
const root = git.stdout.trim();
const catalog = join(root, "gradle/libs.versions.toml");
if (!existsSync(join(root, "config/bin/ktfmt")) || !existsSync(catalog)) process.exit(0);

const hasJava = process.env.JAVA_HOME
  ? existsSync(join(process.env.JAVA_HOME, "bin/java"))
  : (process.env.PATH ?? "").split(delimiter).some((dir) => existsSync(join(dir, "java")));
if (!hasJava) process.exit(0);

// The wrappers download a missing jar from Maven Central. Run only with jars already cached, so
// an edit never waits on a download.
const cache =
  process.env.CLERK_TOOLS_CACHE ??
  join(process.env.XDG_CACHE_HOME ?? join(homedir(), ".cache"), "clerk-android/tools");
const versions = readFileSync(catalog, "utf8");
const version = (key) => versions.match(new RegExp(`^\\s*${key}\\s*=\\s*"([^"]+)"`, "m"))?.[1];
const cached = (jar) => existsSync(join(cache, jar));

function run(tool, args) {
  const result = spawnSync(join(root, "config/bin", tool), args, { cwd: root, encoding: "utf8" });
  if (result.status === 0) return;
  process.stderr.write(`${tool} failed on ${file}:\n${result.stdout}${result.stderr}`);
  process.exit(2);
}

if (cached(`ktfmt-${version("ktfmt")}-with-dependencies.jar`)) {
  run("ktfmt", ["--google-style", file]);
}
if (cached(`detekt-cli-${version("detekt")}-all.jar`)) {
  run("detekt-cli", [
    "--auto-correct",
    "--baseline",
    join(root, "config/detekt/detekt-baseline.xml"),
    "--config",
    join(root, "config/detekt/detekt.yml"),
    "--build-upon-default-config",
    "--input",
    file,
  ]);
}
