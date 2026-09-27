# docs/12-C2a-DI规则测试用例.md

## 1. 说明

- 阶段：C2a，为 B2 中 DI 分类 7 条规则编写正反例、夹具、测试用例、expected、索引。
- 范围：Java 17+/Spring Boot 3.x，只读审查，不修改代码、不提交、不训练模型。
- 统一元数据：`version` 为 `1.0.0`，`generated_at` 为 `2026-09-24T00:00:00Z`，`generated_by` 为 `manual/1.0.0`。
- 严格遵循 C0 结构约束 12 条与 A4.2 索引字段。
- C1 已有条目以 `docs/11-C1-AR-CF试点用例.md` 冻结内容为准；本文件按合并视图列出完整索引。
- 本次新增 7 条 DI 规则测试资产，正例不命中目标规则，反例命中目标规则。

## 2. 正反例代码

### examples/DI/positive/SRS-DI-01-001_positive.java

```java
package com.example.di;

import org.springframework.stereotype.Service;

@Service
class Di01PositiveService {
    private final Dependency dependency;

    public Di01PositiveService(Dependency dependency) {
        this.dependency = dependency;
    }

    static class Dependency {}
}
```

### examples/DI/negative/SRS-DI-01-001_negative.java

```java
package com.example.di;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
class Di01NegativeService {
    @Autowired
    private Dependency dependency;

    static class Dependency {}
}
```

### examples/DI/positive/SRS-DI-02-001_positive.java

```java
package com.example.di;

import org.springframework.stereotype.Service;

@Service
class Di02PositiveService {
    private final Dependency dependency;

    public Di02PositiveService(Dependency dependency) {
        this.dependency = dependency;
    }

    static class Dependency {}
}
```

### examples/DI/negative/SRS-DI-02-001_negative.java

```java
package com.example.di;

import org.springframework.stereotype.Service;

@Service
class Di02NegativeService {
    private Dependency dependency;

    public Di02NegativeService(Dependency dependency) {
        this.dependency = dependency;
    }

    static class Dependency {}
}
```

### examples/DI/positive/SRS-DI-03-001_positive.java

```java
package com.example.di;

import org.springframework.stereotype.Service;

@Service
class Di03ServiceA {
    private final Di03ServiceB b;

    public Di03ServiceA(Di03ServiceB b) {
        this.b = b;
    }
}

@Service
class Di03ServiceB {
    private final Di03ServiceC c;

    public Di03ServiceB(Di03ServiceC c) {
        this.c = c;
    }
}

@Service
class Di03ServiceC {
}
```

### examples/DI/negative/SRS-DI-03-001_negative.java

```java
package com.example.di;

import org.springframework.stereotype.Service;

@Service
class Di03BeanA {
    private final Di03BeanB b;

    public Di03BeanA(Di03BeanB b) {
        this.b = b;
    }
}

@Service
class Di03BeanB {
    private final Di03BeanA a;

    public Di03BeanB(Di03BeanA a) {
        this.a = a;
    }
}
```

### examples/DI/positive/SRS-DI-04-001_positive.java

```java
package com.example.di;

import org.springframework.stereotype.Service;

@Service
class Di04PositiveService {
    public void execute() {
    }
}
```

### examples/DI/negative/SRS-DI-04-001_negative.java

```java
package com.example.di;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
class Di04NegativeService {
    private final Map<String, String> cache = new HashMap<>();

    public void put(String key, String value) {
        cache.put(key, value);
    }
}
```

### examples/DI/positive/SRS-DI-05-001_positive.java

```java
package com.example.di;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
@Scope("prototype")
class Di05PrototypeBean {
}

@Service
class Di05PositiveService {
    private final ObjectProvider<Di05PrototypeBean> provider;

    public Di05PositiveService(ObjectProvider<Di05PrototypeBean> provider) {
        this.provider = provider;
    }
}
```

### examples/DI/negative/SRS-DI-05-001_negative.java

```java
package com.example.di;

import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

@RequestScope
class Di05RequestBean {
}

@Service
class Di05NegativeService {
    private final Di05RequestBean requestBean;

    public Di05NegativeService(Di05RequestBean requestBean) {
        this.requestBean = requestBean;
    }
}
```

