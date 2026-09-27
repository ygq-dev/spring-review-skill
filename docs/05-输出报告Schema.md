# docs/05-输出报告Schema.md

## 1. 设计原则

- 只读审查：报告只描述发现、证据、建议与运行指标，不修改代码、不提交、不训练模型。
- 单次审查：本 Schema 仅定义一次审查输出，不定义汇总报告、索引文件、规则文件。
- 严格可校验：完整 Schema 基于 JSON Schema Draft 2020-12，顶层 `additionalProperties: false`。
- 枚举复用 A2：`severity`、`confidence`、`detection_method`、`tool` 必须复用 A2 枚举。
- 可追溯：`issue.rule_id` 可反查 `rules/index.json`，`issue.test_case_id` 可反查 `tests/cases/index.json`。
- 可量化：承载 A1 中 S4、S7、S9、S15、S17 所需字段。
- 可脱敏：`evidence`、`snippet`、`metadata` 可承载脱敏后信息，避免敏感代码外部泄露。
- 路径统一：文件路径使用仓库根相对路径、POSIX 风格。

## 2. 报告顶层结构

顶层字段：

| 字段             | 必填 | 说明                                 |
| ---------------- | ---- | ------------------------------------ |
| `report_id`      | 是   | 单次审查报告唯一标识                 |
| `schema_version` | 是   | 报告 Schema 版本，语义化版本         |
| `generated_at`   | 是   | 生成时间，RFC 3339 / date-time       |
| `scope`          | 是   | 审查范围                             |
| `summary`        | 是   | 汇总统计                             |
| `issues`         | 是   | 问题条目数组                         |
| `tool_runs`      | 是   | 工具运行记录数组                     |
| `metrics`        | 是   | 指标                                 |
| `metadata`       | 否   | 扩展元数据，如人工评审、隐私事件计数 |

## 3. 必填字段与可选字段清单

### 3.1 顶层

- 必填：`report_id`、`schema_version`、`generated_at`、`scope`、`summary`、`issues`、`tool_runs`、`metrics`
- 可选：`metadata`

### 3.2 scope

- 必填：`mode`、`project`、`base_ref`、`head_ref`、`paths`、`diff_lines`
- 可选：无

### 3.3 summary

- 必填：`total_issues`、`by_severity`、`by_category`、`blocking`、`duration_ms`、`token_usage`
- 可选：无

### 3.4 issues 每项

- 必填：`issue_id`、`rule_id`、`severity`、`confidence`、`detection_method`、`file`、`line`、`column`、`evidence`、`message`、`remediation`
- 可选：`tool`、`tool_rule_id`、`test_case_id`、`snippet`、`fingerprint`、`first_seen_at`、`last_seen_at`、`issue_status`、`tags`

### 3.5 tool_runs 每项

- 必填：`tool`、`tool_version`、`status`、`started_at`、`ended_at`、`duration_ms`、`exit_code`
- 可选：`issues_count`、`error_message`

### 3.6 metrics

- 必填：`total_files_scanned`、`total_lines_scanned`、`duration_ms`、`tool_success_rate`
- 可选：`cache_hit_rate`、`llm_calls`、`by_severity`、`by_category`

## 4. 字段的完整 Schema 定义

完整 JSON Schema 见第 10 节。关键字段定义如下。

### 4.1 顶层与通用

| 字段             | type   | format    | enum/pattern      | min/max | 必填 | description          | examples                             |
| ---------------- | ------ | --------- | ----------------- | ------- | ---- | -------------------- | ------------------------------------ |
| `report_id`      | string | —         | ^RPT-\d{4}-\d{4}$ | —       | 是   | 单次审查报告唯一标识 | `RPT-2026-0001`                      |
| `schema_version` | string | —         | `^\d+\.\d+\.\d+$` | —       | 是   | 报告 Schema 版本     | `1.0.0`                              |
| `generated_at`   | string | date-time | —                 | —       | 是   | 生成时间             | `2026-01-01T12:00:00Z`               |
| `metadata`       | object | —         | —                 | —       | 否   | 扩展元数据           | `{"human_review":{"approved":true}}` |

### 4.2 scope

