# docs/04-规则元数据Schema.md

## 1. 设计原则

- 只读审查：Schema 仅用于校验规则元数据，不修改规则、不提交、不训练模型。
- 严格对齐 A2：字段名、枚举值、必填项、长度范围、编号格式必须与 `docs/01-规则分类与元数据模板.md` v0.1 一致。
- 强约束优先：顶层 `additionalProperties` 为 `false`，防止未定义字段进入规则元数据。
- 版本化：`rule_version`、`metadata_version`、`schema_version` 均使用 semver。
- 可校验：完整 Schema 遵循 JSON Schema Draft 2020-12。
- 可扩展但受控：A5 新增补充字段必须登记，并走 A2 变更流程。
- 与索引分离：元数据字段使用 `category_l1`、`category_l2`；`rules/index.json` 使用 `category`、`subcategory`，两者通过对照关系映射，不强制统一命名。

## 2. Schema 顶层结构

| 关键字                 | 值                                                           |
| ---------------------- | ------------------------------------------------------------ |
| `$schema`              | `https://json-schema.org/draft/2020-12/schema`               |
| `$id`                  | `https://spring-review-skill.dev/schema/rule-metadata.schema.json` |
| `title`                | `Spring Review Skill 规则元数据 Schema`                      |
| `type`                 | `object`                                                     |
| `required`             | `rule_id`、`name`、`description`、`category_l1`、`category_l2`、`status`、`severity`、`confidence`、`detection_method`、`tool`、`enabled`、`applies_to`、`message`、`remediation`、`positive_example`、`negative_example`、`sources`、`rule_version`、`metadata_version`、`schema_version`、`owner`、`created_at`、`updated_at` |
| `properties`           | 六大组字段完整定义                                           |
| `additionalProperties` | `false`                                                      |

## 3. 六大组字段的完整 Schema 定义

### 组 1 标识与分类

| 字段          | type          | format | enum                                                         | pattern                                                      | minimum | maximum | minLength | maxLength | description          | examples                               |
| ------------- | ------------- | ------ | ------------------------------------------------------------ | ------------------------------------------------------------ | ------- | ------- | --------- | --------- | -------------------- | -------------------------------------- |
| `rule_id`     | string        | —      | —                                                            | `^SRS-(AR\|CF\|DI\|WEB\|DAO\|SEC\|CON\|PERF\|OBS\|EXC\|RES\|TEST\|BUILD\|STYLE\|NULL\|I18N\|MIG\|CLOUD)-\d{2}-\d{3}$` | —       | —       | —         | —         | 规则编号             | `SRS-AR-01-001`                        |
| `name`        | string        | —      | —                                                            | —                                                            | —       | —       | 1         | 120       | 规则名称             | `禁止 Controller 直接依赖 Repository`  |
| `description` | string        | —      | —                                                            | —                                                            | —       | —       | 1         | 2000      | 规则描述             | `Controller 不应直接依赖 Repository。` |
| `category_l1` | string        | —      | `AR`、`CF`、`DI`、`WEB`、`DAO`、`SEC`、`CON`、`PERF`、`OBS`、`EXC`、`RES`、`TEST`、`BUILD`、`STYLE`、`NULL`、`I18N`、`MIG`、`CLOUD` | —                                                            | —       | —       | —         | —         | 一级分类码           | `AR`                                   |
| `category_l2` | string        | —      | —                                                            | `^\d{2}$`                                                    | —       | —       | —         | —         | 二级分类码，2 位数字 | `01`                                   |
| `tags`        | array<string> | —      | —                                                            | —                                                            | —       | —       | —         | —         | 标签                 | `["architecture", "layer"]`            |
| `status`      | string        | —      | `DRAFT`、`REVIEW`、`ACTIVE`、`DEPRECATED`、`REMOVED`         | —                                                            | —       | —       | —         | —         | 状态                 | `ACTIVE`                               |

### 组 2 严重与检测

