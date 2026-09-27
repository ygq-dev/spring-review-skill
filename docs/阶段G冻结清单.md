# 阶段 G 冻结清单

冻结日期：2026-09-26
阶段状态：冻结（G1、G2、G.1a～G.1e 完成；G3/G4/G5 转入 H 阶段）

## 1. 冻结产出

| 编号 | 产出                                                         | 状态 | 备注                                   |
| ---- | ------------------------------------------------------------ | ---- | -------------------------------------- |
| G1   | docs/34-规则参数设计.md                                      | 冻结 | 首批 9 条规则参数                      |
| G1   | scripts/patch-rule-parameters.py                             | 冻结 | 幂等；多文档 YAML；追加至 11 条        |
| G1   | rules/*.yaml（参数补丁）                                     | 冻结 | SEC/EXC/BUILD/I18N/MIG/CF/CLOUD/DI/DAO |
| G.1a | RegexDetector（限流 10/文件/规则）                           | 冻结 | 防误报爆炸                             |
| G.1a | AstHeuristicDetector（限流 5/文件/规则）                     | 冻结 | evidence 带 class 名                   |
| G.1b | ParsedSource（新增 compilationUnit 字段）                    | 冻结 | nullable；不参与 JSON                  |
| G.1b | AstPreciseDetector（7 条规则）                               | 冻结 | DI-01/02、DAO-01/02/03、WEB-01/02      |
| G.1c | AstPreciseDetectorTest（17 用例）                            | 冻结 | 正反例成对                             |
| G.1d | AstPreciseDetector（+3 条：CON-01/SEC-03/EXC-01）            | 冻结 | 10 条规则                              |
| G.1d | AstPreciseDetectorTest（+9 用例）                            | 冻结 | 每条 1 命中 + 2 不命中                 |
| G.1e | AstPreciseDetector（+5 条：DAO-04/WEB-04/SEC-04/RES-01/OBS-01） | 冻结 | 共 15 条规则                           |
| G.1e | AstPreciseDetectorTest（+13 用例）                           | 冻结 | 每条 1 命中 + 1～2 不命中              |
| G2   | config/checkstyle/checkstyle.xml                             | 冻结 | 命名族 + 编码规范                      |
| G2   | config/pmd/pmd.xml                                           | 冻结 | 5 条规则                               |
| G2   | config/README.md                                             | 冻结 | 工具下载/放置/未安装行为               |
| G2   | RuleMapping（fallbackByTool）                                | 冻结 | tool 级兜底                            |
| G2   | ToolPaths（skillHome 统一）                                  | 冻结 | 不再依赖目标仓库                       |
| G2   | CheckstyleRunner / PmdRunner / SpotbugsRunner（路径与 CLI 兼容） | 冻结 | 支持 PMD 7 CLI                         |
| G2   | StaticAnalysisFacade（去 repoRoot）                          | 冻结 | Checkstyle/PMD 调用                    |

## 2. 冻结决定

### 2.1 规则参数（G1）
- 首批 9 条：SEC-01/02、EXC-02、BUILD-02、I18N-01、MIG-01（REGEX）、
  CF-01、CLOUD-01/02（ConfigKey）。
- G.1a 追加 DI-01、DAO-01 参数 → 共 11 条。
- 参数化脚本幂等；多文档 YAML 兼容；不改 docs/rules/*.md。
- 正则避免负向先行断言，防止灾难性回溯。

### 2.2 检测器限流（G.1a）
- RegexDetector：单文件单规则最多 10 条候选。
- AstHeuristicDetector：单文件单规则最多 5 条候选，evidence 带 class 名。
- AstPreciseDetector：不限流（精准，无需限流）。
- 效果：13 行文件 issues 53 → 5；candidates 845 → 50。

### 2.3 M09 与 AST 检测器（G.1b）
- ParsedSource 新增 compilationUnit 字段（nullable，不参与 JSON）。
- JavaSourceParser 三处构造点补 cu/null：emptySource、解析失败、解析成功。
- AstPreciseDetector 检测器清单：15 条规则。
- RuleEngine 注册顺序：AR 专用 → Build 文件 → AST 精准 → ConfigKey → Regex → AST 回退 → LLM 语义。

### 2.4 15 条 AST 检测器规则清单

| rule_id        | 规则语义                     | 检测信号                                            |
| -------------- | ---------------------------- | --------------------------------------------------- |
| SRS-DI-01-001  | 字段注入                     | 字段带 @Autowired/@Inject/@Resource                 |
| SRS-DI-02-001  | 非 final 依赖字段            | 注入字段未 final                                    |
| SRS-DAO-01-001 | @Transactional private/final | 事务方法 private 或 final                           |
| SRS-DAO-02-001 | 同类自调用 @Transactional    | this.txMethod() 调用                                |
| SRS-DAO-03-001 | 只读查询未设 readOnly        | find*/get*/query* 方法无 readOnly=true              |
| SRS-DAO-04-001 | 事务内远程调用               | @Transactional 方法内调 RestTemplate/WebClient      |
| SRS-WEB-01-001 | Controller 可变实例字段      | @Controller/@RestController 类的非 final 字段       |
| SRS-WEB-02-001 | @RequestBody 缺 @Valid       | @RequestBody 参数无 @Valid/@Validated               |
| SRS-WEB-04-001 | 跨域过宽                     | allowedOrigins("*") + allowCredentials(true) 共现   |
| SRS-CON-01-001 | 静态共享 SimpleDateFormat    | static 字段类型 SimpleDateFormat                    |
| SRS-SEC-03-001 | SQL 字符串拼接               | SQL 关键字字面量 + 非字面量操作数的 + 表达式        |
| SRS-SEC-04-001 | 关闭 CSRF                    | csrf().disable() 或 CsrfConfigurer.disable()        |
| SRS-EXC-01-001 | catch 块吞异常               | catch 体无 statements                               |
| SRS-RES-01-001 | 未关闭资源                   | 资源类型局部变量未 try-with-resources 也未 .close() |
| SRS-OBS-01-001 | 日志占位符拼接               | log.info("x" + var) 形式                            |

