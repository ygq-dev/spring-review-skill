# docs/20-C7-TEST-STYLE-NULL-I18N规则测试用例.md

## 1. 说明

本文件为 C7 阶段产出，覆盖 B7 冻结的 TEST 2 条、STYLE 2 条、NULL 2 条、I18N 1 条规则。  
本次输出 14 个正反例、7 个夹具、7 个测试用例 JSON、7 个期望结果 JSON，以及 examples/index.json 与 tests/cases/index.json 的新增条目片段。  
索引合并统一留到 C8 阶段，本文件不修改已有索引。  
元数据统一使用：`version=1.0.0`、`status=ACTIVE`、`generated_at=2026-09-24T00:00:00Z`、`generated_by=manual/1.0.0`。

## 2. 正反例代码（7 对，共 14 个）

### examples/TEST/positive/SRS-TEST-01-001_positive.java

```java
package com.example.test;

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
public class UserRepositoryInMemoryTest {
    public void loadFromInMemoryDatabase() {
        // 使用内存数据库 H2，不连接真实 MySQL
    }
}
```

### examples/TEST/negative/SRS-TEST-01-001_negative.java

```java
package com.example.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
public class UserRepositoryRealDbTest {

    @Sql(scripts = "classpath:real-mysql-schema.sql")
    public void loadFromRealDatabase() {
        // 直接连接真实 MySQL 数据库
    }
}
```

### examples/TEST/positive/SRS-TEST-02-001_positive.java

```java
package com.example.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

@SpringBootTest
class ApplicationSmokeTest {
    void contextLoads() {}
}

@WebMvcTest
class UserControllerSliceTest {
    void testController() {}
}

@WebMvcTest
class OrderControllerSliceTest {
    void testOrder() {}
}
```

### examples/TEST/negative/SRS-TEST-02-001_negative.java

```java
package com.example.test;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class FirstFullContextTest {
    void testA() {}
}

@SpringBootTest
class SecondFullContextTest {
    void testB() {}
}

@SpringBootTest
class ThirdFullContextTest {
    void testC() {}
}
```

### examples/STYLE/positive/SRS-STYLE-01-001_positive.java

```java
package com.example.style;

public class UserService {
    public void getUser() {
        String userName = "test";
    }
}
```

### examples/STYLE/negative/SRS-STYLE-01-001_negative.java

```java
package com.example.style;

public class user_service {
    public void GetUser() {
        String UserName = "test";
    }
}
```

### examples/STYLE/positive/SRS-STYLE-02-001_positive.java

```java
package com.example.style;

public class SimpleService {
    public int add(int a, int b) {
        return a + b;
    }
}
```

### examples/STYLE/negative/SRS-STYLE-02-001_negative.java

```java
package com.example.style;

public class ComplexService {
    public void process(int[] items) {
        for (int i = 0; i < items.length; i++) {
            if (items[i] > 0) {
                for (int j = 0; j < items[i]; j++) {
                    if (j % 2 == 0) {
                        if (j > 10) {
                            System.out.println(j);
                        }
                    }
                }
            }
        }
    }
}
```

### examples/NULL/positive/SRS-NULL-01-001_positive.java

```java
package com.example.nullcheck;

import java.util.Optional;

public class UserService {
    public Optional<String> findUserName(Long id) {
        return Optional.empty();
    }
}
```

### examples/NULL/negative/SRS-NULL-01-001_negative.java

```java
package com.example.nullcheck;

import java.util.Optional;

public class UserService {
    private Optional<String> userName;

    public void setUserName(Optional<String> userName) {
        this.userName = userName;
    }
}
```

### examples/NULL/positive/SRS-NULL-02-001_positive.java

```java
package com.example.nullcheck;

import java.util.Collections;
import java.util.List;

public class OrderService {
    public List<String> findOrders() {
        return Collections.emptyList();
    }
}
```

### examples/NULL/negative/SRS-NULL-02-001_negative.java

