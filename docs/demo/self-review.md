# 能力样例：对 spring-review-skill 自身源码的审查

本页是工具审查**自己源码**（MODULE 模式，120 个文件）的输出样例，数据取自
2026-09-28 的真实运行，未经修饰。审查自身代码是最诚实的 dogfooding：工具报出的
每个问题，都发生在实现这套工具的代码里。

## 运行方式

```bash
mvn -q -DskipTests package
java -jar target/spring-review-skill-*.jar \
  --mode MODULE --repo . --module src --offline \
  --output-dir reports/self-review
```

## Summary

- 总发现：**96**（BLOCKER 4 / CRITICAL 0 / MAJOR 7 / MINOR 85）
- 类别分布：SEC 4，EXC 7，I18N 85
- blocking：4；耗时 1985ms；模式 offline（不调用 LLM）

## 代表性发现（MAJOR 及以上）

| 级别 | 规则 | 位置 | 置信度 | 消息 |
| --- | --- | --- | --- | --- |
| BLOCKER | `SRS-SEC-01-001` | `src/test/java/dev/springreview/DiffModeEndToEndTest.java:187` | MEDIUM | 禁止硬编码密钥、密码或 token。（测试路径：假凭据/夹具常见，建议人工确认） |
| BLOCKER | `SRS-SEC-01-001` | `src/test/java/dev/springreview/DiffModeEndToEndTest.java:222` | MEDIUM | 禁止硬编码密钥、密码或 token。（测试路径：假凭据/夹具常见，建议人工确认） |
| BLOCKER | `SRS-SEC-01-001` | `src/test/java/dev/springreview/DiffModeEndToEndTest.java:249` | MEDIUM | 禁止硬编码密钥、密码或 token。（测试路径：假凭据/夹具常见，建议人工确认） |
| BLOCKER | `SRS-SEC-01-001` | `src/test/java/dev/springreview/EndToEndIntegrationTest.java:45` | MEDIUM | 禁止硬编码密钥、密码或 token。（测试路径：假凭据/夹具常见，建议人工确认） |
| MAJOR | `SRS-EXC-01-001` | `src/main/java/dev/springreview/config/ConfigLoader.java:223` | MEDIUM | 捕获异常后不得静默吞没，应记录日志、抛出或转换为业务异常。（catch 块含注释说明意图，建议人工确认） |
| MAJOR | `SRS-EXC-01-001` | `src/main/java/dev/springreview/config/ConfigLoader.java:240` | MEDIUM | 捕获异常后不得静默吞没，应记录日志、抛出或转换为业务异常。（catch 块含注释说明意图，建议人工确认） |
| MAJOR | `SRS-EXC-01-001` | `src/main/java/dev/springreview/config/ConfigLoader.java:258` | MEDIUM | 捕获异常后不得静默吞没，应记录日志、抛出或转换为业务异常。（catch 块含注释说明意图，建议人工确认） |
| MAJOR | `SRS-EXC-01-001` | `src/main/java/dev/springreview/exit/SummaryPrinter.java:47` | MEDIUM | 捕获异常后不得静默吞没，应记录日志、抛出或转换为业务异常。（catch 块含注释说明意图，建议人工确认） |
| MAJOR | `SRS-EXC-01-001` | `src/main/java/dev/springreview/tools/PmdRunner.java:103` | MEDIUM | 捕获异常后不得静默吞没，应记录日志、抛出或转换为业务异常。（catch 块含注释说明意图，建议人工确认） |
| MAJOR | `SRS-EXC-01-001` | `src/test/java/dev/springreview/scope/LineRangeMergeTest.java:43` | MEDIUM | 捕获异常后不得静默吞没，应记录日志、抛出或转换为业务异常。（catch 块含注释说明意图，建议人工确认） |
| MAJOR | `SRS-EXC-01-001` | `src/test/java/dev/springreview/scope/LineRangeMergeTest.java:49` | MEDIUM | 捕获异常后不得静默吞没，应记录日志、抛出或转换为业务异常。（catch 块含注释说明意图，建议人工确认） |

注意 SEC 类的置信度全部为 MEDIUM——这些命中位于 `src/test/`，按 SKILL.md §8
边界策略不自动豁免安全类发现，但降级置信度并附人工确认提示（测试里的
假凭据是夹具惯例，不应打断 CI）。

## 检测精度治理前后对比

检测引擎经过一轮精度治理（占位符插值、类别→文件类型绑定、边界策略、
空 catch 分档），同口径自审查的前后对比：

| 指标 | 治理前 | 治理后 |
| --- | --- | --- |
| CRITICAL 误报（BUILD 规则误扫 .java） | 1 | **0** |
| 消息占位符泄漏 | 1 | **0** |
| SEC 测试夹具置信度 | HIGH ×4 | MEDIUM ×4 |
| EXC 注释型空 catch 置信度 | HIGH ×7 | MEDIUM ×7 |
| blocking | 5 | **4** |

降噪不掩盖：EXC 的 7 条命中**仍然是发现**（规则规格规定注释不算处理逻辑），
只是从"直接高危"降为"建议人工确认"。

## 完整报告

`reports/` 目录默认 gitignore（每次运行重新生成）。克隆本仓库后按上面
「运行方式」的命令执行，即可得到与本文数据一致的完整输出：

- `reports/self-review/latest/review-report.md`——全部 96 条发现与逐条修复建议
- `reports/self-review/latest/review-report.json`——机器可读格式，符合 A6 Schema
- 历史归档在 `reports/history/YYYY/MM/DD/<report_id>/`
