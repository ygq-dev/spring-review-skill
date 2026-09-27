"""
Spring Review Skill - 规则拆分脚本

从 docs/rules/*.md 中提取所有规则 YAML，按 category_l1 分组，
写入 rules/<分类>.yaml，每条规则用 --- 分隔（多文档 YAML）。

修复：
- 分类正则支持数字（I18N）
- CON 等 Windows 保留名改写为 CON_
- 每个分类内按 rule_id 升序

用法：
    cd spring-review-skill
    python scripts/split_rules.py
"""

import re
import sys
from pathlib import Path
from collections import defaultdict

DOCS_RULES_DIR = Path("docs/rules")
RULES_DIR = Path("rules")

RULE_DOCS = [
    "AR-CF-BUILD-CLOUD-MIG.md",
    "DI-CON.md",
    "WEB-SEC.md",
    "DAO-EXC-RES.md",
    "EXC-RES.md",
    "PERF-OBS.md",
    "TEST-STYLE-NULL-I18N.md",
]

CODE_RE = re.compile(r'```yaml\s*\n(.*?)\n```', re.DOTALL)
CAT_RE = re.compile(r'^category_l1:\s*"?([A-Z0-9]+)"?\s*$', re.MULTILINE)
RULE_ID_RE = re.compile(r'^rule_id:\s*"?([A-Z0-9\-]+)"?\s*$', re.MULTILINE)

EXPECTED_CATEGORIES = {
    "AR", "CF", "DI", "WEB", "DAO", "SEC", "CON", "PERF",
    "OBS", "EXC", "RES", "TEST", "BUILD", "STYLE", "NULL",
    "I18N", "MIG", "CLOUD",
}

WIN_RESERVED = {
    "CON", "PRN", "AUX", "NUL",
    "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
    "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9",
}


def normalize_category(cat: str) -> str:
    """Windows 保留名加下划线，如 CON → CON_。"""
    if cat.upper() in WIN_RESERVED:
        return cat + "_"
    return cat


def extract_rule_id(block: str) -> str:
    m = RULE_ID_RE.search(block)
    return m.group(1) if m else ""


def main():
    if not DOCS_RULES_DIR.exists():
        print(f"错误：找不到 {DOCS_RULES_DIR.resolve()}")
        sys.exit(1)

    by_category = defaultdict(list)
    total = 0
    unknown_cat = []

    for doc_name in RULE_DOCS:
        doc_path = DOCS_RULES_DIR / doc_name
        if not doc_path.exists():
            print(f"[跳过] {doc_name} 不存在")
            continue
        text = doc_path.read_text(encoding="utf-8")
        blocks = CODE_RE.findall(text)
        matched = 0
        for block in blocks:
            block = block.strip()
            cm = CAT_RE.search(block)
            if not cm:
                continue
            cat = cm.group(1)
            if cat not in EXPECTED_CATEGORIES:
                unknown_cat.append((doc_name, cat))
                continue
            by_category[cat].append(block)
            matched += 1
            total += 1
        print(f"[提取] {doc_name}: {matched} 条规则")

    print(f"\n共提取 {total} 条规则，分布到 {len(by_category)} 个分类")

    if unknown_cat:
        print(f"警告：遇到未知分类 {unknown_cat}")

    RULES_DIR.mkdir(exist_ok=True)
    written = 0
    for cat in sorted(by_category):
        rules = by_category[cat]
        # 按 rule_id 升序
        rules.sort(key=lambda r: extract_rule_id(r))

        out = "\n---\n".join(rules) + "\n"
        filename = normalize_category(cat) + ".yaml"
        path = RULES_DIR / filename
        path.write_text(out, encoding="utf-8")
        ids = [extract_rule_id(r) or "?" for r in rules]
        print(f"  {path}: {len(rules)} 条  {' '.join(ids)}")
        written += 1

    print(f"\n完成，共 {written} 个文件")

    missing = EXPECTED_CATEGORIES - set(by_category.keys())
    if missing:
        print(f"注意：以下分类无规则，不会生成文件：{sorted(missing)}")


if __name__ == "__main__":
    main()