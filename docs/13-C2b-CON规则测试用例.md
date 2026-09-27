# docs/13-C2b-CON规则测试用例.md

## 1. 说明

本文件为 C2b 阶段产出：为 B2 中 CON 分类的 5 条规则补齐正反例、夹具、测试用例、expected 与索引更新。严格遵循 C0、A4.2 冻结字段与统一元数据值。

---

## 2. 正反例代码

### SRS-CON-01-001 positive
保存路径：`examples/CON/positive/SRS-CON-01-001_positive.java`
```java
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateUtil {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String format(LocalDate date) {
        return FORMATTER.format(date);
    }
}
```

### SRS-CON-01-001 negative
保存路径：`examples/CON/negative/SRS-CON-01-001_negative.java`
```java
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtil {
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");

    public static String format(Date date) {
        return SDF.format(date);
    }
}
```

### SRS-CON-02-001 positive
保存路径：`examples/CON/positive/SRS-CON-02-001_positive.java`
```java
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class CacheService {
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public void put(String key, String value) {
        cache.put(key, value);
    }
}
```

### SRS-CON-02-001 negative
保存路径：`examples/CON/negative/SRS-CON-02-001_negative.java`
```java
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CacheService {
    private final Map<String, String> cache = new HashMap<>();

    public void put(String key, String value) {
        cache.put(key, value);
    }
}
```

### SRS-CON-03-001 positive
保存路径：`examples/CON/positive/SRS-CON-03-001_positive.java`
```java
public class Singleton {
    private static final Singleton INSTANCE = new Singleton();

    private Singleton() {
    }

    public static Singleton getInstance() {
        return INSTANCE;
    }
}
```

### SRS-CON-03-001 negative
保存路径：`examples/CON/negative/SRS-CON-03-001_negative.java`
```java
public class Singleton {
    private static Singleton instance;

    private Singleton() {
    }

    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```

### SRS-CON-04-001 positive
保存路径：`examples/CON/positive/SRS-CON-04-001_positive.java`
```java
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ExecutorConfig {
    public ThreadPoolExecutor executor() {
        return new ThreadPoolExecutor(
                2,
                4,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }
}
```

### SRS-CON-04-001 negative
保存路径：`examples/CON/negative/SRS-CON-04-001_negative.java`
```java
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ExecutorConfig {
    public ThreadPoolExecutor executor() {
        return new ThreadPoolExecutor(
                2,
                4,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>()
        );
    }
}
```

### SRS-CON-05-001 positive
保存路径：`examples/CON/positive/SRS-CON-05-001_positive.java`
```java
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class UserService {
    private final TransactionTemplate transactionTemplate;

    public UserService(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    public void asyncUpdate(Runnable task) {
        new Thread(() -> transactionTemplate.execute(status -> {
            task.run();
            return null;
        })).start();
    }
}
```

### SRS-CON-05-001 negative
保存路径：`examples/CON/negative/SRS-CON-05-001_negative.java`
```java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    @Transactional
    public void update() {
        new Thread(() -> {
            // repository.save(...)
        }).start();
    }
}
```

---

## 3. 夹具

### TC-CON-001
保存路径：`tests/fixtures/TC-CON-001/input.java`
```java
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtil {
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");

    public static String format(Date date) {
        return SDF.format(date);
    }
}
```

### TC-CON-002
保存路径：`tests/fixtures/TC-CON-002/input.java`
```java
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CacheService {
    private final Map<String, String> cache = new HashMap<>();

    public void put(String key, String value) {
        cache.put(key, value);
    }
}
```

### TC-CON-003
保存路径：`tests/fixtures/TC-CON-003/input.java`
```java
public class Singleton {
    private static Singleton instance;

    private Singleton() {
    }

    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```

### TC-CON-004
保存路径：`tests/fixtures/TC-CON-004/input.java`
```java
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ExecutorConfig {
    public ThreadPoolExecutor executor() {
        return new ThreadPoolExecutor(
                2,
                4,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>()
        );
    }
}
```

### TC-CON-005
保存路径：`tests/fixtures/TC-CON-005/input.java`
```java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    @Transactional
    public void update() {
        new Thread(() -> {
            // repository.save(...)
        }).start();
    }
}
```

---

## 4. 测试用例 JSON

