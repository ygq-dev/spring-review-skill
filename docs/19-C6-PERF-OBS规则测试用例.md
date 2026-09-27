# docs/19-C6-PERF-OBS规则测试用例.md

## 1. 说明

本文件为 C6 阶段产出，覆盖 B6 冻结的 PERF 2 条规则与 OBS 3 条规则。  
本次输出 10 个正反例、5 个夹具、5 个测试用例 JSON、5 个期望结果 JSON，以及 examples/index.json 与 tests/cases/index.json 的新增条目片段。  
索引合并统一留到 C8 阶段，本文件不修改已有索引。  
元数据统一使用：`version=1.0.0`、`status=ACTIVE`、`generated_at=2026-09-24T00:00:00Z`、`generated_by=manual/1.0.0`。

## 2. 正反例代码（5 对，共 10 个）

### examples/PERF/positive/SRS-PERF-01-001_positive.java

```java
package com.example.perf;

import org.springframework.web.client.RestTemplate;

public class RemoteCallBatch {
    private final RestTemplate restTemplate = new RestTemplate();

    public void fetchAll(String[] ids) {
        String idsParam = String.join(",", ids);
        restTemplate.getForObject("https://example.com/api/items?ids=" + idsParam, String.class);
    }
}
```

### examples/PERF/negative/SRS-PERF-01-001_negative.java

```java
package com.example.perf;

import org.springframework.web.client.RestTemplate;

public class RemoteCallInLoop {
    private final RestTemplate restTemplate = new RestTemplate();

    public void fetchAll(String[] ids) {
        for (String id : ids) {
            restTemplate.getForObject("https://example.com/api/items/" + id, String.class);
        }
    }
}
```

### examples/PERF/positive/SRS-PERF-02-001_positive.java

```java
package com.example.perf;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

public interface UserRepository extends Repository<User, Long> {
    Page<User> findAll(Pageable pageable);
}

class User {}
```

### examples/PERF/negative/SRS-PERF-02-001_negative.java

```java
package com.example.perf;

import java.util.List;
import org.springframework.data.repository.Repository;

public interface UserRepository extends Repository<User, Long> {
    List<User> findAll();
}

class User {}
```

### examples/OBS/positive/SRS-OBS-01-001_positive.java

```java
package com.example.obs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public void logUser(String userId) {
        log.info("user={}", userId);
    }
}
```

### examples/OBS/negative/SRS-OBS-01-001_negative.java

```java
package com.example.obs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public void logUser(String userId) {
        log.info("user=" + userId);
    }
}
```

### examples/OBS/positive/SRS-OBS-02-001_positive.yml

```yaml
management:
  endpoints:
    web:
      exposure:
        include: "health,info"
  security:
    enabled: true
```

### examples/OBS/negative/SRS-OBS-02-001_negative.yml

```yaml
management:
  endpoints:
    web:
      exposure:
        include: "*"
  security:
    enabled: false
```

### examples/OBS/positive/SRS-OBS-03-001_positive.java

```java
package com.example.obs;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class ScopedPointcutAspect {
    @Pointcut("execution(* com.example.service..*(..))")
    public void serviceMethods() {}

    @Before("serviceMethods()")
    public void beforeService() {}
}
```

### examples/OBS/negative/SRS-OBS-03-001_negative.java

```java
package com.example.obs;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class WidePointcutAspect {
    @Pointcut("execution(* *(..))")
    public void anyMethod() {}

    @Before("anyMethod()")
    public void beforeAny() {}
}
```

## 3. 夹具（5 个）

### tests/fixtures/TC-PERF-001/input.java

```java
package com.example.perf;

import org.springframework.web.client.RestTemplate;

public class RemoteCallInLoop {
    private final RestTemplate restTemplate = new RestTemplate();

    public void fetchAll(String[] ids) {
        for (String id : ids) {
            String url = "https://example.com/api/items/" + id;
            restTemplate.getForObject(url, String.class);
        }
    }
}
```

### tests/fixtures/TC-PERF-002/input.java

```java
package com.example.perf;

import java.util.List;
import org.springframework.data.repository.Repository;

public interface UserRepository extends Repository<User, Long> {
    List<User> findAll();
}

class User {}
```

### tests/fixtures/TC-OBS-001/input.java

```java
package com.example.obs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public void logUser(String userId) {
        log.info("user=" + userId);
    }
}
```

