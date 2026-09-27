任务编号：C3a  
完成内容：为 B3 中 WEB 分类 5 条规则完成正反例 10 个、夹具 5 个、测试用例 5 个、expected 5 个，并给出两个索引的新增条目片段。  
产出文件：docs/14-C3a-WEB规则测试用例.md（含 examples/WEB/...、tests/fixtures/TC-WEB-001~005、tests/cases/TC-WEB-001~005.json、tests/expected/TC-WEB-001~005.json、examples/index.json 新增 10 条、tests/cases/index.json 新增 5 条）。  
规则数：5  
关键决定：
- 严格使用统一元数据：generated_at、generated_by、version、status、severity 全大写。
- 测试用例 JSON 的 expected 使用 expected_ref 对象，避免路径字符串或完整 expected 结构。
- expected JSON 使用 version、generated_at、generated_by、case_id、rule_id、expected。
- 索引只输出本次新增条目，待追加到已有 items 末尾；不覆盖、不篡改 C1/C2a/C2b。
- examples 索引用 polarity/file；cases 索引用 file；未使用 type/path/generated_by 等禁用字段。

未决问题：
- examples/index.json 完整 40 条、tests/cases/index.json 完整 20 条尚未合并：缺少 C1/C2a/C2b 已有 30/15 条原文，无法逐字保留后追加。
- 需复核 TC-WEB-* JSON 顶层 expected 的 expected_ref 结构是否与 C0 冻结完全一致。

下一个任务建议：
- 先提供 C1/C2a/C2b 两个 index 原文，完成 C3a 索引合并与校验。
- 再启动 C3b：为 SEC 分类 5 条规则编写测试资产。

可直接粘贴到下一个对话的上下文包：

【Spring Review Skill / C3a 交接】
项目：Spring Review Skill，只读审查，不改代码/不提交/不训练；范围 Java 17+/Spring Boot 3.x，Maven/Gradle；输出 Markdown+JSON。
阶段：C 测试用例与正反例。
已完成：C3a 为 B3 的 WEB 5 条规则完成正反例 10、夹具 5、测试用例 5、expected 5，文档 docs/14-C3a-WEB规则测试用例.md。
规则：SRS-WEB-01-001 CRITICAL、SRS-WEB-02-001 MAJOR、SRS-WEB-03-001 MAJOR、SRS-WEB-04-001 CRITICAL、SRS-WEB-05-001 MAJOR。
已有产出：docs/10 C0、11 C1、12 C2a、13 C2b、rules/WEB-SEC.md B3、handoff/C0/C1/C2a/C2b。
约束：测试用例 JSON 顶层字段 id、rule_id、category、subcategory、title、description、input、expected、positive_case、negative_case、tags、version、status；expected JSON 顶层 version、generated_at、generated_by、case_id、rule_id、expected；统一元数据 generated_at=2026-09-24T00:00:00Z，generated_by=manual/1.0.0，version=1.0.0，status=ACTIVE；severity 全大写。
索引：examples items 字段 id、rule_id、category、subcategory、polarity、file、status、version、language、title；cases items 字段 id、rule_id、category、file、status、version、title、tags。合并时已有 C1/C2a/C2b 条目逐字保留，只追加，不修改；已有问题只报告。
未决：两个 index 完整 40/20 未合并，缺已有 items 原文；需复核测试用例 expected 字段格式。
下一步：合并 C3a 索引；启动 C3b SEC 规则测试资产。
注意：不要重犯历史错误（type/path 代替 polarity/file、input.type=file、content 未置 null、location_hint 字符串、expected 文件用 id/status、encoding 大小写、expected 路径字符串、篡改已有条目 id/title/tags 等）。