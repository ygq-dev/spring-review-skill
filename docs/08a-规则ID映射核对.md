# docs/08a-规则ID映射核对.md

## 1. 说明
- B0 候选：55 条。
- B1～B7 规则：55 条。
- 核对口径：B0 与 B1～B7 一一对应；上一轮“53 条”作废。
- 本文只核对 rule_id 映射、差集、重复、二级编码连续性、格式与分类码一致性。

## 2. 映射核对表

| CAND-ID  | rule_id          | 来源文件 | 是否一致 |
| -------- | ---------------- | -------- | -------- |
| CAND-001 | SRS-DI-01-001    | B2       | 是       |
| CAND-002 | SRS-DI-02-001    | B2       | 是       |
| CAND-003 | SRS-DI-03-001    | B2       | 是       |
| CAND-004 | SRS-DI-04-001    | B2       | 是       |
| CAND-005 | SRS-DI-05-001    | B2       | 是       |
| CAND-006 | SRS-WEB-01-001   | B3       | 是       |
| CAND-007 | SRS-WEB-02-001   | B3       | 是       |
| CAND-008 | SRS-WEB-03-001   | B3       | 是       |
| CAND-009 | SRS-WEB-04-001   | B3       | 是       |
| CAND-010 | SRS-WEB-05-001   | B3       | 是       |
| CAND-011 | SRS-DAO-01-001   | B4       | 是       |
| CAND-012 | SRS-DAO-02-001   | B4       | 是       |
| CAND-013 | SRS-DAO-03-001   | B4       | 是       |
| CAND-014 | SRS-DAO-04-001   | B4       | 是       |
| CAND-015 | SRS-DAO-05-001   | B4       | 是       |
| CAND-016 | SRS-SEC-01-001   | B3       | 是       |
| CAND-017 | SRS-SEC-02-001   | B3       | 是       |
| CAND-018 | SRS-SEC-03-001   | B3       | 是       |
| CAND-019 | SRS-SEC-04-001   | B3       | 是       |
| CAND-020 | SRS-SEC-05-001   | B3       | 是       |
| CAND-021 | SRS-CON-01-001   | B2       | 是       |
| CAND-022 | SRS-CON-02-001   | B2       | 是       |
| CAND-023 | SRS-CON-03-001   | B2       | 是       |
| CAND-024 | SRS-CON-04-001   | B2       | 是       |
| CAND-025 | SRS-CON-05-001   | B2       | 是       |
| CAND-026 | SRS-EXC-01-001   | B5       | 是       |
| CAND-027 | SRS-EXC-02-001   | B5       | 是       |
| CAND-028 | SRS-EXC-03-001   | B4       | 是       |
| CAND-029 | SRS-EXC-04-001   | B5       | 是       |
| CAND-030 | SRS-EXC-05-001   | B5       | 是       |
| CAND-031 | SRS-AR-01-001    | B1       | 是       |
| CAND-032 | SRS-AR-02-001    | B1       | 是       |
| CAND-033 | SRS-CF-01-001    | B1       | 是       |
| CAND-034 | SRS-CF-02-001    | B1       | 是       |
| CAND-035 | SRS-PERF-01-001  | B6       | 是       |
| CAND-036 | SRS-PERF-02-001  | B6       | 是       |
| CAND-037 | SRS-OBS-01-001   | B6       | 是       |
| CAND-038 | SRS-OBS-02-001   | B6       | 是       |
| CAND-039 | SRS-RES-01-001   | B5       | 是       |
| CAND-040 | SRS-RES-02-001   | B4       | 是       |
| CAND-041 | SRS-TEST-01-001  | B7       | 是       |
| CAND-042 | SRS-TEST-02-001  | B7       | 是       |
| CAND-043 | SRS-BUILD-01-001 | B1       | 是       |
| CAND-044 | SRS-BUILD-02-001 | B1       | 是       |
| CAND-045 | SRS-STYLE-01-001 | B7       | 是       |
| CAND-046 | SRS-STYLE-02-001 | B7       | 是       |
| CAND-047 | SRS-NULL-01-001  | B7       | 是       |
| CAND-048 | SRS-NULL-02-001  | B7       | 是       |
| CAND-049 | SRS-CLOUD-01-001 | B1       | 是       |
| CAND-050 | SRS-CLOUD-02-001 | B1       | 是       |
| CAND-051 | SRS-DI-06-001    | B2       | 是       |
| CAND-052 | SRS-DI-07-001    | B2       | 是       |
| CAND-053 | SRS-OBS-03-001   | B6       | 是       |
| CAND-054 | SRS-I18N-01-001  | B7       | 是       |
| CAND-055 | SRS-MIG-01-001   | B1       | 是       |

## 3. 差集检查结果

| 检查项           | 结果 |
| ---------------- | ---- |
| B0 有、B1～B7 无 | 无   |
| B1～B7 有、B0 无 | 无   |
| 交集数量         | 55   |
| 并集数量         | 55   |

## 4. 重复 ID 检查

| 范围   | 重复 rule_id | 结果 |
| ------ | ------------ | ---- |
| B0     | 无           | 通过 |
| B1～B7 | 无           | 通过 |
| 合并后 | 无           | 通过 |

## 5. 二级编码连续性检查（按一级分类）

| 一级分类 | 出现二级编码               | 是否连续 |
| -------- | -------------------------- | -------- |
| AR       | 01、02                     | 是       |
| CF       | 01、02                     | 是       |
| DI       | 01、02、03、04、05、06、07 | 是       |
| WEB      | 01、02、03、04、05         | 是       |
| DAO      | 01、02、03、04、05         | 是       |
| SEC      | 01、02、03、04、05         | 是       |
| CON      | 01、02、03、04、05         | 是       |
| PERF     | 01、02                     | 是       |
| OBS      | 01、02、03                 | 是       |
| EXC      | 01、02、03、04、05         | 是       |
| RES      | 01、02                     | 是       |
| TEST     | 01、02                     | 是       |
| BUILD    | 01、02                     | 是       |
| STYLE    | 01、02                     | 是       |
| NULL     | 01、02                     | 是       |
| I18N     | 01                         | 是       |
| MIG      | 01                         | 是       |
| CLOUD    | 01、02                     | 是       |

## 6. 格式校验

- 正则：`^SRS-(AR|CF|DI|WEB|DAO|SEC|CON|PERF|OBS|EXC|RES|TEST|BUILD|STYLE|NULL|I18N|MIG|CLOUD)-\d{2}-\d{3}$`
- 总数：55
- 匹配：55
- 不匹配：0
- 不匹配 ID：无
- 一级分类码、二级分类码与候选 rule_id：全部一致

## 7. 结论

B0 的 55 条 rule_id 与 B1～B7 的 55 条 rule_id 完全一致，一一对应。  
无差集，无重复，二级编码连续，格式全部通过。  
映射核对通过；上一轮“53 条”误报作废。