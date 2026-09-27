任务编号：G.1c
完成内容：AST 精准检测器单测。
- AstPreciseDetectorTest 17 个用例：
  - DI-01：命中 @Autowired 字段 / 不命中构造器注入
  - DI-02：命中 non-final / 不命中 final
  - DAO-01：命中 private @Transactional / 不命中 public
  - DAO-02：命中 this.inner() / 不命中 other.inner()
  - DAO-03：命中 findX 无 readOnly / 不命中 readOnly=true / 不命中 save 写方法
  - WEB-01：命中 Controller 可变字段 / 不命中 final / 不命中 @Service
  - WEB-02：命中 @RequestBody 无 @Valid / 不命中有 @Valid
  - 未知规则不响应
关键决定：
- 测试用内联代码，不落盘、不依赖 Git。
- 断言 evidence 关键词，不依赖精确行列号。
- 正反例成对，避免只测命中。
未决问题：
- 若某测试失败，需按实际 AST 结构微调。
- DAO-02 未覆盖 AopContext.currentProxy() 场景。
- WEB-01 未覆盖 setter 赋值场景（当前无赋值也命中）。
下一步建议：
- G3 真实项目验证；
- 或 G.1d 补 CON/EXC/RES/SEC 类 AST 检测器；
- 或阶段 G 收尾。