# -*- coding: utf-8 -*-
"""
G1：为 rules/*.yaml 中首批 9 条规则补全 parameters。

用法：
  python scripts/patch-rule-parameters.py

特性：
- 只修改 rules/*.yaml，不动 docs/rules/*.md。
- 按多文档 YAML（--- 分隔）逐文档匹配 rule_id。
- 幂等：跑第二次不改变已正确的文件。
- UTF-8，无 BOM。
兼容 Python 3.8+。
"""
import re
import sys
from pathlib import Path

RULES_DIR = Path('rules')

PATCHES = {
    "SRS-SEC-01-001": {
        "patterns": [
            r'(?i)\b(password|passwd|pwd|secret|api[_-]?key|apikey|token|access[_-]?key)\b\s*=\s*"[^"]{3,}"',
        ],
    },
    "SRS-SEC-02-001": {
        "patterns": [
            r'(?i)log\.(info|debug|warn|error|trace)\s*\([^)]*\b(password|passwd|token|idCard|cardNo)\b',
        ],
    },
    "SRS-EXC-02-001": {
        "patterns": [
            r'\.printStackTrace\s*\(\s*\)',
        ],
    },
    "SRS-BUILD-02-001": {
        "pattern": "-SNAPSHOT",
    },
    "SRS-I18N-01-001": {
        "patterns": [
            r'"[^"]*[\u4e00-\u9fa5]{2,}[^"]*"',
        ],
    },
    "SRS-MIG-01-001": {
        "patterns": [
            r'import\s+javax\.(servlet|persistence|validation|annotation|transaction)\b',
        ],
    },
    "SRS-CF-01-001": {
        "forbidden_keys": ["show-sql", "h2-console", "devtools"],
    },
    "SRS-CLOUD-01-001": {
        "required_keys": ["shutdown"],
        "expected_value": "graceful",
    },
    "SRS-CLOUD-02-001": {
        "external_dependencies": ["db", "redis", "mq"],
    },
    "SRS-DI-01-001": {
        "patterns": [
            r'@(Autowired|Inject)\b',
        ],
    },
    "SRS-DAO-01-001": {
        "patterns": [
            r'@Transactional[^\n]*\n\s*private\s+[\w<>,\s]+\s+\w+\s*\(',
        ],
    },
}

# 只匹配行首 --- 到行尾（不含换行符），避免 \s* 吞掉换行
DOC_SEP_RE = re.compile(r'(?m)^---[ \t]*$')


def yaml_single_quote(s: str) -> str:
    return "'" + s.replace("'", "''") + "'"


def gen_yaml_block(params: dict, indent: int = 0) -> list:
    lines = []
    pad = "  " * (indent + 1)
    pad2 = "  " * (indent + 2)
    for k, v in params.items():
        if isinstance(v, list):
            lines.append(f"{pad}{k}:")
            for item in v:
                lines.append(f"{pad2}- {yaml_single_quote(str(item))}")
        else:
            lines.append(f"{pad}{k}: {yaml_single_quote(str(v))}")
    return lines


def patch_document(doc_text: str, rule_id: str, new_body: list) -> str:
    lines = doc_text.split('\n')
    out = []
    i = 0
    n = len(lines)
    target_found = False

    while i < n:
        line = lines[i]
        m = re.match(r'^rule_id:\s*"?([^"\n]+?)"?\s*$', line)
        if m:
            target_found = (m.group(1).strip() == rule_id)

        if target_found and re.match(r'^parameters:\s*', line):
            out.append('parameters:')
            out.extend(new_body)
            i += 1
            # 跳过原 parameters 的缩进子行
            while i < n:
                l = lines[i]
                if l == '':
                    # 若下一行是缩进（继续 parameters 子块），跳过空行继续
                    if i + 1 < n and (lines[i + 1][:1] in (' ', '\t')):
                        i += 1
                        continue
                    break
                if l[:1] in (' ', '\t'):
                    i += 1
                    continue
                break
            target_found = False
            continue

        out.append(line)
        i += 1

    return '\n'.join(out)


def patch_file(path: Path) -> bool:
    text = path.read_text(encoding='utf-8')
    # 关键修复：分隔符正则不吞换行
    docs = DOC_SEP_RE.split(text)
    new_docs = []
    any_patched = False
    for doc in docs:
        new_doc = doc
        for rule_id, params in PATCHES.items():
            if (f'rule_id: {rule_id}' in doc
                    or f'rule_id: "{rule_id}"' in doc
                    or f"rule_id: '{rule_id}'" in doc):
                body = gen_yaml_block(params)
                before = new_doc
                new_doc = patch_document(new_doc, rule_id, body)
                if new_doc != before:
                    any_patched = True
        new_docs.append(new_doc)

    new_text = '---\n'.join(new_docs)
    if any_patched and new_text != text:
        with open(path, 'w', encoding='utf-8', newline='\n') as f:
            f.write(new_text)
        return True
    return False


def main():
    if not RULES_DIR.exists():
        print(f'[skip] {RULES_DIR} not found', file=sys.stderr)
        sys.exit(1)

    patched = 0
    total = 0
    for path in sorted(RULES_DIR.glob('*.yaml')):
        total += 1
        if patch_file(path):
            print(f'[fixed] {path}')
            patched += 1
        else:
            print(f'[ok]    {path}')

    print(f'Done. {patched} file(s) changed out of {total}.')


if __name__ == '__main__':
    main()