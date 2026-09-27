任务编号：C2a  
完成内容：为 DI 分类 7 条规则编写正反例、夹具、测试用例、expected 与索引。  
产出文件：docs/12-C2a-DI规则测试用例.md  
规则数：7  

关键决定：
- 严格遵循 C0 的 12 条结构约束与 A4.2 索引字段。
- examples/index.json 合并 C1 6 条 + 本次 14 条 = 20 条，按 id 升序。
- tests/cases/index.json 合并 C1 3 条 + 本次 7 条 = 10 条，按 id 升序。
- 统一元数据：version=1.0.0、generated_at=2026-04-26T00:00:00Z、generated_by=spring-review-skill。
- 测试用例使用 fixture_ref + utf-8；expected 均含 object 类型 location_hint。
- 正例不命中目标规则，反例命中目标规则。
- 未修改 A5、A6 Schema，未修改 B1～B7 规则文件。

未决问题：
- expected 中 line_start/line_end/symbol 需在真实解析器上执行校验。
- 正例目前只作为 examples，尚未单独建 tests/cases 不触发用例，是否补充待定。
- C1 已有索引条目需与冻结文件最终合并核对。

下一个任务建议：
- 进入 C2b 或 DI 测试资产评审。
- 执行 7 条 DI 测试用例，验证 expected 与真实输出一致。
- 通过后扩展其他分类测试资产。

可直接粘贴到下一个对话的上下文包：
```text
项目：Spring Review Skill（只读审查，不修改代码/不提交/不训练模型）
当前阶段：C2a 已完成
产出：docs/12-C2a-DI规则测试用例.md
范围：DI 7 条规则 SRS-DI-01-001 ~ SRS-DI-07-001
资产：14 个正反例、7 个夹具、7 个测试用例、7 个 expected、examples/index.json 20 条、tests/cases/index.json 10 条
约束：C0 12 条结构约束、A4.2 索引字段、不重犯 C1 的 8 个错误
元数据：version=1.0.0，generated_at=2026-04-26T00:00:00Z，generated_by=spring-review-skill
下一步：C2b 或 DI 测试资产评审/执行验证
```