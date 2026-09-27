# -*- coding: utf-8 -*-
"""
删除 rules/*.yaml 与 docs/rules/*.md 中的 `tool_rule_id: null` 行。
- 只删整行；不动其他内容。
- 幂等：跑第二次不改任何文件。
- UTF-8 无 BOM。
兼容 Python 3.8+。
"""
import re
import sys
from pathlib import Path

PATTERN = re.compile(r'(?m)^\s*tool_rule_id:\s*null\s*\r?\n')

TARGET_DIRS = [Path('rules'), Path('docs/rules')]


def process(path: Path) -> bool:
    raw = path.read_text(encoding='utf-8')
    new = PATTERN.sub('', raw)
    if new != raw:
        with open(path, 'w', encoding='utf-8', newline='\n') as f:
            f.write(new)
        print(f'[fixed] {path}')
        return True
    print(f'[ok]    {path}')
    return False


def main():
    changed = 0
    seen = 0
    for d in TARGET_DIRS:
        if not d.exists():
            print(f'[skip] {d} not found')
            continue
        for ext in ('*.yaml', '*.yml', '*.md'):
            for p in sorted(d.rglob(ext)):
                seen += 1
                if process(p):
                    changed += 1
    print(f'Done. {changed} file(s) changed out of {seen}.')


if __name__ == '__main__':
    main()