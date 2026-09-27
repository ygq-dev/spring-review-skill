任务编号：G.1d
完成内容：补 3 条 AST 检测器 + 9 个新单测。
- CON-01：static 字段类型为 SimpleDateFormat。
- SEC-03：包含 SQL 关键字字面量 + 非字面量操作数的 + 表达式。
- EXC-01：catch 体无 statements（纯空）。
- 新增 9 个测试：每条规则 1 命中 + 2 不命中。
关键决定：
- 未引入新的 regex 参数（避免与 AST 双路径重叠）。
- SEC-03 只报"字面量中带 SQL 关键字"，避免字符串拼接误报。
- EXC-01 不报 printStackTrace（EXC-02 单独处理）。
- CON-01 只报 static 字段，忽略局部变量。
未决问题：
- SEC-03 未覆盖 PreparedStatement 参数化写法（应放过）。
- EXC-01 未覆盖 "catch 后仅注释" 的变体（当前空 statements 才算）。
- CON-01 未覆盖直接 new 静态块（罕见）。
下一步建议：
- 或 G.1e 补 3 条检测器（RES-01、OBS-01、SEC-04）。
- 或 G3 真实项目验证。
- 或阶段 G 收尾。