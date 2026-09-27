# docs/16-C4-DAO-EXC-RES规则测试用例.md

## 1. 说明

本文件为 C4 阶段产出，覆盖 B4 冻结的 7 条规则：DAO 5 条、EXC 1 条、RES 1 条。本次产出 14 个正反例、7 个夹具、7 个测试用例 JSON、7 个期望结果 JSON、2 个索引新增条目片段。严格遵循 C0 结构，不修改代码、不提交、不训练模型，不修改 A5、A6 Schema，不修改 B1～B7 规则文件。

## 2. 正反例代码（7 对，共 14 个）

### 2.1 SRS-DAO-01-001

**保存路径：** `examples/DAO/positive/SRS-DAO-01-001_positive.java`

```java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicTransactionalService {
    @Transactional
    public void createOrder() {
        // ...
    }
}
```

**保存路径：** `examples/DAO/negative/SRS-DAO-01-001_negative.java`

```java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrivateTransactionalService {
    @Transactional
    private void createOrder() {
        // ...
    }
}
```

### 2.2 SRS-DAO-02-001

**保存路径：** `examples/DAO/positive/SRS-DAO-02-001_positive.java`

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SelfProxyService {
    @Autowired
    private SelfProxyService self;

    public void outer() {
        self.inner();
    }

    @Transactional
    public void inner() {
        // ...
    }
}
```

**保存路径：** `examples/DAO/negative/SRS-DAO-02-001_negative.java`

```java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SelfInvocationService {
    public void outer() {
        this.inner();
    }

    @Transactional
    public void inner() {
        // ...
    }
}
```

### 2.3 SRS-DAO-03-001

**保存路径：** `examples/DAO/positive/SRS-DAO-03-001_positive.java`

```java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReadOnlyQueryService {
    @Transactional(readOnly = true)
    public String findName(long id) {
        return "name";
    }
}
```

**保存路径：** `examples/DAO/negative/SRS-DAO-03-001_negative.java`

```java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MissingReadOnlyService {
    @Transactional
    public String findName(long id) {
        return "name";
    }
}
```

### 2.4 SRS-DAO-04-001

**保存路径：** `examples/DAO/positive/SRS-DAO-04-001_positive.java`

```java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
public class RemoteOutsideTransactionService {
    private final RestTemplate restTemplate = new RestTemplate();

    public void process() {
        String result = restTemplate.getForObject("http://example.com", String.class);
        save(result);
    }