| 字段         | type          | format | enum/pattern              | min/max   | 必填 | description                | examples                   |
| ------------ | ------------- | ------ | ------------------------- | --------- | ---- | -------------------------- | -------------------------- |
| `mode`       | string        | —      | `DIFF`、`MODULE`、`FILES` | —         | 是   | 审查模式                   | `DIFF`                     |
| `project`    | string        | —      | minLength 1               | —         | 是   | 项目名                     | `spring-review-skill-demo` |
| `base_ref`   | string        | —      | —                         | —         | 是   | 基准引用                   | `main`                     |
| `head_ref`   | string        | —      | —                         | —         | 是   | 目标引用                   | `feature/security-fix`     |
| `paths`      | array[string] | —      | minLength 1               | —         | 是   | 仓库根相对路径，POSIX 风格 | `["src/main/java"]`        |
| `diff_lines` | integer       | —      | —                         | minimum 0 | 是   | diff 行数                  | `1200`                     |

### 4.3 summary

| 字段           | type    | format | enum/pattern        | min/max      | 必填 | description    | examples                |
| -------------- | ------- | ------ | ------------------- | ------------ | ---- | -------------- | ----------------------- |
| `total_issues` | integer | —      | —                   | minimum 0    | 是   | 问题总数       | `1`                     |
| `by_severity`  | object  | —      | 属性名复用 severity | 值 minimum 0 | 是   | 按严重级别统计 | `{"CRITICAL":1}`        |
| `by_category`  | object  | —      | 属性名复用一级分类  | 值 minimum 0 | 是   | 按类别统计     | `{"SEC":1}`             |
| `blocking`     | integer | —      | —                   | minimum 0    | 是   | 阻塞问题数     | `1`                     |
| `duration_ms`  | integer | —      | —                   | minimum 0    | 是   | 审查总耗时     | `58000`                 |
| `token_usage`  | object  | —      | —                   | —            | 是   | Token 用量     | `{"total_tokens":1500}` |

by_severity：按严重级别统计。建议包含全部 5 个 severity 键，未出现的填 0。
by_category：按一级分类统计，建议仅列出非零项。

### 4.4 issue

| 字段               | type          | format    | enum/pattern                                            | min/max   | 必填 | description    | examples                                        |
| ------------------ | ------------- | --------- | ------------------------------------------------------- | --------- | ---- | -------------- | ----------------------------------------------- |
| `issue_id`         | string        | —         | `^ISSUE-[0-9A-F]{8}$`                                   | —         | 是   | 问题唯一 ID    | `ISSUE-1A2B3C4D`                                |
| `rule_id`          | string        | —         | A5 rule_id pattern                                      | —         | 是   | 规则 ID        | `SRS-SEC-01-001`                                |
| `severity`         | string        | —         | A2 severity                                             | —         | 是   | 严重级别       | `CRITICAL`                                      |
| `confidence`       | string        | —         | A2 confidence                                           | —         | 是   | 置信度         | `HIGH`                                          |
| `detection_method` | string        | —         | A2 detection_method                                     | —         | 是   | 检测方法       | `STATIC_ANALYSIS`                               |
| `file`             | string        | —         | minLength 1                                             | —         | 是   | 仓库根相对路径 | `src/main/java/com/example/UserController.java` |
| `line`             | integer       | —         | —                                                       | minimum 1 | 是   | 行号           | `42`                                            |
| `column`           | integer       | —         | —                                                       | minimum 1 | 是   | 列号           | `9`                                             |
| `evidence`         | string        | —         | minLength 1                                             | —         | 是   | 证据           | `String sql = ...`                              |
| `message`          | string        | —         | minLength 1                                             | —         | 是   | 问题说明       | `检测到 SQL 拼接`                               |
| `remediation`      | string        | —         | minLength 1                                             | —         | 是   | 修复建议       | `使用 PreparedStatement`                        |
| `tool`             | string        | —         | A2 tool                                                 | —         | 否   | 工具           | `PMD`                                           |
| `tool_rule_id`     | string        | —         | —                                                       | —         | 否   | 工具规则 ID    | `AvoidSqlInjection`                             |
| `test_case_id`     | string        | —         | A5 test_case_id pattern                                 | —         | 否   | 测试用例 ID    | `TC-SEC-001`                                    |
| `snippet`          | string        | —         | —                                                       | —         | 否   | 代码片段       | `...`                                           |
| `fingerprint`      | string        | —         | —                                                       | —         | 否   | 指纹           | `sha256:abcd1234`                               |
| `first_seen_at`    | string        | date-time | —                                                       | —         | 否   | 首次发现时间   | `2026-01-01T12:00:00Z`                          |
| `last_seen_at`     | string        | date-time | —                                                       | —         | 否   | 最近发现时间   | `2026-01-01T12:00:00Z`                          |
| `issue_status`     | string        | —         | `OPEN`、`FIXED`、`IGNORED`、`FALSE_POSITIVE`、`WONTFIX` | —         | 否   | 问题状态       | `OPEN`                                          |
| `tags`             | array[string] | —         | minLength 1                                             | —         | 否   | 标签           | `["security"]`                                  |

