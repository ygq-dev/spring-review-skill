任务编号：G.1a
完成内容：检测器限流 + 追加 2 条正则参数。
- RegexDetector：单文件单规则 MAX_PER_FILE_PER_RULE=10。
- AstHeuristicDetector：单文件单规则 5，evidence 带 class 名。
- PATCHES 追加 DI-01（@Autowired/@Inject）、DAO-01（@Transactional 后跟 private）。
关键决定：
- 不改 M09 契约（ParsedSource 不暴露 CU）；AST 精细检测留 G.1b。
- 优先正则覆盖，避免复杂负向断言。
效果：
- 13 行文件 issues 53 → 5；候选 845 → 50。
- 72 测试通过。
未决问题：
- 误报率尚无度量；需要真实项目或基准集验证。
- M09 未暴露 CU，DI-02/WEB-01/WEB-02 等负向断言规则无法精确实现。
- 部分规则（HYBRID）实际检出为 0；参数空。
下一步建议：
- G2 工具配置（checkstyle.xml + pmd.xml）；
- 或 G.1b M09 暴露 CU + AST 精细检测；
- 或 G3 真实项目验证。