| 字段               | type    | format | enum                                                         | pattern | minimum | maximum | minLength | maxLength | description | examples                                              |
| ------------------ | ------- | ------ | ------------------------------------------------------------ | ------- | ------- | ------- | --------- | --------- | ----------- | ----------------------------------------------------- |
| `severity`         | string  | —      | `BLOCKER`、`CRITICAL`、`MAJOR`、`MINOR`、`INFO`              | —       | —       | —       | —         | —         | 严重级别    | `MAJOR`                                               |
| `confidence`       | string  | —      | `HIGH`、`MEDIUM`、`LOW`                                      | —       | —       | —       | —         | —         | 置信度      | `HIGH`                                                |
| `detection_method` | string  | —      | `STATIC_ANALYSIS`、`AST`、`ARCH_TEST`、`RULE_ENGINE`、`REGEX`、`HEURISTIC`、`HYBRID`、`MANUAL` | —       | —       | —       | —         | —         | 检测方式    | `ARCH_TEST`                                           |
| `tool`             | string  | —      | `CHECKSTYLE`、`PMD`、`ARCHUNIT`、`SPOTBUGS`、`CUSTOM`        | —       | —       | —       | —         | —         | 工具        | `ARCHUNIT`                                            |
| `tool_rule_id`     | string  | —      | —                                                            | —       | —       | —       | 1         | —         | 工具规则 ID | `no_controller_to_repository`                         |
| `parameters`       | object  | —      | —                                                            | —       | —       | —       | —         | —         | 工具参数    | `{"layers": ["Controller", "Service", "Repository"]}` |
| `enabled`          | boolean | —      | —                                                            | —       | —       | —       | —         | —         | 是否启用    | `true`                                                |

### 组 3 适用范围

| 字段                             | type          | format | enum | pattern | minimum | maximum | minLength | maxLength | description          | examples              |
| -------------------------------- | ------------- | ------ | ---- | ------- | ------- | ------- | --------- | --------- | -------------------- | --------------------- |
| `applies_to`                     | object        | —      | —    | —       | —       | —       | —         | —         | 适用范围             | 见下                  |
| `applies_to.java_version`        | string        | —      | —    | —       | —       | —       | 1         | —         | Java 版本范围        | `17+`                 |
| `applies_to.spring_boot_version` | string        | —      | —    | —       | —       | —       | 1         | —         | Spring Boot 版本范围 | `3.x`                 |
| `applies_to.build_tool`          | array<string> | —      | —    | —       | —       | —       | —         | —         | 构建工具，至少 1 项  | `["maven", "gradle"]` |
| `applies_to.module_type`         | array<string> | —      | —    | —       | —       | —       | —         | —         | 模块类型，至少 1 项  | `["web", "service"]`  |
| `applies_to.framework`           | array<string> | —      | —    | —       | —       | —       | —         | —         | 框架                 | `["spring-mvc"]`      |
| `applies_to.exclude`             | array<string> | —      | —    | —       | —       | —       | —         | —         | 排除路径             | `["**/legacy/**"]`    |

### 组 4 内容与示例

| 字段                   | type   | format | enum | pattern | minimum | maximum | minLength | maxLength | description | examples                               |
| ---------------------- | ------ | ------ | ---- | ------- | ------- | ------- | --------- | --------- | ----------- | -------------------------------------- |
| `message`              | string | —      | —    | —       | —       | —       | 1         | 500       | 违规消息    | `Controller 不应直接依赖 Repository。` |
| `remediation`          | string | —      | —    | —       | —       | —       | 1         | 2000      | 修复建议    | `将 Repository 调用下沉到 Service。`   |
| `positive_example`     | string | —      | —    | —       | —       | —       | 1         | 5000      | 正例        | `Controller 仅依赖 Service。`          |
| `negative_example`     | string | —      | —    | —       | —       | —       | 1         | 5000      | 反例        | `Controller 直接依赖 Repository。`     |
| `false_positive_notes` | string | —      | —    | —       | —       | —       | 1         | 1000      | 误报说明    | `测试代码可排除。`                     |

### 组 5 来源与版本

| 字段                | type          | format | enum                                                         | pattern           | minimum | maximum | minLength | maxLength | description        | examples              |
| ------------------- | ------------- | ------ | ------------------------------------------------------------ | ----------------- | ------- | ------- | --------- | --------- | ------------------ | --------------------- |
| `sources`           | array<object> | —      | —                                                            | —                 | —       | —       | —         | —         | 来源，至少 1 项    | 见下                  |
| `sources[].type`    | string        | —      | `OFFICIAL_SPEC`、`FRAMEWORK_DOC`、`BOOK`、`COMMUNITY`、`CVE`、`INTERNAL_EXPERIENCE`、`TOOL_RULE` | —                 | —       | —       | —         | —         | 来源类型           | `INTERNAL_EXPERIENCE` |
| `sources[].ref`     | string        | —      | —                                                            | —                 | —       | —       | 1         | —         | 来源引用           | `Spring 分层架构规范` |
| `sources[].version` | string        | —      | —                                                            | —                 | —       | —       | 1         | —         | 来源版本           | `1.0.0`               |
| `sources[].note`    | string        | —      | —                                                            | —                 | —       | —       | 1         | 500       | 备注               | `内部约定`            |
| `rule_version`      | string        | —      | —                                                            | `^\d+\.\d+\.\d+$` | —       | —       | —         | —         | 规则版本 semver    | `1.0.0`               |
| `metadata_version`  | string        | —      | —                                                            | `^\d+\.\d+\.\d+$` | —       | —       | —         | —         | 元数据版本 semver  | `1.0.0`               |
| `schema_version`    | string        | —      | —                                                            | `^\d+\.\d+\.\d+$` | —       | —       | —         | —         | Schema 版本 semver | `1.0.0`               |