### 2.5 工具配置（G2）
- 工具与配置属于 skill 包；路径基于 skillHome（SPRING_REVIEW_HOME 或 user.dir）。
- 工具缺失 → SKIPPED，不失败。
- Checkstyle 命名族通过 RuleMapping fallbackByTool 映射到 STYLE-01-001。
- PMD 用 --file-list + --use-version java-17，兼容 PMD 7.4 CLI。
- SpotBugs 依赖 target/classes 或 build/classes。

### 2.6 测试与覆盖
- 测试总数：72 → 102（G.1c 17 + G.1d 9 + G.1e 13 → 但 G.1c 也在累计里）。
- AST 精准检测器测试：39 个用例（15 条规则 × 命中/不命中）。
- 端到端：FILES/MODULE（EndToEndIntegrationTest）+ DIFF（DiffModeEndToEndTest）。

## 3. 遗留问题（转入 H 阶段或后续）

### 3.1 未做（A1 MVP 退出标准）
- **G3 真实项目验证**：A1 要求 ≥ 3 个真实 Spring Boot 项目端到端验证，未做。
- **G4 基准评测集**：A1 要求 ≥ 30 基准案例，未做。
- **G5 性能验证**：A1 要求 1000 行 diff P95 ≤ 60s，未做。
- **量化指标**：S5 检出率 ≥ 80%、S6 误报率 ≤ 20%，未度量。

### 3.2 规则覆盖未完整（40 / 55 条）
- 已有实际检测能力：约 20 条（AST 15 + 正则/ConfigKey 11，去重后约 20）。
- 剩余 35 条仅有规则元数据，无检测器：
  - DI-03/05/06/07（循环依赖、@Scope、@Aspect、proceed）
  - WEB-03/05（全局异常、异步超时）
  - DAO-05（N+1）
  - SEC-05（反序列化）
  - CON-02/03/04/05（非线程安全集合、懒初始化、线程池、跨线程事务）
  - PERF-01/02（循环远程调用、分页缺失）
  - OBS-02/03（Actuator、切点过宽）
  - EXC-03/04/05（回滚异常、对外堆栈、异步异常）
  - RES-02（事务中连接）
  - TEST-01/02（真实 DB、@SpringBootTest 滥用）
  - STYLE-01/02（Checkstyle/PMD 依赖）
  - NULL-01/02（Optional、null 集合）
  - CF-02（@Value 默认值）
  - CLOUD-02（健康检查隔离）
  - CLOUD-01 已有 ConfigKey 覆盖
  - AR-01/02（依赖图，已有 ArHybridDetector）
  - MIG-01、BUILD-01（部分覆盖）
  - I18N-01、SEC-01/02（正则覆盖）

### 3.3 检测器精细度
- DAO-02 未覆盖 AopContext.currentProxy() 场景。
- DAO-03 基于方法名前缀，可能误报/漏报。
- WEB-01 未分析 setter 赋值，可能对只读字段误报。
- SEC-03 未覆盖 PreparedStatement 参数化（应放过）。
- RES-01 未检测"包装资源"传递。
- OBS-01 未检测 String.format。
- SEC-04 未解析 CorsConfiguration bean 结构。
- DAO-04 未检测 HTTP 客户端抽象层。

### 3.4 工具与报告
- Checkstyle/PMD jar 未下载；工具链 SKIPPED。
- LLM token usage 未汇总到报告。
- 报告保留策略未做。
- stderr 编码 Windows Git Bash 需 -Dfile.encoding=UTF-8。

### 3.5 内容一致性
- docs/rules/*.md 与 rules/*.yaml 的参数差异未同步（仅 yaml 生效）。
- SKILL.md 内容未更新至 G 阶段产出。

## 4. 下一步

阶段 G 完成后，MVP 的"基础能力"已闭环：
- 三种模式可运行；规则可加载、可分发；15 条规则可实际检出；
- 报告符合 A6；CI 就绪；102 个测试。

**建议进入 H 阶段：MVP 验收**，具体：
- H1：真实项目验证（≥ 3 个 Spring Boot 项目）；
- H2：基准评测集（≥ 30 案例）；
- H3：性能验证（1000 行 diff P95 ≤ 60s）；
- H4：量化指标计算（S5 检出率、S6 误报率）；
- H5：MVP 验收报告。

或按需调整。