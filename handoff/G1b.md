任务编号：G.1b
完成内容：M09 暴露 CU + AST 精细检测器。
- ParsedSource 新增 compilationUnit 字段（nullable，不参与 JSON）。
- JavaSourceParser 三处构造点补 cu/null。
- AstPreciseDetector 覆盖 7 条规则：
  DI-01/DI-02（字段注入/non-final）、DAO-01/DAO-02/DAO-03（事务可见性/自调用/readOnly）、
  WEB-01（Controller 可变字段）、WEB-02（@RequestBody 缺 @Valid）。
- RuleEngine 注册 AstPreciseDetector，位于通用检测器之前。
关键决定：
- 保留 G1 的 DI-01、DAO-01 正则参数；IssueAggregator 按 rule_id+file+line+msg 去重。
- AstPreciseDetector 只处理支持清单内的规则，其余不干扰。
- 单文件候选不限流（AST 精准，无需限流）。
未决问题：
- DI-01 正则与 AST 双路径可能造成同规则多行命中，但去重后统一。
- DAO-02 自调用检测：跳过调用者自身是 @Transactional 的场景（合法 self-proxy）。
- DAO-03 只读识别基于方法名前缀启发式，可能误报/漏报。
- WEB-01 mutable field 判定不分析 setter/赋值，可能对只读字段误报。
- 未来如需，可移除 G1 的 DI-01/DAO-01 正则参数。
下一个任务建议：
- G3 真实项目验证；
- 或补更多 AST 检测器（CON、EXC、RES、SEC）。