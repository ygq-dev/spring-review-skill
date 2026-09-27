package com.example.obs;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class WidePointcutAspect {
    @Pointcut("execution(* *(..))")
    public void anyMethod() {}

    @Before("anyMethod()")
    public void beforeAny() {}
}