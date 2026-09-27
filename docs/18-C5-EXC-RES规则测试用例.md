# 18-C5-EXC-RES规则测试用例

## 1. 说明

本文件为 C5 阶段产出，覆盖 B5 冻结的 EXC 4 条与 RES 1 条规则。  
内容包含正反例、夹具、测试用例、期望结果、索引新增条目片段。  
遵循 C0 冻结结构；本次不合并索引，索引合并留到 C8。  
不修改 A5、A6 Schema，不修改 B1～B7 规则文件。

## 2. 正反例代码（5 对，共 10 个）

### 2.1 SRS-EXC-01-001

`examples/EXC/positive/SRS-EXC-01-001_positive.java`

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SRS_EXC_01_001_positive {
    private static final Logger log = LoggerFactory.getLogger(SRS_EXC_01_001_positive.class);

    void handle() {
        try {
            doWork();
        } catch (Exception e) {
            log.error("handle failed", e);
            throw new IllegalStateException("handle failed", e);
        }
    }

    private void doWork() {
        throw new IllegalArgumentException("bad");
    }
}
```

`examples/EXC/negative/SRS-EXC-01-001_negative.java`

```java
class SRS_EXC_01_001_negative {
    void handle() {
        try {
            doWork();
        } catch (Exception e) {
            // ignore
        }
    }

    private void doWork() {
        throw new IllegalArgumentException("bad");
    }
}
```

### 2.2 SRS-EXC-02-001

`examples/EXC/positive/SRS-EXC-02-001_positive.java`

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SRS_EXC_02_001_positive {
    private static final Logger log = LoggerFactory.getLogger(SRS_EXC_02_001_positive.class);

    void handle() {
        try {
            doWork();
        } catch (Exception e) {
            log.error("doWork failed", e);
        }
    }

    private void doWork() {
        throw new IllegalArgumentException("bad");
    }
}
```

`examples/EXC/negative/SRS-EXC-02-001_negative.java`

```java
class SRS_EXC_02_001_negative {
    void handle() {
        try {
            doWork();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doWork() {
        throw new IllegalArgumentException("bad");
    }
}
```

### 2.3 SRS-EXC-04-001

`examples/EXC/positive/SRS-EXC-04-001_positive.java`

```java
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
class SRS_EXC_04_001_positive {
    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> handle(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.<String, Object>of("code", "INTERNAL_ERROR", "message", "系统繁忙，请稍后重试"));
    }
}
```

`examples/EXC/negative/SRS-EXC-04-001_negative.java`

```java
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
class SRS_EXC_04_001_negative {
    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> handle(Exception e) {
        return ResponseEntity.internalServerError()
                .body(Map.<String, Object>of("message", e.getMessage(), "stack", e.getStackTrace()));
    }
}
```

### 2.4 SRS-EXC-05-001

`examples/EXC/positive/SRS-EXC-05-001_positive.java`

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

class SRS_EXC_05_001_positive {
    private static final Logger log = LoggerFactory.getLogger(SRS_EXC_05_001_positive.class);

    @Async
    public void run() {
        try {
            doWork();
        } catch (Exception e) {
            log.error("async task failed", e);
        }
    }

    private void doWork() {
        throw new IllegalStateException("bad");
    }
}
```

`examples/EXC/negative/SRS-EXC-05-001_negative.java`

```java
import org.springframework.scheduling.annotation.Async;

class SRS_EXC_05_001_negative {
    @Async
    public void run() {
        doWork();
    }

    private void doWork() {
        throw new IllegalStateException("bad");
    }
}
```

### 2.5 SRS-RES-01-001

`examples/RES/positive/SRS-RES-01-001_positive.java`

```java
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

class SRS_RES_01_001_positive {
    void read() throws IOException {
        try (InputStream in = new FileInputStream("data.txt")) {
            in.read();
        }
    }
}
```

`examples/RES/negative/SRS-RES-01-001_negative.java`

```java
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

class SRS_RES_01_001_negative {
    void read() throws IOException {
        InputStream in = new FileInputStream("data.txt");
        in.read();
    }
}
```

## 3. 夹具（5 个）

### 3.1 tests/fixtures/TC-EXC-001/input.java

```java
class Input {
    void handle() {
        try {
            doWork();
        } catch (Exception e) {
            // ignore
        }
    }

