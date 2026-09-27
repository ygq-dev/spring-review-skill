任务编号：F8b
完成内容：LLM 语义规则接入（默认关闭）。
- LlmSemanticRequest、LlmPromptBuilder、LlmResponseParser、LlmSemanticDetector。
- RuleEngine 新增构造器接受 LlmClient；LlmSemanticDetector 加入检测器链末端。
- Orchestrator 步骤 5b 创建 LlmClient（LlmClientFactory），传给 RuleEngine。
- 单元测试 6 个：supports / disabled / valid / invalid / malformed / downgrade。
关键决定：
- LLM 分支默认关闭：仅当 rule.parameters.use_llm=true 时触发。
- eligible detection_method：HYBRID、HEURISTIC、RULE_ENGINE。
- prompt：system 带规则说明 + JSON schema；user 放脱敏代码片段；
  单文件 6KB、总计 24KB 上限。
- 响应格式：{"issues":[{"file","line","column","evidence","message","confidence"}]}
- 无效 confidence 降级为 LOW；缺字段的条目丢弃。
- 失败或 offline：返回空列表，不阻断。
- token usage 暂未累加（F8b.1 或 F9.1 补）。
未决问题：
- 现有 55 条规则均未声明 use_llm，LLM 分支实际 0 调用。
- 若后续需要启用，只需在规则 YAML 的 parameters 下加 "use_llm": true。
- prompt 中源码未脱敏（E8 第 14 节说 M12 负责）；当前直接发送。
  后续可加 Redactor 处理 evidence 字段，或按 secret 模式过滤。
- LLM 调用记录未进入 metrics.llm_calls；F9.1 补。
- token usage 未汇总到 summary.token_usage；F9.1 补。
下一个任务建议：
- F10 测试补齐与端到端集成。
- 或 F11 CI 示例。
- 或 F9.1 把 token usage / llm_calls 补进报告。