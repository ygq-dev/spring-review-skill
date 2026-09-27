# docs/14-C3a-WEB规则测试用例.md

## 1. 说明

- 范围：为 B3 冻结的 WEB 5 条规则编写正反例、夹具、测试用例、expected、索引新增条目。
- 统一元数据：`generated_at="2026-09-24T00:00:00Z"`、`generated_by="manual/1.0.0"`、`version="1.0.0"`、`status="ACTIVE"`。
- 阻塞报告：本次未提供 C1、C2a、C2b 已有 `examples/index.json` 与 `tests/cases/index.json` 的具体 items 内容，无法在不臆造、不篡改已有条目的前提下输出“完整 40 条 / 20 条”合并索引。以下第 6、7 节仅列出本次新增条目，供追加到已有 items 末尾；已有条目必须逐字保留。若需完整索引，请提供已有两个 index 文件内容后合并。

## 2. 正反例代码（5 对，共 10 个）

### examples/WEB/positive/SRS-WEB-01-001_positive.java

```java
package examples.web.positive;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    @GetMapping("/user")
    public String getUser(@RequestParam String user) {
        String currentUser = user;
        return currentUser;
    }
}
```

### examples/WEB/negative/SRS-WEB-01-001_negative.java

```java
package examples.web.negative;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    private String currentUser;

    @GetMapping("/user")
    public String getUser() {
        currentUser = "request-user";
        return currentUser;
    }
}
```

### examples/WEB/positive/SRS-WEB-02-001_positive.java

```java
package examples.web.positive;

import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    @PostMapping("/users")
    public String create(@Valid @RequestBody UserRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "invalid";
        }
        return request.getName();
    }
}

class UserRequest {
    private String name;
    public String getName() { return name; }
}
```

### examples/WEB/negative/SRS-WEB-02-001_negative.java

```java
package examples.web.negative;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    @PostMapping("/users")
    public String create(@RequestBody UserRequest request) {
        return request.getName();
    }
}

class UserRequest {
    private String name;
    public String getName() { return name; }
}
```

### examples/WEB/positive/SRS-WEB-03-001_positive.java

```java
package examples.web.positive;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handle(IllegalStateException ex) {
        return ResponseEntity.badRequest().body("request failed");
    }
}
```

### examples/WEB/negative/SRS-WEB-03-001_negative.java

```java
package examples.web.negative;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ErrorController {
    @GetMapping("/error-demo")
    public String error() {
        try {
            throw new IllegalStateException("boom");
        } catch (Exception e) {
            return e.getMessage() + "\n" + e.getStackTrace();
        }
    }
}
```

### examples/WEB/positive/SRS-WEB-04-001_positive.java

```java
package examples.web.positive;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("https://app.example.com")
            .allowCredentials(true);
    }
}
```

### examples/WEB/negative/SRS-WEB-04-001_negative.java

```java
package examples.web.negative;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("*")
            .allowCredentials(true);
    }
}
```

### examples/WEB/positive/SRS-WEB-05-001_positive.java

```java
package examples.web.positive;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.WebAsyncTask;

@RestController
public class AsyncController {
    @GetMapping("/async")
    public WebAsyncTask<String> run() {
        WebAsyncTask<String> task = new WebAsyncTask<>(5000L, () -> "ok");
        task.onTimeout(() -> "timeout");
        task.onError(() -> "error");
        return task;
    }
}
```

### examples/WEB/negative/SRS-WEB-05-001_negative.java

```java
package examples.web.negative;

import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AsyncController {
    @Async
    @GetMapping("/async")
    public void run() {
        throw new IllegalStateException("async failure");
    }
}
```

## 3. 夹具（5 个）

### tests/fixtures/TC-WEB-001/input.java

```java
package tests.fixtures.tcweb001;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    private String currentUser;

    @GetMapping("/user")
    public String getUser() {
        currentUser = "request-user";
        return currentUser;
    }
}
```

### tests/fixtures/TC-WEB-002/input.java

```java
package tests.fixtures.tcweb002;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    @PostMapping("/users")
    public String create(@RequestBody UserRequest request) {
        return request.getName();
    }
}

class UserRequest {
    private String name;
    public String getName() { return name; }
}
```

### tests/fixtures/TC-WEB-003/input.java

```java
package tests.fixtures.tcweb003;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ErrorController {
    @GetMapping("/error-demo")
    public String error() {
        try {
            throw new IllegalStateException("boom");
        } catch (Exception e) {
            return e.getMessage() + "\n" + e.getStackTrace();
        }
    }
}
```

### tests/fixtures/TC-WEB-004/input.java

