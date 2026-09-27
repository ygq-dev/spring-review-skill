"""
Spring Review Skill - 索引合并脚本
把 C1~C7 的索引片段合并为完整索引：
- examples/index.json：完整索引，110 条
- tests/cases/index.json：完整索引，55 条

用法：
    cd spring-review-skill
    python scripts/merge_indexes.py
"""

import json
import re
from pathlib import Path

DOCS_DIR = Path("docs")

# C2b 提供完整索引（包含 C1 + C2a + C2b）
COMPLETE_DOC = "13-C2b-CON规则测试用例.md"

# C3a 起的文档提供增量片段
PARTIAL_DOCS = [
    "14-C3a-WEB规则测试用例.md",
    "15-C3b-SEC规则测试用例.md",
    "16-C4-DAO-EXC-RES规则测试用例.md",
    "17-C4.5-CF-BUILD-CLOUD-MIG规则测试用例.md",
    "18-C5-EXC-RES规则测试用例.md",
    "19-C6-PERF-OBS规则测试用例.md",
    "20-C7-TEST-STYLE-NULL-I18N规则测试用例.md",
]


def extract_json_block(text: str, section_num: int):
    """从 Markdown 文本的 '## <N>. ...' 节中提取第一个 ```json 块。"""
    pattern = rf'^##\s+{section_num}\..*?```json\s*\n(.*?)\n```'
    m = re.search(pattern, text, re.DOTALL | re.MULTILINE)
    if not m:
        raise ValueError(f"找不到第 {section_num} 节的 JSON 块")
    return json.loads(m.group(1))


def main():
    # 1. 读完整索引
    complete_text = (DOCS_DIR / COMPLETE_DOC).read_text(encoding="utf-8")
    examples_items = extract_json_block(complete_text, 6)
    cases_items = extract_json_block(complete_text, 7)
    print(f"[完整] {COMPLETE_DOC}: examples {len(examples_items)} 条, cases {len(cases_items)} 条")

    # 2. 追加增量
    for doc_name in PARTIAL_DOCS:
        text = (DOCS_DIR / doc_name).read_text(encoding="utf-8")
        inc_examples = extract_json_block(text, 6)
        inc_cases = extract_json_block(text, 7)
        examples_items.extend(inc_examples)
        cases_items.extend(inc_cases)
        print(f"[增量] {doc_name}: +{len(inc_examples)} examples, +{len(inc_cases)} cases")

    # 3. 按 id 升序排序
    examples_items.sort(key=lambda x: x["id"])
    cases_items.sort(key=lambda x: x["id"])

    # 4. 校验 id 唯一性
    ex_ids = [x["id"] for x in examples_items]
    ca_ids = [x["id"] for x in cases_items]
    assert len(ex_ids) == len(set(ex_ids)), f"examples 存在重复 id: {ex_ids}"
    assert len(ca_ids) == len(set(ca_ids)), f"cases 存在重复 id: {ca_ids}"

    # 5. 写出完整索引
    examples_index = {
        "version": "1.0.0",
        "generated_at": "2026-09-24T00:00:00Z",
        "generated_by": "manual/1.0.0",
        "count": len(examples_items),
        "items": examples_items,
    }
    cases_index = {
        "version": "1.0.0",
        "generated_at": "2026-09-24T00:00:00Z",
        "generated_by": "manual/1.0.0",
        "count": len(cases_items),
        "items": cases_items,
    }

    Path("examples").mkdir(exist_ok=True)
    Path("tests/cases").mkdir(parents=True, exist_ok=True)
    (Path("examples") / "index.json").write_text(
        json.dumps(examples_index, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    (Path("tests") / "cases" / "index.json").write_text(
        json.dumps(cases_index, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    print(f"\n最终 examples/index.json: {len(examples_items)} 条")
    print(f"最终 tests/cases/index.json: {len(cases_items)} 条")


if __name__ == "__main__":
    main()