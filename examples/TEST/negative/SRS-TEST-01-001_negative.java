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