    private void doWork() {
        throw new IllegalArgumentException("bad");
    }
}
```

### 3.2 tests/fixtures/TC-EXC-002/input.java

```java
class Input {
    void handle() {
        try {
            doWork();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doWork() {
        throw new IllegalArgumentException("bad");
    }
}
```

### 3.3 tests/fixtures/TC-EXC-004/input.java

```java
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
class Input {
    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> handle(Exception e) {
        return ResponseEntity.internalServerError()
                .body(Map.<String, Object>of("message", e.getMessage(), "stack", e.getStackTrace()));
    }
}
```

### 3.4 tests/fixtures/TC-EXC-005/input.java

```java
import org.springframework.scheduling.annotation.Async;

class Input {
    @Async
    public void run() {
        doWork();
    }

    private void doWork() {
        throw new IllegalStateException("bad");
    }
}
```

### 3.5 tests/fixtures/TC-RES-001/input.java

```java
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

class Input {
    void read() throws IOException {
        InputStream in = new FileInputStream("data.txt");
        in.read();
    }
}
```

## 4. 测试用例 JSON（5 个完整文件）

### 4.1 tests/cases/TC-EXC-001.json

```json
{
  "id": "TC-EXC-001",
  "rule_id": "SRS-EXC-01-001",
  "category": "EXC",
  "subcategory": "01",
  "title": "禁止捕获 Exception 后吞没异常",
  "description": "catch (Exception) 块为空或仅注释，未记录、未抛出、未处理。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-EXC-001/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["catch", "Exception", "异常吞没"],
    "location_hint": {
      "file": "tests/fixtures/TC-EXC-001/input.java",
      "line_start": 5,
      "line_end": 7,
      "symbol": "Input.handle"
    }
  },
  "positive_case": "examples/EXC/positive/SRS-EXC-01-001_positive.java",
  "negative_case": "examples/EXC/negative/SRS-EXC-01-001_negative.java",
  "tags": ["EXC", "01", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

### 4.2 tests/cases/TC-EXC-002.json

```json
{
  "id": "TC-EXC-002",
  "rule_id": "SRS-EXC-02-001",
  "category": "EXC",
  "subcategory": "02",
  "title": "禁止 catch 后仅 printStackTrace",
  "description": "catch 块中仅调用 e.printStackTrace()，未使用日志框架或统一异常处理。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-EXC-002/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["printStackTrace", "日志"],
    "location_hint": {
      "file": "tests/fixtures/TC-EXC-002/input.java",
      "line_start": 5,
      "line_end": 7,
      "symbol": "Input.handle"
    }
  },
  "positive_case": "examples/EXC/positive/SRS-EXC-02-001_positive.java",
  "negative_case": "examples/EXC/negative/SRS-EXC-02-001_negative.java",
  "tags": ["EXC", "02", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

### 4.3 tests/cases/TC-EXC-004.json

```json
{
  "id": "TC-EXC-004",
  "rule_id": "SRS-EXC-04-001",
  "category": "EXC",
  "subcategory": "04",
  "title": "禁止对外异常响应暴露堆栈或内部类",
  "description": "API 异常响应直接返回 e.getMessage() 和 e.getStackTrace()，暴露堆栈信息。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-EXC-004/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["getStackTrace", "getMessage", "堆栈"],
    "location_hint": {
      "file": "tests/fixtures/TC-EXC-004/input.java",
      "line_start": 10,
      "line_end": 12,
      "symbol": "Input.handle"
    }
  },
  "positive_case": "examples/EXC/positive/SRS-EXC-04-001_positive.java",
  "negative_case": "examples/EXC/negative/SRS-EXC-04-001_negative.java",
  "tags": ["EXC", "04", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

### 4.4 tests/cases/TC-EXC-005.json

```json
{
  "id": "TC-EXC-005",
  "rule_id": "SRS-EXC-05-001",
  "category": "EXC",
  "subcategory": "05",
  "title": "异步或定时任务异常必须兜底处理",
  "description": "@Async 方法体未使用 try-catch 或统一异常处理，异常直接抛出。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-EXC-005/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "CRITICAL",
    "message_contains": ["@Async", "try-catch", "异步异常"],
    "location_hint": {
      "file": "tests/fixtures/TC-EXC-005/input.java",
      "line_start": 4,
      "line_end": 7,
      "symbol": "Input.run"
    }
  },
  "positive_case": "examples/EXC/positive/SRS-EXC-05-001_positive.java",
  "negative_case": "examples/EXC/negative/SRS-EXC-05-001_negative.java",
  "tags": ["EXC", "05", "fixture", "negative", "pending-detection-evaluation"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

### 4.5 tests/cases/TC-RES-001.json

```json
{
  "id": "TC-RES-001",
  "rule_id": "SRS-RES-01-001",
  "category": "RES",
  "subcategory": "01",
  "title": "禁止未关闭 InputStream 或 Connection",
  "description": "InputStream 未在 try-with-resources 或 finally 中关闭。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-RES-001/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["InputStream", "close", "资源未释放"],
    "location_hint": {
      "file": "tests/fixtures/TC-RES-001/input.java",
      "line_start": 7,
      "line_end": 8,
      "symbol": "Input.read"
    }
  },
  "positive_case": "examples/RES/positive/SRS-RES-01-001_positive.java",
  "negative_case": "examples/RES/negative/SRS-RES-01-001_negative.java",
  "tags": ["RES", "01", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

## 5. 期望结果 JSON（5 个完整文件）

### 5.1 tests/expected/TC-EXC-001.json

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-EXC-001",
  "rule_id": "SRS-EXC-01-001",
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["catch", "Exception", "异常吞没"],
    "location_hint": {
      "file": "tests/fixtures/TC-EXC-001/input.java",
      "line_start": 5,
      "line_end": 7,
      "symbol": "Input.handle"
    }
  }
}
```

### 5.2 tests/expected/TC-EXC-002.json

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-EXC-002",
  "rule_id": "SRS-EXC-02-001",
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["printStackTrace", "日志"],
    "location_hint": {
      "file": "tests/fixtures/TC-EXC-002/input.java",
      "line_start": 5,
      "line_end": 7,
      "symbol": "Input.handle"
    }
  }
}
```

### 5.3 tests/expected/TC-EXC-004.json

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-EXC-004",
  "rule_id": "SRS-EXC-04-001",
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["getStackTrace", "getMessage", "堆栈"],
    "location_hint": {
      "file": "tests/fixtures/TC-EXC-004/input.java",
      "line_start": 10,
      "line_end": 12,
      "symbol": "Input.handle"
    }
  }
}
```

### 5.4 tests/expected/TC-EXC-005.json

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-EXC-005",
  "rule_id": "SRS-EXC-05-001",
  "expected": {
    "triggered": true,
    "severity": "CRITICAL",
    "message_contains": ["@Async", "try-catch", "异步异常"],
    "location_hint": {
      "file": "tests/fixtures/TC-EXC-005/input.java",
      "line_start": 4,
      "line_end": 7,
      "symbol": "Input.run"
    }
  }
}
```

### 5.5 tests/expected/TC-RES-001.json

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-RES-001",
  "rule_id": "SRS-RES-01-001",
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["InputStream", "close", "资源未释放"],
    "location_hint": {
      "file": "tests/fixtures/TC-RES-001/input.java",
      "line_start": 7,
      "line_end": 8,
      "symbol": "Input.read"
    }
  }
}
```

## 6. examples/index.json 新增条目（10 条，待 C8 合并）

本次新增 10 条，待 C8 阶段统一合并。C8 合并时按 id 升序、已有条目逐字保留。

```json
[
  {
    "id": "SRS-EXC-01-001_negative",
    "rule_id": "SRS-EXC-01-001",
    "category": "EXC",
    "subcategory": "01",
    "polarity": "negative",
    "file": "examples/EXC/negative/SRS-EXC-01-001_negative.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "禁止捕获 Exception 后吞没异常-反例"
  },
  {
    "id": "SRS-EXC-01-001_positive",
    "rule_id": "SRS-EXC-01-001",
    "category": "EXC",
    "subcategory": "01",
    "polarity": "positive",
    "file": "examples/EXC/positive/SRS-EXC-01-001_positive.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "禁止捕获 Exception 后吞没异常-正例"
  },
  {
    "id": "SRS-EXC-02-001_negative",
    "rule_id": "SRS-EXC-02-001",
    "category": "EXC",
    "subcategory": "02",
    "polarity": "negative",
    "file": "examples/EXC/negative/SRS-EXC-02-001_negative.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "禁止 catch 后仅 printStackTrace-反例"
  },
  {
    "id": "SRS-EXC-02-001_positive",
    "rule_id": "SRS-EXC-02-001",
    "category": "EXC",
    "subcategory": "02",
    "polarity": "positive",
    "file": "examples/EXC/positive/SRS-EXC-02-001_positive.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "禁止 catch 后仅 printStackTrace-正例"
  },
  {
    "id": "SRS-EXC-04-001_negative",
    "rule_id": "SRS-EXC-04-001",
    "category": "EXC",
    "subcategory": "04",
    "polarity": "negative",
    "file": "examples/EXC/negative/SRS-EXC-04-001_negative.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "禁止对外异常响应暴露堆栈或内部类-反例"
  },
  {
    "id": "SRS-EXC-04-001_positive",
    "rule_id": "SRS-EXC-04-001",
    "category": "EXC",
    "subcategory": "04",
    "polarity": "positive",
    "file": "examples/EXC/positive/SRS-EXC-04-001_positive.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "禁止对外异常响应暴露堆栈或内部类-正例"
  },
  {
    "id": "SRS-EXC-05-001_negative",
    "rule_id": "SRS-EXC-05-001",
    "category": "EXC",
    "subcategory": "05",
    "polarity": "negative",
    "file": "examples/EXC/negative/SRS-EXC-05-001_negative.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "异步或定时任务异常必须兜底处理-反例"
  },
  {
    "id": "SRS-EXC-05-001_positive",
    "rule_id": "SRS-EXC-05-001",
    "category": "EXC",
    "subcategory": "05",
    "polarity": "positive",
    "file": "examples/EXC/positive/SRS-EXC-05-001_positive.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "异步或定时任务异常必须兜底处理-正例"
  },
  {
    "id": "SRS-RES-01-001_negative",
    "rule_id": "SRS-RES-01-001",
    "category": "RES",
    "subcategory": "01",
    "polarity": "negative",
    "file": "examples/RES/negative/SRS-RES-01-001_negative.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "禁止未关闭 InputStream 或 Connection-反例"
  },
  {
    "id": "SRS-RES-01-001_positive",
    "rule_id": "SRS-RES-01-001",
    "category": "RES",
    "subcategory": "01",
    "polarity": "positive",
    "file": "examples/RES/positive/SRS-RES-01-001_positive.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "禁止未关闭 InputStream 或 Connection-正例"
  }
]
```

## 7. tests/cases/index.json 新增条目（5 条，待 C8 合并）

本次新增 5 条，待 C8 阶段统一合并。C8 合并时按 id 升序、已有条目逐字保留。

```json
[
  {
    "id": "TC-EXC-001",
    "rule_id": "SRS-EXC-01-001",
    "category": "EXC",
    "file": "tests/cases/TC-EXC-001.json",
    "status": "ACTIVE",
    "version": "1.0.0",
    "title": "禁止捕获 Exception 后吞没异常",
    "tags": ["EXC", "01", "fixture", "negative"]
  },
  {
    "id": "TC-EXC-002",
    "rule_id": "SRS-EXC-02-001",
    "category": "EXC",
    "file": "tests/cases/TC-EXC-002.json",
    "status": "ACTIVE",
    "version": "1.0.0",
    "title": "禁止 catch 后仅 printStackTrace",
    "tags": ["EXC", "02", "fixture", "negative"]
  },
  {
    "id": "TC-EXC-004",
    "rule_id": "SRS-EXC-04-001",
    "category": "EXC",
    "file": "tests/cases/TC-EXC-004.json",
    "status": "ACTIVE",
    "version": "1.0.0",
    "title": "禁止对外异常响应暴露堆栈或内部类",
    "tags": ["EXC", "04", "fixture", "negative"]
  },
  {
    "id": "TC-EXC-005",
    "rule_id": "SRS-EXC-05-001",
    "category": "EXC",
    "file": "tests/cases/TC-EXC-005.json",
    "status": "ACTIVE",
    "version": "1.0.0",
    "title": "异步或定时任务异常必须兜底处理",
    "tags": ["EXC", "05", "fixture", "negative", "pending-detection-evaluation"]
  },
  {
    "id": "TC-RES-001",
    "rule_id": "SRS-RES-01-001",
    "category": "RES",
    "file": "tests/cases/TC-RES-001.json",
    "status": "ACTIVE",
    "version": "1.0.0",
    "title": "禁止未关闭 InputStream 或 Connection",
    "tags": ["RES", "01", "fixture", "negative"]
  }
]
```

## 8. 试点结论

C5 已为 EXC 4 条与 RES 1 条规则补齐正反例、夹具、测试用例、期望结果与索引新增片段。  
测试用例 JSON 保持顶层 13 字段，expected 为完整 object，positive_case/negative_case 为字符串路径。  
expected JSON 保持顶层 6 字段并使用 case_id。  
索引片段仅输出新增条目，待 C8 阶段按 id 升序统一合并，已有条目逐字保留。