```java
package com.example.nullcheck;

import java.util.List;

public class OrderService {
    public List<String> findOrders() {
        return null;
    }
}
```

### examples/I18N/positive/SRS-I18N-01-001_positive.java

```java
package com.example.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

public class LoginController {
    private final MessageSource messageSource;

    public LoginController(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String login(String username, String password) {
        if (username == null || password == null) {
            return messageSource.getMessage("login.failed", null, LocaleContextHolder.getLocale());
        }
        return messageSource.getMessage("login.success", null, LocaleContextHolder.getLocale());
    }
}
```

### examples/I18N/negative/SRS-I18N-01-001_negative.java

```java
package com.example.i18n;

public class LoginController {
    public String login(String username, String password) {
        if (username == null || password == null) {
            return "登录失败，请重试";
        }
        return "登录成功";
    }
}
```

## 3. 夹具（7 个）

### tests/fixtures/TC-TEST-001/input.java

```java
package com.example.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
public class UserRepositoryRealDbTest {

    @Sql(scripts = "classpath:real-mysql-schema.sql")
    public void loadFromRealDatabase() {
        // 直接连接真实 MySQL 数据库
    }
}
```

### tests/fixtures/TC-TEST-002/input.java

```java
package com.example.test;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class FirstFullContextTest {
    void testA() {}
}

@SpringBootTest
class SecondFullContextTest {
    void testB() {}
}

@SpringBootTest
class ThirdFullContextTest {
    void testC() {}
}
```

### tests/fixtures/TC-STYLE-001/input.java

```java
package com.example.style;

public class user_service {
    public void GetUser() {
        String UserName = "test";
    }
}
```

### tests/fixtures/TC-STYLE-002/input.java

```java
package com.example.style;

public class ComplexService {
    public void process(int[] items) {
        for (int i = 0; i < items.length; i++) {
            if (items[i] > 0) {
                for (int j = 0; j < items[i]; j++) {
                    if (j % 2 == 0) {
                        if (j > 10) {
                            System.out.println(j);
                        }
                    }
                }
            }
        }
    }
}
```

### tests/fixtures/TC-NULL-001/input.java

```java
package com.example.nullcheck;

import java.util.Optional;

public class UserService {
    private Optional<String> userName;

    public void setUserName(Optional<String> userName) {
        this.userName = userName;
    }
}
```

### tests/fixtures/TC-NULL-002/input.java

```java
package com.example.nullcheck;

import java.util.List;

public class OrderService {
    public List<String> findOrders() {
        return null;
    }
}
```

### tests/fixtures/TC-I18N-001/input.java

```java
package com.example.i18n;

public class LoginController {
    public String login(String username, String password) {
        if (username == null || password == null) {
            return "登录失败，请重试";
        }
        return "登录成功";
    }
}
```

## 4. 测试用例 JSON（7 个完整文件）

### tests/cases/TC-TEST-001.json

