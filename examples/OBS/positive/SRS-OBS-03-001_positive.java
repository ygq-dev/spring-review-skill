package com.example.obs;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class ScopedPointcutAspect {
    @Pointcut("execution(* com.example.service..*(..))")
    public void serviceMethods() {}

    @Before("serviceMethods()")
    public void beforeService() {}
}