### examples/DI/positive/SRS-DI-06-001_positive.java

```java
package com.example.di;

import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;

@Aspect
@Order(1)
class Di06AspectA {
}

@Aspect
@Order(2)
class Di06AspectB {
}
```

### examples/DI/negative/SRS-DI-06-001_negative.java

```java
package com.example.di;

import org.aspectj.lang.annotation.Aspect;

@Aspect
class Di06AspectA {
}

@Aspect
class Di06AspectB {
}
```

### examples/DI/positive/SRS-DI-07-001_positive.java

```java
package com.example.di;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
class Di07PositiveAspect {
    @Around("execution(* com.example.di..*(..))")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        return pjp.proceed();
    }
}
```

### examples/DI/negative/SRS-DI-07-001_negative.java

```java
package com.example.di;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
class Di07NegativeAspect {
    @Around("execution(* com.example.di..*(..))")
    public Object around(ProceedingJoinPoint pjp) {
        return null;
    }
}
```

## 3. 夹具

### tests/fixtures/TC-DI-001/input.java

```java
package com.example.di;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Di01NegativeService {
    @Autowired
    private Dependency dependency;

    static class Dependency {}
}
```

### tests/fixtures/TC-DI-002/input.java

```java
package com.example.di;

import org.springframework.stereotype.Service;

@Service
public class Di02NegativeService {
    private Dependency dependency;

    public Di02NegativeService(Dependency dependency) {
        this.dependency = dependency;
    }

    static class Dependency {}
}
```

### tests/fixtures/TC-DI-003/input.java

```java
package com.example.di;

import org.springframework.stereotype.Service;

@Service
class Di03BeanA {
    private final Di03BeanB b;

    public Di03BeanA(Di03BeanB b) {
        this.b = b;
    }
}

@Service
class Di03BeanB {
    private final Di03BeanA a;

    public Di03BeanB(Di03BeanA a) {
        this.a = a;
    }
}
```

### tests/fixtures/TC-DI-004/input.java

```java
package com.example.di;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class Di04NegativeService {
    private final Map<String, String> cache = new HashMap<>();

    public void put(String key, String value) {
        cache.put(key, value);
    }
}
```

### tests/fixtures/TC-DI-005/input.java

```java
package com.example.di;

import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

@RequestScope
class Di05RequestBean {
}

@Service
public class Di05NegativeService {
    private final Di05RequestBean requestBean;

    public Di05NegativeService(Di05RequestBean requestBean) {
        this.requestBean = requestBean;
    }
}
```

### tests/fixtures/TC-DI-006/input.java

```java
package com.example.di;

import org.aspectj.lang.annotation.Aspect;

@Aspect
class Di06AspectA {
}

@Aspect
class Di06AspectB {
}
```

### tests/fixtures/TC-DI-007/input.java

```java
package com.example.di;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class Di07NegativeAspect {
    @Around("execution(* com.example.di..*(..))")
    public Object around(ProceedingJoinPoint pjp) {
        return null;
    }
}
```

## 4. 测试用例 JSON

### tests/cases/TC-DI-001.json