### tests/fixtures/TC-OBS-002/input.yml

```yaml
management:
  endpoints:
    web:
      exposure:
        include: "*"
  security:
    enabled: false
```

### tests/fixtures/TC-OBS-003/input.java

```java
package com.example.obs;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class WidePointcutAspect {
    @Pointcut("execution(* *(..))")
    public void anyMethod() {}

    @Before("anyMethod()")
    public void beforeAny() {}
}
```

## 4. 测试用例 JSON（5 个完整文件）

### tests/cases/TC-PERF-001.json

```json
{
  "id": "TC-PERF-001",
  "rule_id": "SRS-PERF-01-001",
  "category": "PERF",
  "subcategory": "01",
  "title": "循环内远程调用",
  "description": "检测 for/while 循环体内调用 RestTemplate/WebClient/Feign 等远程调用。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-PERF-001/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["循环", "远程调用", "RestTemplate"],
    "location_hint": {
      "file": "tests/fixtures/TC-PERF-001/input.java",
      "line_start": 11,
      "line_end": 11,
      "symbol": "RemoteCallInLoop.fetchAll"
    }
  },
  "positive_case": "examples/PERF/positive/SRS-PERF-01-001_positive.java",
  "negative_case": "examples/PERF/negative/SRS-PERF-01-001_negative.java",
  "tags": ["PERF", "01", "fixture", "negative", "pending-detection-evaluation"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

### tests/cases/TC-PERF-002.json

```json
{
  "id": "TC-PERF-002",
  "rule_id": "SRS-PERF-02-001",
  "category": "PERF",
  "subcategory": "02",
  "title": "大集合全量加载/分页缺失",
  "description": "检测 Repository 查询返回 List 且未传 Pageable/limit。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-PERF-002/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["List", "分页", "Pageable"],
    "location_hint": {
      "file": "tests/fixtures/TC-PERF-002/input.java",
      "line_start": 7,
      "line_end": 7,
      "symbol": "UserRepository.findAll"
    }
  },
  "positive_case": "examples/PERF/positive/SRS-PERF-02-001_positive.java",
  "negative_case": "examples/PERF/negative/SRS-PERF-02-001_negative.java",
  "tags": ["PERF", "02", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

### tests/cases/TC-OBS-001.json

```json
{
  "id": "TC-OBS-001",
  "rule_id": "SRS-OBS-01-001",
  "category": "OBS",
  "subcategory": "01",
  "title": "日志占位符拼接字符串",
  "description": "检测 log.info(\"x=\" + var) 而非占位符。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-OBS-001/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "MINOR",
    "message_contains": ["日志", "占位符", "字符串拼接"],
    "location_hint": {
      "file": "tests/fixtures/TC-OBS-001/input.java",
      "line_start": 10,
      "line_end": 10,
      "symbol": "UserService.logUser"
    }
  },
  "positive_case": "examples/OBS/positive/SRS-OBS-01-001_positive.java",
  "negative_case": "examples/OBS/negative/SRS-OBS-01-001_negative.java",
  "tags": ["OBS", "01", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

### tests/cases/TC-OBS-002.json

```json
{
  "id": "TC-OBS-002",
  "rule_id": "SRS-OBS-02-001",
  "category": "OBS",
  "subcategory": "02",
  "title": "Actuator 端点未授权暴露",
  "description": "检测 management.endpoints.web.exposure.include=* 且无授权。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-OBS-002/input.yml",
    "content": null,
    "language": "yaml",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "CRITICAL",
    "message_contains": ["Actuator", "端点", "未授权", "exposure.include"],
    "location_hint": {
      "file": "tests/fixtures/TC-OBS-002/input.yml",
      "line_start": 5,
      "line_end": 5,
      "symbol": "management.endpoints.web.exposure.include"
    }
  },
  "positive_case": "examples/OBS/positive/SRS-OBS-02-001_positive.yml",
  "negative_case": "examples/OBS/negative/SRS-OBS-02-001_negative.yml",
  "tags": ["OBS", "02", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

### tests/cases/TC-OBS-003.json

```json
{
  "id": "TC-OBS-003",
  "rule_id": "SRS-OBS-03-001",
  "category": "OBS",
  "subcategory": "03",
  "title": "日志或事务切点表达式过宽",
  "description": "检测 @Pointcut/@Around execution 表达式匹配过宽。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-OBS-003/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["Pointcut", "execution", "过宽"],
    "location_hint": {
      "file": "tests/fixtures/TC-OBS-003/input.java",
      "line_start": 9,
      "line_end": 9,
      "symbol": "WidePointcutAspect.anyMethod"
    }
  },
  "positive_case": "examples/OBS/positive/SRS-OBS-03-001_positive.java",
  "negative_case": "examples/OBS/negative/SRS-OBS-03-001_negative.java",
  "tags": ["OBS", "03", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

## 5. 期望结果 JSON（5 个完整文件）

### tests/expected/TC-PERF-001.json

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-PERF-001",
  "rule_id": "SRS-PERF-01-001",
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["循环", "远程调用", "RestTemplate"],
    "location_hint": {
      "file": "tests/fixtures/TC-PERF-001/input.java",
      "line_start": 11,
      "line_end": 11,
      "symbol": "RemoteCallInLoop.fetchAll"
    }
  }
}
```

### tests/expected/TC-PERF-002.json

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-PERF-002",
  "rule_id": "SRS-PERF-02-001",
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["List", "分页", "Pageable"],
    "location_hint": {
      "file": "tests/fixtures/TC-PERF-002/input.java",
      "line_start": 7,
      "line_end": 7,
      "symbol": "UserRepository.findAll"
    }
  }
}
```

### tests/expected/TC-OBS-001.json

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-OBS-001",
  "rule_id": "SRS-OBS-01-001",
  "expected": {
    "triggered": true,
    "severity": "MINOR",
    "message_contains": ["日志", "占位符", "字符串拼接"],
    "location_hint": {
      "file": "tests/fixtures/TC-OBS-001/input.java",
      "line_start": 10,
      "line_end": 10,
      "symbol": "UserService.logUser"
    }
  }
}
```