### 4.5 tool_run

| 字段            | type    | format    | enum/pattern                              | min/max   | 必填 | description | examples               |
| --------------- | ------- | --------- | ----------------------------------------- | --------- | ---- | ----------- | ---------------------- |
| `tool`          | string  | —         | A2 tool                                   | —         | 是   | 工具        | `PMD`                  |
| `tool_version`  | string  | —         | minLength 1                               | —         | 是   | 工具版本    | `7.0.0`                |
| `status`        | string  | —         | `SUCCESS`、`FAILED`、`SKIPPED`、`TIMEOUT` | —         | 是   | 运行状态    | `SUCCESS`              |
| `started_at`    | string  | date-time | —                                         | —         | 是   | 开始时间    | `2026-01-01T11:59:00Z` |
| `ended_at`      | string  | date-time | —                                         | —         | 是   | 结束时间    | `2026-01-01T11:59:05Z` |
| `duration_ms`   | integer | —         | —                                         | minimum 0 | 是   | 耗时        | `5000`                 |
| `exit_code`     | integer | —         | —                                         | —         | 是   | 退出码      | `0`                    |
| `issues_count`  | integer | —         | —                                         | minimum 0 | 否   | 问题数      | `1`                    |
| `error_message` | string  | —         | —                                         | —         | 否   | 错误信息    | `timeout`              |

### 4.6 metrics

| 字段                  | type    | format | enum/pattern        | min/max      | 必填 | description    | examples         |
| :-------------------- | :------ | :----- | :------------------ | :----------- | :--- | :------------- | :--------------- |
| `total_files_scanned` | integer | —      | —                   | minimum 0    | 是   | 扫描文件数     | `10`             |
| `total_lines_scanned` | integer | —      | —                   | minimum 0    | 是   | 扫描行数       | `1200`           |
| `duration_ms`         | integer | —      | —                   | minimum 0    | 是   | 总耗时         | `58000`          |
| `tool_success_rate`   | number  | —      | —                   | 0..1         | 是   | 工具成功率     | `1.0`            |
| `cache_hit_rate`      | number  | —      | —                   | 0..1         | 否   | 缓存命中率     | `0.5`            |
| `llm_calls`           | integer | —      | —                   | minimum 0    | 否   | LLM 调用次数   | `1`              |
| `by_severity`         | object  | —      | 属性名复用 severity | 值 minimum 0 | 否   | 按严重级别统计 | `{"CRITICAL":1}` |
| `by_category`         | object  | —      | 属性名复用一级分类  | 值 minimum 0 | 否   | 按类别统计     | `{"SEC":1}`      |

## 5. 问题条目 issue 的结构定义

`issue` 表示单条审查发现。

必填字段：

- `issue_id`：`ISSUE-<8 位大写十六进制>`
- `rule_id`：必须匹配 A5 `rule_id` pattern
- `severity`：A2 severity
- `confidence`：A2 confidence
- `detection_method`：A2 detection_method
- `file`：仓库根相对路径，POSIX 风格
- `line`：整数，≥1
- `column`：整数，≥1
- `evidence`：非空字符串
- `message`：非空字符串
- `remediation`：非空字符串

可选字段：

- `tool`：A2 tool
- `tool_rule_id`：工具原生规则 ID
- `test_case_id`：A5 `test_case_id` pattern
- `snippet`：代码片段，可脱敏
- `fingerprint`：问题指纹
- `first_seen_at`、`last_seen_at`：date-time
- `issue_status`：`OPEN`、`FIXED`、`IGNORED`、`FALSE_POSITIVE`、`WONTFIX`
- `tags`：字符串数组