```java
package tests.fixtures.tcweb004;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("*")
            .allowCredentials(true);
    }
}
```

### tests/fixtures/TC-WEB-005/input.java

```java
package tests.fixtures.tcweb005;

import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AsyncController {
    @Async
    @GetMapping("/async")
    public void run() {
        throw new IllegalStateException("async failure");
    }
}
```

## 4. 测试用例 JSON（5 个完整文件）

### tests/cases/TC-WEB-001.json

```json
{
  "id": "TC-WEB-001",
  "rule_id": "SRS-WEB-01-001",
  "category": "WEB",
  "subcategory": "01",
  "title": "Controller 中保存请求态可变字段",
  "description": "检测 @Controller/@RestController 单例中的可变实例字段被请求处理方法写入。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-WEB-001/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "CRITICAL",
    "message_contains": ["Controller", "可变实例字段", "currentUser"],
    "location_hint": {
      "file": "tests/fixtures/TC-WEB-001/input.java",
      "line_start": 8,
      "line_end": 8,
      "symbol": "UserController.currentUser"
    }
  },
  "positive_case": "examples/WEB/positive/SRS-WEB-01-001_positive.java",
  "negative_case": "examples/WEB/negative/SRS-WEB-01-001_negative.java",
  "tags": ["WEB", "SRS-WEB-01-001", "AST", "CUSTOM", "controller", "mutable-field"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

### tests/cases/TC-WEB-002.json

```json
{
  "id": "TC-WEB-002",
  "rule_id": "SRS-WEB-02-001",
  "category": "WEB",
  "subcategory": "02",
  "title": "请求参数缺少 @Valid/@Validated",
  "description": "检测 @RequestBody、@ModelAttribute 参数未加校验注解。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-WEB-002/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["@RequestBody", "@Valid", "校验"],
    "location_hint": {
      "file": "tests/fixtures/TC-WEB-002/input.java",
      "line_start": 10,
      "line_end": 10,
      "symbol": "UserController.create"
    }
  },
  "positive_case": "examples/WEB/positive/SRS-WEB-02-001_positive.java",
  "negative_case": "examples/WEB/negative/SRS-WEB-02-001_negative.java",
  "tags": ["WEB", "SRS-WEB-02-001", "AST", "CUSTOM", "validation", "request-body"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

### tests/cases/TC-WEB-003.json

```json
{
  "id": "TC-WEB-003",
  "rule_id": "SRS-WEB-03-001",
  "category": "WEB",
  "subcategory": "03",
  "title": "全局异常处理缺失或返回堆栈",
  "description": "检测 @ControllerAdvice/@ExceptionHandler 缺失或控制器返回异常堆栈。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-WEB-003/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["全局异常", "堆栈", "@ControllerAdvice"],
    "location_hint": {
      "file": "tests/fixtures/TC-WEB-003/input.java",
      "line_start": 13,
      "line_end": 13,
      "symbol": "ErrorController.error"
    }
  },
  "positive_case": "examples/WEB/positive/SRS-WEB-03-001_positive.java",
  "negative_case": "examples/WEB/negative/SRS-WEB-03-001_negative.java",
  "tags": ["WEB", "SRS-WEB-03-001", "RULE_ENGINE", "CUSTOM", "exception-handler", "stacktrace"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

### tests/cases/TC-WEB-004.json

```json
{
  "id": "TC-WEB-004",
  "rule_id": "SRS-WEB-04-001",
  "category": "WEB",
  "subcategory": "04",
  "title": "跨域配置过宽",
  "description": "检测 allowedOrigins=* 且 allowCredentials=true 的过宽跨域配置。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-WEB-004/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "CRITICAL",
    "message_contains": ["allowedOrigins", "*", "allowCredentials"],
    "location_hint": {
      "file": "tests/fixtures/TC-WEB-004/input.java",
      "line_start": 12,
      "line_end": 13,
      "symbol": "CorsConfig.addCorsMappings"
    }
  },
  "positive_case": "examples/WEB/positive/SRS-WEB-04-001_positive.java",
  "negative_case": "examples/WEB/negative/SRS-WEB-04-001_negative.java",
  "tags": ["WEB", "SRS-WEB-04-001", "AST", "CUSTOM", "cors", "credentials"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

### tests/cases/TC-WEB-005.json

```json
{
  "id": "TC-WEB-005",
  "rule_id": "SRS-WEB-05-001",
  "category": "WEB",
  "subcategory": "05",
  "title": "异步接口未处理超时/异常",
  "description": "检测 @Async、WebAsyncTask、DeferredResult 未配置超时或异常处理。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-WEB-005/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["@Async", "异常", "超时"],
    "location_hint": {
      "file": "tests/fixtures/TC-WEB-005/input.java",
      "line_start": 9,
      "line_end": 12,
      "symbol": "AsyncController.run"
    }
  },
  "positive_case": "examples/WEB/positive/SRS-WEB-05-001_positive.java",
  "negative_case": "examples/WEB/negative/SRS-WEB-05-001_negative.java",
  "tags": ["WEB", "SRS-WEB-05-001", "AST", "CUSTOM", "async", "timeout"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

## 5. 期望结果 JSON（5 个完整文件）

### tests/expected/TC-WEB-001.json

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-WEB-001",
  "rule_id": "SRS-WEB-01-001",
  "expected": {
    "triggered": true,
    "severity": "CRITICAL",
    "message_contains": ["Controller", "可变实例字段", "currentUser"],
    "location_hint": {
      "file": "tests/fixtures/TC-WEB-001/input.java",
      "line_start": 8,
      "line_end": 8,
      "symbol": "UserController.currentUser"
    }
  }
}
```

### tests/expected/TC-WEB-002.json

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-WEB-002",
  "rule_id": "SRS-WEB-02-001",
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["@RequestBody", "@Valid", "校验"],
    "location_hint": {
      "file": "tests/fixtures/TC-WEB-002/input.java",
      "line_start": 10,
      "line_end": 10,
      "symbol": "UserController.create"
    }
  }
}
```

### tests/expected/TC-WEB-003.json

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-WEB-003",
  "rule_id": "SRS-WEB-03-001",
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["全局异常", "堆栈", "@ControllerAdvice"],
    "location_hint": {
      "file": "tests/fixtures/TC-WEB-003/input.java",
      "line_start": 13,
      "line_end": 13,
      "symbol": "ErrorController.error"
    }
  }
}
```

