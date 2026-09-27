# G 阶段交接摘要

## 1. 基本信息

- 项目：Spring Review Skill
- 阶段：G MVP 收口（部分）
- 产出：规则参数、AST 检测器、工具配置、单测
- 状态：G1/G2/G.1a～G.1e 冻结；G3/G4/G5 转入 H 阶段
- 约束遵守：只读审查；不改目标仓库；不提交；不训练模型；A5/A6 未改

## 2. 关键决定

1. 规则参数只对可立即生效的 11 条落地；其余 44 条待后续补检测器。
2. 检测器限流：Regex 10/文件/规则；AST 回退 5/文件/规则；AST 精准不限流。
3. M09 ParsedSource 新增 compilationUnit 字段（nullable，不参与 JSON）。
4. AstPreciseDetector 覆盖 15 条高频规则；RuleEngine 按"专用优先"顺序注册。
5. 工具与配置属于 skill 包；路径基于 skillHome（SPRING_REVIEW_HOME 或 user.dir）。
6. PMD 7 CLI 兼容：--file-list、--use-version java-17。
7. 工具缺失 → SKIPPED，不失败。

## 3. 交付清单

### 代码（G 阶段新增/修改）
- src/main/java/dev/springreview/engine/AstPreciseDetector.java（15 规则）
- src/main/java/dev/springreview/engine/RegexDetector.java（限流）
- src/main/java/dev/springreview/engine/AstHeuristicDetector.java（限流）
- src/main/java/dev/springreview/engine/RuleEngine.java（注册顺序）
- src/main/java/dev/springreview/engine/ConfigKeyDetector.java（evidence 兜底）
- src/main/java/dev/springreview/engine/IssueAggregator.java（evidence 兜底）
- src/main/java/dev/springreview/parser/ParsedSource.java（+compilationUnit）
- src/main/java/dev/springreview/parser/JavaSourceParser.java（三处构造点）
- src/main/java/dev/springreview/tools/RuleMapping.java（fallbackByTool）
- src/main/java/dev/springreview/tools/ToolPaths.java（skillHome）
- src/main/java/dev/springreview/tools/CheckstyleRunner.java
- src/main/java/dev/springreview/tools/PmdRunner.java
- src/main/java/dev/springreview/tools/SpotbugsRunner.java
- src/main/java/dev/springreview/tools/StaticAnalysisFacade.java

### 测试
- src/test/java/dev/springreview/engine/AstPreciseDetectorTest.java（39 用例）

### 配置
- config/checkstyle/checkstyle.xml
- config/pmd/pmd.xml
- config/README.md

### 脚本
- scripts/patch-rule-parameters.py

### 规则
- rules/*.yaml（11 条含 parameters）

### 文档
- docs/34-规则参数设计.md

## 4. 测试与覆盖

- 测试总数：102（unit + integration + e2e）。
- AST 精准检测器：15 条规则 × 命中/不命中，覆盖正反例。
- 端到端：FILES/MODULE/DIFF 三模式。
- 规则库：55 条加载；约 20 条可实际检出。

## 5. 已知遗留

### 未做（A1 要求）
- 真实项目验证 ≥ 3 个（G3）
- 基准评测集 ≥ 30 案例（G4）
- 性能验证 P95 ≤ 60s（G5）
- 检出率 ≥ 80%、误报率 ≤ 20% 量化

### 检测器未覆盖的 35 条规则
- DI-03/05/06/07、WEB-03/05、DAO-05、SEC-05、CON-02～05、
  PERF-01/02、OBS-02/03、EXC-03/04/05、RES-02、TEST-01/02、
  STYLE-01/02、NULL-01/02、CF-02、CLOUD-02 等。

### 检测器精细度
- DAO-02、DAO-03、WEB-01、SEC-03、RES-01、OBS-01、SEC-04、DAO-04
  各有边界未覆盖。

### 工具与报告
- Checkstyle/PMD jar 未下载；工具链 SKIPPED。
- LLM token usage 未汇总。
- docs/rules/*.md 与 rules/*.yaml 参数差异未同步。
- SKILL.md 未更新至 G 阶段产出。

## 6. 下一步

进入 **H 阶段：MVP 验收**：
- H1：真实项目验证（≥ 3 个 Spring Boot 项目）；
- H2：基准评测集（≥ 30 案例）；
- H3：性能验证（P95 ≤ 60s）；
- H4：量化指标计算（S5、S6）；
- H5：MVP 验收报告。

## 7. 可直接粘贴到下一个对话的上下文包

```text
【项目】Spring Review Skill，只读审查；Java 17+/Spring Boot 3.x。
【阶段】G 已完成并冻结；产出规则参数、AST 检测器、工具配置、单测。
【状态】MVP 基础能力闭环：三模式、55 条规则加载、15 条 AST 检出、
       102 测试通过、CI 就绪。
【G 关键决定】
1. 11 条规则参数落地（REGEX + ConfigKey）。
2. 检测器限流：Regex 10、AST 回退 5、AST 精准不限。
3. M09 ParsedSource 增 compilationUnit 字段（nullable）。
4. AstPreciseDetector 覆盖 15 条规则；RuleEngine 专用优先。
5. 工具/配置属 skill 包；路径基于 skillHome；缺失 SKIPPED。
6. PMD 7 CLI：--file-list + --use-version java-17。
【已知遗留】
- G3 真实项目验证（≥3）、G4 评测集（≥30）、G5 性能（P95≤60s）未做。
- 40 条规则仅有元数据无检测器。
- 部分检测器边界未覆盖。
- Checkstyle/PMD jar 未下载。
- LLM token usage 未汇总。
- docs/rules/*.md 与 rules/*.yaml 参数差异未同步。
【下一步】H 阶段（MVP 验收）：H1 真实项目验证；H2 评测集；
             H3 性能验证；H4 量化指标；H5 验收报告。
```