### tests/expected/TC-OBS-002.json

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-OBS-002",
  "rule_id": "SRS-OBS-02-001",
  "expected": {
    "triggered": true,
    "severity": "CRITICAL",
    "message_contains": ["Actuator", "端点", "未授权", "exposure.include"],
    "location_hint": {
      "file": "tests/fixtures/TC-OBS-002/input.yml",
      "line_start": 5,
      "line_end": 5,
      "symbol": "management.endpoints.web.exposure.include"
    }
  }
}
```

### tests/expected/TC-OBS-003.json

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-OBS-003",
  "rule_id": "SRS-OBS-03-001",
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["Pointcut", "execution", "过宽"],
    "location_hint": {
      "file": "tests/fixtures/TC-OBS-003/input.java",
      "line_start": 9,
      "line_end": 9,
      "symbol": "WidePointcutAspect.anyMethod"
    }
  }
}
```

## 6. examples/index.json 新增条目（10 条，待 C8 合并）

本次新增 10 条，待 C8 阶段统一合并。C8 合并时按 id 升序、已有条目逐字保留。

```json
[
  {
    "id": "SRS-PERF-01-001_positive",
    "rule_id": "SRS-PERF-01-001",
    "category": "PERF",
    "subcategory": "01",
    "polarity": "positive",
    "file": "examples/PERF/positive/SRS-PERF-01-001_positive.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "循环外批量远程调用"
  },
  {
    "id": "SRS-PERF-01-001_negative",
    "rule_id": "SRS-PERF-01-001",
    "category": "PERF",
    "subcategory": "01",
    "polarity": "negative",
    "file": "examples/PERF/negative/SRS-PERF-01-001_negative.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "循环内远程调用"
  },
  {
    "id": "SRS-PERF-02-001_positive",
    "rule_id": "SRS-PERF-02-001",
    "category": "PERF",
    "subcategory": "02",
    "polarity": "positive",
    "file": "examples/PERF/positive/SRS-PERF-02-001_positive.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "Repository 分页查询"
  },
  {
    "id": "SRS-PERF-02-001_negative",
    "rule_id": "SRS-PERF-02-001",
    "category": "PERF",
    "subcategory": "02",
    "polarity": "negative",
    "file": "examples/PERF/negative/SRS-PERF-02-001_negative.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "Repository 无分页全量查询"
  },
  {
    "id": "SRS-OBS-01-001_positive",
    "rule_id": "SRS-OBS-01-001",
    "category": "OBS",
    "subcategory": "01",
    "polarity": "positive",
    "file": "examples/OBS/positive/SRS-OBS-01-001_positive.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "日志占位符"
  },
  {
    "id": "SRS-OBS-01-001_negative",
    "rule_id": "SRS-OBS-01-001",
    "category": "OBS",
    "subcategory": "01",
    "polarity": "negative",
    "file": "examples/OBS/negative/SRS-OBS-01-001_negative.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "日志字符串拼接"
  },
  {
    "id": "SRS-OBS-02-001_positive",
    "rule_id": "SRS-OBS-02-001",
    "category": "OBS",
    "subcategory": "02",
    "polarity": "positive",
    "file": "examples/OBS/positive/SRS-OBS-02-001_positive.yml",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "yaml",
    "title": "Actuator 最小化暴露并授权"
  },
  {
    "id": "SRS-OBS-02-001_negative",
    "rule_id": "SRS-OBS-02-001",
    "category": "OBS",
    "subcategory": "02",
    "polarity": "negative",
    "file": "examples/OBS/negative/SRS-OBS-02-001_negative.yml",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "yaml",
    "title": "Actuator 端点全暴露未授权"
  },
  {
    "id": "SRS-OBS-03-001_positive",
    "rule_id": "SRS-OBS-03-001",
    "category": "OBS",
    "subcategory": "03",
    "polarity": "positive",
    "file": "examples/OBS/positive/SRS-OBS-03-001_positive.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "切点表达式限定包"
  },
  {
    "id": "SRS-OBS-03-001_negative",
    "rule_id": "SRS-OBS-03-001",
    "category": "OBS",
    "subcategory": "03",
    "polarity": "negative",
    "file": "examples/OBS/negative/SRS-OBS-03-001_negative.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "切点表达式全匹配"
  }
]
```