```json
{
  "id": "TC-DI-001",
  "rule_id": "SRS-DI-01-001",
  "category": "DI",
  "subcategory": "01",
  "title": "禁止字段注入-反例触发",
  "description": "验证 @Autowired 字段注入时命中目标规则。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-DI-001/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["@Autowired", "字段注入"],
    "location_hint": {
      "file": "tests/fixtures/TC-DI-001/input.java",
      "line_start": 8,
      "line_end": 9,
      "symbol": "Di01NegativeService.dependency"
    }
  },
  "positive_case": "examples/DI/positive/SRS-DI-01-001_positive.java",
  "negative_case": "examples/DI/negative/SRS-DI-01-001_negative.java",
  "tags": ["DI", "01", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

### tests/cases/TC-DI-002.json

```json
{
  "id": "TC-DI-002",
  "rule_id": "SRS-DI-02-001",
  "category": "DI",
  "subcategory": "02",
  "title": "必需依赖应为构造器注入且 final-反例触发",
  "description": "验证必需依赖非 final 时命中目标规则。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-DI-002/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["final", "必需依赖"],
    "location_hint": {
      "file": "tests/fixtures/TC-DI-002/input.java",
      "line_start": 7,
      "line_end": 7,
      "symbol": "Di02NegativeService.dependency"
    }
  },
  "positive_case": "examples/DI/positive/SRS-DI-02-001_positive.java",
  "negative_case": "examples/DI/negative/SRS-DI-02-001_negative.java",
  "tags": ["DI", "02", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

### tests/cases/TC-DI-003.json

```json
{
  "id": "TC-DI-003",
  "rule_id": "SRS-DI-03-001",
  "category": "DI",
  "subcategory": "03",
  "title": "检测构造器/字段循环依赖-反例触发",
  "description": "验证构造器循环依赖时命中目标规则。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-DI-003/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "CRITICAL",
    "message_contains": ["循环依赖", "构造器"],
    "location_hint": {
      "file": "tests/fixtures/TC-DI-003/input.java",
      "line_start": 6,
      "line_end": 19,
      "symbol": "Di03BeanA <-> Di03BeanB"
    }
  },
  "positive_case": "examples/DI/positive/SRS-DI-03-001_positive.java",
  "negative_case": "examples/DI/negative/SRS-DI-03-001_negative.java",
  "tags": ["DI", "03", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

### tests/cases/TC-DI-004.json

```json
{
  "id": "TC-DI-004",
  "rule_id": "SRS-DI-04-001",
  "category": "DI",
  "subcategory": "04",
  "title": "单例 Bean 中非线程安全可变状态-反例触发",
  "description": "验证单例 Bean 中可变 HashMap 状态时命中目标规则。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-DI-004/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "CRITICAL",
    "message_contains": ["HashMap", "可变状态"],
    "location_hint": {
      "file": "tests/fixtures/TC-DI-004/input.java",
      "line_start": 9,
      "line_end": 12,
      "symbol": "Di04NegativeService.cache"
    }
  },
  "positive_case": "examples/DI/positive/SRS-DI-04-001_positive.java",
  "negative_case": "examples/DI/negative/SRS-DI-04-001_negative.java",
  "tags": ["DI", "04", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

### tests/cases/TC-DI-005.json

```json
{
  "id": "TC-DI-005",
  "rule_id": "SRS-DI-05-001",
  "category": "DI",
  "subcategory": "05",
  "title": "@Scope 与注入点生命周期不匹配-反例触发",
  "description": "验证 request 作用域 Bean 直接注入单例 Bean 时命中目标规则。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-DI-005/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["@RequestScope", "作用域"],
    "location_hint": {
      "file": "tests/fixtures/TC-DI-005/input.java",
      "line_start": 11,
      "line_end": 15,
      "symbol": "Di05NegativeService.requestBean"
    }
  },
  "positive_case": "examples/DI/positive/SRS-DI-05-001_positive.java",
  "negative_case": "examples/DI/negative/SRS-DI-05-001_negative.java",
  "tags": ["DI", "05", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

### tests/cases/TC-DI-006.json

```json
{
  "id": "TC-DI-006",
  "rule_id": "SRS-DI-06-001",
  "category": "DI",
  "subcategory": "06",
  "title": "多个 @Aspect 未指定 @Order-反例触发",
  "description": "验证多个 @Aspect 均未指定 @Order 时命中目标规则。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-DI-006/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["@Aspect", "@Order"],
    "location_hint": {
      "file": "tests/fixtures/TC-DI-006/input.java",
      "line_start": 5,
      "line_end": 10,
      "symbol": "Di06AspectA, Di06AspectB"
    }
  },
  "positive_case": "examples/DI/positive/SRS-DI-06-001_positive.java",
  "negative_case": "examples/DI/negative/SRS-DI-06-001_negative.java",
  "tags": ["DI", "06", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

### tests/cases/TC-DI-007.json

```json
{
  "id": "TC-DI-007",
  "rule_id": "SRS-DI-07-001",
  "category": "DI",
  "subcategory": "07",
  "title": "@Around 未调用 proceed()-反例触发",
  "description": "验证 @Around 未调用 proceed() 时命中目标规则。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-DI-007/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["@Around", "proceed"],
    "location_hint": {
      "file": "tests/fixtures/TC-DI-007/input.java",
      "line_start": 9,
      "line_end": 11,
      "symbol": "Di07NegativeAspect.around"
    }
  },
  "positive_case": "examples/DI/positive/SRS-DI-07-001_positive.java",
  "negative_case": "examples/DI/negative/SRS-DI-07-001_negative.java",
  "tags": ["DI", "07", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

## 5. 期望结果 JSON

### tests/expected/TC-DI-001.json

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-DI-001",
  "rule_id": "SRS-DI-01-001",
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["@Autowired", "字段注入"],
    "location_hint": {
      "file": "tests/fixtures/TC-DI-001/input.java",
      "line_start": 8,
      "line_end": 9,
      "symbol": "Di01NegativeService.dependency"
    }
  }
}
```

### tests/expected/TC-DI-002.json

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-DI-002",
  "rule_id": "SRS-DI-02-001",
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["final", "必需依赖"],
    "location_hint": {
      "file": "tests/fixtures/TC-DI-002/input.java",
      "line_start": 7,
      "line_end": 7,
      "symbol": "Di02NegativeService.dependency"
    }
  }
}
```

### tests/expected/TC-DI-003.json

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-DI-003",
  "rule_id": "SRS-DI-03-001",
  "expected": {
    "triggered": true,
    "severity": "CRITICAL",
    "message_contains": ["循环依赖", "构造器"],
    "location_hint": {
      "file": "tests/fixtures/TC-DI-003/input.java",
      "line_start": 6,
      "line_end": 19,
      "symbol": "Di03BeanA <-> Di03BeanB"
    }
  }
}
```

### tests/expected/TC-DI-004.json

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-DI-004",
  "rule_id": "SRS-DI-04-001",
  "expected": {
    "triggered": true,
    "severity": "CRITICAL",
    "message_contains": ["HashMap", "可变状态"],
    "location_hint": {
      "file": "tests/fixtures/TC-DI-004/input.java",
      "line_start": 9,
      "line_end": 12,
      "symbol": "Di04NegativeService.cache"
    }
  }
}
```

### tests/expected/TC-DI-005.json

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-DI-005",
  "rule_id": "SRS-DI-05-001",
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["@RequestScope", "作用域"],
    "location_hint": {
      "file": "tests/fixtures/TC-DI-005/input.java",
      "line_start": 11,
      "line_end": 15,
      "symbol": "Di05NegativeService.requestBean"
    }
  }
}
```

