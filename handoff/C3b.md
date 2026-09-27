任务编号：C3b
完成内容：为 SEC 分类 5 条规则编写正反例 10 个、夹具 5 个、测试用例 5 个、expected 5 个、索引新增片段 2 个。
产出文件：docs/15-C3b-SEC规则测试用例.md
规则数：5
规则清单：SRS-SEC-01-001、SRS-SEC-02-001、SRS-SEC-03-001、SRS-SEC-04-001、SRS-SEC-05-001

关键决定：
- 严格遵循 C0 结构约束与完整 JSON 骨架。
- 测试用例 JSON 顶层 13 字段；expected 是完整 object；positive_case/negative_case 是字符串。
- expected JSON 顶层 6 字段；用 case_id。
- 索引只输出新增片段：examples 新增 10 条，tests/cases 新增 5 条，待 C8 合并。
- 统一元数据：generated_at=2026-09-24T00:00:00Z，generated_by=manual/1.0.0，version=1.0.0，status=ACTIVE，severity 全大写。
- input.type=fixture_ref，content=null，encoding=utf-8。
- 未重复 C1、C2a、C2b、C3a 历史错误。
- 未修改 A5/A6 Schema、B1～B7 规则文件。

未决问题：
- C8 索引合并未做。
- TC-SEC-002 夹具中的身份证号 110101199001011234 建议 C8 统一改为明显占位符（如 11010119900101XXXX）。
- SEC-03-001 的 PMD 精确规则名待 E 阶段确认。
- SEC-05-001 的 SpotBugs 精确规则名待 E 阶段确认。
- location_hint 的 line_start/line_end 需真实解析器校验。

下一个任务建议：
进入 C4（DAO 5 + EXC 1 + RES 1）；或先做 C3a、C3b 测试资产评审。

可直接粘贴到下一个对话的上下文包：
【项目】Spring Review Skill，只读审查；Java 17+/Spring Boot 3.x；输出 Markdown+JSON。
【阶段】C 测试用例与正反例；C3b 已完成。
【产出】docs/15-C3b-SEC规则测试用例.md，覆盖 SEC 5 条规则。
【C3b 关键】测试用例 JSON 顶层 13 字段；expected 是完整 object；positive_case/negative_case 是字符串；expected JSON 用 case_id；索引只输出新增片段待 C8 合并；统一元数据 generated_at=2026-09-24T00:00:00Z，generated_by=manual/1.0.0。
【历史错误】C1 的 8 个、C2a 的 5 个、C2b 的 4 个、C3a 的 2 个，不再重犯。
【下一步】C4 DAO + EXC + RES 规则测试资产。