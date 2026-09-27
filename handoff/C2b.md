任务编号：C2b  
完成内容：为 CON 分类 5 条规则补齐正反例 10 个、夹具 5 个、测试用例 5 个、expected 5 个，并覆盖保存 examples/index.json（30 条）与 tests/cases/index.json（15 条）。  
产出文件：docs/13-C2b-CON规则测试用例.md  
规则数：5  
关键决定：严格使用 C0/A4.2 冻结字段；input 用 fixture_ref 且 content=null；encoding=utf-8；expected 顶层用 case_id、无 status；索引用 polarity/file；统一元数据 version=1.0.0、status=ACTIVE、generated_at=2026-09-24T00:00:00Z、generated_by=manual/1.0.0。  
未决问题：需与仓库真实 C1/C2a 索引条目核对并覆盖保存；未实际运行校验/测试执行；CON-04/CON-05 的 CUSTOM/HYBRID 工具检测实现待确认；message_contains/location_hint 可能需要按真实输出微调。  
下一个任务建议：先落盘并校验 C2b 产出，再进入 C2c（剩余分类测试资产）或 C3（全量索引一致性、Schema、用例执行校验）。  
可直接粘贴到下一个对话的上下文包：

```text
项目：Spring Review Skill（只读审查，不改代码/不提交/不训练）
当前阶段：C2b 已完成，产出 docs/13-C2b-CON规则测试用例.md
覆盖规则：SRS-CON-01-001、SRS-CON-02-001、SRS-CON-03-001、SRS-CON-04-001、SRS-CON-05-001
本次产出：CON 正反例 10、夹具 5、测试用例 5、expected 5、examples/index.json 30 条、tests/cases/index.json 15 条
冻结约束：C0 测试用例顶层字段、expected 顶层字段、input/expected 结构；A4.2 索引字段；统一元数据值
易错点：不要重犯 C1 的 8 个错误、C2a 的 5 个错误；保留 C1/C2a 已有索引条目原值
未决：实际落盘/校验未执行；CUSTOM/HYBRID 检测实现待确认；expected 关键词/行号可能需微调
下一步建议：C2c 或 C3 全量一致性校验
```