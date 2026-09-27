# -*- coding: utf-8 -*-
"""
H2：基准评测集运行器。
用法：
  python scripts/run-evaluation.py
"""
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
EVAL_DIR = ROOT / "tests/evaluation"
REPORTS_DIR = ROOT / "reports/evaluation"
WORK_DIR = ROOT / "target/eval-work"

JAR_GLOB = "spring-review-skill-*.jar"


def find_jar():
    target = ROOT / "target"
    if not target.exists():
        print("[FATAL] target/ 不存在；请先 mvn -q -DskipTests package", file=sys.stderr)
        sys.exit(2)
    jars = sorted(target.glob(JAR_GLOB))
    jars = [j for j in jars if "sources" not in j.name and "javadoc" not in j.name]
    if not jars:
        print("[FATAL] 未找到 target/spring-review-skill-*.jar", file=sys.stderr)
        sys.exit(2)
    return jars[-1]


def run_case(jar, case_id, input_path):
    out_dir = WORK_DIR / case_id
    out_dir.mkdir(parents=True, exist_ok=True)
    rel_input = input_path.relative_to(ROOT).as_posix()
    cmd = [
        "java",
        "-Dfile.encoding=UTF-8",
        "-Dsun.stdout.encoding=UTF-8",
        "-Dsun.stderr.encoding=UTF-8",
        "-jar", str(jar),
        "--mode", "FILES",
        "--repo", str(ROOT),
        "--files", rel_input,
        "--offline",
        "--output-dir", str(out_dir),
        "--log-level", "WARN",
    ]
    proc = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8",
                          errors="replace", timeout=120)
    report_path = out_dir / "latest/review-report.json"
    if not report_path.exists():
        return None, proc.returncode, (proc.stderr or "") + (proc.stdout or "")
    return json.loads(report_path.read_text(encoding="utf-8")), proc.returncode, ""


def evaluate():
    jar = find_jar()
    REPORTS_DIR.mkdir(parents=True, exist_ok=True)
    WORK_DIR.mkdir(parents=True, exist_ok=True)

    expected_dir = EVAL_DIR / "expected"
    results = []

    defect_total = 0
    defect_hit = 0
    clean_total = 0
    clean_false = 0

    for expected_file in sorted(expected_dir.glob("*.json")):
        expected = json.loads(expected_file.read_text(encoding="utf-8"))
        case_id = expected["case_id"]
        input_path = EVAL_DIR / expected["input"]

        report, exit_code, stderr = run_case(jar, case_id, input_path)
        if report is None:
            tail = "\n".join((stderr or "").splitlines()[-8:])
            print(f"[ERROR] {case_id}: 报告未生成 exit={exit_code}\n{tail}")
            results.append({
                "case_id": case_id,
                "type": expected["type"],
                "error": "no report",
                "exit_code": exit_code,
            })
            if expected["type"] == "defect":
                defect_total += 1
            else:
                clean_total += 1
            continue

        fired_rules = sorted({i["rule_id"] for i in report.get("issues", [])})
        expected_rules = expected["expected_rules"]

        if expected["type"] == "defect":
            defect_total += 1
            hit = any(r in fired_rules for r in expected_rules)
            if hit:
                defect_hit += 1
            results.append({
                "case_id": case_id,
                "type": "defect",
                "expected": expected_rules,
                "fired": fired_rules,
                "hit": hit,
            })
        else:
            clean_total += 1
            false_positive = len(fired_rules) > 0
            if false_positive:
                clean_false += 1
            results.append({
                "case_id": case_id,
                "type": "clean",
                "fired": fired_rules,
                "false_positive": false_positive,
            })

    detection_rate = defect_hit / defect_total if defect_total else 0.0
    false_positive_rate = clean_false / clean_total if clean_total else 0.0

    summary = {
        "defect_total": defect_total,
        "defect_hit": defect_hit,
        "detection_rate": round(detection_rate, 4),
        "clean_total": clean_total,
        "clean_false_positive": clean_false,
        "false_positive_rate": round(false_positive_rate, 4),
        "s5_pass": detection_rate >= 0.80,
        "s6_pass": false_positive_rate <= 0.20,
        "cases": results,
    }

    out_json = REPORTS_DIR / "evaluation-result.json"
    with open(out_json, "w", encoding="utf-8", newline="\n") as f:
        f.write(json.dumps(summary, ensure_ascii=False, indent=2))

    out_md = REPORTS_DIR / "evaluation-report.md"
    with open(out_md, "w", encoding="utf-8", newline="\n") as f:
        f.write("# 基准评测结果\n\n")
        f.write(f"- 缺陷用例：{defect_total}\n")
        f.write(f"- 检出：{defect_hit}\n")
        f.write(f"- 检出率：{detection_rate:.2%}  （S5 目标 ≥ 80%：{'PASS' if summary['s5_pass'] else 'FAIL'}）\n")
        f.write(f"- 干净用例：{clean_total}\n")
        f.write(f"- 误报：{clean_false}\n")
        f.write(f"- 误报率：{false_positive_rate:.2%}  （S6 目标 ≤ 20%：{'PASS' if summary['s6_pass'] else 'FAIL'}）\n\n")
        f.write("## 逐用例\n\n")
        f.write("| case | type | expected | fired | result |\n")
        f.write("|------|------|----------|-------|--------|\n")
        for r in results:
            cid = r["case_id"]
            typ = r["type"]
            if typ == "defect":
                exp = ", ".join(r.get("expected", []))
                fired = ", ".join(r.get("fired", []))
                res = "HIT" if r.get("hit") else "MISS"
            else:
                exp = "—"
                fired = ", ".join(r.get("fired", []))
                res = "FALSE_POSITIVE" if r.get("false_positive") else "CLEAN"
            f.write(f"| {cid} | {typ} | {exp} | {fired} | {res} |\n")

    print(f"Detection rate: {detection_rate:.2%} (S5 >= 80%: {'PASS' if summary['s5_pass'] else 'FAIL'})")
    print(f"False positive rate: {false_positive_rate:.2%} (S6 <= 20%: {'PASS' if summary['s6_pass'] else 'FAIL'})")
    print(f"Details: {out_md}")
    return 0 if (summary["s5_pass"] and summary["s6_pass"]) else 1


if __name__ == "__main__":
    sys.exit(evaluate())