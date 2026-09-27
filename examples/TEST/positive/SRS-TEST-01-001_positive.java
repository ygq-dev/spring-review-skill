package com.example.test;

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
public class UserRepositoryInMemoryTest {
    public void loadFromInMemoryDatabase() {
        // 使用内存数据库 H2，不连接真实 MySQL
    }
}