## 6. 工具运行记录 tool_run 的结构定义

`tool_run` 表示一次工具运行。

必填字段：

- `tool`：A2 tool
- `tool_version`：非空字符串
- `status`：`SUCCESS`、`FAILED`、`SKIPPED`、`TIMEOUT`
- `started_at`：date-time
- `ended_at`：date-time
- `duration_ms`：整数，≥0
- `exit_code`：整数

可选字段：

- `issues_count`：整数，≥0
- `error_message`：字符串

## 7. 指标 metrics 的结构定义

`metrics` 表示本次审查的量化指标。

必填字段：

- `total_files_scanned`：整数，≥0
- `total_lines_scanned`：整数，≥0
- `duration_ms`：整数，≥0
- `tool_success_rate`：number，0..1

可选字段：

- `cache_hit_rate`：number，0..1
- `llm_calls`：整数，≥0
- `by_severity`：对象，属性名复用 severity
- `by_category`：对象，属性名复用一级分类

## 8. 条件约束

JSON Schema 中实现或工具侧校验以下条件：

- `scope.mode = DIFF` 时，`base_ref`、`head_ref` 应非空。
- `scope.mode = MODULE` 或 `FILES` 时，`paths` 应至少包含 1 项。
- `issue.first_seen_at` 与 `issue.last_seen_at` 同时出现或同时不出现。
- `issue.issue_status = FALSE_POSITIVE` 时，建议 `confidence = LOW` 或提供人工复核说明。
- `tool_run.status = FAILED` 或 `TIMEOUT` 时，建议填写 `error_message`。
- `summary.total_issues` 应等于 `issues` 数组长度。
- `summary.by_severity` 值之和应等于 `summary.total_issues`。
- `summary.by_category` 值之和应等于 `summary.total_issues`。
- `summary.blocking` 建议等于 `severity` 为 `BLOCKER` 或 `CRITICAL` 的问题数。

## 9. 引用完整性约束

JSON Schema 无法直接校验仓库内索引文件，需由审查工具或 CI 校验：

- `issue.rule_id` 必须存在于 `rules/index.json` 的 `items[].id`。
- `issue.test_case_id` 若存在，必须存在于 `tests/cases/index.json` 的 `items[].id`。
- `issue.tool` 若存在，必须与 `tool_runs[].tool` 中的某次运行一致。
- `issue.tool_rule_id` 若存在，应属于对应 `tool` 的原生规则。
- `issue.file` 应为仓库根相对路径，POSIX 风格。
- `scope.paths` 中每一项应为仓库根相对路径，POSIX 风格。
- 所有 `date-time` 字段应为 RFC 3339 格式。
- 所有枚举值必须严格复用 A2，不得扩展。

## 10. 完整 JSON Schema