### 组 6 生命周期与测试

| 字段                       | type          | format | enum | pattern                                                      | minimum | maximum | minLength | maxLength | description                                            | examples                                |
| -------------------------- | ------------- | ------ | ---- | ------------------------------------------------------------ | ------- | ------- | --------- | --------- | ------------------------------------------------------ | --------------------------------------- |
| `owner`                    | string        | —      | —    | —                                                            | —       | —       | 1         | 64        | 负责人                                                 | `architecture-team`                     |
| `created_at`               | string        | date   | —    | —                                                            | —       | —       | —         | —         | 创建日期 ISO 8601                                      | `2026-01-15`                            |
| `updated_at`               | string        | date   | —    | —                                                            | —       | —       | —         | —         | 更新日期 ISO 8601                                      | `2026-01-15`                            |
| `change_log`               | array<object> | —      | —    | —                                                            | —       | —       | —         | —         | 变更日志                                               | 见下                                    |
| `change_log[].version`     | string        | —      | —    | `^\d+\.\d+\.\d+$`                                            | —       | —       | —         | —         | 版本                                                   | `1.0.0`                                 |
| `change_log[].date`        | string        | date   | —    | —                                                            | —       | —       | —         | —         | 日期                                                   | `2026-01-15`                            |
| `change_log[].description` | string        | —      | —    | —                                                            | —       | —       | 1         | 2000      | 变更说明                                               | `初始版本`                              |
| `test_cases`               | array<string> | —      | —    | `^TC-(AR|CF|DI|WEB|DAO|SEC|CON|PERF|OBS|EXC|RES|TEST|BUILD|STYLE|NULL|I18N|MIG|CLOUD)-\d{3}$` | —       | —       | —         | —         | 测试用例 ID 数组，ID 格式 `TC-<一级分类码>-<三位序号>` | `["TC-AR-001", "TC-AR-002"]`            |
| `coverage`                 | string        | —      | —    | —                                                            | —       | —       | 1         | 500       | 覆盖说明                                               | `覆盖 Controller 直接注入 Repository。` |
| `deprecated_at`            | string        | date   | —    | —                                                            | —       | —       | —         | —         | A5 新增补充字段：废弃日期                              | `2026-02-01`                            |
| `removed_at`               | string        | date   | —    | —                                                            | —       | —       | —         | —         | A5 新增补充字段：移除日期                              | `2026-03-01`                            |

## 4. 条件约束

### 4.1 if/then/else

- 若 `status` 为 `DEPRECATED`，则必须存在 `deprecated_at`。
- 若 `status` 为 `REMOVED`，则必须存在 `deprecated_at` 和 `removed_at`。
- 若存在 `deprecated_at`，则 `status` 必须为 `DEPRECATED` 或 `REMOVED`。
- 若存在 `removed_at`，则 `status` 必须为 `REMOVED`。

### 4.2 dependentRequired

- `tool_rule_id` 依赖 `tool`：若出现 `tool_rule_id`，则必须出现 `tool`。由于 `tool` 已为顶层必填，该约束用于显式表达引用关系。

### 4.3 dependentSchemas

- `deprecated_at` 出现时，等价要求 `status` 属于 `DEPRECATED`、`REMOVED`。
- `removed_at` 出现时，等价要求 `status` 为 `REMOVED`。
- 本 Schema 使用 `allOf` + `if/then` 实现上述约束。

## 5. 引用完整性约束