```json
{
  "id": "TC-TEST-001",
  "rule_id": "SRS-TEST-01-001",
  "category": "TEST",
  "subcategory": "01",
  "title": "测试依赖真实外部服务或数据库",
  "description": "检测 @SpringBootTest、@DataJpaTest 等测试类直接连接真实外部服务或数据库。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-TEST-001/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["@SpringBootTest", "真实数据库", "外部服务"],
    "location_hint": {
      "file": "tests/fixtures/TC-TEST-001/input.java",
      "line_start": 6,
      "line_end": 6,
      "symbol": "UserRepositoryRealDbTest"
    }
  },
  "positive_case": "examples/TEST/positive/SRS-TEST-01-001_positive.java",
  "negative_case": "examples/TEST/negative/SRS-TEST-01-001_negative.java",
  "tags": ["TEST", "01", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

### tests/cases/TC-TEST-002.json

```json
{
  "id": "TC-TEST-002",
  "rule_id": "SRS-TEST-02-001",
  "category": "TEST",
  "subcategory": "02",
  "title": "@SpringBootTest 全量上下文滥用",
  "description": "检测大量测试类使用 @SpringBootTest 加载全量上下文。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-TEST-002/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "MINOR",
    "message_contains": ["@SpringBootTest", "全量上下文", "滥用"],
    "location_hint": {
      "file": "tests/fixtures/TC-TEST-002/input.java",
      "line_start": 5,
      "line_end": 5,
      "symbol": "FirstFullContextTest"
    }
  },
  "positive_case": "examples/TEST/positive/SRS-TEST-02-001_positive.java",
  "negative_case": "examples/TEST/negative/SRS-TEST-02-001_negative.java",
  "tags": ["TEST", "02", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

### tests/cases/TC-STYLE-001.json

```json
{
  "id": "TC-STYLE-001",
  "rule_id": "SRS-STYLE-01-001",
  "category": "STYLE",
  "subcategory": "01",
  "title": "命名不符合 Java/Spring 约定",
  "description": "检测类、方法、变量、常量、包命名违规。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-STYLE-001/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "MINOR",
    "message_contains": ["类名", "方法名", "命名约定"],
    "location_hint": {
      "file": "tests/fixtures/TC-STYLE-001/input.java",
      "line_start": 3,
      "line_end": 3,
      "symbol": "user_service"
    }
  },
  "positive_case": "examples/STYLE/positive/SRS-STYLE-01-001_positive.java",
  "negative_case": "examples/STYLE/negative/SRS-STYLE-01-001_negative.java",
  "tags": ["STYLE", "01", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

### tests/cases/TC-STYLE-002.json

```json
{
  "id": "TC-STYLE-002",
  "rule_id": "SRS-STYLE-02-001",
  "category": "STYLE",
  "subcategory": "02",
  "title": "方法过长/复杂度过高",
  "description": "检测方法长度、圈复杂度、NPath 超阈值。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-STYLE-002/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "MINOR",
    "message_contains": ["方法长度", "圈复杂度", "嵌套"],
    "location_hint": {
      "file": "tests/fixtures/TC-STYLE-002/input.java",
      "line_start": 4,
      "line_end": 4,
      "symbol": "ComplexService.process"
    }
  },
  "positive_case": "examples/STYLE/positive/SRS-STYLE-02-001_positive.java",
  "negative_case": "examples/STYLE/negative/SRS-STYLE-02-001_negative.java",
  "tags": ["STYLE", "02", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

### tests/cases/TC-NULL-001.json

```json
{
  "id": "TC-NULL-001",
  "rule_id": "SRS-NULL-01-001",
  "category": "NULL",
  "subcategory": "01",
  "title": "Optional 作为字段/参数",
  "description": "检测 Optional 作为实体字段、方法参数、构造器参数。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-NULL-001/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "MINOR",
    "message_contains": ["Optional", "字段", "参数"],
    "location_hint": {
      "file": "tests/fixtures/TC-NULL-001/input.java",
      "line_start": 6,
      "line_end": 6,
      "symbol": "UserService.userName"
    }
  },
  "positive_case": "examples/NULL/positive/SRS-NULL-01-001_positive.java",
  "negative_case": "examples/NULL/negative/SRS-NULL-01-001_negative.java",
  "tags": ["NULL", "01", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

### tests/cases/TC-NULL-002.json

```json
{
  "id": "TC-NULL-002",
  "rule_id": "SRS-NULL-02-001",
  "category": "NULL",
  "subcategory": "02",
  "title": "可能返回 null 的集合/数组",
  "description": "检测方法返回 null 集合或 null 数组。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-NULL-002/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["返回 null", "集合", "数组"],
    "location_hint": {
      "file": "tests/fixtures/TC-NULL-002/input.java",
      "line_start": 6,
      "line_end": 6,
      "symbol": "OrderService.findOrders"
    }
  },
  "positive_case": "examples/NULL/positive/SRS-NULL-02-001_positive.java",
  "negative_case": "examples/NULL/negative/SRS-NULL-02-001_negative.java",
  "tags": ["NULL", "02", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

### tests/cases/TC-I18N-001.json

```json
{
  "id": "TC-I18N-001",
  "rule_id": "SRS-I18N-01-001",
  "category": "I18N",
  "subcategory": "01",
  "title": "硬编码文案未外部化",
  "description": "检测面向用户的中文提示、错误、标签文案硬编码。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-I18N-001/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "MINOR",
    "message_contains": ["硬编码", "文案", "未外部化"],
    "location_hint": {
      "file": "tests/fixtures/TC-I18N-001/input.java",
      "line_start": 6,
      "line_end": 6,
      "symbol": "LoginController.login"
    }
  },
  "positive_case": "examples/I18N/positive/SRS-I18N-01-001_positive.java",
  "negative_case": "examples/I18N/negative/SRS-I18N-01-001_negative.java",
  "tags": ["I18N", "01", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

## 5. 期望结果 JSON（7 个完整文件）

### tests/expected/TC-TEST-001.json

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-TEST-001",
  "rule_id": "SRS-TEST-01-001",
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["@SpringBootTest", "真实数据库", "外部服务"],
    "location_hint": {
      "file": "tests/fixtures/TC-TEST-001/input.java",
      "line_start": 6,
      "line_end": 6,
      "symbol": "UserRepositoryRealDbTest"
    }
  }
}
```

### tests/expected/TC-TEST-002.json

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-TEST-002",
  "rule_id": "SRS-TEST-02-001",
  "expected": {
    "triggered": true,
    "severity": "MINOR",
    "message_contains": ["@SpringBootTest", "全量上下文", "滥用"],
    "location_hint": {
      "file": "tests/fixtures/TC-TEST-002/input.java",
      "line_start": 5,
      "line_end": 5,
      "symbol": "FirstFullContextTest"
    }
  }
}
```

### tests/expected/TC-STYLE-001.json

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-STYLE-001",
  "rule_id": "SRS-STYLE-01-001",
  "expected": {
    "triggered": true,
    "severity": "MINOR",
    "message_contains": ["类名", "方法名", "命名约定"],
    "location_hint": {
      "file": "tests/fixtures/TC-STYLE-001/input.java",
      "line_start": 3,
      "line_end": 3,
      "symbol": "user_service"
    }
  }
}
```

### tests/expected/TC-STYLE-002.json

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-STYLE-002",
  "rule_id": "SRS-STYLE-02-001",
  "expected": {
    "triggered": true,
    "severity": "MINOR",
    "message_contains": ["方法长度", "圈复杂度", "嵌套"],
    "location_hint": {
      "file": "tests/fixtures/TC-STYLE-002/input.java",
      "line_start": 4,
      "line_end": 4,
      "symbol": "ComplexService.process"
    }
  }
}
```

### tests/expected/TC-NULL-001.json

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-NULL-001",
  "rule_id": "SRS-NULL-01-001",
  "expected": {
    "triggered": true,
    "severity": "MINOR",
    "message_contains": ["Optional", "字段", "参数"],
    "location_hint": {
      "file": "tests/fixtures/TC-NULL-001/input.java",
      "line_start": 6,
      "line_end": 6,
      "symbol": "UserService.userName"
    }
  }
}
```

### tests/expected/TC-NULL-002.json

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-NULL-002",
  "rule_id": "SRS-NULL-02-001",
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["返回 null", "集合", "数组"],
    "location_hint": {
      "file": "tests/fixtures/TC-NULL-002/input.java",
      "line_start": 6,
      "line_end": 6,
      "symbol": "OrderService.findOrders"
    }
  }
}
```

### tests/expected/TC-I18N-001.json

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-I18N-001",
  "rule_id": "SRS-I18N-01-001",
  "expected": {
    "triggered": true,
    "severity": "MINOR",
    "message_contains": ["硬编码", "文案", "未外部化"],
    "location_hint": {
      "file": "tests/fixtures/TC-I18N-001/input.java",
      "line_start": 6,
      "line_end": 6,
      "symbol": "LoginController.login"
    }
  }
}
```

