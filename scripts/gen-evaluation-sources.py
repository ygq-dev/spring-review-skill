# -*- coding: utf-8 -*-
"""
H2：生成基准评测集 30 个 .java 文件。
运行：python scripts/gen-evaluation-sources.py
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DEFECTS = ROOT / "tests/evaluation/defects"
CLEAN = ROOT / "tests/evaluation/clean"

DEFECT_SOURCES = {
    "DEFECT-001": """package evaluation.defects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DEFECT_001 {
    @Autowired
    private Dependency dependency;

    public String run() {
        return dependency != null ? "ok" : "no";
    }

    interface Dependency {
    }
}
""",
    "DEFECT-002": """package evaluation.defects;

import org.springframework.beans.factory.annotation.Autowired;

public class DEFECT_002 {
    @Autowired
    private Dependency dependency;

    interface Dependency {
    }
}
""",
    "DEFECT-003": """package evaluation.defects;

import org.springframework.transaction.annotation.Transactional;

public class DEFECT_003 {
    @Transactional
    private void badTransaction() {
    }
}
""",
    "DEFECT-004": """package evaluation.defects;

import org.springframework.transaction.annotation.Transactional;

public class DEFECT_004 {
    public void outer() {
        this.inner();
    }

    @Transactional
    public void inner() {
    }
}
""",
    "DEFECT-005": """package evaluation.defects;

import org.springframework.transaction.annotation.Transactional;

public class DEFECT_005 {
    @Transactional
    public String findUser(long id) {
        return "user-" + id;
    }
}
""",
    "DEFECT-006": """package evaluation.defects;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

public class DEFECT_006 {
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public void process() {
        restTemplate.getForObject("https://example.com", String.class);
    }
}
""",
    "DEFECT-007": """package evaluation.defects;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class DEFECT_007 {
    private String currentUser;
}
""",
    "DEFECT-008": """package evaluation.defects;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DEFECT_008 {
    @PostMapping("/users")
    public String create(@RequestBody UserRequest request) {
        return request.getName();
    }

    static class UserRequest {
        private String name;

        public String getName() {
            return name;
        }
    }
}
""",
    "DEFECT-009": """package evaluation.defects;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class DEFECT_009 implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowCredentials(true);
    }
}
""",
    "DEFECT-010": """package evaluation.defects;

import java.text.SimpleDateFormat;

public class DEFECT_010 {
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");
}
""",
    "DEFECT-011": """package evaluation.defects;

public class DEFECT_011 {
    private final String password = "admin123456";
    private final String apiKey = "sk-abcdef123456";
}
""",
    "DEFECT-012": """package evaluation.defects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DEFECT_012 {
    private static final Logger log = LoggerFactory.getLogger(DEFECT_012.class);

    public void login(String password, String token) {
        log.info("login password={} token={}", password, token);
    }
}
""",
    "DEFECT-013": """package evaluation.defects;

public class DEFECT_013 {
    public String findUser(String name) {
        return "SELECT * FROM users WHERE name = '" + name + "'";
    }
}
""",
    "DEFECT-014": """package evaluation.defects;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

public class DEFECT_014 {
    public void configure(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());
    }
}
""",
    "DEFECT-015": """package evaluation.defects;

public class DEFECT_015 {
    public void run() {
        try {
            doWork();
        } catch (Exception e) {
        }
    }

    private void doWork() {
    }
}
""",
    "DEFECT-016": """package evaluation.defects;

public class DEFECT_016 {
    public void run() {
        try {
            doWork();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doWork() {
    }
}
""",
    "DEFECT-017": """package evaluation.defects;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DEFECT_017 {
    public void read() throws IOException {
        InputStream in = new FileInputStream("data.txt");
        in.read();
    }
}
""",
    "DEFECT-018": """package evaluation.defects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DEFECT_018 {
    private static final Logger log = LoggerFactory.getLogger(DEFECT_018.class);

    public void logUser(String userId) {
        log.info("user=" + userId);
    }
}
""",
    "DEFECT-019": """package evaluation.defects;

import javax.servlet.http.HttpServletRequest;

public class DEFECT_019 {
    public String extract(HttpServletRequest request) {
        return request.getHeader("X-User");
    }
}
""",
    "DEFECT-020": """package evaluation.defects;

public class DEFECT_020 {
    public String login(boolean ok) {
        if (ok) {
            return "登录成功";
        }
        return "登录失败，请重试";
    }
}
""",
}

CLEAN_SOURCES = {
    "CLEAN-001": """package evaluation.clean;

import org.springframework.stereotype.Service;

@Service
public class CLEAN_001 {
    private final Dependency dependency;

    public CLEAN_001(Dependency dependency) {
        this.dependency = dependency;
    }

    interface Dependency {
    }
}
""",
    "CLEAN-002": """package evaluation.clean;

import org.springframework.transaction.annotation.Transactional;

public class CLEAN_002 {
    @Transactional
    public void save(String value) {
    }
}
""",
    "CLEAN-003": """package evaluation.clean;

import org.springframework.transaction.annotation.Transactional;

public class CLEAN_003 {
    @Transactional(readOnly = true)
    public String findUser(long id) {
        return "user-" + id;
    }
}
""",
    "CLEAN-004": """package evaluation.clean;

public class CLEAN_004 {
    public String findUser() {
        return "SELECT * FROM users WHERE name = ?";
    }
}
""",
    "CLEAN-005": """package evaluation.clean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CLEAN_005 {
    private static final Logger log = LoggerFactory.getLogger(CLEAN_005.class);

    public void logUser(String userId) {
        log.info("user={}", userId);
    }
}
""",
    "CLEAN-006": """package evaluation.clean;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CLEAN_006 {
    public void read() throws IOException {
        try (InputStream in = new FileInputStream("data.txt")) {
            in.read();
        }
    }
}
""",
    "CLEAN-007": """package evaluation.clean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CLEAN_007 {
    private static final Logger log = LoggerFactory.getLogger(CLEAN_007.class);

    public void run() {
        try {
            doWork();
        } catch (Exception e) {
            log.error("fail", e);
        }
    }

    private void doWork() {
    }
}
""",
    "CLEAN-008": """package evaluation.clean;

import java.time.format.DateTimeFormatter;

public class CLEAN_008 {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
}
""",
    "CLEAN-009": """package evaluation.clean;

public class CLEAN_009 {
    private final String password = System.getenv("APP_PASSWORD");
}
""",
    "CLEAN-010": """package evaluation.clean;

import org.springframework.context.MessageSource;

public class CLEAN_010 {
    private final MessageSource messageSource;

    public CLEAN_010(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String login(boolean ok) {
        return messageSource.getMessage(ok ? "login.ok" : "login.fail", null, null);
    }
}
""",
}


def main():
    DEFECTS.mkdir(parents=True, exist_ok=True)
    CLEAN.mkdir(parents=True, exist_ok=True)

    n = 0
    for case_id, src in DEFECT_SOURCES.items():
        (DEFECTS / f"{case_id}.java").write_text(src, encoding="utf-8")
        n += 1
    for case_id, src in CLEAN_SOURCES.items():
        (CLEAN / f"{case_id}.java").write_text(src, encoding="utf-8")
        n += 1

    print(f"Generated {n} source files.")


if __name__ == "__main__":
    main()