- `rule_id` 必须匹配 `^SRS-(AR|CF|DI|WEB|DAO|SEC|CON|PERF|OBS|EXC|RES|TEST|BUILD|STYLE|NULL|I18N|MIG|CLOUD)-\d{2}-\d{3}$`。
- `category_l1` 必须为 A2 定义的 18 类之一。
- `category_l2` 必须匹配 `^\d{2}$`。
- 建议额外校验 `category_l1`、`category_l2` 与 `rule_id` 中的一级分类码、二级分类码一致。
- `sources` 至少 1 项，且每项必须包含 `type`、`ref`、`version`。
- `sources[].version` 必填。
- `tool_rule_id` 出现时，`tool` 必须存在；`tool` 已顶层必填。
- `status` 与 `deprecated_at`、`removed_at` 的关系见第 4 章。
- `rules/index.json` 的 `category`、`subcategory` 与元数据 `category_l1`、`category_l2` 建立对照关系，但不强制统一命名。
- `schema_version` 应与 `schema/versions/v<semver>/` 中的 Schema 版本对应。
- `test_cases` 中的 ID 必须存在于 `tests/cases/index.json`。
- `test_cases` 的 ID 格式为 `TC-<一级分类码>-<三位序号>`，对应文件为 `tests/cases/<ID>.json`。
- `rule_id` 与 `category_l1/category_l2` 的一致性由程序化校验器实现，不放进 JSON Schema；校验器在生成 `rules/index.json` 时执行，失败则不产出索引。