## 6. examples/index.json 新增条目（14 条，待 C8 合并）

本次新增 14 条，待 C8 阶段统一合并。C8 合并时按 id 升序、已有条目逐字保留。

```json
[
  {
    "id": "SRS-TEST-01-001_positive",
    "rule_id": "SRS-TEST-01-001",
    "category": "TEST",
    "subcategory": "01",
    "polarity": "positive",
    "file": "examples/TEST/positive/SRS-TEST-01-001_positive.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "测试使用内存替身"
  },
  {
    "id": "SRS-TEST-01-001_negative",
    "rule_id": "SRS-TEST-01-001",
    "category": "TEST",
    "subcategory": "01",
    "polarity": "negative",
    "file": "examples/TEST/negative/SRS-TEST-01-001_negative.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "测试连接真实 MySQL"
  },
  {
    "id": "SRS-TEST-02-001_positive",
    "rule_id": "SRS-TEST-02-001",
    "category": "TEST",
    "subcategory": "02",
    "polarity": "positive",
    "file": "examples/TEST/positive/SRS-TEST-02-001_positive.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "少量全量上下文加切片测试"
  },
  {
    "id": "SRS-TEST-02-001_negative",
    "rule_id": "SRS-TEST-02-001",
    "category": "TEST",
    "subcategory": "02",
    "polarity": "negative",
    "file": "examples/TEST/negative/SRS-TEST-02-001_negative.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "所有测试使用全量上下文"
  },
  {
    "id": "SRS-STYLE-01-001_positive",
    "rule_id": "SRS-STYLE-01-001",
    "category": "STYLE",
    "subcategory": "01",
    "polarity": "positive",
    "file": "examples/STYLE/positive/SRS-STYLE-01-001_positive.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "符合 Java 命名约定"
  },
  {
    "id": "SRS-STYLE-01-001_negative",
    "rule_id": "SRS-STYLE-01-001",
    "category": "STYLE",
    "subcategory": "01",
    "polarity": "negative",
    "file": "examples/STYLE/negative/SRS-STYLE-01-001_negative.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "命名不符合约定"
  },
  {
    "id": "SRS-STYLE-02-001_positive",
    "rule_id": "SRS-STYLE-02-001",
    "category": "STYLE",
    "subcategory": "02",
    "polarity": "positive",
    "file": "examples/STYLE/positive/SRS-STYLE-02-001_positive.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "短小方法"
  },
  {
    "id": "SRS-STYLE-02-001_negative",
    "rule_id": "SRS-STYLE-02-001",
    "category": "STYLE",
    "subcategory": "02",
    "polarity": "negative",
    "file": "examples/STYLE/negative/SRS-STYLE-02-001_negative.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "巨型嵌套方法"
  },
  {
    "id": "SRS-NULL-01-001_positive",
    "rule_id": "SRS-NULL-01-001",
    "category": "NULL",
    "subcategory": "01",
    "polarity": "positive",
    "file": "examples/NULL/positive/SRS-NULL-01-001_positive.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "Optional 仅用于返回值"
  },
  {
    "id": "SRS-NULL-01-001_negative",
    "rule_id": "SRS-NULL-01-001",
    "category": "NULL",
    "subcategory": "01",
    "polarity": "negative",
    "file": "examples/NULL/negative/SRS-NULL-01-001_negative.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "Optional 作为字段和参数"
  },
  {
    "id": "SRS-NULL-02-001_positive",
    "rule_id": "SRS-NULL-02-001",
    "category": "NULL",
    "subcategory": "02",
    "polarity": "positive",
    "file": "examples/NULL/positive/SRS-NULL-02-001_positive.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "返回空集合"
  },
  {
    "id": "SRS-NULL-02-001_negative",
    "rule_id": "SRS-NULL-02-001",
    "category": "NULL",
    "subcategory": "02",
    "polarity": "negative",
    "file": "examples/NULL/negative/SRS-NULL-02-001_negative.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "返回 null 集合"
  },
  {
    "id": "SRS-I18N-01-001_positive",
    "rule_id": "SRS-I18N-01-001",
    "category": "I18N",
    "subcategory": "01",
    "polarity": "positive",
    "file": "examples/I18N/positive/SRS-I18N-01-001_positive.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "文案通过 MessageSource 读取"
  },
  {
    "id": "SRS-I18N-01-001_negative",
    "rule_id": "SRS-I18N-01-001",
    "category": "I18N",
    "subcategory": "01",
    "polarity": "negative",
    "file": "examples/I18N/negative/SRS-I18N-01-001_negative.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "硬编码中文文案"
  }
]
```

