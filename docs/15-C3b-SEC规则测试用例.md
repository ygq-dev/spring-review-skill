# docs/15-C3b-SEC规则测试用例.md

## 1. 说明

本文件为 C3b 阶段产出，覆盖 B3 冻结的 5 条 SEC 规则：SRS-SEC-01-001、SRS-SEC-02-001、SRS-SEC-03-001、SRS-SEC-04-001、SRS-SEC-05-001。

本次产出：10 个正反例、5 个夹具、5 个测试用例 JSON、5 个期望结果 JSON、2 个索引新增条目片段。严格遵循 C0 测试用例结构，不修改代码、不提交、不训练模型，不修改 A5、A6 Schema，不修改 B1～B7 规则文件。

## 2. 正反例代码（5 对，共 10 个）

**保存路径：** `examples/SEC/positive/SRS-SEC-01-001_positive.java`

```java
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SecretConfigService {
    private final String password;

    public SecretConfigService(@Value("${app.password}") String password) {
        this.password = password;
    }

    public String loadSecret() {
        return password;
    }
}
```

**保存路径：** `examples/SEC/negative/SRS-SEC-01-001_negative.java`

```java
import org.springframework.stereotype.Service;

@Service
public class HardcodedSecretService {
    private final String password = "admin123";
    private final String apiKey = "sk-xxx";

    public String loadSecret() {
        return password + apiKey;
    }
}
```

**保存路径：** `examples/SEC/positive/SRS-SEC-02-001_positive.java`

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SafeLogService {
    private static final Logger log = LoggerFactory.getLogger(SafeLogService.class);

    public void login(String userId) {
        log.info("user login id={}", mask(userId));
    }

    private String mask(String value) {
        return value == null ? null : value.replaceAll("(\\d{3})\\d+(\\d{2})", "$1****$2");
    }
}
```

**保存路径：** `examples/SEC/negative/SRS-SEC-02-001_negative.java`

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SensitiveLogService {
    private static final Logger log = LoggerFactory.getLogger(SensitiveLogService.class);

    public void login(String userId, String password, String token) {
        log.info("user login password={}, token={}", password, token);
        log.info("idCard=110101199001011234");
    }
}
```

**保存路径：** `examples/SEC/positive/SRS-SEC-03-001_positive.java`

```java
import java.sql.Connection;
import java.sql.PreparedStatement;
import org.springframework.stereotype.Service;

@Service
public class SafeSqlService {
    private final Connection connection;

    public SafeSqlService(Connection connection) {
        this.connection = connection;
    }

    public void findUser(String userName) throws Exception {
        String sql = "SELECT * FROM users WHERE name = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, userName);
        statement.executeQuery();
    }
}
```

**保存路径：** `examples/SEC/negative/SRS-SEC-03-001_negative.java`

```java
import java.sql.Connection;
import java.sql.Statement;
import org.springframework.stereotype.Service;

@Service
public class SqlConcatService {
    private final Connection connection;

    public SqlConcatService(Connection connection) {
        this.connection = connection;
    }

    public void findUser(String userName) throws Exception {
        String sql = "SELECT * FROM users WHERE name = '" + userName + "'";
        Statement statement = connection.createStatement();
        statement.executeQuery(sql);
    }
}
```

**保存路径：** `examples/SEC/positive/SRS-SEC-04-001_positive.java`

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.csrfTokenRepository(
                org.springframework.security.web.csrf.CookieCsrfTokenRepository.withHttpOnlyFalse()))
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
        return http.build();
    }
}
```

**保存路径：** `examples/SEC/negative/SRS-SEC-04-001_negative.java`

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
```

**保存路径：** `examples/SEC/positive/SRS-SEC-05-001_positive.java`

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class SafeDeserializeService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SafeDto read(String json) throws Exception {
        return objectMapper.readValue(json, SafeDto.class);
    }

    public static class SafeDto {
        public String name;
    }
}
```

**保存路径：** `examples/SEC/negative/SRS-SEC-05-001_negative.java`

```java
import java.io.InputStream;
import java.io.ObjectInputStream;
import org.springframework.stereotype.Service;