## 6. 完整 JSON Schema

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://spring-review-skill.dev/schema/rule-metadata.schema.json",
  "title": "Spring Review Skill 规则元数据 Schema",
  "type": "object",
  "required": [
    "rule_id",
    "name",
    "description",
    "category_l1",
    "category_l2",
    "status",
    "severity",
    "confidence",
    "detection_method",
    "tool",
    "enabled",
    "applies_to",
    "message",
    "remediation",
    "positive_example",
    "negative_example",
    "sources",
    "rule_version",
    "metadata_version",
    "schema_version",
    "owner",
    "created_at",
    "updated_at"
  ],
  "properties": {
    "rule_id": {
      "type": "string",
      "pattern": "^SRS-(AR|CF|DI|WEB|DAO|SEC|CON|PERF|OBS|EXC|RES|TEST|BUILD|STYLE|NULL|I18N|MIG|CLOUD)-\\d{2}-\\d{3}$",
      "description": "规则编号",
      "examples": ["SRS-AR-01-001"]
    },
    "name": {
      "type": "string",
      "minLength": 1,
      "maxLength": 120,
      "description": "规则名称",
      "examples": ["禁止 Controller 直接依赖 Repository"]
    },
    "description": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2000,
      "description": "规则描述",
      "examples": ["Controller 层不应直接依赖 Repository。"]
    },
    "category_l1": {
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
      ],
      "description": "一级分类码"
    },
    "category_l2": {
      "type": "string",
      "pattern": "^\\d{2}$",
      "description": "二级分类码，2 位数字"
    },
    "tags": {
      "type": "array",
      "items": {
        "type": "string"
      },
      "uniqueItems": true,
      "description": "标签"
    },
    "status": {
      "type": "string",
      "enum": ["DRAFT", "REVIEW", "ACTIVE", "DEPRECATED", "REMOVED"],
      "description": "状态"
    },
    "severity": {
      "type": "string",
      "enum": ["BLOCKER", "CRITICAL", "MAJOR", "MINOR", "INFO"],
      "description": "严重级别"
    },
    "confidence": {
      "type": "string",
      "enum": ["HIGH", "MEDIUM", "LOW"],
      "description": "置信度"
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
      ],
      "description": "检测方式"
    },
    "tool": {
      "type": "string",
      "enum": ["CHECKSTYLE", "PMD", "ARCHUNIT", "SPOTBUGS", "CUSTOM"],
      "description": "工具"
    },
    "tool_rule_id": {
      "type": "string",
      "minLength": 1,
      "description": "工具规则 ID"
    },
    "parameters": {
      "type": "object",
      "additionalProperties": true,
      "description": "工具参数"
    },
    "enabled": {
      "type": "boolean",
      "description": "是否启用"
    },
    "applies_to": {
      "type": "object",
      "required": [
        "java_version",
        "spring_boot_version",
        "build_tool",
        "module_type"
      ],
      "properties": {
        "java_version": {
          "type": "string",
          "minLength": 1,
          "description": "Java 版本范围"
        },
        "spring_boot_version": {
          "type": "string",
          "minLength": 1,
          "description": "Spring Boot 版本范围"
        },
        "build_tool": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "minItems": 1,
          "description": "构建工具"
        },
        "module_type": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "minItems": 1,
          "description": "模块类型"
        },
        "framework": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "description": "框架"
        },
        "exclude": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "description": "排除路径"
        }
      },
      "additionalProperties": false,
      "description": "适用范围"
    },
    "message": {
      "type": "string",
      "minLength": 1,
      "maxLength": 500,
      "description": "违规消息"
    },
    "remediation": {
      "type": "string",
      "minLength": 1,
      "maxLength": 2000,
      "description": "修复建议"
    },
    "positive_example": {
      "type": "string",
      "minLength": 1,
      "maxLength": 5000,
      "description": "正例"
    },
    "negative_example": {
      "type": "string",
      "minLength": 1,
      "maxLength": 5000,
      "description": "反例"
    },
    "false_positive_notes": {
      "type": "string",
      "minLength": 1,
      "maxLength": 1000,
      "description": "误报说明"
    },
    "sources": {
      "type": "array",
      "minItems": 1,
      "items": {
        "type": "object",
        "required": ["type", "ref", "version"],
        "properties": {
          "type": {
            "type": "string",
            "enum": [
              "OFFICIAL_SPEC",
              "FRAMEWORK_DOC",
              "BOOK",
              "COMMUNITY",
              "CVE",
              "INTERNAL_EXPERIENCE",
              "TOOL_RULE"
            ],
            "description": "来源类型"
          },
          "ref": {
            "type": "string",
            "minLength": 1,
            "description": "来源引用"
          },
          "version": {
            "type": "string",
            "minLength": 1,
            "description": "来源版本"
          },
          "note": {
            "type": "string",
            "minLength": 1,
            "maxLength": 500,
            "description": "备注"
          }
        },
        "additionalProperties": false
      },
      "description": "来源"
    },
    "rule_version": {
      "type": "string",
      "pattern": "^\\d+\\.\\d+\\.\\d+$",
      "description": "规则版本 semver"
    },
    "metadata_version": {
      "type": "string",
      "pattern": "^\\d+\\.\\d+\\.\\d+$",
      "description": "元数据版本 semver"
    },
    "schema_version": {
      "type": "string",
      "pattern": "^\\d+\\.\\d+\\.\\d+$",
      "description": "Schema 版本 semver"
    },
    "owner": {
      "type": "string",
      "minLength": 1,
      "maxLength": 64,
      "description": "负责人"
    },
    "created_at": {
      "type": "string",
      "format": "date",
      "description": "创建日期 ISO 8601"
    },
    "updated_at": {
      "type": "string",
      "format": "date",
      "description": "更新日期 ISO 8601"
    },
    "change_log": {
      "type": "array",
      "items": {
        "type": "object",
        "required": ["version", "date", "description"],
        "properties": {
          "version": {
            "type": "string",
            "pattern": "^\\d+\\.\\d+\\.\\d+$"
          },
          "date": {
            "type": "string",
            "format": "date"
          },
          "description": {
            "type": "string",
            "minLength": 1,
            "maxLength": 2000
          }
        },
        "additionalProperties": false
      },
      "description": "变更日志"
    },
    "test_cases": {
      "type": "array",
      "items": {
        "type": "string",
        "pattern": "^TC-(AR|CF|DI|WEB|DAO|SEC|CON|PERF|OBS|EXC|RES|TEST|BUILD|STYLE|NULL|I18N|MIG|CLOUD)-\\d{3}$"
      },
      "description": "测试用例 ID 数组，ID 格式 TC-<一级分类码>-<三位序号>，需存在于 tests/cases/index.json"
    },
    "coverage": {
      "type": "string",
      "minLength": 1,
      "maxLength": 500,
      "description": "覆盖说明"
    },
    "deprecated_at": {
      "type": "string",
      "format": "date",
      "description": "A5 新增补充字段：废弃日期"
    },
    "removed_at": {
      "type": "string",
      "format": "date",
      "description": "A5 新增补充字段：移除日期"
    }
  },
  "additionalProperties": false,
  "dependentRequired": {
    "tool_rule_id": ["tool"]
  },
  "allOf": [
    {
      "if": {
        "properties": {
          "status": {
            "const": "DEPRECATED"
          }
        },
        "required": ["status"]
      },
      "then": {
        "required": ["deprecated_at"]
      }
    },
    {
      "if": {
        "properties": {
          "status": {
            "const": "REMOVED"
          }
        },
        "required": ["status"]
      },
      "then": {
        "required": ["deprecated_at", "removed_at"]
      }
    },
    {
      "if": {
        "required": ["deprecated_at"]
      },
      "then": {
        "properties": {
          "status": {
            "enum": ["DEPRECATED", "REMOVED"]
          }
        }
      }
    },
    {
      "if": {
        "required": ["removed_at"]
      },
      "then": {
        "properties": {
          "status": {
            "const": "REMOVED"
          }
        }
      }
    }
  ]
}
```

## 7. 三个完整示例

### 7.1 AR 示例

```json
{
  "rule_id": "SRS-AR-01-001",
  "name": "禁止 Controller 直接依赖 Repository",
  "description": "Controller 层不应直接依赖 Repository，应通过 Service 层访问数据。",
  "category_l1": "AR",
  "category_l2": "01",
  "tags": ["architecture", "layer"],
  "status": "ACTIVE",
  "severity": "MAJOR",
  "confidence": "HIGH",
  "detection_method": "ARCH_TEST",
  "tool": "ARCHUNIT",
  "tool_rule_id": "no_controller_to_repository",
  "parameters": {
    "layers": ["Controller", "Service", "Repository"]
  },
  "enabled": true,
  "applies_to": {
    "java_version": "17+",
    "spring_boot_version": "3.x",
    "build_tool": ["maven", "gradle"],
    "module_type": ["web", "service"],
    "framework": ["spring-mvc", "spring-data-jpa"],
    "exclude": ["**/legacy/**"]
  },
  "message": "Controller 不应直接依赖 Repository，请通过 Service 层访问。",
  "remediation": "将 Repository 调用下沉到 Service，并让 Controller 仅依赖 Service。",
  "positive_example": "Controller 仅依赖 Service。",
  "negative_example": "Controller 直接依赖 Repository。",
  "false_positive_notes": "测试代码或代码生成模块可排除。",
  "sources": [
    {
      "type": "INTERNAL_EXPERIENCE",
      "ref": "Spring 分层架构规范",
      "version": "1.0.0",
      "note": "内部约定"
    }
  ],
  "rule_version": "1.0.0",
  "metadata_version": "1.0.0",
  "schema_version": "1.0.0",
  "owner": "architecture-team",
  "created_at": "2026-01-15",
  "updated_at": "2026-01-15",
  "change_log": [
    {
      "version": "1.0.0",
      "date": "2026-01-15",
      "description": "初始版本"
    }
  ],
  "test_cases": [
    "TC-AR-001"
  ],
  "coverage": "覆盖 Controller 直接注入 Repository 的静态结构。"
}
```

### 7.2 SEC 示例

```json
{
  "rule_id": "SRS-SEC-02-001",
  "name": "禁止硬编码密码",
  "description": "禁止在配置、代码或测试中硬编码密码、令牌等敏感凭据。",
  "category_l1": "SEC",
  "category_l2": "02",
  "tags": ["security", "secret"],
  "status": "ACTIVE",
  "severity": "BLOCKER",
  "confidence": "HIGH",
  "detection_method": "REGEX",
  "tool": "PMD",
  "tool_rule_id": "AvoidHardCodedPassword",
  "parameters": {
    "keywords": ["password", "secret", "token"]
  },
  "enabled": true,
  "applies_to": {
    "java_version": "17+",
    "spring_boot_version": "3.x",
    "build_tool": ["maven", "gradle"],
    "module_type": ["web", "service", "batch"],
    "framework": ["spring-boot"]
  },
  "message": "检测到硬编码敏感凭据。",
  "remediation": "将敏感凭据迁移到环境变量、密钥管理服务或加密配置中。",
  "positive_example": "通过环境变量或配置中心读取密码。",
  "negative_example": "在源码中直接写入 password=admin123。",
  "false_positive_notes": "示例文档、测试夹具中的占位符可白名单处理。",
  "sources": [
    {
      "type": "OFFICIAL_SPEC",
      "ref": "OWASP Secrets Management Cheat Sheet",
      "version": "2026.1",
      "note": "安全基线"
    }
  ],
  "rule_version": "1.0.0",
  "metadata_version": "1.0.0",
  "schema_version": "1.0.0",
  "owner": "security-team",
  "created_at": "2026-01-15",
  "updated_at": "2026-01-15",
  "change_log": [
    {
      "version": "1.0.0",
      "date": "2026-01-15",
      "description": "初始版本"
    }
  ],
  "test_cases": [
    "TC-SEC-001"
  ],
  "coverage": "覆盖 Java 源码与配置中的硬编码密码模式。"
}
```

### 7.3 DAO 示例

```json
{
  "rule_id": "SRS-DAO-01-001",
  "name": "禁止在 DAO 中拼接 SQL",
  "description": "DAO 层不应通过字符串拼接构造 SQL，应使用参数化查询或 ORM 绑定参数。",
  "category_l1": "DAO",
  "category_l2": "01",
  "tags": ["dao", "sql-injection"],
  "status": "ACTIVE",
  "severity": "CRITICAL",
  "confidence": "MEDIUM",
  "detection_method": "AST",
  "tool": "CUSTOM",
  "tool_rule_id": "dao_sql_string_concat",
  "parameters": {
    "sql_keywords": ["select", "insert", "update", "delete"]
  },
  "enabled": true,
  "applies_to": {
    "java_version": "17+",
    "spring_boot_version": "3.x",
    "build_tool": ["maven", "gradle"],
    "module_type": ["dao", "repository"],
    "framework": ["spring-jdbc", "spring-data-jpa"]
  },
  "message": "DAO 中存在 SQL 字符串拼接。",
  "remediation": "使用 PreparedStatement、JPA 参数绑定或 MyBatis 参数占位符。",
  "positive_example": "使用参数占位符绑定查询条件。",
  "negative_example": "将用户输入拼接到 SQL 字符串中。",
  "false_positive_notes": "静态常量 SQL 且不包含外部输入时可降低告警。",
  "sources": [
    {
      "type": "TOOL_RULE",
      "ref": "Spring Review Skill DAO 规则集",
      "version": "1.0.0",
      "note": "内部规则"
    }
  ],
  "rule_version": "1.0.0",
  "metadata_version": "1.0.0",
  "schema_version": "1.0.0",
  "owner": "dao-team",
  "created_at": "2026-01-15",
  "updated_at": "2026-01-15",
  "change_log": [
    {
      "version": "1.0.0",
      "date": "2026-01-15",
      "description": "初始版本"
    }
  ],
  "test_cases": [
    "TC-DAO-001"
  ],
  "coverage": "覆盖 DAO/Repository 中的 SQL 字符串拼接模式。"
}
```

## 8. 与 A2 元数据模板的对照表

| A2 字段                          | Schema 字段                      | 约束                                                         | 备注                                           |
| -------------------------------- | -------------------------------- | ------------------------------------------------------------ | ---------------------------------------------- |
| `rule_id`                        | `rule_id`                        | string，pattern `^SRS-(AR\|CF\|DI\|WEB\|DAO\|SEC\|CON\|PERF\|OBS\|EXC\|RES\|TEST\|BUILD\|STYLE\|NULL\|I18N\|MIG\|CLOUD)-\d{2}-\d{3}$` | 必填                                           |
| `name`                           | `name`                           | string，1-120                                                | 必填                                           |
| `description`                    | `description`                    | string，1-2000                                               | 必填                                           |
| `category_l1`                    | `category_l1`                    | enum 18 类                                                   | 必填                                           |
| `category_l2`                    | `category_l2`                    | pattern `^\d{2}$`                                            | 必填                                           |
| `tags`                           | `tags`                           | array<string>，uniqueItems                                   | 可选                                           |
| `status`                         | `status`                         | enum `DRAFT`、`REVIEW`、`ACTIVE`、`DEPRECATED`、`REMOVED`    | 必填                                           |
| `severity`                       | `severity`                       | enum `BLOCKER`、`CRITICAL`、`MAJOR`、`MINOR`、`INFO`         | 必填                                           |
| `confidence`                     | `confidence`                     | enum `HIGH`、`MEDIUM`、`LOW`                                 | 必填                                           |
| `detection_method`               | `detection_method`               | enum 8 项                                                    | 必填                                           |
| `tool`                           | `tool`                           | enum `CHECKSTYLE`、`PMD`、`ARCHUNIT`、`SPOTBUGS`、`CUSTOM`   | 必填                                           |
| `tool_rule_id`                   | `tool_rule_id`                   | string，minLength 1                                          | 可选；出现时依赖 `tool`                        |
| `parameters`                     | `parameters`                     | object，additionalProperties true                            | 可选                                           |
| `enabled`                        | `enabled`                        | boolean                                                      | 必填                                           |
| `applies_to`                     | `applies_to`                     | object，additionalProperties false                           | 必填                                           |
| `applies_to.java_version`        | `applies_to.java_version`        | string，minLength 1                                          | 必填                                           |
| `applies_to.spring_boot_version` | `applies_to.spring_boot_version` | string，minLength 1                                          | 必填                                           |
| `applies_to.build_tool`          | `applies_to.build_tool`          | array<string>，minItems 1                                    | 必填                                           |
| `applies_to.module_type`         | `applies_to.module_type`         | array<string>，minItems 1                                    | 必填                                           |
| `applies_to.framework`           | `applies_to.framework`           | array<string>                                                | 可选                                           |
| `applies_to.exclude`             | `applies_to.exclude`             | array<string>                                                | 可选                                           |
| `message`                        | `message`                        | string，1-500                                                | 必填                                           |
| `remediation`                    | `remediation`                    | string，1-2000                                               | 必填                                           |
| `positive_example`               | `positive_example`               | string，1-5000                                               | 必填                                           |
| `negative_example`               | `negative_example`               | string，1-5000                                               | 必填                                           |
| `false_positive_notes`           | `false_positive_notes`           | string，1-1000                                               | 可选                                           |
| `sources`                        | `sources`                        | array<object>，minItems 1                                    | 必填                                           |
| `sources[].type`                 | `sources[].type`                 | enum 7 项                                                    | 必填                                           |
| `sources[].ref`                  | `sources[].ref`                  | string，minLength 1                                          | 必填                                           |
| `sources[].version`              | `sources[].version`              | string，minLength 1                                          | 必填                                           |
| `sources[].note`                 | `sources[].note`                 | string，1-500                                                | 可选                                           |
| `rule_version`                   | `rule_version`                   | semver pattern                                               | 必填                                           |
| `metadata_version`               | `metadata_version`               | semver pattern                                               | 必填                                           |
| `schema_version`                 | `schema_version`                 | semver pattern                                               | 必填                                           |
| `owner`                          | `owner`                          | string，1-64                                                 | 必填                                           |
| `created_at`                     | `created_at`                     | string，format date                                          | 必填                                           |
| `updated_at`                     | `updated_at`                     | string，format date                                          | 必填                                           |
| `change_log`                     | `change_log`                     | array<object>                                                | 可选                                           |
| `change_log[].version`           | `change_log[].version`           | semver pattern                                               | 必填                                           |
| `change_log[].date`              | `change_log[].date`              | string，format date                                          | 必填                                           |
| `change_log[].description`       | `change_log[].description`       | string，1-2000                                               | 必填                                           |
| `test_cases`                     | `test_cases`                     | array<string>，ID pattern^TC-(AR\|CF\|DI\|WEB\|DAO\|SEC\|CON\|PERF\|OBS\|EXC\|RES\|TEST\|BUILD\|STYLE\|NULL\|I18N\|MIG\|CLOUD)-\d{3}$ | 测试用例 ID，需存在于 `tests/cases/index.json` |
| `coverage`                       | `coverage`                       | string，1-500                                                | 可选                                           |
| —                                | `deprecated_at`                  | string，format date                                          | A5 新增补充字段，待 A2 变更确认；暂不启用      |
| —                                | `removed_at`                     | string，format date                                          | A5 新增补充字段，待 A2 变更确认；暂不启用      |

## 9. 校验方式与工具建议

- 使用 JSON Schema Draft 2020-12 校验器。
- 建议启用 `format` 校验，以校验 `date` 字段。
- 建议在 CI 中执行只读校验：规则元数据 YAML/JSON → JSON → Schema 校验。
- 可使用通用校验器：`ajv`、`python-jsonschema`、`check-jsonschema`。
- IDE 可绑定 `schema/rule-metadata.schema.json`，获得实时提示。
- 校验失败时输出 Markdown + JSON 报告，但本阶段不定义报告 Schema。
- 本阶段不进行技术选型，不绑定具体 Maven/Gradle 插件。

## 10. 版本与变更规则

- `schema_version` 使用 semver。
- major：不兼容变更，例如删除字段、修改必填项、收紧枚举。
- minor：向后兼容新增，例如新增可选字段。
- patch：描述、示例、文档修正，不改变校验语义。
- 枚举值必须与 A2 完全一致，统一大写 + 下划线。
- `additionalProperties` 保持 `false`；新增字段必须同步更新 Schema 与 A2。
- A5 新增补充字段 `deprecated_at`、`removed_at` 已登记，需走 A2 变更流程。
- `rule_version`、`metadata_version`、`schema_version` 独立演进。
- `rules/index.json` 的 `category`、`subcategory` 与元数据 `category_l1`、`category_l2` 保持对照关系，不强制统一命名。
- 规则文件仍按 A3：`rules/<一级分类码>.yaml`。
- Schema 文件仍按 A3：`schema/<领域>.schema.json`，版本目录 `schema/versions/v<semver>/`。