## 7. tests/cases/index.json 新增条目（7 条，待 C8 合并）

本次新增 7 条，待 C8 阶段统一合并。C8 合并时按 id 升序、已有条目逐字保留。

```json
[
  {
    "id": "TC-TEST-001",
    "rule_id": "SRS-TEST-01-001",
    "category": "TEST",
    "file": "tests/cases/TC-TEST-001.json",
    "status": "ACTIVE",
    "version": "1.0.0",
    "title": "测试依赖真实外部服务或数据库",
    "tags": ["TEST", "01", "fixture", "negative"]
  },
  {
    "id": "TC-TEST-002",
    "rule_id": "SRS-TEST-02-001",
    "category": "TEST",
    "file": "tests/cases/TC-TEST-002.json",
    "status": "ACTIVE",
    "version": "1.0.0",
    "title": "@SpringBootTest 全量上下文滥用",
    "tags": ["TEST", "02", "fixture", "negative"]
  },
  {
    "id": "TC-STYLE-001",
    "rule_id": "SRS-STYLE-01-001",
    "category": "STYLE",
    "file": "tests/cases/TC-STYLE-001.json",
    "status": "ACTIVE",
    "version": "1.0.0",
    "title": "命名不符合 Java/Spring 约定",
    "tags": ["STYLE", "01", "fixture", "negative"]
  },
  {
    "id": "TC-STYLE-002",
    "rule_id": "SRS-STYLE-02-001",
    "category": "STYLE",
    "file": "tests/cases/TC-STYLE-002.json",
    "status": "ACTIVE",
    "version": "1.0.0",
    "title": "方法过长/复杂度过高",
    "tags": ["STYLE", "02", "fixture", "negative"]
  },
  {
    "id": "TC-NULL-001",
    "rule_id": "SRS-NULL-01-001",
    "category": "NULL",
    "file": "tests/cases/TC-NULL-001.json",
    "status": "ACTIVE",
    "version": "1.0.0",
    "title": "Optional 作为字段/参数",
    "tags": ["NULL", "01", "fixture", "negative"]
  },
  {
    "id": "TC-NULL-002",
    "rule_id": "SRS-NULL-02-001",
    "category": "NULL",
    "file": "tests/cases/TC-NULL-002.json",
    "status": "ACTIVE",
    "version": "1.0.0",
    "title": "可能返回 null 的集合/数组",
    "tags": ["NULL", "02", "fixture", "negative"]
  },
  {
    "id": "TC-I18N-001",
    "rule_id": "SRS-I18N-01-001",
    "category": "I18N",
    "file": "tests/cases/TC-I18N-001.json",
    "status": "ACTIVE",
    "version": "1.0.0",
    "title": "硬编码文案未外部化",
    "tags": ["I18N", "01", "fixture", "negative"]
  }
]
```

## 8. 试点结论

本次 C7 已为 TEST 2 条、STYLE 2 条、NULL 2 条、I18N 1 条规则补齐正反例、夹具、测试用例、expected 与索引新增片段。  
测试用例 JSON 顶层保持 13 字段，expected 为完整 object，positive_case/negative_case 为字符串路径；expected JSON 顶层保持 6 字段并使用 case_id。  
message_contains 未重复 rule_id。索引仅输出新增条目片段，未合并已有索引。  
未复现 C1～C6 历史错误。待 C8 按 id 升序统一合并索引，后续执行验证规则命中与 expected 一致性。