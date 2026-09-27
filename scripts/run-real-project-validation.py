# -*- coding: utf-8 -*-
"""
H1：真实项目验证。
对给定项目列表逐一执行 MODULE 模式只读审查，汇总 S16 结果。

用法一（命令行）：
  python scripts/run-real-project-validation.py \
      --project seckill=../seckill \
      --project feedly=../feedly

用法二（本地配置文件 real-projects.local.json）：
  {
    "projects": [
      {"name": "seckill", "path": "../seckill"},
      {"name": "feedly", "path": "../feedly"}
    ]
  }
  python scripts/run-real-project-validation.py

说明：
- 路径可为绝对或相对；相对路径以项目根为基准。
- 本地配置文件 real-projects.local.json 已被 .gitignore 屏蔽。
"""
import argparse
import json
import subprocess
import sys
import time
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LOCAL_CONFIG = ROOT / "real-projects.local.json"
OUT_BASE = ROOT / "reports/real-projects"


def find_jar():
    target = ROOT / "target"
    jars = [j for j in sorted(target.glob("spring-review-skill-*.jar"))
            if "sources" not in j.name
            and "javadoc" not in j.name
            and not j.name.endswith(".original")]
    if not jars:
        print("[FATAL] 未找到 target/spring-review-skill-*.jar；"
              "请先 mvn -q -DskipTests package", file=sys.stderr)
        sys.exit(2)
    return jars[-1]


def parse_args():
    p = argparse.ArgumentParser(description="真实项目验证")
    p.add_argument("--project", action="append", default=[],
                   metavar="NAME=PATH",
                   help="项目，可重复；NAME=PATH，如 --project seckill=../seckill")
    p.add_argument("--config", default=None,
                   help="本地配置文件路径（默认 real-projects.local.json）")
    return p.parse_args()


def load_projects(args):
    projects = []
    for spec in args.project:
        if "=" not in spec:
            print(f"[ERROR] --project 格式应为 NAME=PATH，实际：{spec}",
                  file=sys.stderr)
            sys.exit(2)
        name, path = spec.split("=", 1)
        projects.append({"name": name.strip(), "path": path.strip()})

    if projects:
        return projects

    cfg = Path(args.config).resolve() if args.config else LOCAL_CONFIG
    if cfg.exists():
        try:
            data = json.loads(cfg.read_text(encoding="utf-8"))
        except Exception as ex:
            print(f"[FATAL] 读取 {cfg} 失败：{ex}", file=sys.stderr)
            sys.exit(2)
        for item in data.get("projects", []):
            projects.append({
                "name": item["name"],
                "path": item["path"],
            })
    return projects


def resolve_path(p):
    path = Path(p)
    if not path.is_absolute():
        path = (ROOT / path).resolve()
    return path


def detect_module(project_path: Path):
    for cand in ("src/main/java", "src", "."):
        p = project_path / cand
        if p.exists() and p.is_dir():
            return cand
    return None


def run_one(jar: Path, name: str, project_path: Path):
    if not project_path.exists():
        return {"name": name, "path": str(project_path),
                "status": "MISSING", "error": "项目路径不存在"}

    module = detect_module(project_path)
    if module is None:
        return {"name": name, "path": str(project_path),
                "status": "NO_SOURCE", "error": "未找到可审查的源码目录"}

    out_dir = OUT_BASE / name
    out_dir.mkdir(parents=True, exist_ok=True)

    cmd = [
        "java", "-Dfile.encoding=UTF-8",
        "-jar", str(jar),
        "--mode", "MODULE",
        "--repo", str(project_path),
        "--module", module,
        "--offline",
        "--output-dir", str(out_dir),
        "--log-level", "WARN",
    ]

    print(f"[{name}] module={module}, repo={project_path}")
    t0 = time.perf_counter()
    try:
        proc = subprocess.run(cmd, capture_output=True, text=True,
                              encoding="utf-8", errors="replace", timeout=300)
    except subprocess.TimeoutExpired:
        return {"name": name, "path": str(project_path), "module": module,
                "status": "TIMEOUT",
                "duration_ms": int((time.perf_counter() - t0) * 1000)}

    duration_ms = int((time.perf_counter() - t0) * 1000)
    report_path = out_dir / "latest/review-report.json"
    if not report_path.exists():
        tail = "\n".join((proc.stderr or "").splitlines()[-10:])
        return {"name": name, "path": str(project_path), "module": module,
                "status": "NO_REPORT", "exit_code": proc.returncode,
                "duration_ms": duration_ms, "stderr_tail": tail}

    report = json.loads(report_path.read_text(encoding="utf-8"))
    summary = report.get("summary", {})
    metrics = report.get("metrics", {})
    tool_runs = report.get("tool_runs", [])
    issues = report.get("issues", [])

    rule_counts = {}
    for i in issues:
        rid = i.get("rule_id", "?")
        rule_counts[rid] = rule_counts.get(rid, 0) + 1

    return {
        "name": name, "path": str(project_path), "module": module,
        "status": "OK", "exit_code": proc.returncode,
        "duration_ms": duration_ms,
        "report_id": report.get("report_id"),
        "files_scanned": metrics.get("total_files_scanned", 0),
        "lines_scanned": metrics.get("total_lines_scanned", 0),
        "total_issues": summary.get("total_issues", 0),
        "by_severity": summary.get("by_severity", {}),
        "by_category": summary.get("by_category", {}),
        "blocking": summary.get("blocking", 0),
        "tool_runs": [
            {"tool": t.get("tool"), "status": t.get("status"),
             "duration_ms": t.get("duration_ms"),
             "issues_count": t.get("issues_count", 0)}
            for t in tool_runs
        ],
        "rule_counts": rule_counts,
    }


