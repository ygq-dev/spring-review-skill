"""
Spring Review Skill - 一键提取与索引合并

功能：
1. 从 docs/11-C1 到 docs/20-C7 共 10 个文档中提取所有「保存路径 + 代码块」对
2. 把内容自动保存为对应独立文件（examples/、tests/fixtures/、tests/cases/、tests/expected/）
3. 合并 C1~C7 的索引片段为完整 examples/index.json（110 条）和 tests/cases/index.json（55 条）

Windows 兼容：
- 路径中出现的 /CON/ 会替换为 /CON_/，因为 CON 是 Windows 保留设备名。
- 索引里 file 字段同步替换，保证与实际文件一致。

用法：
    cd spring-review-skill
    python scripts/extract_all.py
"""

import json
import re
import sys
from pathlib import Path

DOCS_DIR = Path("docs")

C_PHASE_DOCS = [
    "11-C1-AR-CF试点用例.md",
    "12-C2a-DI规则测试用例.md",
    "13-C2b-CON规则测试用例.md",
    "14-C3a-WEB规则测试用例.md",
    "15-C3b-SEC规则测试用例.md",
    "16-C4-DAO-EXC-RES规则测试用例.md",
    "17-C4.5-CF-BUILD-CLOUD-MIG规则测试用例.md",
    "18-C5-EXC-RES规则测试用例.md",
    "19-C6-PERF-OBS规则测试用例.md",
    "20-C7-TEST-STYLE-NULL-I18N规则测试用例.md",
]

INDEX_BASE_DOC = "13-C2b-CON规则测试用例.md"
INDEX_INCREMENT_DOCS = C_PHASE_DOCS[3:]

PATH_RE = re.compile(
    r'(?:保存路径[:：]\s*)?\*{0,2}\s*`?'
    r'((?:examples|tests/fixtures|tests/cases|tests/expected|rules)/'
    r'[^\s`*<>]+?\.(?:java|xml|yml|yaml|json|gradle|kts|properties|md))'
    r'`?\s*\*{0,2}',
    re.MULTILINE,
)

CODE_RE = re.compile(r'```(\w*)\s*\n(.*?)\n\s*```', re.DOTALL)
SKIP_SECTIONS = {6, 7}


def normalize_path(path: str) -> str:
    """把 Windows 保留名 CON 替换为 CON_。只替换完整目录段。"""
    parts = path.split('/')
    parts = ['CON_' if p == 'CON' else p for p in parts]
    return '/'.join(parts)


def strip_index_sections(text: str) -> str:
    parts = re.split(r'(^## \d+\..*$)', text, flags=re.MULTILINE)
    out, skip = [], False
    for part in parts:
        if re.match(r'^## \d+\.', part):
            skip = int(re.match(r'^## (\d+)\.', part).group(1)) in SKIP_SECTIONS
        if not skip:
            out.append(part)
    return '\n'.join(out)


def extract_files(text: str):
    clean = strip_index_sections(text)
    result = []
    for cm in CODE_RE.finditer(clean):
        head = clean[max(0, cm.start() - 300):cm.start()]
        pms = list(PATH_RE.finditer(head))
        if not pms:
            continue
        path = normalize_path(pms[-1].group(1))
        result.append((path, cm.group(2)))
    return result


def save_files(extracted, seen):
    added = 0
    for path, content in extracted:
        if path in seen:
            continue
        p = Path(path)
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(content, encoding="utf-8")
        seen.add(path)
        added += 1
    return added


def extract_index(text: str, section_num: int):
    """
    从 Markdown 文档中提取第 N 节的 JSON 索引块。
    兼容两种格式：
    - 完整对象：{"version": ..., "count": ..., "items": [...]}  → 返回 items
    - 纯列表：  [ {...}, {...} ]                                 → 直接返回
    """
    pat = re.compile(
        rf'^## {section_num}\..*?```json\s*\n(.*?)\n```',
        re.DOTALL | re.MULTILINE,
    )
    m = pat.search(text)
    if not m:
        return None
    data = json.loads(m.group(1))
    if isinstance(data, dict):
        return data.get("items", [])
    if isinstance(data, list):
        return data
    raise ValueError(
        f"第 {section_num} 节的 JSON 既不是 object 也不是 array: {type(data)}"
    )


