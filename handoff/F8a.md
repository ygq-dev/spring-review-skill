任务编号：F8a
完成内容：M12/M13 规则引擎（确定性）。
- 数据模型：Issue（A6 兼容）、Summary、AggregatedResult。
- Detector 接口 + 5 个检测器：
  - ArHybridDetector：AR-01/AR-02 走 M09 依赖图
  - BuildFileDetector：BUILD 规则走 pom.xml/build.gradle
  - ConfigKeyDetector：CF/CLOUD/OBS 规则走配置文件
  - RegexDetector：REGEX 规则读 parameters.patterns/pattern
  - AstHeuristicDetector：AST 规则读 parameters.annotations
- RuleEngine：按规则分派到检测器；单检测器失败不阻断。
- IssueAggregator：dedupe（rule+file+line+msg，保留最高 confidence）、
  6 级排序（severity/confidence/file/line/column/rule_id）、
  Summary 生成（bySeverity、byCategory、blocking、tokenUsage）、
  issue_id = SHA-256(fingerprint) 前 8 位大写。
- Orchestrator 接入 M12/M13，合并 M10 静态候选 + M12 引擎候选。
关键决定：
- 检测器按规则 ID / category / detection_method 组合分派；
  未知规则走通用回退（parameters 驱动）。
- 专用检测器优先（AR、BUILD、Config），AST 通用回退兜底。
- 去重键不包含 column，减少语义相同但列号不同导致漏合。
- issue_id 用 SHA-256 前 8 位十六进制；碰撞概率极低。
- LLM 语义分支未接入，HYBRID 规则暂只走确定性部分。
未决问题：
- 大部分 AST 规则的精细逻辑（如 @Transactional 非 public、
  字段注入、@Around 未调用 proceed）未实现；F8b 或后续补。
- HYBRID 规则的置信度合并未做；当前用规则元数据。
- LLM 语义候选未接入；F8b 做。
- 部分正则未按测试用例的 message_contains 校准；可能出现检出但关键词不匹配。
下一个任务建议：
- F9 报告 M14/M15/M16/M17：把 AggregatedResult 写入 review-report.json + .md。
- 或 F8b：LLM 语义规则接入，补齐 HYBRID 的 LLM 分支。