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