@Service
public class UnsafeDeserializeService {

    public Object read(InputStream inputStream) throws Exception {
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        return objectInputStream.readObject();
    }
}
```

## 3. 夹具（5 个）

**保存路径：** `tests/fixtures/TC-SEC-001/input.java`

```java
import org.springframework.stereotype.Service;

@Service
public class HardcodedSecretService {

    private final String password = "admin123";
    private final String apiKey = "sk-xxx";

    public String loadSecret() {
        return password + apiKey;
    }
}
```

**保存路径：** `tests/fixtures/TC-SEC-002/input.java`

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SensitiveLogService {
    private static final Logger log = LoggerFactory.getLogger(SensitiveLogService.class);

    public void login(String userId, String password, String token) {
        log.info("user login password={}, token={}", password, token);
        log.info("idCard=110101199001011234");
    }
}
```

**保存路径：** `tests/fixtures/TC-SEC-003/input.java`

```java
import java.sql.Connection;
import java.sql.Statement;
import org.springframework.stereotype.Service;

@Service
public class SqlConcatService {
    private final Connection connection;

    public SqlConcatService(Connection connection) {
        this.connection = connection;
    }

    public void findUser(String userName) throws Exception {
        String sql = "SELECT * FROM users WHERE name = '" + userName + "'";
        Statement statement = connection.createStatement();
        statement.executeQuery(sql);
    }
}
```

**保存路径：** `tests/fixtures/TC-SEC-004/input.java`

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
```

**保存路径：** `tests/fixtures/TC-SEC-005/input.java`

```java
import java.io.InputStream;
import java.io.ObjectInputStream;
import org.springframework.stereotype.Service;

@Service
public class UnsafeDeserializeService {

