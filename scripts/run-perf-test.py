# -*- coding: utf-8 -*-
"""
H3：性能测试。
- 对 1000 行 diff 反复跑审查，统计 P95 耗时。
- 目标：P95 ≤ 60s。
用法：
  python scripts/gen-perf-fixture.py
  python scripts/run-perf-test.py
"""
import json
import statistics
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
REPO = ROOT / "target/perf-fixture/repo"
OUT_DIR = ROOT / "target/perf-work"
REPORTS = ROOT / "reports/evaluation"

ITERATIONS = 10
WARMUP = 2
TARGET_P95_MS = 60_000


def find_jar():
    target = ROOT / "target"
    jars = [j for j in sorted(target.glob("spring-review-skill-*.jar"))
            if "sources" not in j.name and "javadoc" not in j.name]
    if not jars:
        print("[FATAL] 未找到 target/spring-review-skill-*.jar", file=sys.stderr)
        sys.exit(2)
    return jars[-1]


def run_once(jar, run_idx):
    out_dir = OUT_DIR / f"run-{run_idx:02d}"
    out_dir.mkdir(parents=True, exist_ok=True)
    cmd = [
        "java",
        "-Dfile.encoding=UTF-8",
        "-jar", str(jar),
        "--mode", "DIFF",
        "--repo", str(REPO),
        "--base", "HEAD~1",
        "--head", "HEAD",
        "--offline",
        "--output-dir", str(out_dir),
        "--log-level", "WARN",
    ]
    t0 = time.perf_counter()
    proc = subprocess.run(cmd, capture_output=True, text=True,
                          encoding="utf-8", errors="replace", timeout=120)
    elapsed_ms = int((time.perf_counter() - t0) * 1000)
    report_path = out_dir / "latest/review-report.json"
    # rc=0：审查成功，未达 failOn
    # rc=1：审查成功，达到 failOn（预期行为，视为成功）
    # rc>=2：真实失败
    ok = proc.returncode in (0, 1) and report_path.exists()
    return elapsed_ms, ok, proc.returncode


def percentile(values, p):
    if not values:
        return 0
    s = sorted(values)
    k = (len(s) - 1) * p
    f = int(k)
    c = min(f + 1, len(s) - 1)
    if f == c:
        return s[f]
    return s[f] + (s[c] - s[f]) * (k - f)


def main():
    if not REPO.exists():
        print("[FATAL] 未生成夹具，请先运行 scripts/gen-perf-fixture.py", file=sys.stderr)
        sys.exit(2)
    jar = find_jar()
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    REPORTS.mkdir(parents=True, exist_ok=True)

    all_durations = []
    failed_runs = 0
    fail_on_runs = 0
    total_runs = WARMUP + ITERATIONS

    for i in range(total_runs):
        ms, ok, rc = run_once(jar, i)
        if rc == 1:
            fail_on_runs += 1
        tag = "warmup" if i < WARMUP else "measure"
        status = "OK" if ok else f"FAIL(rc={rc})"
        print(f"[{i+1:02d}/{total_runs}] {tag}: {ms}ms {status}")
        if i >= WARMUP:
            if ok:
                all_durations.append(ms)
            else:
                failed_runs += 1

    if not all_durations:
        print("[FATAL] 无成功测量", file=sys.stderr)
        sys.exit(1)

    p50 = percentile(all_durations, 0.50)
    p95 = percentile(all_durations, 0.95)
    p99 = percentile(all_durations, 0.99)
    mean = statistics.mean(all_durations)
    mn = min(all_durations)
    mx = max(all_durations)

    passed = p95 <= TARGET_P95_MS

    result = {
        "iterations": ITERATIONS,
        "warmup": WARMUP,
        "failed_runs": failed_runs,
        "fail_on_runs": fail_on_runs,
        "durations_ms": all_durations,
        "min_ms": mn,
        "max_ms": mx,
        "mean_ms": int(mean),
        "p50_ms": int(p50),
        "p95_ms": int(p95),
        "p99_ms": int(p99),
        "target_p95_ms": TARGET_P95_MS,
        "s7_pass": passed,
    }

    (REPORTS / "perf-result.json").write_text(
        json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")

    md = [
        "# 性能测试结果",
        "",
        f"- 迭代次数：{ITERATIONS}（预热 {WARMUP} 次已剔除）",
        f"- 真实失败次数（rc≥2）：{failed_runs}",
        f"- 达到 failOn 次数（rc=1）：{fail_on_runs}",
        f"- 最小值：{mn} ms",
        f"- 最大值：{mx} ms",
        f"- 平均值：{int(mean)} ms",
        f"- P50：{int(p50)} ms",
        f"- P95：{int(p95)} ms  （S7 目标 ≤ {TARGET_P95_MS} ms：{'PASS' if passed else 'FAIL'}）",
        f"- P99：{int(p99)} ms",
        "",
    ]
    with open(REPORTS / "perf-report.md", "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(md))

    print()
    print(f"P50: {int(p50)} ms")
    print(f"P95: {int(p95)} ms  (S7 <= {TARGET_P95_MS} ms: {'PASS' if passed else 'FAIL'})")
    print(f"P99: {int(p99)} ms")
    print(f"Report: {REPORTS / 'perf-report.md'}")
    sys.exit(0 if passed else 1)


if __name__ == "__main__":
    main()