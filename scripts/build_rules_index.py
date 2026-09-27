"""
Spring Review Skill - 规则索引生成脚本

从 rules/*.yaml 读取全部规则，提取 A4.2 定义的索引字段，
按 rule_id 升序，输出 rules/index.json。

A4.2 冻结的 items[] 字段：
    id、category、subcategory、file、status、severity、version、
    title（可选）、confidence（可选）、detection_method（可选）、tool（可选）、tags（可选）

用法：
    cd spring-review-skill
    pip install pyyaml   # 若未安装
    python scripts/build_rules_index.py
"""

import json
import sys
from pathlib import Path

try:
    import yaml
except ImportError:
    print("错误：需要 PyYAML，请先执行 pip install pyyaml")
    sys.exit(1)

RULES_DIR = Path("rules")
INDEX_PATH = RULES_DIR / "index.json"


def main():
    if not RULES_DIR.exists():
        print(f"错误：找不到 {RULES_DIR.resolve()}")
        sys.exit(1)

    files = sorted(RULES_DIR.glob("*.yaml"))
    if not files:
        print("错误：rules/ 下没有 YAML 文件")
        sys.exit(1)

    items = []
    for path in files:
        text = path.read_text(encoding="utf-8")
        for doc in yaml.safe_load_all(text):
            if not isinstance(doc, dict):
                continue
            rule_id = doc.get("rule_id")
            if not rule_id:
                print(f"警告：{path} 中存在缺少 rule_id 的规则，跳过")
                continue

            item = {
                "id": rule_id,
                "category": doc.get("category_l1", ""),
                "subcategory": str(doc.get("category_l2", "")),
                "file": f"rules/{path.name}",
                "status": doc.get("status", "ACTIVE"),
                "severity": doc.get("severity", "INFO"),
                "version": doc.get("rule_version", "1.0.0"),
            }
            # 可选字段，仅在存在时写入
            if doc.get("name"):
                item["title"] = doc["name"]
            if doc.get("confidence"):
                item["confidence"] = doc["confidence"]
            if doc.get("detection_method"):
                item["detection_method"] = doc["detection_method"]
            if doc.get("tool"):
                item["tool"] = [doc["tool"]] if isinstance(doc["tool"], str) else doc["tool"]
            if doc.get("tags"):
                item["tags"] = doc["tags"]

            items.append(item)

    # 按 id 升序
    items.sort(key=lambda x: x["id"])

    # 唯一性检查
    ids = [it["id"] for it in items]
    dup = {x for x in ids if ids.count(x) > 1}
    if dup:
        print(f"错误：存在重复 rule_id：{sorted(dup)}")
        sys.exit(1)

    index = {
        "version": "1.0.0",
        "generated_at": "2026-09-24T00:00:00Z",
        "generated_by": "manual/1.0.0",
        "count": len(items),
        "items": items,
    }

    RULES_DIR.mkdir(exist_ok=True)
    INDEX_PATH.write_text(
        json.dumps(index, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    print(f"完成：{INDEX_PATH}")
    print(f"规则总数：{len(items)}")
    print(f"来源文件数：{len(files)}")
    print()
    print("按分类统计：")
    from collections import Counter
    cat_counter = Counter(it["category"] for it in items)
    for cat in sorted(cat_counter):
        print(f"  {cat:6s} {cat_counter[cat]} 条")


if __name__ == "__main__":
    main()