### tests/expected/TC-WEB-004.json

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-WEB-004",
  "rule_id": "SRS-WEB-04-001",
  "expected": {
    "triggered": true,
    "severity": "CRITICAL",
    "message_contains": ["allowedOrigins", "*", "allowCredentials"],
    "location_hint": {
      "file": "tests/fixtures/TC-WEB-004/input.java",
      "line_start": 12,
      "line_end": 13,
      "symbol": "CorsConfig.addCorsMappings"
    }
  }
}
```

### tests/expected/TC-WEB-005.json

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-WEB-005",
  "rule_id": "SRS-WEB-05-001",
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["@Async", "异常", "超时"],
    "location_hint": {
      "file": "tests/fixtures/TC-WEB-005/input.java",
      "line_start": 9,
      "line_end": 12,
      "symbol": "AsyncController.run"
    }
  }
}
```

## 6. examples/index.json（本次新增 10 条，待 C8 阶段统一合并）

本次新增 10 条，待 C8 阶段统一合并。C8 合并时按 id 升序、已有条目逐字保留。

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "count": 10,
  "items": [
    {
      "id": "SRS-WEB-01-001_negative",
      "rule_id": "SRS-WEB-01-001",
      "category": "WEB",
      "subcategory": "01",
      "polarity": "negative",
      "file": "examples/WEB/negative/SRS-WEB-01-001_negative.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "Controller 可变实例字段命中"
    },
    {
      "id": "SRS-WEB-01-001_positive",
      "rule_id": "SRS-WEB-01-001",
      "category": "WEB",
      "subcategory": "01",
      "polarity": "positive",
      "file": "examples/WEB/positive/SRS-WEB-01-001_positive.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "Controller 无请求态可变字段"
    },
    {
      "id": "SRS-WEB-02-001_negative",
      "rule_id": "SRS-WEB-02-001",
      "category": "WEB",
      "subcategory": "02",
      "polarity": "negative",
      "file": "examples/WEB/negative/SRS-WEB-02-001_negative.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "请求参数缺少 @Valid"
    },
    {
      "id": "SRS-WEB-02-001_positive",
      "rule_id": "SRS-WEB-02-001",
      "category": "WEB",
      "subcategory": "02",
      "polarity": "positive",
      "file": "examples/WEB/positive/SRS-WEB-02-001_positive.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "请求参数使用 @Valid 校验"
    },
    {
      "id": "SRS-WEB-03-001_negative",
      "rule_id": "SRS-WEB-03-001",
      "category": "WEB",
      "subcategory": "03",
      "polarity": "negative",
      "file": "examples/WEB/negative/SRS-WEB-03-001_negative.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "缺少全局异常处理并返回堆栈"
    },
    {
      "id": "SRS-WEB-03-001_positive",
      "rule_id": "SRS-WEB-03-001",
      "category": "WEB",
      "subcategory": "03",
      "polarity": "positive",
      "file": "examples/WEB/positive/SRS-WEB-03-001_positive.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "全局异常处理返回统一错误响应"
    },
    {
      "id": "SRS-WEB-04-001_negative",
      "rule_id": "SRS-WEB-04-001",
      "category": "WEB",
      "subcategory": "04",
      "polarity": "negative",
      "file": "examples/WEB/negative/SRS-WEB-04-001_negative.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "跨域配置过宽"
    },
    {
      "id": "SRS-WEB-04-001_positive",
      "rule_id": "SRS-WEB-04-001",
      "category": "WEB",
      "subcategory": "04",
      "polarity": "positive",
      "file": "examples/WEB/positive/SRS-WEB-04-001_positive.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "跨域白名单配置"
    },
    {
      "id": "SRS-WEB-05-001_negative",
      "rule_id": "SRS-WEB-05-001",
      "category": "WEB",
      "subcategory": "05",
      "polarity": "negative",
      "file": "examples/WEB/negative/SRS-WEB-05-001_negative.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "异步接口未处理超时与异常"
    },
    {
      "id": "SRS-WEB-05-001_positive",
      "rule_id": "SRS-WEB-05-001",
      "category": "WEB",
      "subcategory": "05",
      "polarity": "positive",
      "file": "examples/WEB/positive/SRS-WEB-05-001_positive.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "WebAsyncTask 配置超时与异常回调"
    }
  ]
}
```

## 7. tests/cases/index.json（本次新增 5 条，待 C8 阶段统一合并）

本次新增 5 条，待 C8 阶段统一合并。C8 合并时按 id 升序、已有条目逐字保留。

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "count": 5,
  "items": [
    {
      "id": "TC-WEB-001",
      "rule_id": "SRS-WEB-01-001",
      "category": "WEB",
      "file": "tests/cases/TC-WEB-001.json",
      "status": "ACTIVE",
      "version": "1.0.0",
      "title": "Controller 中保存请求态可变字段",
      "tags": ["WEB", "SRS-WEB-01-001", "AST", "CUSTOM", "controller", "mutable-field"]
    },
    {
      "id": "TC-WEB-002",
      "rule_id": "SRS-WEB-02-001",
      "category": "WEB",
      "file": "tests/cases/TC-WEB-002.json",
      "status": "ACTIVE",
      "version": "1.0.0",
      "title": "请求参数缺少 @Valid/@Validated",
      "tags": ["WEB", "SRS-WEB-02-001", "AST", "CUSTOM", "validation", "request-body"]
    },
    {
      "id": "TC-WEB-003",
      "rule_id": "SRS-WEB-03-001",
      "category": "WEB",
      "file": "tests/cases/TC-WEB-003.json",
      "status": "ACTIVE",
      "version": "1.0.0",
      "title": "全局异常处理缺失或返回堆栈",
      "tags": ["WEB", "SRS-WEB-03-001", "RULE_ENGINE", "CUSTOM", "exception-handler", "stacktrace"]
    },
    {
      "id": "TC-WEB-004",
      "rule_id": "SRS-WEB-04-001",
      "category": "WEB",
      "file": "tests/cases/TC-WEB-004.json",
      "status": "ACTIVE",
      "version": "1.0.0",
      "title": "跨域配置过宽",
      "tags": ["WEB", "SRS-WEB-04-001", "AST", "CUSTOM", "cors", "credentials"]
    },
    {
      "id": "TC-WEB-005",
      "rule_id": "SRS-WEB-05-001",
      "category": "WEB",
      "file": "tests/cases/TC-WEB-005.json",
      "status": "ACTIVE",
      "version": "1.0.0",
      "title": "异步接口未处理超时/异常",
      "tags": ["WEB", "SRS-WEB-05-001", "AST", "CUSTOM", "async", "timeout"]
    }
  ]
}
```

## 8. 试点结论

- 已完成 WEB 5 条规则的正反例 10 个、夹具 5 个、测试用例 5 个、expected 5 个。
- 新增索引条目字段符合 A4.2：examples 使用 `polarity`、`file`，tests/cases 使用 `file`，未使用 `type`、`path`、`generated_by`、`subcategory`、`expected_path`。
- 未篡改 C1、C2a、C2b 已有条目。
- 完整 `examples/index.json` 40 条与 `tests/cases/index.json` 20 条需在 C8 阶段统一合并，按 id 升序、已有条目逐字保留。