可直接保存为 `schema/review-report.schema.json`。

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://spring-review-skill.dev/schema/review-report.schema.json",
  "title": "Spring Review Skill 单次审查输出报告",
  "type": "object",
  "additionalProperties": false,
  "required": [
    "report_id",
    "schema_version",
    "generated_at",
    "scope",
    "summary",
    "issues",
    "tool_runs",
    "metrics"
  ],
  "properties": {
    "report_id": {
      "type": "string",
      "pattern": "^RPT-\\d{4}-\\d{4}$",
      "description": "单次审查报告唯一标识",
      "examples": ["RPT-2026-0001"]
    },
    "schema_version": {
      "type": "string",
      "pattern": "^\\d+\\.\\d+\\.\\d+$",
      "description": "报告 Schema 版本",
      "examples": ["1.0.0"]
    },
    "generated_at": {
      "type": "string",
      "format": "date-time",
      "description": "生成时间，RFC 3339",
      "examples": ["2026-01-01T12:00:00Z"]
    },
    "scope": {
      "$ref": "#/$defs/scope"
    },
    "summary": {
      "$ref": "#/$defs/summary"
    },
    "issues": {
      "type": "array",
      "items": {
        "$ref": "#/$defs/issue"
      }
    },
    "tool_runs": {
      "type": "array",
      "items": {
        "$ref": "#/$defs/tool_run"
      }
    },
    "metrics": {
      "$ref": "#/$defs/metrics"
    },
    "metadata": {
      "type": "object",
      "description": "扩展元数据，可选",
      "additionalProperties": true
    }
  },
  "allOf": [
    {
  	  "if": {
    	"properties": {
      	  "scope": {
        	"properties": { "mode": { "enum": ["MODULE", "FILES"] } },
        	"required": ["mode"]
      	  }
    	},
    	"required": ["scope"]
  	  },
  	  "then": {
    	"properties": {
      	  "scope": {
        	"properties": { "paths": { "minItems": 1 } }
      	  }
    	}
  	  }
	},
    {
      "if": {
        "properties": {
          "scope": {
            "properties": {
              "mode": {
                "const": "DIFF"
              }
            },
            "required": ["mode"]
          }
        },
        "required": ["scope"]
      },
      "then": {
        "properties": {
          "scope": {
            "properties": {
              "base_ref": {
                "minLength": 1
              },
              "head_ref": {
                "minLength": 1
              }
            }
          }
        }
      }
    }
  ],
  "$defs": {
    "severity": {
      "type": "string",
      "enum": ["BLOCKER", "CRITICAL", "MAJOR", "MINOR", "INFO"]
    },
    "confidence": {
      "type": "string",
      "enum": ["HIGH", "MEDIUM", "LOW"]
    },
    "detection_method": {
      "type": "string",
      "enum": [
        "STATIC_ANALYSIS",
        "AST",
        "ARCH_TEST",
        "RULE_ENGINE",
        "REGEX",
        "HEURISTIC",
        "HYBRID",
        "MANUAL"
      ]
    },
    "tool": {
      "type": "string",
      "enum": ["CHECKSTYLE", "PMD", "ARCHUNIT", "SPOTBUGS", "CUSTOM"]
    },
    "category": {
      "type": "string",
      "enum": [
        "AR",
        "CF",
        "DI",
        "WEB",
        "DAO",
        "SEC",
        "CON",
        "PERF",
        "OBS",
        "EXC",
        "RES",
        "TEST",
        "BUILD",
        "STYLE",
        "NULL",
        "I18N",
        "MIG",
        "CLOUD"
      ]
    },
    "issue_status": {
      "type": "string",
      "enum": ["OPEN", "FIXED", "IGNORED", "FALSE_POSITIVE", "WONTFIX"]
    },
    "tool_status": {
      "type": "string",
      "enum": ["SUCCESS", "FAILED", "SKIPPED", "TIMEOUT"]
    },
    "rule_id": {
      "type": "string",
      "pattern": "^SRS-(AR|CF|DI|WEB|DAO|SEC|CON|PERF|OBS|EXC|RES|TEST|BUILD|STYLE|NULL|I18N|MIG|CLOUD)-\\d{2}-\\d{3}$"
    },
    "test_case_id": {
      "type": "string",
      "pattern": "^TC-(AR|CF|DI|WEB|DAO|SEC|CON|PERF|OBS|EXC|RES|TEST|BUILD|STYLE|NULL|I18N|MIG|CLOUD)-\\d{3}$"
    },
    "by_severity": {
      "type": "object",
      "propertyNames": {
        "$ref": "#/$defs/severity"
      },
      "additionalProperties": {
        "type": "integer",
        "minimum": 0
      }
    },
    "by_category": {
      "type": "object",
      "propertyNames": {
        "$ref": "#/$defs/category"
      },
      "additionalProperties": {
        "type": "integer",
        "minimum": 0
      }
    },
    "token_usage": {
      "type": "object",
      "additionalProperties": false,
      "required": ["total_tokens"],
      "properties": {
        "input_tokens": {
          "type": "integer",
          "minimum": 0
        },
        "output_tokens": {
          "type": "integer",
          "minimum": 0
        },
        "total_tokens": {
          "type": "integer",
          "minimum": 0
        },
        "cost_usd": {
          "type": "number",
          "minimum": 0
        }
      }
    },
    "scope": {
      "type": "object",
      "additionalProperties": false,
      "required": [
        "mode",
        "project",
        "base_ref",
        "head_ref",
        "paths",
        "diff_lines"
      ],
      "properties": {
        "mode": {
          "type": "string",
          "enum": ["DIFF", "MODULE", "FILES"]
        },
        "project": {
          "type": "string",
          "minLength": 1
        },
        "base_ref": {
          "type": "string"
        },
        "head_ref": {
          "type": "string"
        },
        "paths": {
          "type": "array",
          "items": {
            "type": "string",
            "minLength": 1
          }
        },
        "diff_lines": {
          "type": "integer",
          "minimum": 0
        }
      }
    },
    "summary": {
      "type": "object",
      "additionalProperties": false,
      "required": [
        "total_issues",
        "by_severity",
        "by_category",
        "blocking",
        "duration_ms",
        "token_usage"
      ],
      "properties": {
        "total_issues": {
          "type": "integer",
          "minimum": 0
        },
        "by_severity": {
          "$ref": "#/$defs/by_severity"
        },
        "by_category": {
          "$ref": "#/$defs/by_category"
        },
        "blocking": {
          "type": "integer",
          "minimum": 0
        },
        "duration_ms": {
          "type": "integer",
          "minimum": 0
        },
        "token_usage": {
          "$ref": "#/$defs/token_usage"
        }
      }
    },
    "issue": {
      "type": "object",
      "additionalProperties": false,
      "required": [
        "issue_id",
        "rule_id",
        "severity",
        "confidence",
        "detection_method",
        "file",
        "line",
        "column",
        "evidence",
        "message",
        "remediation"
      ],
      "properties": {
        "issue_id": {
          "type": "string",
          "pattern": "^ISSUE-[0-9A-F]{8}$"
        },
        "rule_id": {
          "$ref": "#/$defs/rule_id"
        },
        "severity": {
          "$ref": "#/$defs/severity"
        },
        "confidence": {
          "$ref": "#/$defs/confidence"
        },
        "detection_method": {
          "$ref": "#/$defs/detection_method"
        },
        "file": {
          "type": "string",
          "minLength": 1
        },
        "line": {
          "type": "integer",
          "minimum": 1
        },
        "column": {
          "type": "integer",
          "minimum": 1
        },
        "evidence": {
          "type": "string",
          "minLength": 1
        },
        "message": {
          "type": "string",
          "minLength": 1
        },
        "remediation": {
          "type": "string",
          "minLength": 1
        },
        "tool": {
          "$ref": "#/$defs/tool"
        },
        "tool_rule_id": {
          "type": "string"
        },
        "test_case_id": {
          "$ref": "#/$defs/test_case_id"
        },
        "snippet": {
          "type": "string"
        },
        "fingerprint": {
          "type": "string"
        },
        "first_seen_at": {
          "type": "string",
          "format": "date-time"
        },
        "last_seen_at": {
          "type": "string",
          "format": "date-time"
        },
        "issue_status": {
          "$ref": "#/$defs/issue_status"
        },
        "tags": {
          "type": "array",
          "items": {
            "type": "string",
            "minLength": 1
          }
        }
      },
      "dependentRequired": {
        "first_seen_at": ["last_seen_at"],
        "last_seen_at": ["first_seen_at"]
      }
    },
    "tool_run": {
      "type": "object",
      "additionalProperties": false,
      "required": [
        "tool",
        "tool_version",
        "status",
        "started_at",
        "ended_at",
        "duration_ms",
        "exit_code"
      ],
      "properties": {
        "tool": {
          "$ref": "#/$defs/tool"
        },
        "tool_version": {
          "type": "string",
          "minLength": 1
        },
        "status": {
          "$ref": "#/$defs/tool_status"
        },
        "started_at": {
          "type": "string",
          "format": "date-time"
        },
        "ended_at": {
          "type": "string",
          "format": "date-time"
        },
        "duration_ms": {
          "type": "integer",
          "minimum": 0
        },
        "exit_code": {
          "type": "integer"
        },
        "issues_count": {
          "type": "integer",
          "minimum": 0
        },
        "error_message": {
          "type": "string"
        }
      }
    },
    "metrics": {
      "type": "object",
      "additionalProperties": false,
      "required": [
        "total_files_scanned",
        "total_lines_scanned",
        "duration_ms",
        "tool_success_rate"
      ],
      "properties": {
        "total_files_scanned": {
          "type": "integer",
          "minimum": 0
        },
        "total_lines_scanned": {
          "type": "integer",
          "minimum": 0
        },
        "duration_ms": {
          "type": "integer",
          "minimum": 0
        },
        "tool_success_rate": {
          "type": "number",
          "minimum": 0,
          "maximum": 1
        },
        "cache_hit_rate": {
          "type": "number",
          "minimum": 0,
          "maximum": 1
        },
        "llm_calls": {
          "type": "integer",
          "minimum": 0
        },
        "by_severity": {
          "$ref": "#/$defs/by_severity"
        },
        "by_category": {
          "$ref": "#/$defs/by_category"
        }
      }
    }
  }
}
```

## 11. 两个完整示例

### 11.1 包含高危问题的报告

```json
{
  "report_id": "RPT-2026-0001",
  "schema_version": "1.0.0",
  "generated_at": "2026-01-01T12:00:00Z",
  "scope": {
    "mode": "DIFF",
    "project": "spring-review-skill-demo",
    "base_ref": "main",
    "head_ref": "feature/security-fix",
    "paths": ["src/main/java/com/example/UserController.java"],
    "diff_lines": 1200
  },
  "summary": {
    "total_issues": 1,
    "by_severity": {
      "BLOCKER": 0,
      "CRITICAL": 1,
      "MAJOR": 0,
      "MINOR": 0,
      "INFO": 0
    },
    "by_category": {
      "SEC": 1
    },
    "blocking": 1,
    "duration_ms": 58000,
    "token_usage": {
      "input_tokens": 1200,
      "output_tokens": 300,
      "total_tokens": 1500,
      "cost_usd": 0.02
    }
  },
  "issues": [
    {
      "issue_id": "ISSUE-1A2B3C4D",
      "rule_id": "SRS-SEC-01-001",
      "severity": "CRITICAL",
      "confidence": "HIGH",
      "detection_method": "STATIC_ANALYSIS",
      "file": "src/main/java/com/example/UserController.java",
      "line": 42,
      "column": 9,
      "evidence": "String sql = \"SELECT * FROM users WHERE name = '\" + name + \"'\";",
      "message": "检测到 SQL 拼接，存在 SQL 注入风险。",
      "remediation": "使用 PreparedStatement 或 Spring Data JPA 参数绑定。",
      "tool": "PMD",
      "tool_rule_id": "AvoidSqlInjection",
      "test_case_id": "TC-SEC-001",
      "snippet": "String sql = \"SELECT * FROM users WHERE name = '\" + name + \"'\";",
      "fingerprint": "sha256:abcd1234",
      "issue_status": "OPEN",
      "tags": ["security", "sql-injection"]
    }
  ],
  "tool_runs": [
    {
      "tool": "PMD",
      "tool_version": "7.0.0",
      "status": "SUCCESS",
      "started_at": "2026-01-01T11:59:00Z",
      "ended_at": "2026-01-01T11:59:05Z",
      "duration_ms": 5000,
      "exit_code": 0,
      "issues_count": 1
    }
  ],
  "metrics": {
    "total_files_scanned": 10,
    "total_lines_scanned": 1200,
    "duration_ms": 58000,
    "tool_success_rate": 1.0,
    "cache_hit_rate": 0.5,
    "llm_calls": 1,
    "by_severity": {
      "BLOCKER": 0,
      "CRITICAL": 1,
      "MAJOR": 0,
      "MINOR": 0,
      "INFO": 0
    },
    "by_category": {
      "SEC": 1
    }
  },
  "metadata": {
    "human_review": {
      "approved": false,
      "reviewers": ["alice"]
    },
    "privacy": {
      "external_leak_events": 0
    }
  }
}
```

### 11.2 无问题报告

```json
{
  "report_id": "RPT-2026-0002",
  "schema_version": "1.0.0",
  "generated_at": "2026-01-01T12:10:00Z",
  "scope": {
    "mode": "MODULE",
    "project": "spring-review-skill-demo",
    "base_ref": "main",
    "head_ref": "main",
    "paths": ["src/main/java/com/example/service"],
    "diff_lines": 0
  },
  "summary": {
    "total_issues": 0,
    "by_severity": {
      "BLOCKER": 0,
      "CRITICAL": 0,
      "MAJOR": 0,
      "MINOR": 0,
      "INFO": 0
    },
    "by_category": {},
    "blocking": 0,
    "duration_ms": 15000,
    "token_usage": {
      "input_tokens": 0,
      "output_tokens": 0,
      "total_tokens": 0,
      "cost_usd": 0
    }
  },
  "issues": [],
  "tool_runs": [
    {
      "tool": "CHECKSTYLE",
      "tool_version": "10.12.0",
      "status": "SUCCESS",
      "started_at": "2026-01-01T12:09:45Z",
      "ended_at": "2026-01-01T12:09:50Z",
      "duration_ms": 5000,
      "exit_code": 0,
      "issues_count": 0
    }
  ],
  "metrics": {
    "total_files_scanned": 25,
    "total_lines_scanned": 3000,
    "duration_ms": 15000,
    "tool_success_rate": 1.0,
    "cache_hit_rate": 0.8,
    "llm_calls": 0,
    "by_severity": {
      "BLOCKER": 0,
      "CRITICAL": 0,
      "MAJOR": 0,
      "MINOR": 0,
      "INFO": 0
    },
    "by_category": {}
  },
  "metadata": {
    "human_review": {
      "approved": true,
      "reviewers": ["bob"]
    },
    "privacy": {
      "external_leak_events": 0
    }
  }
}
```

## 12. 与 A1 量化成功标准的对照表

| A1 标准                                                      | 报告承载方式                                                 |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| S4：JSON 通过 Schema 100%                                    | 本文件第 10 节完整 JSON Schema，Draft 2020-12，顶层与嵌套对象严格约束 |
| S5：预置缺陷检出率 ≥ 80%                                     | `issues` 承载 `rule_id`、`test_case_id`、`evidence`、`confidence`，可统计命中 |
| S6：正常代码误报率 ≤ 20%                                     | `issue_status = FALSE_POSITIVE`、`confidence`、`evidence` 支持人工与自动统计 |
| S7：1000 行 diff P95 ≤ 60 秒                                 | `scope.diff_lines`、`summary.duration_ms`、`metrics.duration_ms` |
| S9：每条问题包含规则 ID、文件、行号、证据、置信度、建议比例 100% | `issue` 必填 `rule_id`、`file`、`line`、`column`、`evidence`、`confidence`、`remediation` |
| S11：评测集规模 ≥ 30                                         | `issue.test_case_id` 可反查 `tests/cases/index.json`，报告本身不直接表达规模 |
| S15：工具调用成功率 ≥ 95%                                    | `tool_runs[].status`、`metrics.tool_success_rate`            |
| S17：人工评审认可率 ≥ 80%                                    | `issue.issue_status`、`metadata.human_review`                |
| S18：敏感代码外部泄露事件 0 次                               | `metadata.privacy.external_leak_events`、`evidence` / `snippet` 可脱敏 |

## 13. 校验方式与工具建议

- JSON Schema 校验器：`ajv`、`ajv-cli`、Python `jsonschema`、`check-jsonschema`。
- IDE：VS Code 通过 `$schema` 关联 `schema/review-report.schema.json`。
- CI：在生成报告后执行 Schema 校验，失败则阻断。
- 引用完整性：额外脚本读取 `rules/index.json`、`tests/cases/index.json` 校验 `rule_id`、`test_case_id`。
- 统计一致性：额外脚本校验 `summary.total_issues == issues.length`，`by_severity`、`by_category` 求和一致。
- 隐私检查：额外脚本扫描 `evidence`、`snippet`、`metadata`，确保无敏感信息外泄。
- 建议命令示例：
  - `npx ajv-cli validate -s schema/review-report.schema.json -d report.json --spec=draft2020`
  - `python -m jsonschema -i report.json schema/review-report.schema.json`

## 14. 版本与变更规则

- `schema_version` 使用语义化版本：`MAJOR.MINOR.PATCH`。
- `MAJOR`：不兼容变更，如删除必填字段、收紧类型、改变枚举含义。
- `MINOR`：向后兼容新增，如新增可选字段、新增可选指标。
- `PATCH`：文档、描述、示例修正，不改变校验语义。
- 枚举必须复用 A2；新增枚举值属于 `MAJOR` 变更。
- 字段弃用：先标记 `deprecated`，保留至少一个 `MINOR` 周期，下一 `MAJOR` 移除。
- 冻结后变更：必须更新本文档、`schema/review-report.schema.json`、`handoff/A6.md`，并记录变更原因与影响。
- 每次变更后必须用两个示例和第 13 节校验方式回归验证。