### TC-CON-001
保存路径：`tests/cases/TC-CON-001.json`
```json
{
  "id": "TC-CON-001",
  "rule_id": "SRS-CON-01-001",
  "category": "CON",
  "subcategory": "01",
  "title": "SimpleDateFormat 静态共享",
  "description": "验证静态共享 SimpleDateFormat 被识别为并发风险。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-CON-001/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "BLOCKER",
    "message_contains": ["SimpleDateFormat", "线程", "静态"],
    "location_hint": {
      "file": "tests/fixtures/TC-CON-001/input.java",
      "line_start": 5,
      "line_end": 5,
      "symbol": "DateUtil.SDF"
    }
  },
  "positive_case": "examples/CON/positive/SRS-CON-01-001_positive.java",
  "negative_case": "examples/CON/negative/SRS-CON-01-001_negative.java",
  "tags": ["CON", "CON-01", "SimpleDateFormat", "thread-safety"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

### TC-CON-002
保存路径：`tests/cases/TC-CON-002.json`
```json
{
  "id": "TC-CON-002",
  "rule_id": "SRS-CON-02-001",
  "category": "CON",
  "subcategory": "02",
  "title": "非线程安全集合作为单例字段",
  "description": "验证单例 Bean 中可变 HashMap 字段被并发修改的风险。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-CON-002/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "CRITICAL",
    "message_contains": ["HashMap", "线程安全", "单例"],
    "location_hint": {
      "file": "tests/fixtures/TC-CON-002/input.java",
      "line_start": 7,
      "line_end": 7,
      "symbol": "CacheService.cache"
    }
  },
  "positive_case": "examples/CON/positive/SRS-CON-02-001_positive.java",
  "negative_case": "examples/CON/negative/SRS-CON-02-001_negative.java",
  "tags": ["CON", "CON-02", "HashMap", "thread-safety"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

### TC-CON-003
保存路径：`tests/cases/TC-CON-003.json`
```json
{
  "id": "TC-CON-003",
  "rule_id": "SRS-CON-03-001",
  "category": "CON",
  "subcategory": "03",
  "title": "未同步的懒初始化单例",
  "description": "验证双重检查锁未使用 volatile 的懒初始化单例风险。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-CON-003/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["双重检查锁", "volatile", "懒初始化"],
    "location_hint": {
      "file": "tests/fixtures/TC-CON-003/input.java",
      "line_start": 2,
      "line_end": 2,
      "symbol": "Singleton.instance"
    }
  },
  "positive_case": "examples/CON/positive/SRS-CON-03-001_positive.java",
  "negative_case": "examples/CON/negative/SRS-CON-03-001_negative.java",
  "tags": ["CON", "CON-03", "Singleton", "double-checked-locking"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

### TC-CON-004
保存路径：`tests/cases/TC-CON-004.json`
```json
{
  "id": "TC-CON-004",
  "rule_id": "SRS-CON-04-001",
  "category": "CON",
  "subcategory": "04",
  "title": "线程池未设置拒绝策略/队列无界",
  "description": "验证无界队列且未设置拒绝策略的线程池风险。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-CON-004/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["ThreadPoolExecutor", "拒绝策略", "无界队列"],
    "location_hint": {
      "file": "tests/fixtures/TC-CON-004/input.java",
      "line_start": 7,
      "line_end": 13,
      "symbol": "ExecutorConfig.executor"
    }
  },
  "positive_case": "examples/CON/positive/SRS-CON-04-001_positive.java",
  "negative_case": "examples/CON/negative/SRS-CON-04-001_negative.java",
  "tags": ["CON", "CON-04", "ThreadPoolExecutor", "rejection-policy"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

### TC-CON-005
保存路径：`tests/cases/TC-CON-005.json`
```json
{
  "id": "TC-CON-005",
  "rule_id": "SRS-CON-05-001",
  "category": "CON",
  "subcategory": "05",
  "title": "@Transactional 跨线程失效未处理",
  "description": "验证 @Transactional 方法内启动新线程导致事务失效的风险。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-CON-005/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "CRITICAL",
    "message_contains": ["@Transactional", "跨线程", "事务失效"],
    "location_hint": {
      "file": "tests/fixtures/TC-CON-005/input.java",
      "line_start": 6,
      "line_end": 10,
      "symbol": "UserService.update"
    }
  },
  "positive_case": "examples/CON/positive/SRS-CON-05-001_positive.java",
  "negative_case": "examples/CON/negative/SRS-CON-05-001_negative.java",
  "tags": ["CON", "CON-05", "Transactional", "cross-thread"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

---

## 5. 期望结果 JSON

### TC-CON-001
保存路径：`tests/expected/TC-CON-001.json`
```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-CON-001",
  "rule_id": "SRS-CON-01-001",
  "expected": {
    "triggered": true,
    "severity": "BLOCKER",
    "message_contains": ["SimpleDateFormat", "线程", "静态"],
    "location_hint": {
      "file": "tests/fixtures/TC-CON-001/input.java",
      "line_start": 5,
      "line_end": 5,
      "symbol": "DateUtil.SDF"
    }
  }
}
```

### TC-CON-002
保存路径：`tests/expected/TC-CON-002.json`
```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-CON-002",
  "rule_id": "SRS-CON-02-001",
  "expected": {
    "triggered": true,
    "severity": "CRITICAL",
    "message_contains": ["HashMap", "线程安全", "单例"],
    "location_hint": {
      "file": "tests/fixtures/TC-CON-002/input.java",
      "line_start": 7,
      "line_end": 7,
      "symbol": "CacheService.cache"
    }
  }
}
```

### TC-CON-003
保存路径：`tests/expected/TC-CON-003.json`
```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-CON-003",
  "rule_id": "SRS-CON-03-001",
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["双重检查锁", "volatile", "懒初始化"],
    "location_hint": {
      "file": "tests/fixtures/TC-CON-003/input.java",
      "line_start": 2,
      "line_end": 2,
      "symbol": "Singleton.instance"
    }
  }
}
```

### TC-CON-004
保存路径：`tests/expected/TC-CON-004.json`
```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-CON-004",
  "rule_id": "SRS-CON-04-001",
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["ThreadPoolExecutor", "拒绝策略", "无界队列"],
    "location_hint": {
      "file": "tests/fixtures/TC-CON-004/input.java",
      "line_start": 7,
      "line_end": 13,
      "symbol": "ExecutorConfig.executor"
    }
  }
}
```

### TC-CON-005
保存路径：`tests/expected/TC-CON-005.json`
```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-CON-005",
  "rule_id": "SRS-CON-05-001",
  "expected": {
    "triggered": true,
    "severity": "CRITICAL",
    "message_contains": ["@Transactional", "跨线程", "事务失效"],
    "location_hint": {
      "file": "tests/fixtures/TC-CON-005/input.java",
      "line_start": 6,
      "line_end": 10,
      "symbol": "UserService.update"
    }
  }
}
```

---

## 6. examples/index.json

保存路径：`examples/index.json`
```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "count": 30,
  "items": [
    {
      "id": "SRS-AR-01-001_negative",
      "rule_id": "SRS-AR-01-001",
      "category": "AR",
      "subcategory": "01",
      "polarity": "negative",
      "file": "examples/AR/negative/SRS-AR-01-001_negative.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "SRS-AR-01-001 反例"
    },
    {
      "id": "SRS-AR-01-001_positive",
      "rule_id": "SRS-AR-01-001",
      "category": "AR",
      "subcategory": "01",
      "polarity": "positive",
      "file": "examples/AR/positive/SRS-AR-01-001_positive.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "SRS-AR-01-001 正例"
    },
    {
      "id": "SRS-AR-02-001_negative",
      "rule_id": "SRS-AR-02-001",
      "category": "AR",
      "subcategory": "02",
      "polarity": "negative",
      "file": "examples/AR/negative/SRS-AR-02-001_negative.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "SRS-AR-02-001 反例"
    },
    {
      "id": "SRS-AR-02-001_positive",
      "rule_id": "SRS-AR-02-001",
      "category": "AR",
      "subcategory": "02",
      "polarity": "positive",
      "file": "examples/AR/positive/SRS-AR-02-001_positive.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "SRS-AR-02-001 正例"
    },
    {
      "id": "SRS-CF-01-001_negative",
      "rule_id": "SRS-CF-01-001",
      "category": "CF",
      "subcategory": "01",
      "polarity": "negative",
      "file": "examples/CF/negative/SRS-CF-01-001_negative.yml",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "yaml",
      "title": "SRS-CF-01-001 反例"
    },
    {
      "id": "SRS-CF-01-001_positive",
      "rule_id": "SRS-CF-01-001",
      "category": "CF",
      "subcategory": "01",
      "polarity": "positive",
      "file": "examples/CF/positive/SRS-CF-01-001_positive.yml",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "yaml",
      "title": "SRS-CF-01-001 正例"
    },
    {
      "id": "SRS-CON-01-001_negative",
      "rule_id": "SRS-CON-01-001",
      "category": "CON",
      "subcategory": "01",
      "polarity": "negative",
      "file": "examples/CON/negative/SRS-CON-01-001_negative.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "SimpleDateFormat 静态共享反例"
    },
    {
      "id": "SRS-CON-01-001_positive",
      "rule_id": "SRS-CON-01-001",
      "category": "CON",
      "subcategory": "01",
      "polarity": "positive",
      "file": "examples/CON/positive/SRS-CON-01-001_positive.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "SimpleDateFormat 静态共享正例"
    },
    {
      "id": "SRS-CON-02-001_negative",
      "rule_id": "SRS-CON-02-001",
      "category": "CON",
      "subcategory": "02",
      "polarity": "negative",
      "file": "examples/CON/negative/SRS-CON-02-001_negative.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "非线程安全集合作为单例字段反例"
    },
    {
      "id": "SRS-CON-02-001_positive",
      "rule_id": "SRS-CON-02-001",
      "category": "CON",
      "subcategory": "02",
      "polarity": "positive",
      "file": "examples/CON/positive/SRS-CON-02-001_positive.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "非线程安全集合作为单例字段正例"
    },
    {
      "id": "SRS-CON-03-001_negative",
      "rule_id": "SRS-CON-03-001",
      "category": "CON",
      "subcategory": "03",
      "polarity": "negative",
      "file": "examples/CON/negative/SRS-CON-03-001_negative.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "未同步的懒初始化单例反例"
    },
    {
      "id": "SRS-CON-03-001_positive",
      "rule_id": "SRS-CON-03-001",
      "category": "CON",
      "subcategory": "03",
      "polarity": "positive",
      "file": "examples/CON/positive/SRS-CON-03-001_positive.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "未同步的懒初始化单例正例"
    },
    {
      "id": "SRS-CON-04-001_negative",
      "rule_id": "SRS-CON-04-001",
      "category": "CON",
      "subcategory": "04",
      "polarity": "negative",
      "file": "examples/CON/negative/SRS-CON-04-001_negative.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "线程池未设置拒绝策略/队列无界反例"
    },
    {
      "id": "SRS-CON-04-001_positive",
      "rule_id": "SRS-CON-04-001",
      "category": "CON",
      "subcategory": "04",
      "polarity": "positive",
      "file": "examples/CON/positive/SRS-CON-04-001_positive.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "线程池未设置拒绝策略/队列无界正例"
    },
    {
      "id": "SRS-CON-05-001_negative",
      "rule_id": "SRS-CON-05-001",
      "category": "CON",
      "subcategory": "05",
      "polarity": "negative",
      "file": "examples/CON/negative/SRS-CON-05-001_negative.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "@Transactional 跨线程失效未处理反例"
    },
    {
      "id": "SRS-CON-05-001_positive",
      "rule_id": "SRS-CON-05-001",
      "category": "CON",
      "subcategory": "05",
      "polarity": "positive",
      "file": "examples/CON/positive/SRS-CON-05-001_positive.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "@Transactional 跨线程失效未处理正例"
    },
    {
      "id": "SRS-DI-01-001_negative",
      "rule_id": "SRS-DI-01-001",
      "category": "DI",
      "subcategory": "01",
      "polarity": "negative",
      "file": "examples/DI/negative/SRS-DI-01-001_negative.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "SRS-DI-01-001 反例"
    },
    {
      "id": "SRS-DI-01-001_positive",
      "rule_id": "SRS-DI-01-001",
      "category": "DI",
      "subcategory": "01",
      "polarity": "positive",
      "file": "examples/DI/positive/SRS-DI-01-001_positive.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "SRS-DI-01-001 正例"
    },
    {
      "id": "SRS-DI-02-001_negative",
      "rule_id": "SRS-DI-02-001",
      "category": "DI",
      "subcategory": "02",
      "polarity": "negative",
      "file": "examples/DI/negative/SRS-DI-02-001_negative.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "SRS-DI-02-001 反例"
    },
    {
      "id": "SRS-DI-02-001_positive",
      "rule_id": "SRS-DI-02-001",
      "category": "DI",
      "subcategory": "02",
      "polarity": "positive",
      "file": "examples/DI/positive/SRS-DI-02-001_positive.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "SRS-DI-02-001 正例"
    },
    {
      "id": "SRS-DI-03-001_negative",
      "rule_id": "SRS-DI-03-001",
      "category": "DI",
      "subcategory": "03",
      "polarity": "negative",
      "file": "examples/DI/negative/SRS-DI-03-001_negative.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "SRS-DI-03-001 反例"
    },
    {
      "id": "SRS-DI-03-001_positive",
      "rule_id": "SRS-DI-03-001",
      "category": "DI",
      "subcategory": "03",
      "polarity": "positive",
      "file": "examples/DI/positive/SRS-DI-03-001_positive.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "SRS-DI-03-001 正例"
    },
    {
      "id": "SRS-DI-04-001_negative",
      "rule_id": "SRS-DI-04-001",
      "category": "DI",
      "subcategory": "04",
      "polarity": "negative",
      "file": "examples/DI/negative/SRS-DI-04-001_negative.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "SRS-DI-04-001 反例"
    },
    {
      "id": "SRS-DI-04-001_positive",
      "rule_id": "SRS-DI-04-001",
      "category": "DI",
      "subcategory": "04",
      "polarity": "positive",
      "file": "examples/DI/positive/SRS-DI-04-001_positive.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "SRS-DI-04-001 正例"
    },
    {
      "id": "SRS-DI-05-001_negative",
      "rule_id": "SRS-DI-05-001",
      "category": "DI",
      "subcategory": "05",
      "polarity": "negative",
      "file": "examples/DI/negative/SRS-DI-05-001_negative.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "SRS-DI-05-001 反例"
    },
    {
      "id": "SRS-DI-05-001_positive",
      "rule_id": "SRS-DI-05-001",
      "category": "DI",
      "subcategory": "05",
      "polarity": "positive",
      "file": "examples/DI/positive/SRS-DI-05-001_positive.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "SRS-DI-05-001 正例"
    },
    {
      "id": "SRS-DI-06-001_negative",
      "rule_id": "SRS-DI-06-001",
      "category": "DI",
      "subcategory": "06",
      "polarity": "negative",
      "file": "examples/DI/negative/SRS-DI-06-001_negative.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "SRS-DI-06-001 反例"
    },
    {
      "id": "SRS-DI-06-001_positive",
      "rule_id": "SRS-DI-06-001",
      "category": "DI",
      "subcategory": "06",
      "polarity": "positive",
      "file": "examples/DI/positive/SRS-DI-06-001_positive.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "SRS-DI-06-001 正例"
    },
    {
      "id": "SRS-DI-07-001_negative",
      "rule_id": "SRS-DI-07-001",
      "category": "DI",
      "subcategory": "07",
      "polarity": "negative",
      "file": "examples/DI/negative/SRS-DI-07-001_negative.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "SRS-DI-07-001 反例"
    },
    {
      "id": "SRS-DI-07-001_positive",
      "rule_id": "SRS-DI-07-001",
      "category": "DI",
      "subcategory": "07",
      "polarity": "positive",
      "file": "examples/DI/positive/SRS-DI-07-001_positive.java",
      "status": "ACTIVE",
      "version": "1.0.0",
      "language": "java",
      "title": "SRS-DI-07-001 正例"
    }
  ]
}
```

---

## 7. tests/cases/index.json

保存路径：`tests/cases/index.json`
```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "count": 15,
  "items": [
    {
      "id": "TC-AR-001",
      "rule_id": "SRS-AR-01-001",
      "category": "AR",
      "file": "tests/cases/TC-AR-001.json",
      "status": "ACTIVE",
      "version": "1.0.0",
      "title": "Controller 直接依赖 Repository 应触发分层依赖倒置",
      "tags": ["architecture", "layering", "archunit"]
    },
    {
      "id": "TC-AR-002",
      "rule_id": "SRS-AR-02-001",
      "category": "AR",
      "file": "tests/cases/TC-AR-002.json",
      "status": "ACTIVE",
      "version": "1.0.0",
      "title": "domain 依赖 web/jdbc 应触发领域层依赖外层",
      "tags": ["architecture", "layering", "archunit"]
    },
    {
      "id": "TC-CF-001",
      "rule_id": "SRS-CF-01-001",
      "category": "CF",
      "file": "tests/cases/TC-CF-001.json",
      "status": "ACTIVE",
      "version": "1.0.0",
      "title": "生产配置启用 show-sql/devtools/debug 应触发配置风险",
      "tags": ["configuration", "prod", "debug"]
    },
    {
      "id": "TC-CON-001",
      "rule_id": "SRS-CON-01-001",
      "category": "CON",
      "file": "tests/cases/TC-CON-001.json",
      "status": "ACTIVE",
      "version": "1.0.0",
      "title": "SimpleDateFormat 静态共享",
      "tags": ["CON", "CON-01", "SimpleDateFormat", "thread-safety"]
    },
    {
      "id": "TC-CON-002",
      "rule_id": "SRS-CON-02-001",
      "category": "CON",
      "file": "tests/cases/TC-CON-002.json",
      "status": "ACTIVE",
      "version": "1.0.0",
      "title": "非线程安全集合作为单例字段",
      "tags": ["CON", "CON-02", "HashMap", "thread-safety"]
    },
    {
      "id": "TC-CON-003",
      "rule_id": "SRS-CON-03-001",
      "category": "CON",
      "file": "tests/cases/TC-CON-003.json",
      "status": "ACTIVE",
      "version": "1.0.0",
      "title": "未同步的懒初始化单例",
      "tags": ["CON", "CON-03", "Singleton", "double-checked-locking"]
    },
    {
      "id": "TC-CON-004",
      "rule_id": "SRS-CON-04-001",
      "category": "CON",
      "file": "tests/cases/TC-CON-004.json",
      "status": "ACTIVE",
      "version": "1.0.0",
      "title": "线程池未设置拒绝策略/队列无界",
      "tags": ["CON", "CON-04", "ThreadPoolExecutor", "rejection-policy"]
    },
    {
      "id": "TC-CON-005",
      "rule_id": "SRS-CON-05-001",
      "category": "CON",
      "file": "tests/cases/TC-CON-005.json",
      "status": "ACTIVE",
      "version": "1.0.0",
      "title": "@Transactional 跨线程失效未处理",
      "tags": ["CON", "CON-05", "Transactional", "cross-thread"]
    },
    {
      "id": "TC-DI-001",
      "rule_id": "SRS-DI-01-001",
      "category": "DI",
      "file": "tests/cases/TC-DI-001.json",
      "status": "ACTIVE",
      "version": "1.0.0",
      "title": "禁止字段注入-反例触发",
      "tags": ["DI", "01", "fixture", "negative"]
    },
    {
      "id": "TC-DI-002",
      "rule_id": "SRS-DI-02-001",
      "category": "DI",
      "file": "tests/cases/TC-DI-002.json",
      "status": "ACTIVE",
      "version": "1.0.0",
      "title": "必需依赖应为构造器注入且 final-反例触发",
      "tags": ["DI", "02", "fixture", "negative"]
    },
    {
      "id": "TC-DI-003",
      "rule_id": "SRS-DI-03-001",
      "category": "DI",
      "file": "tests/cases/TC-DI-003.json",
      "status": "ACTIVE",
      "version": "1.0.0",
      "title": "检测构造器/字段循环依赖-反例触发",
      "tags": ["DI", "03", "fixture", "negative"]
    },
    {
      "id": "TC-DI-004",
      "rule_id": "SRS-DI-04-001",
      "category": "DI",
      "file": "tests/cases/TC-DI-004.json",
      "status": "ACTIVE",
      "version": "1.0.0",
      "title": "单例 Bean 中非线程安全可变状态-反例触发",
      "tags": ["DI", "04", "fixture", "negative"]
    },
    {
      "id": "TC-DI-005",
      "rule_id": "SRS-DI-05-001",
      "category": "DI",
      "file": "tests/cases/TC-DI-005.json",
      "status": "ACTIVE",
      "version": "1.0.0",
      "title": "@Scope 与注入点生命周期不匹配-反例触发",
      "tags": ["DI", "05", "fixture", "negative"]
    },
    {
      "id": "TC-DI-006",
      "rule_id": "SRS-DI-06-001",
      "category": "DI",
      "file": "tests/cases/TC-DI-006.json",
      "status": "ACTIVE",
      "version": "1.0.0",
      "title": "多个 @Aspect 未指定 @Order-反例触发",
      "tags": ["DI", "06", "fixture", "negative"]
    },
    {
      "id": "TC-DI-007",
      "rule_id": "SRS-DI-07-001",
      "category": "DI",
      "file": "tests/cases/TC-DI-007.json",
      "status": "ACTIVE",
      "version": "1.0.0",
      "title": "@Around 未调用 proceed()-反例触发",
      "tags": ["DI", "07", "fixture", "negative"]
    }
  ]
}
```

---

## 8. 试点结论

C2b 已完成 CON 分类 5 条规则的正反例、夹具、测试用例、expected 与索引覆盖保存。字段遵循 C0、A4.2 冻结结构，统一元数据值已使用，未重复 C1、C2a 已列错误。