    @Transactional
    public void save(String value) {
        // ...
    }
}
```

**保存路径：** `examples/DAO/negative/SRS-DAO-04-001_negative.java`

```java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
public class RemoteInsideTransactionService {
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public void process() {
        restTemplate.getForObject("http://example.com", String.class);
    }
}
```

### 2.5 SRS-DAO-05-001

**保存路径：** `examples/DAO/positive/SRS-DAO-05-001_positive.java`

```java
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BatchQueryService {
    private final UserRepository userRepository;

    public BatchQueryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> findUsers(List<Long> ids) {
        return userRepository.findAllById(ids);
    }

    interface UserRepository {
        List<User> findAllById(List<Long> ids);
    }

    static class User {
    }
}
```

**保存路径：** `examples/DAO/negative/SRS-DAO-05-001_negative.java`

```java
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LoopQueryService {
    private final UserRepository userRepository;

    public LoopQueryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> findUsers(List<Long> ids) {
        List<User> users = new ArrayList<>();
        for (Long id : ids) {
            users.add(userRepository.findById(id).orElse(null));
        }
        return users;
    }

    interface UserRepository {
        java.util.Optional<User> findById(Long id);
    }

    static class User {
    }
}
```

### 2.6 SRS-EXC-03-001

**保存路径：** `examples/EXC/positive/SRS-EXC-03-001_positive.java`

```java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CorrectRollbackService {
    @Transactional(rollbackFor = BusinessException.class)
    public void execute() throws BusinessException {
        throw new BusinessException("business error");
    }

    static class BusinessException extends Exception {
        BusinessException(String message) {
            super(message);
        }
    }
}
```

**保存路径：** `examples/EXC/negative/SRS-EXC-03-001_negative.java`

```java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MissingRollbackService {
    @Transactional
    public void execute() throws Exception {
        throw new Exception("checked error");
    }
}
```

### 2.7 SRS-RES-02-001

**保存路径：** `examples/RES/positive/SRS-RES-02-001_positive.java`

```java
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SafeResourceService {
    private final DataSource dataSource;

    public SafeResourceService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Transactional
    public void query() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT 1")) {
            resultSet.next();
        }
    }
}
```

**保存路径：** `examples/RES/negative/SRS-RES-02-001_negative.java`

```java
import java.sql.Connection;
import javax.sql.DataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnsafeResourceService {
    private final DataSource dataSource;

    public UnsafeResourceService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Transactional
    public void query() throws Exception {
        Connection connection = dataSource.getConnection();
        connection.createStatement().executeQuery("SELECT 1");
    }
}
```

## 3. 夹具（7 个）

**保存路径：** `tests/fixtures/TC-DAO-001/input.java`

```java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrivateTransactionalService {
    @Transactional
    private void createOrder() {
        // ...
    }
}
```

**保存路径：** `tests/fixtures/TC-DAO-002/input.java`

```java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SelfInvocationService {
    public void outer() {
        this.inner();
    }

    @Transactional
    public void inner() {
        // ...
    }
}
```

**保存路径：** `tests/fixtures/TC-DAO-003/input.java`

```java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MissingReadOnlyService {
    @Transactional
    public String findName(long id) {
        return "name";
    }
}
```

**保存路径：** `tests/fixtures/TC-DAO-004/input.java`

```java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
public class RemoteInsideTransactionService {
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public void process() {
        restTemplate.getForObject("http://example.com", String.class);
    }
}
```

**保存路径：** `tests/fixtures/TC-DAO-005/input.java`

```java
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LoopQueryService {
    private final UserRepository userRepository;

    public LoopQueryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> findUsers(List<Long> ids) {
        List<User> users = new ArrayList<>();
        for (Long id : ids) {
            users.add(userRepository.findById(id).orElse(null));
        }
        return users;
    }

    interface UserRepository {
        java.util.Optional<User> findById(Long id);
    }

    static class User {
    }
}
```

**保存路径：** `tests/fixtures/TC-EXC-003/input.java`

```java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MissingRollbackService {
    @Transactional
    public void execute() throws Exception {
        throw new Exception("checked error");
    }
}
```

**保存路径：** `tests/fixtures/TC-RES-002/input.java`

```java
import java.sql.Connection;
import javax.sql.DataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnsafeResourceService {
    private final DataSource dataSource;

    public UnsafeResourceService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Transactional
    public void query() throws Exception {
        Connection connection = dataSource.getConnection();
        connection.createStatement().executeQuery("SELECT 1");
    }
}
```

## 4. 测试用例 JSON（7 个完整文件）

**保存路径：** `tests/cases/TC-DAO-001.json`

```json
{
  "id": "TC-DAO-001",
  "rule_id": "SRS-DAO-01-001",
  "category": "DAO",
  "subcategory": "01",
  "title": "@Transactional 标注 private 方法反例触发",
  "description": "检测 private 方法上的 @Transactional。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-DAO-001/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "BLOCKER",
    "message_contains": ["@Transactional", "private"],
    "location_hint": {
      "file": "tests/fixtures/TC-DAO-001/input.java",
      "line_start": 6,
      "line_end": 6,
      "symbol": "PrivateTransactionalService.createOrder"
    }
  },
  "positive_case": "examples/DAO/positive/SRS-DAO-01-001_positive.java",
  "negative_case": "examples/DAO/negative/SRS-DAO-01-001_negative.java",
  "tags": ["DAO", "01", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

**保存路径：** `tests/cases/TC-DAO-002.json`

```json
{
  "id": "TC-DAO-002",
  "rule_id": "SRS-DAO-02-001",
  "category": "DAO",
  "subcategory": "02",
  "title": "同类 this 自调用事务方法反例触发",
  "description": "检测同类 this 自调用导致事务失效。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-DAO-002/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "CRITICAL",
    "message_contains": ["this", "inner"],
    "location_hint": {
      "file": "tests/fixtures/TC-DAO-002/input.java",
      "line_start": 7,
      "line_end": 7,
      "symbol": "SelfInvocationService.outer"
    }
  },
  "positive_case": "examples/DAO/positive/SRS-DAO-02-001_positive.java",
  "negative_case": "examples/DAO/negative/SRS-DAO-02-001_negative.java",
  "tags": ["DAO", "02", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

**保存路径：** `tests/cases/TC-DAO-003.json`

```json
{
  "id": "TC-DAO-003",
  "rule_id": "SRS-DAO-03-001",
  "category": "DAO",
  "subcategory": "03",
  "title": "只读查询未设置 readOnly 反例触发",
  "description": "检测只读查询方法未设置 readOnly=true。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-DAO-003/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "MINOR",
    "message_contains": ["readOnly", "findName"],
    "location_hint": {
      "file": "tests/fixtures/TC-DAO-003/input.java",
      "line_start": 6,
      "line_end": 6,
      "symbol": "MissingReadOnlyService.findName"
    }
  },
  "positive_case": "examples/DAO/positive/SRS-DAO-03-001_positive.java",
  "negative_case": "examples/DAO/negative/SRS-DAO-03-001_negative.java",
  "tags": ["DAO", "03", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

**保存路径：** `tests/cases/TC-DAO-004.json`

```json
{
  "id": "TC-DAO-004",
  "rule_id": "SRS-DAO-04-001",
  "category": "DAO",
  "subcategory": "04",
  "title": "事务内远程调用反例触发",
  "description": "检测 @Transactional 方法内调用 RestTemplate。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-DAO-004/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "CRITICAL",
    "message_contains": ["@Transactional", "RestTemplate"],
    "location_hint": {
      "file": "tests/fixtures/TC-DAO-004/input.java",
      "line_start": 9,
      "line_end": 11,
      "symbol": "RemoteInsideTransactionService.process"
    }
  },
  "positive_case": "examples/DAO/positive/SRS-DAO-04-001_positive.java",
  "negative_case": "examples/DAO/negative/SRS-DAO-04-001_negative.java",
  "tags": ["DAO", "04", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

**保存路径：** `tests/cases/TC-DAO-005.json`

```json
{
  "id": "TC-DAO-005",
  "rule_id": "SRS-DAO-05-001",
  "category": "DAO",
  "subcategory": "05",
  "title": "循环内查询 N+1 反例触发",
  "description": "检测 for 循环内调用 repository.findById。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-DAO-005/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["for", "findById"],
    "location_hint": {
      "file": "tests/fixtures/TC-DAO-005/input.java",
      "line_start": 15,
      "line_end": 16,
      "symbol": "LoopQueryService.findUsers"
    }
  },
  "positive_case": "examples/DAO/positive/SRS-DAO-05-001_positive.java",
  "negative_case": "examples/DAO/negative/SRS-DAO-05-001_negative.java",
  "tags": ["DAO", "05", "fixture", "negative", "pending-detection-evaluation"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

**保存路径：** `tests/cases/TC-EXC-003.json`

```json
{
  "id": "TC-EXC-003",
  "rule_id": "SRS-EXC-03-001",
  "category": "EXC",
  "subcategory": "03",
  "title": "事务回滚异常类型配置缺失反例触发",
  "description": "检测未配置 rollbackFor 且抛受检异常。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-EXC-003/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "CRITICAL",
    "message_contains": ["rollbackFor", "Exception"],
    "location_hint": {
      "file": "tests/fixtures/TC-EXC-003/input.java",
      "line_start": 6,
      "line_end": 8,
      "symbol": "MissingRollbackService.execute"
    }
  },
  "positive_case": "examples/EXC/positive/SRS-EXC-03-001_positive.java",
  "negative_case": "examples/EXC/negative/SRS-EXC-03-001_negative.java",
  "tags": ["EXC", "03", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

**保存路径：** `tests/cases/TC-RES-002.json`

```json
{
  "id": "TC-RES-002",
  "rule_id": "SRS-RES-02-001",
  "category": "RES",
  "subcategory": "02",
  "title": "事务中手动连接未释放反例触发",
  "description": "检测事务方法内手动获取 Connection 未释放。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-RES-002/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "CRITICAL",
    "message_contains": ["Connection", "getConnection"],
    "location_hint": {
      "file": "tests/fixtures/TC-RES-002/input.java",
      "line_start": 16,
      "line_end": 17,
      "symbol": "UnsafeResourceService.query"
    }
  },
  "positive_case": "examples/RES/positive/SRS-RES-02-001_positive.java",
  "negative_case": "examples/RES/negative/SRS-RES-02-001_negative.java",
  "tags": ["RES", "02", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

## 5. 期望结果 JSON（7 个完整文件）

**保存路径：** `tests/expected/TC-DAO-001.json`

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-DAO-001",
  "rule_id": "SRS-DAO-01-001",
  "expected": {
    "triggered": true,
    "severity": "BLOCKER",
    "message_contains": ["@Transactional", "private"],
    "location_hint": {
      "file": "tests/fixtures/TC-DAO-001/input.java",
      "line_start": 6,
      "line_end": 6,
      "symbol": "PrivateTransactionalService.createOrder"
    }
  }
}
```

**保存路径：** `tests/expected/TC-DAO-002.json`

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-DAO-002",
  "rule_id": "SRS-DAO-02-001",
  "expected": {
    "triggered": true,
    "severity": "CRITICAL",
    "message_contains": ["this", "inner"],
    "location_hint": {
      "file": "tests/fixtures/TC-DAO-002/input.java",
      "line_start": 7,
      "line_end": 7,
      "symbol": "SelfInvocationService.outer"
    }
  }
}
```

**保存路径：** `tests/expected/TC-DAO-003.json`

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-DAO-003",
  "rule_id": "SRS-DAO-03-001",
  "expected": {
    "triggered": true,
    "severity": "MINOR",
    "message_contains": ["readOnly", "findName"],
    "location_hint": {
      "file": "tests/fixtures/TC-DAO-003/input.java",
      "line_start": 6,
      "line_end": 6,
      "symbol": "MissingReadOnlyService.findName"
    }
  }
}
```

**保存路径：** `tests/expected/TC-DAO-004.json`

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-DAO-004",
  "rule_id": "SRS-DAO-04-001",
  "expected": {
    "triggered": true,
    "severity": "CRITICAL",
    "message_contains": ["@Transactional", "RestTemplate"],
    "location_hint": {
      "file": "tests/fixtures/TC-DAO-004/input.java",
      "line_start": 9,
      "line_end": 11,
      "symbol": "RemoteInsideTransactionService.process"
    }
  }
}
```

**保存路径：** `tests/expected/TC-DAO-005.json`

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-DAO-005",
  "rule_id": "SRS-DAO-05-001",
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["for", "findById"],
    "location_hint": {
      "file": "tests/fixtures/TC-DAO-005/input.java",
      "line_start": 15,
      "line_end": 16,
      "symbol": "LoopQueryService.findUsers"
    }
  }
}
```

**保存路径：** `tests/expected/TC-EXC-003.json`

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-EXC-003",
  "rule_id": "SRS-EXC-03-001",
  "expected": {
    "triggered": true,
    "severity": "CRITICAL",
    "message_contains": ["rollbackFor", "Exception"],
    "location_hint": {
      "file": "tests/fixtures/TC-EXC-003/input.java",
      "line_start": 6,
      "line_end": 8,
      "symbol": "MissingRollbackService.execute"
    }
  }
}
```