def main():
    args = parse_args()
    projects = load_projects(args)
    if not projects:
        print("未指定项目。用法：", file=sys.stderr)
        print("  python scripts/run-real-project-validation.py "
              "--project NAME=PATH [--project NAME=PATH ...]", file=sys.stderr)
        print("或在 real-projects.local.json 中配置 projects 数组。",
              file=sys.stderr)
        sys.exit(2)

    jar = find_jar()
    OUT_BASE.mkdir(parents=True, exist_ok=True)

    results = []
    for proj in projects:
        path = resolve_path(proj["path"])
        r = run_one(jar, proj["name"], path)
        results.append(r)
        if r["status"] == "OK":
            print(f"[{r['name']}] OK  issues={r['total_issues']} "
                  f"files={r['files_scanned']} duration={r['duration_ms']}ms")
        else:
            print(f"[{r['name']}] {r['status']}: {r.get('error', '')}")

    ok_count = sum(1 for r in results if r["status"] == "OK")
    s16_pass = ok_count >= 3

    summary = {
        "generated_at": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "projects": results,
        "ok_count": ok_count,
        "target": ">=3",
        "s16_pass": s16_pass,
    }
    with open(OUT_BASE / "validation-result.json", "w",
              encoding="utf-8", newline="\n") as f:
        f.write(json.dumps(summary, ensure_ascii=False, indent=2))

    md = [
        "# H1 真实项目验证结果",
        "",
        f"生成时间：{summary['generated_at']}",
        "",
        f"- 验证项目数：{len(results)}",
        f"- 成功完成端到端审查：{ok_count}",
        f"- S16 目标：≥ 3  |  结果：**{'PASS' if s16_pass else 'FAIL'}**",
        "",
        "## 逐项目结果",
        "",
        "| 项目 | 状态 | 文件数 | 行数 | 问题数 | BLOCKER | CRITICAL | MAJOR | MINOR | 耗时(ms) |",
        "|------|------|-------|------|-------|---------|----------|-------|-------|---------|",
    ]
    for r in results:
        if r["status"] == "OK":
            bs = r["by_severity"]
            md.append(f"| {r['name']} | {r['status']} | {r['files_scanned']} | "
                      f"{r['lines_scanned']} | {r['total_issues']} | "
                      f"{bs.get('BLOCKER', 0)} | {bs.get('CRITICAL', 0)} | "
                      f"{bs.get('MAJOR', 0)} | {bs.get('MINOR', 0)} | {r['duration_ms']} |")
        else:
            md.append(f"| {r['name']} | {r['status']} | — | — | — | — | — | — | — | — |")

    md += ["", "## 逐项目规则命中 Top", ""]
    for r in results:
        if r["status"] != "OK":
            continue
        md.append(f"### {r['name']}")
        md.append("")
        if not r["rule_counts"]:
            md.append("_无问题。_")
            md.append("")
            continue
        md.append("| rule_id | 命中数 |")
        md.append("|---------|--------|")
        for rid, count in sorted(r["rule_counts"].items(), key=lambda kv: -kv[1]):
            md.append(f"| {rid} | {count} |")
        md.append("")

    md += ["", "## 说明", "",
           "- 每个项目执行 `--mode MODULE --offline` 只读审查，不修改目标仓库。",
           "- 工具链未下载时 Checkstyle/PMD 为 SKIPPED；不影响审查闭环。",
           "- 报告落盘在 `reports/real-projects/<name>/`，含 JSON + Markdown。",
           ""]

    with open(OUT_BASE / "validation-report.md", "w",
              encoding="utf-8", newline="\n") as f:
        f.write("\n".join(md))

    print()
    print(f"OK: {ok_count}/{len(results)}  S16: {'PASS' if s16_pass else 'FAIL'}")
    print(f"Report: {OUT_BASE / 'validation-report.md'}")


if __name__ == "__main__":
    main()