## 7. tests/cases/index.json 新增条目（5 条，待 C8 合并）

本次新增 5 条，待 C8 阶段统一合并。C8 合并时按 id 升序、已有条目逐字保留。

```json
[
  {
    "id": "TC-PERF-001",
    "rule_id": "SRS-PERF-01-001",
    "category": "PERF",
    "file": "tests/cases/TC-PERF-001.json",
    "status": "ACTIVE",
    "version": "1.0.0",
    "title": "循环内远程调用",
    "tags": ["PERF", "01", "fixture", "negative", "pending-detection-evaluation"]
  },
  {
    "id": "TC-PERF-002",
    "rule_id": "SRS-PERF-02-001",
    "category": "PERF",
    "file": "tests/cases/TC-PERF-002.json",
    "status": "ACTIVE",
    "version": "1.0.0",
    "title": "大集合全量加载/分页缺失",
    "tags": ["PERF", "02", "fixture", "negative"]
  },
  {
    "id": "TC-OBS-001",
    "rule_id": "SRS-OBS-01-001",
    "category": "OBS",
    "file": "tests/cases/TC-OBS-001.json",
    "status": "ACTIVE",
    "version": "1.0.0",
    "title": "日志占位符拼接字符串",
    "tags": ["OBS", "01", "fixture", "negative"]
  },
  {
    "id": "TC-OBS-002",
    "rule_id": "SRS-OBS-02-001",
    "category": "OBS",
    "file": "tests/cases/TC-OBS-002.json",
    "status": "ACTIVE",
    "version": "1.0.0",
    "title": "Actuator 端点未授权暴露",
    "tags": ["OBS", "02", "fixture", "negative"]
  },
  {
    "id": "TC-OBS-003",
    "rule_id": "SRS-OBS-03-001",
    "category": "OBS",
    "file": "tests/cases/TC-OBS-003.json",
    "status": "ACTIVE",
    "version": "1.0.0",
    "title": "日志或事务切点表达式过宽",
    "tags": ["OBS", "03", "fixture", "negative"]
  }
]
```

## 8. 试点结论

本次 C6 已为 PERF 2 条、OBS 3 条规则补齐正反例、夹具、测试用例、expected 与索引新增片段。  
测试用例 JSON 顶层保持 13 字段，expected 为完整 object，positive_case/negative_case 为字符串路径；expected JSON 顶层保持 6 字段并使用 case_id。  
message_contains 未重复 rule_id。索引仅输出新增条目片段，未合并已有索引。  
TC-PERF-001 已加入 `pending-detection-evaluation`。YAML 夹具与正反例使用 `language=yaml`。  
未复现 C1～C5 历史错误。待 C8 按 id 升序统一合并索引，后续执行验证规则命中与 expected 一致性。