**保存路径：** `tests/expected/TC-RES-002.json`

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-RES-002",
  "rule_id": "SRS-RES-02-001",
  "expected": {
    "triggered": true,
    "severity": "CRITICAL",
    "message_contains": ["Connection", "getConnection"],
    "location_hint": {
      "file": "tests/fixtures/TC-RES-002/input.java",
      "line_start": 16,
      "line_end": 17,
      "symbol": "UnsafeResourceService.query"
    }
  }
}
```

## 6. examples/index.json 新增条目（14 条，待 C8 合并）

本次新增 14 条，待 C8 阶段统一合并。C8 合并时按 id 升序、已有条目逐字保留。

```json
[
  {
    "id": "SRS-DAO-01-001_positive",
    "rule_id": "SRS-DAO-01-001",
    "category": "DAO",
    "subcategory": "01",
    "polarity": "positive",
    "file": "examples/DAO/positive/SRS-DAO-01-001_positive.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "public @Transactional 正例"
  },
  {
    "id": "SRS-DAO-01-001_negative",
    "rule_id": "SRS-DAO-01-001",
    "category": "DAO",
    "subcategory": "01",
    "polarity": "negative",
    "file": "examples/DAO/negative/SRS-DAO-01-001_negative.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "private @Transactional 反例"
  },
  {
    "id": "SRS-DAO-02-001_positive",
    "rule_id": "SRS-DAO-02-001",
    "category": "DAO",
    "subcategory": "02",
    "polarity": "positive",
    "file": "examples/DAO/positive/SRS-DAO-02-001_positive.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "注入自身代理调用正例"
  },
  {
    "id": "SRS-DAO-02-001_negative",
    "rule_id": "SRS-DAO-02-001",
    "category": "DAO",
    "subcategory": "02",
    "polarity": "negative",
    "file": "examples/DAO/negative/SRS-DAO-02-001_negative.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "同类 this 自调用反例"
  },
  {
    "id": "SRS-DAO-03-001_positive",
    "rule_id": "SRS-DAO-03-001",
    "category": "DAO",
    "subcategory": "03",
    "polarity": "positive",
    "file": "examples/DAO/positive/SRS-DAO-03-001_positive.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "只读方法设置 readOnly=true 正例"
  },
  {
    "id": "SRS-DAO-03-001_negative",
    "rule_id": "SRS-DAO-03-001",
    "category": "DAO",
    "subcategory": "03",
    "polarity": "negative",
    "file": "examples/DAO/negative/SRS-DAO-03-001_negative.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "只读查询未设置 readOnly 反例"
  },
  {
    "id": "SRS-DAO-04-001_positive",
    "rule_id": "SRS-DAO-04-001",
    "category": "DAO",
    "subcategory": "04",
    "polarity": "positive",
    "file": "examples/DAO/positive/SRS-DAO-04-001_positive.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "事务外执行远程调用正例"
  },
  {
    "id": "SRS-DAO-04-001_negative",
    "rule_id": "SRS-DAO-04-001",
    "category": "DAO",
    "subcategory": "04",
    "polarity": "negative",
    "file": "examples/DAO/negative/SRS-DAO-04-001_negative.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "事务内调用 RestTemplate 反例"
  },
  {
    "id": "SRS-DAO-05-001_positive",
    "rule_id": "SRS-DAO-05-001",
    "category": "DAO",
    "subcategory": "05",
    "polarity": "positive",
    "file": "examples/DAO/positive/SRS-DAO-05-001_positive.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "循环外批量查询正例"
  },
  {
    "id": "SRS-DAO-05-001_negative",
    "rule_id": "SRS-DAO-05-001",
    "category": "DAO",
    "subcategory": "05",
    "polarity": "negative",
    "file": "examples/DAO/negative/SRS-DAO-05-001_negative.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "循环内 findById 反例"
  },
  {
    "id": "SRS-EXC-03-001_positive",
    "rule_id": "SRS-EXC-03-001",
    "category": "EXC",
    "subcategory": "03",
    "polarity": "positive",
    "file": "examples/EXC/positive/SRS-EXC-03-001_positive.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "rollbackFor 匹配业务异常正例"
  },
  {
    "id": "SRS-EXC-03-001_negative",
    "rule_id": "SRS-EXC-03-001",
    "category": "EXC",
    "subcategory": "03",
    "polarity": "negative",
    "file": "examples/EXC/negative/SRS-EXC-03-001_negative.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "未配置 rollbackFor 抛受检异常反例"
  },
  {
    "id": "SRS-RES-02-001_positive",
    "rule_id": "SRS-RES-02-001",
    "category": "RES",
    "subcategory": "02",
    "polarity": "positive",
    "file": "examples/RES/positive/SRS-RES-02-001_positive.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "try-with-resources 释放连接正例"
  },
  {
    "id": "SRS-RES-02-001_negative",
    "rule_id": "SRS-RES-02-001",
    "category": "RES",
    "subcategory": "02",
    "polarity": "negative",
    "file": "examples/RES/negative/SRS-RES-02-001_negative.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "手动获取 Connection 未释放反例"
  }
]
```

## 7. tests/cases/index.json 新增条目（7 条，待 C8 合并）

本次新增 7 条，待 C8 阶段统一合并。C8 合并时按 id 升序、已有条目逐字保留。

```json
[
  {
    "id": "TC-DAO-001",
    "rule_id": "SRS-DAO-01-001",
    "category": "DAO",
    "file": "tests/cases/TC-DAO-001.json",
    "status": "ACTIVE",
    "version": "1.0.0",
    "title": "@Transactional 标注 private 方法反例触发",
    "tags": ["DAO", "01", "fixture", "negative"]
  },
  {
    "id": "TC-DAO-002",
    "rule_id": "SRS-DAO-02-001",
    "category": "DAO",
    "file": "tests/cases/TC-DAO-002.json",
    "status": "ACTIVE",
    "version": "1.0.0",
    "title": "同类 this 自调用事务方法反例触发",
    "tags": ["DAO", "02", "fixture", "negative"]
  },
  {
    "id": "TC-DAO-003",
    "rule_id": "SRS-DAO-03-001",
    "category": "DAO",
    "file": "tests/cases/TC-DAO-003.json",
    "status": "ACTIVE",
    "version": "1.0.0",
    "title": "只读查询未设置 readOnly 反例触发",
    "tags": ["DAO", "03", "fixture", "negative"]
  },
  {
    "id": "TC-DAO-004",
    "rule_id": "SRS-DAO-04-001",
    "category": "DAO",
    "file": "tests/cases/TC-DAO-004.json",
    "status": "ACTIVE",
    "version": "1.0.0",
    "title": "事务内远程调用反例触发",
    "tags": ["DAO", "04", "fixture", "negative"]
  },
  {
    "id": "TC-DAO-005",
    "rule_id": "SRS-DAO-05-001",
    "category": "DAO",
    "file": "tests/cases/TC-DAO-005.json",
    "status": "ACTIVE",
    "version": "1.0.0",
    "title": "循环内查询 N+1 反例触发",
    "tags": ["DAO", "05", "fixture", "negative", "pending-detection-evaluation"]
  },
  {
    "id": "TC-EXC-003",
    "rule_id": "SRS-EXC-03-001",
    "category": "EXC",
    "file": "tests/cases/TC-EXC-003.json",
    "status": "ACTIVE",
    "version": "1.0.0",
    "title": "事务回滚异常类型配置缺失反例触发",
    "tags": ["EXC", "03", "fixture", "negative"]
  },
  {
    "id": "TC-RES-002",
    "rule_id": "SRS-RES-02-001",
    "category": "RES",
    "file": "tests/cases/TC-RES-002.json",
    "status": "ACTIVE",
    "version": "1.0.0",
    "title": "事务中手动连接未释放反例触发",
    "tags": ["RES", "02", "fixture", "negative"]
  }
]
```

## 8. 试点结论

C4 已完成 DAO 5 条、EXC 1 条、RES 1 条共 7 条规则的正反例、夹具、测试用例、期望结果与索引新增片段。测试用例 JSON 顶层 13 字段，expected 为完整 object，positive_case/negative_case 为字符串路径；expected JSON 顶层 6 字段并使用 case_id。索引仅输出本次新增片段，待 C8 阶段统一合并。未修改规则文件与 Schema，未重复 C1～C3b 历史错误。