### tests/expected/TC-DI-006.json

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-DI-006",
  "rule_id": "SRS-DI-06-001",
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["@Aspect", "@Order"],
    "location_hint": {
      "file": "tests/fixtures/TC-DI-006/input.java",
      "line_start": 5,
      "line_end": 10,
      "symbol": "Di06AspectA, Di06AspectB"
    }
  }
}
```

### tests/expected/TC-DI-007.json

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-DI-007",
  "rule_id": "SRS-DI-07-001",
  "expected": {
    "triggered": true,
    "severity": "MAJOR",
    "message_contains": ["@Around", "proceed"],
    "location_hint": {
      "file": "tests/fixtures/TC-DI-007/input.java",
      "line_start": 9,
      "line_end": 11,
      "symbol": "Di07NegativeAspect.around"
    }
  }
}
```

## 6. examples/index.json

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "count": 20,
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

## 7. tests/cases/index.json

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "count": 10,
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

## 8. 试点结论

- 已为 DI 分类 7 条规则补齐正反例、夹具、测试用例、expected、examples 索引、tests/cases 索引。
- 本次 14 个正反例、7 个夹具、7 个测试用例、7 个 expected 均按 C0 结构约束编写。
- 两个索引严格使用 A4.2 字段，items 按 id 升序，count 与 items.length 一致。
- 未重复 C1 的 8 个错误；未修改 A5、A6 Schema；未修改 B1～B7 规则文件。
- 建议进入下一阶段：C2b 或 DI 规则测试资产评审。