# docs/11-C1-AR-CF试点用例.md

## 1. 说明

- 范围：C1 试点，覆盖 `SRS-AR-01-001`、`SRS-AR-02-001`、`SRS-CF-01-001`。
- 产出：正例、反例、夹具、测试用例、期望结果、索引。
- 约束：只读审查资产；不改 A5/A6 Schema；不改 B1～B7 规则文件。
- `status`：`ACTIVE`；`severity`：大写枚举。

## 2. 正例与反例代码

保存路径：`examples/AR/positive/SRS-AR-01-001_positive.java`
```java
package com.example.order.controller;

import com.example.order.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/orders/{id}")
    public String getOrder(@PathVariable Long id) {
        return orderService.getOrder(id);
    }
}
```

保存路径：`examples/AR/negative/SRS-AR-01-001_negative.java`
```java
package com.example.order.controller;

import com.example.order.repository.OrderRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {
    private final OrderRepository orderRepository;

    public OrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @GetMapping("/orders/{id}")
    public Object getOrder(@PathVariable Long id) {
        return orderRepository.findById(id);
    }
}
```

保存路径：`examples/AR/positive/SRS-AR-02-001_positive.java`
```java
package com.example.order.domain;

public class Order {
    private final Long id;
    private final String status;

    public Order(Long id, String status) {
        this.id = id;
        this.status = status;
    }

    public Long id() {
        return id;
    }

    public String status() {
        return status;
    }
}
```

保存路径：`examples/AR/negative/SRS-AR-02-001_negative.java`
```java
package com.example.order.domain;

import com.example.order.infrastructure.persistence.JdbcOrderRepository;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderDomainService {
    private final JdbcOrderRepository repository;

    public OrderDomainService(JdbcOrderRepository repository) {
        this.repository = repository;
    }

    public Object load(Long id) {
        return repository.findById(id);
    }
}
```

保存路径：`examples/CF/positive/SRS-CF-01-001_positive.yml`
```yaml
spring:
  datasource:
    url: jdbc:mysql://prod-db:3306/app
    username: app
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
server:
  port: 8080
logging:
  level:
    root: INFO
```

保存路径：`examples/CF/negative/SRS-CF-01-001_negative.yml`
```yaml
spring:
  datasource:
    url: jdbc:mysql://prod-db:3306/app
    username: app
    password: ${DB_PASSWORD}
  jpa:
    show-sql: true
  devtools:
    restart:
      enabled: true
debug: true
logging:
  level:
    root: DEBUG
```

## 3. 夹具文件内容

保存路径：`tests/fixtures/TC-AR-001/input.java`
```java
package com.example.order.controller;

import com.example.order.repository.OrderRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {
    private final OrderRepository orderRepository;

    public OrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @GetMapping("/orders/{id}")
    public Object getOrder(@PathVariable Long id) {
        return orderRepository.findById(id);
    }
}
```

保存路径：`tests/fixtures/TC-AR-002/input.java`
```java
package com.example.order.domain;

import com.example.order.infrastructure.persistence.JdbcOrderRepository;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderDomainService {
    private final JdbcOrderRepository repository;

    public OrderDomainService(JdbcOrderRepository repository) {
        this.repository = repository;
    }

    public Object load(Long id) {
        return repository.findById(id);
    }
}
```

保存路径：`tests/fixtures/TC-CF-001/input.yml`
```yaml
spring:
  datasource:
    url: jdbc:mysql://prod-db:3306/app
    username: app
    password: ${DB_PASSWORD}
  jpa:
    show-sql: true
  devtools:
    restart:
      enabled: true
debug: true
logging:
  level:
    root: DEBUG
```

## 4. 测试用例 JSON

