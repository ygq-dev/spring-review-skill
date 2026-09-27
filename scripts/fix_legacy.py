# -*- coding: utf-8 -*-
"""
遗留项修复脚本（A/B/C/D 阶段遗留）
- FIX-03: 删除所有 `tool_rule_id: null` 行
- FIX-04: module_type 列表中若含 all 且同时含其他值，改为 ["all"] 或单元素块
- FIX-05: java_version ">=17" -> "17+"；spring_boot_version ">=3.0.0" -> "3.x"
- FIX-01/FIX-02 由脚本外的规则文件重写覆盖（AR.yaml 与 AR-CF-BUILD-CLOUD-MIG.md 单独处理）

用法：
  python scripts/fix_legacy.py --rules-dir rules --docs-rules-dir docs/rules
"""

import argparse
import io
import re
import sys
from pathlib import Path

TOOL_RULE_ID_NULL_RE = re.compile(r'^\s*tool_rule_id:\s*null\s*$')
JAVA_VERSION_RE = re.compile(r'(java_version:\s*)"(>=?17)"')
BOOT_VERSION_RE = re.compile(r'(spring_boot_version:\s*)"(>=?3\.0\.0|>=3\.x)"')

MODULE_TYPE_BLOCK_RE = re.compile(
    r'(module_type:\s*)\n((?:\s*-\s*[^\n]+\n)+)',
    re.MULTILINE,
)
MODULE_TYPE_INLINE_RE = re.compile(r'(module_type:\s*)\[([^\]]+)\]')

ALL_TOKEN = 'all'


def fix_tool_rule_id_null(text: str) -> str:
    return '\n'.join(
        line for line in text.splitlines() if not TOOL_RULE_ID_NULL_RE.match(line)
    )


def fix_version_styles(text: str) -> str:
    text = JAVA_VERSION_RE.sub(r'\1"17+"', text)
    text = BOOT_VERSION_RE.sub(r'\1"3.x"', text)
    return text


def _normalize_module_type_values(values):
    cleaned = [v.strip().strip('"').strip("'") for v in values]
    if ALL_TOKEN in cleaned and len(cleaned) > 1:
        return [ALL_TOKEN]
    return cleaned


def fix_module_type(text: str) -> str:
    def block_repl(m):
        head = m.group(1)
        body = m.group(2)
        items = re.findall(r'-\s*([^\n]+)', body)
        normalized = _normalize_module_type_values(items)
        if normalized == [v.strip().strip('"').strip("'") for v in items]:
            return m.group(0)
        new_body = ''.join(f'    - {v}\n' for v in normalized)
        return head + '\n' + new_body

    def inline_repl(m):
        head = m.group(1)
        items = [v.strip() for v in m.group(2).split(',')]
        normalized = _normalize_module_type_values(items)
        new_items = ', '.join(f'"{v}"' for v in normalized)
        return f'{head}[{new_items}]'

    text = MODULE_TYPE_BLOCK_RE.sub(block_repl, text)
    text = MODULE_TYPE_INLINE_RE.sub(inline_repl, text)
    return text


def process_file(path: Path) -> bool:
    raw = path.read_text(encoding='utf-8')
    original = raw
    raw = fix_tool_rule_id_null(raw)
    raw = fix_version_styles(raw)
    raw = fix_module_type(raw)
    if raw != original:
        path.write_text(raw, encoding='utf-8', newline='\n')
        print(f'[fixed] {path}')
        return True
    print(f'[ok]    {path}')
    return False


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--rules-dir', default='rules')
    ap.add_argument('--docs-rules-dir', default='docs/rules')
    args = ap.parse_args()

    roots = [Path(args.rules_dir), Path(args.docs_rules_dir)]
    targets = []
    for root in roots:
        if not root.exists():
            print(f'[skip] {root} not found', file=sys.stderr)
            continue
        for ext in ('*.yaml', '*.yml', '*.md'):
            targets.extend(sorted(root.glob(ext)))

    changed = 0
    for p in targets:
        if process_file(p):
            changed += 1
    print(f'Done. {changed} file(s) changed out of {len(targets)}.')


if __name__ == '__main__':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', newline='\n')
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', newline='\n')
    main()