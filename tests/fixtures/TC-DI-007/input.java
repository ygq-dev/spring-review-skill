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