保存路径：`tests/cases/TC-AR-001.json`
```json
{
  "id": "TC-AR-001",
  "rule_id": "SRS-AR-01-001",
  "category": "AR",
  "subcategory": "01",
  "title": "Controller 直接依赖 Repository 应触发分层依赖倒置",
  "description": "验证 Controller 直接注入并调用 Repository 时命中目标规则。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-AR-001/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "CRITICAL",
    "message_contains": ["Controller", "Repository"],
    "location_hint": {
      "file": "tests/fixtures/TC-AR-001/input.java",
      "line_start": 11,
      "line_end": 11,
      "symbol": "com.example.order.controller.OrderController"
    }
  },
  "positive_case": "examples/AR/positive/SRS-AR-01-001_positive.java",
  "negative_case": "examples/AR/negative/SRS-AR-01-001_negative.java",
  "tags": ["architecture", "layering", "archunit"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

保存路径：`tests/cases/TC-AR-002.json`
```json
{
  "id": "TC-AR-002",
  "rule_id": "SRS-AR-02-001",
  "category": "AR",
  "subcategory": "02",
  "title": "domain 依赖 web/jdbc 应触发领域层依赖外层",
  "description": "验证 domain 包类 import web 注解或基础设施实现时命中目标规则。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-AR-002/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["domain", "web", "jdbc"],
    "location_hint": {
      "file": "tests/fixtures/TC-AR-002/input.java",
      "line_start": 7,
      "line_end": 7,
      "symbol": "com.example.order.domain.OrderDomainService"
    }
  },
  "positive_case": "examples/AR/positive/SRS-AR-02-001_positive.java",
  "negative_case": "examples/AR/negative/SRS-AR-02-001_negative.java",
  "tags": ["architecture", "layering", "archunit"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

保存路径：`tests/cases/TC-CF-001.json`
```json
{
  "id": "TC-CF-001",
  "rule_id": "SRS-CF-01-001",
  "category": "CF",
  "subcategory": "01",
  "title": "生产配置启用 show-sql/devtools/debug 应触发配置风险",
  "description": "验证 application-prod 配置中包含调试开关时命中目标规则。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-CF-001/input.yml",
    "content": null,
    "language": "yaml",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "CRITICAL",
    "message_contains": ["show-sql", "devtools", "debug"],
    "location_hint": {
      "file": "tests/fixtures/TC-CF-001/input.yml",
      "line_start": 9,
      "line_end": 9,
      "symbol": "spring.jpa.show-sql"
    }
  },
  "positive_case": "examples/CF/positive/SRS-CF-01-001_positive.yml",
  "negative_case": "examples/CF/negative/SRS-CF-01-001_negative.yml",
  "tags": ["configuration", "prod", "debug"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

## 5. 期望结果 JSON

保存路径：`tests/expected/TC-AR-001.json`
```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-AR-001",
  "rule_id": "SRS-AR-01-001",
  "expected": {
    "triggered": true,
    "severity": "CRITICAL",
    "message_contains": ["Controller", "Repository"],
    "location_hint": {
      "file": "tests/fixtures/TC-AR-001/input.java",
      "line_start": 11,
      "line_end": 11,
      "symbol": "com.example.order.controller.OrderController"
    }
  }
}
```

保存路径：`tests/expected/TC-AR-002.json`
```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-AR-002",
  "rule_id": "SRS-AR-02-001",
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["domain", "web", "jdbc"],
    "location_hint": {
      "file": "tests/fixtures/TC-AR-002/input.java",
      "line_start": 7,
      "line_end": 7,
      "symbol": "com.example.order.domain.OrderDomainService"
    }
  }
}
```

保存路径：`tests/expected/TC-CF-001.json`
```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-CF-001",
  "rule_id": "SRS-CF-01-001",
  "expected": {
    "triggered": true,
    "severity": "CRITICAL",
    "message_contains": ["show-sql", "devtools", "debug"],
    "location_hint": {
      "file": "tests/fixtures/TC-CF-001/input.yml",
      "line_start": 9,
      "line_end": 9,
      "symbol": "spring.jpa.show-sql"
    }
  }
}
```

## 6. examples/index.json

保存路径：`examples/index.json`
```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "count": 6,
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
    }
  ]
}
```

## 7. tests/cases/index.json

保存路径：`tests/cases/index.json`
```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "count": 3,
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
    }
  ]
}
```

## 8. 试点验证结论

- C0 结构可用：`examples`、`fixtures`、`cases`、`expected` 分层能覆盖 AR/CF 与 ARCH_TEST/RULE_ENGINE 两类检测。
- C0 JSON 字段可覆盖输入、期望、正反例引用、标签、版本、状态。
- `expected.triggered=true` 时，`severity`、`message_contains`、`location_hint` 约束可执行。
- 索引 `items` 可按 `id` 升序排列，`count` 与 `items.length` 可机械校验。
- 无需调整 A5、A6、B1～B7；C0 结构在本试点中无需调整。