def normalize_index_items(items):
    """把索引里 file 字段的 /CON/ 替换为 /CON_/。"""
    for it in items:
        if "file" in it and isinstance(it["file"], str):
            it["file"] = normalize_path(it["file"])
    return items


def dedup_sort(items):
    seen, out = set(), []
    for it in items:
        if it["id"] not in seen:
            seen.add(it["id"])
            out.append(it)
    out.sort(key=lambda x: x["id"])
    return out


def main():
    if not DOCS_DIR.exists():
        print(f"错误：找不到 docs 目录 {DOCS_DIR.resolve()}")
        sys.exit(1)

    print("=" * 60)
    print("阶段 1：从文档提取独立文件")
    print("=" * 60)
    seen, total, missing = set(), 0, []
    for doc_name in C_PHASE_DOCS:
        doc_path = DOCS_DIR / doc_name
        if not doc_path.exists():
            missing.append(doc_name)
            print(f"[跳过] {doc_name} 不存在")
            continue
        text = doc_path.read_text(encoding="utf-8")
        files = extract_files(text)
        n = save_files(files, seen)
        total += n
        print(f"[提取] {doc_name}: +{n} 个文件")
    print(f"\n共保存 {total} 个独立文件")
    if missing:
        print(f"缺失的文档：{missing}")

    print()
    print("=" * 60)
    print("阶段 2：合并索引")
    print("=" * 60)
    base_path = DOCS_DIR / INDEX_BASE_DOC
    if not base_path.exists():
        print(f"错误：找不到基准索引文档 {INDEX_BASE_DOC}")
        sys.exit(1)
    base_text = base_path.read_text(encoding="utf-8")
    examples = normalize_index_items(extract_index(base_text, 6) or [])
    cases = normalize_index_items(extract_index(base_text, 7) or [])
    print(f"[基准] {INDEX_BASE_DOC}: examples={len(examples)}, cases={len(cases)}")

    for doc_name in INDEX_INCREMENT_DOCS:
        doc_path = DOCS_DIR / doc_name
        if not doc_path.exists():
            continue
        text = doc_path.read_text(encoding="utf-8")
        inc_ex = normalize_index_items(extract_index(text, 6) or [])
        inc_ca = normalize_index_items(extract_index(text, 7) or [])
        examples.extend(inc_ex)
        cases.extend(inc_ca)
        print(f"[增量] {doc_name}: +{len(inc_ex)} examples, +{len(inc_ca)} cases")

    examples = dedup_sort(examples)
    cases = dedup_sort(cases)

    ex_index = {
        "version": "1.0.0",
        "generated_at": "2026-09-24T00:00:00Z",
        "generated_by": "manual/1.0.0",
        "count": len(examples),
        "items": examples,
    }
    ca_index = {
        "version": "1.0.0",
        "generated_at": "2026-09-24T00:00:00Z",
        "generated_by": "manual/1.0.0",
        "count": len(cases),
        "items": cases,
    }

    Path("examples").mkdir(exist_ok=True)
    Path("tests/cases").mkdir(parents=True, exist_ok=True)
    (Path("examples") / "index.json").write_text(
        json.dumps(ex_index, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    (Path("tests") / "cases" / "index.json").write_text(
        json.dumps(ca_index, ensure_ascii=False, indent=2), encoding="utf-8"
    )

    print()
    print("=" * 60)
    print("完成")
    print("=" * 60)
    print(f"examples/index.json    : {len(examples)} 条")
    print(f"tests/cases/index.json : {len(cases)} 条")


if __name__ == "__main__":
    main()