    public Object read(InputStream inputStream) throws Exception {
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        return objectInputStream.readObject();
    }
}
```

## 4. 测试用例 JSON（5 个完整文件）

**保存路径：** `tests/cases/TC-SEC-001.json`

```json
{
  "id": "TC-SEC-001",
  "rule_id": "SRS-SEC-01-001",
  "category": "SEC",
  "subcategory": "01",
  "title": "硬编码密钥/密码/token 反例触发",
  "description": "检测源码中硬编码 password 和 apiKey 字面量。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-SEC-001/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "BLOCKER",
    "message_contains": ["password", "apiKey"],
    "location_hint": {
      "file": "tests/fixtures/TC-SEC-001/input.java",
      "line_start": 6,
      "line_end": 7,
      "symbol": "HardcodedSecretService.password"
    }
  },
  "positive_case": "examples/SEC/positive/SRS-SEC-01-001_positive.java",
  "negative_case": "examples/SEC/negative/SRS-SEC-01-001_negative.java",
  "tags": ["SEC", "01", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

**保存路径：** `tests/cases/TC-SEC-002.json`

```json
{
  "id": "TC-SEC-002",
  "rule_id": "SRS-SEC-02-001",
  "category": "SEC",
  "subcategory": "02",
  "title": "日志输出敏感信息反例触发",
  "description": "检测日志中输出完整 password、token 和身份证号。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-SEC-002/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "CRITICAL",
    "message_contains": ["password", "token"],
    "location_hint": {
      "file": "tests/fixtures/TC-SEC-002/input.java",
      "line_start": 10,
      "line_end": 11,
      "symbol": "SensitiveLogService.login"
    }
  },
  "positive_case": "examples/SEC/positive/SRS-SEC-02-001_positive.java",
  "negative_case": "examples/SEC/negative/SRS-SEC-02-001_negative.java",
  "tags": ["SEC", "02", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

**保存路径：** `tests/cases/TC-SEC-003.json`

```json
{
  "id": "TC-SEC-003",
  "rule_id": "SRS-SEC-03-001",
  "category": "SEC",
  "subcategory": "03",
  "title": "SQL 字符串拼接反例触发",
  "description": "检测使用字符串拼接构造包含用户输入的 SQL。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-SEC-003/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "BLOCKER",
    "message_contains": ["SELECT", "userName"],
    "location_hint": {
      "file": "tests/fixtures/TC-SEC-003/input.java",
      "line_start": 14,
      "line_end": 14,
      "symbol": "SqlConcatService.findUser"
    }
  },
  "positive_case": "examples/SEC/positive/SRS-SEC-03-001_positive.java",
  "negative_case": "examples/SEC/negative/SRS-SEC-03-001_negative.java",
  "tags": ["SEC", "03", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

**保存路径：** `tests/cases/TC-SEC-004.json`

```json
{
  "id": "TC-SEC-004",
  "rule_id": "SRS-SEC-04-001",
  "category": "SEC",
  "subcategory": "04",
  "title": "关闭 CSRF 且无豁免说明反例触发",
  "description": "检测全局 csrf().disable() 且无注释或路径限制。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-SEC-004/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "CRITICAL",
    "message_contains": ["csrf", "disable"],
    "location_hint": {
      "file": "tests/fixtures/TC-SEC-004/input.java",
      "line_start": 11,
      "line_end": 11,
      "symbol": "SecurityConfig.filterChain"
    }
  },
  "positive_case": "examples/SEC/positive/SRS-SEC-04-001_positive.java",
  "negative_case": "examples/SEC/negative/SRS-SEC-04-001_negative.java",
  "tags": ["SEC", "04", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

**保存路径：** `tests/cases/TC-SEC-005.json`

```json
{
  "id": "TC-SEC-005",
  "rule_id": "SRS-SEC-05-001",
  "category": "SEC",
  "subcategory": "05",
  "title": "不安全反序列化反例触发",
  "description": "检测 ObjectInputStream.readObject 直接处理输入流。",
  "input": {
    "type": "fixture_ref",
    "path": "tests/fixtures/TC-SEC-005/input.java",
    "content": null,
    "language": "java",
    "encoding": "utf-8"
  },
  "expected": {
    "triggered": true,
    "severity": "BLOCKER",
    "message_contains": ["ObjectInputStream", "readObject"],
    "location_hint": {
      "file": "tests/fixtures/TC-SEC-005/input.java",
      "line_start": 9,
      "line_end": 10,
      "symbol": "UnsafeDeserializeService.read"
    }
  },
  "positive_case": "examples/SEC/positive/SRS-SEC-05-001_positive.java",
  "negative_case": "examples/SEC/negative/SRS-SEC-05-001_negative.java",
  "tags": ["SEC", "05", "fixture", "negative"],
  "version": "1.0.0",
  "status": "ACTIVE"
}
```

## 5. 期望结果 JSON（5 个完整文件）

**保存路径：** `tests/expected/TC-SEC-001.json`

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-SEC-001",
  "rule_id": "SRS-SEC-01-001",
  "expected": {
    "triggered": true,
    "severity": "BLOCKER",
    "message_contains": ["password", "apiKey"],
    "location_hint": {
      "file": "tests/fixtures/TC-SEC-001/input.java",
      "line_start": 6,
      "line_end": 7,
      "symbol": "HardcodedSecretService.password"
    }
  }
}
```

**保存路径：** `tests/expected/TC-SEC-002.json`

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-SEC-002",
  "rule_id": "SRS-SEC-02-001",
  "expected": {
    "triggered": true,
    "severity": "CRITICAL",
    "message_contains": ["password", "token"],
    "location_hint": {
      "file": "tests/fixtures/TC-SEC-002/input.java",
      "line_start": 10,
      "line_end": 11,
      "symbol": "SensitiveLogService.login"
    }
  }
}
```

**保存路径：** `tests/expected/TC-SEC-003.json`

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-SEC-003",
  "rule_id": "SRS-SEC-03-001",
  "expected": {
    "triggered": true,
    "severity": "BLOCKER",
    "message_contains": ["SELECT", "userName"],
    "location_hint": {
      "file": "tests/fixtures/TC-SEC-003/input.java",
      "line_start": 14,
      "line_end": 14,
      "symbol": "SqlConcatService.findUser"
    }
  }
}
```

**保存路径：** `tests/expected/TC-SEC-004.json`

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-SEC-004",
  "rule_id": "SRS-SEC-04-001",
  "expected": {
    "triggered": true,
    "severity": "CRITICAL",
    "message_contains": ["csrf", "disable"],
    "location_hint": {
      "file": "tests/fixtures/TC-SEC-004/input.java",
      "line_start": 11,
      "line_end": 11,
      "symbol": "SecurityConfig.filterChain"
    }
  }
}
```

**保存路径：** `tests/expected/TC-SEC-005.json`

```json
{
  "version": "1.0.0",
  "generated_at": "2026-09-24T00:00:00Z",
  "generated_by": "manual/1.0.0",
  "case_id": "TC-SEC-005",
  "rule_id": "SRS-SEC-05-001",
  "expected": {
    "triggered": true,
    "severity": "BLOCKER",
    "message_contains": ["ObjectInputStream", "readObject"],
    "location_hint": {
      "file": "tests/fixtures/TC-SEC-005/input.java",
      "line_start": 9,
      "line_end": 10,
      "symbol": "UnsafeDeserializeService.read"
    }
  }
}
```

## 6. examples/index.json 新增条目（10 条，待 C8 合并）

本次新增 10 条，待 C8 阶段统一合并。C8 合并时按 id 升序、已有条目逐字保留。

```json
[
  {
    "id": "SRS-SEC-01-001_positive",
    "rule_id": "SRS-SEC-01-001",
    "category": "SEC",
    "subcategory": "01",
    "polarity": "positive",
    "file": "examples/SEC/positive/SRS-SEC-01-001_positive.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "从环境变量或配置读取密钥正例"
  },
  {
    "id": "SRS-SEC-01-001_negative",
    "rule_id": "SRS-SEC-01-001",
    "category": "SEC",
    "subcategory": "01",
    "polarity": "negative",
    "file": "examples/SEC/negative/SRS-SEC-01-001_negative.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "硬编码密钥/密码/token 反例"
  },
  {
    "id": "SRS-SEC-02-001_positive",
    "rule_id": "SRS-SEC-02-001",
    "category": "SEC",
    "subcategory": "02",
    "polarity": "positive",
    "file": "examples/SEC/positive/SRS-SEC-02-001_positive.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "日志只打印脱敏用户标识正例"
  },
  {
    "id": "SRS-SEC-02-001_negative",
    "rule_id": "SRS-SEC-02-001",
    "category": "SEC",
    "subcategory": "02",
    "polarity": "negative",
    "file": "examples/SEC/negative/SRS-SEC-02-001_negative.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "日志输出敏感信息反例"
  },
  {
    "id": "SRS-SEC-03-001_positive",
    "rule_id": "SRS-SEC-03-001",
    "category": "SEC",
    "subcategory": "03",
    "polarity": "positive",
    "file": "examples/SEC/positive/SRS-SEC-03-001_positive.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "PreparedStatement 参数化查询正例"
  },
  {
    "id": "SRS-SEC-03-001_negative",
    "rule_id": "SRS-SEC-03-001",
    "category": "SEC",
    "subcategory": "03",
    "polarity": "negative",
    "file": "examples/SEC/negative/SRS-SEC-03-001_negative.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "SQL 字符串拼接反例"
  },
  {
    "id": "SRS-SEC-04-001_positive",
    "rule_id": "SRS-SEC-04-001",
    "category": "SEC",
    "subcategory": "04",
    "polarity": "positive",
    "file": "examples/SEC/positive/SRS-SEC-04-001_positive.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "保持 CSRF 启用正例"
  },
  {
    "id": "SRS-SEC-04-001_negative",
    "rule_id": "SRS-SEC-04-001",
    "category": "SEC",
    "subcategory": "04",
    "polarity": "negative",
    "file": "examples/SEC/negative/SRS-SEC-04-001_negative.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "全局关闭 CSRF 且无豁免说明反例"
  },
  {
    "id": "SRS-SEC-05-001_positive",
    "rule_id": "SRS-SEC-05-001",
    "category": "SEC",
    "subcategory": "05",
    "polarity": "positive",
    "file": "examples/SEC/positive/SRS-SEC-05-001_positive.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "白名单类型安全反序列化正例"
  },
  {
    "id": "SRS-SEC-05-001_negative",
    "rule_id": "SRS-SEC-05-001",
    "category": "SEC",
    "subcategory": "05",
    "polarity": "negative",
    "file": "examples/SEC/negative/SRS-SEC-05-001_negative.java",
    "status": "ACTIVE",
    "version": "1.0.0",
    "language": "java",
    "title": "ObjectInputStream.readObject 反例"
  }
]
```

## 7. tests/cases/index.json 新增条目（5 条，待 C8 合并）

本次新增 5 条，待 C8 阶段统一合并。C8 合并时按 id 升序、已有条目逐字保留。

```json
[
  {
    "id": "TC-SEC-001",
    "rule_id": "SRS-SEC-01-001",
    "category": "SEC",
    "file": "tests/cases/TC-SEC-001.json",
    "status": "ACTIVE",
    "version": "1.0.0",
    "title": "硬编码密钥/密码/token 反例触发",
    "tags": ["SEC", "01", "fixture", "negative"]
  },
  {
    "id": "TC-SEC-002",
    "rule_id": "SRS-SEC-02-001",
    "category": "SEC",
    "file": "tests/cases/TC-SEC-002.json",
    "status": "ACTIVE",
    "version": "1.0.0",
    "title": "日志输出敏感信息反例触发",
    "tags": ["SEC", "02", "fixture", "negative"]
  },
  {
    "id": "TC-SEC-003",
    "rule_id": "SRS-SEC-03-001",
    "category": "SEC",
    "file": "tests/cases/TC-SEC-003.json",
    "status": "ACTIVE",
    "version": "1.0.0",
    "title": "SQL 字符串拼接反例触发",
    "tags": ["SEC", "03", "fixture", "negative"]
  },
  {
    "id": "TC-SEC-004",
    "rule_id": "SRS-SEC-04-001",
    "category": "SEC",
    "file": "tests/cases/TC-SEC-004.json",
    "status": "ACTIVE",
    "version": "1.0.0",
    "title": "关闭 CSRF 且无豁免说明反例触发",
    "tags": ["SEC", "04", "fixture", "negative"]
  },
  {
    "id": "TC-SEC-005",
    "rule_id": "SRS-SEC-05-001",
    "category": "SEC",
    "file": "tests/cases/TC-SEC-005.json",
    "status": "ACTIVE",
    "version": "1.0.0",
    "title": "不安全反序列化反例触发",
    "tags": ["SEC", "05", "fixture", "negative"]
  }
]
```

## 8. 试点结论

C3b 已完成 SEC 5 条规则的正反例、夹具、测试用例、期望结果与索引新增片段。结构严格遵循 C0，测试用例 JSON 顶层 13 字段，expected 为完整 object，positive_case/negative_case 为字符串路径；expected JSON 顶层 6 字段并使用 case_id。索引新增片段仅输出本次新增条目，待 C8 阶段统一合并。未修改规则文件与 Schema。