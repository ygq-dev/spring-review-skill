# 基准评测集

## 1. 目的

按 A1 的 S5/S6 要求，提供可离线、可自动化的基准评测集：
- 检出率 ≥ 80%（20 个缺陷至少检出 16 个）
- 误报率 ≤ 20%（10 个干净片段最多误报 2 个）

## 2. 结构

- `defects/`：20 个缺陷用例，每个应触发至少 1 条规则
- `clean/`：10 个干净用例，不应触发任何规则
- `expected/`：每个用例的期望结果 JSON

## 3. 期望 JSON 格式

缺陷用例：
```json
{
  "case_id": "DEFECT-001",
  "type": "defect",
  "input": "defects/DEFECT-001.java",
  "expected_rules": ["SRS-DI-01-001"]
}
```

干净用例：

```json
{
  "case_id": "CLEAN-001",
  "type": "clean",
  "input": "clean/CLEAN-001.java",
  "expected_rules": []
}
```

