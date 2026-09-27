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