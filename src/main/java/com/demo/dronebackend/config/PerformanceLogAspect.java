package com.demo.dronebackend.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class PerformanceLogAspect {

//    // 切 Controller 层
//    @Pointcut("execution(* com.demo.dronebackend.controller..*(..))")
//    public void controllerMethods() {}
//
//    // 切 Service 层
//    @Pointcut("execution(* com.demo.dronebackend.service..*(..))")
//    public void serviceMethods() {}
//
//    // 切 Mapper 层
//    @Pointcut("execution(* com.demo.dronebackend.mapper..*(..))")
//    public void mapperMethods() {}
//
//    // Controller 层耗时
//    @Around("controllerMethods()")
//    public Object logControllerTime(ProceedingJoinPoint joinPoint) throws Throwable {
//        long start = System.currentTimeMillis();
//        Object result = joinPoint.proceed();
//        long cost = System.currentTimeMillis() - start;
//
//        log.info("[Controller] {}.{}() 耗时: {} ms",
//                joinPoint.getSignature().getDeclaringTypeName(),
//                joinPoint.getSignature().getName(),
//                cost);
//        return result;
//    }
//
//    // Service 层耗时
//    @Around("serviceMethods()")
//    public Object logServiceTime(ProceedingJoinPoint joinPoint) throws Throwable {
//        long start = System.currentTimeMillis();
//        Object result = joinPoint.proceed();
//        long cost = System.currentTimeMillis() - start;
//
//        log.info("[Service] {}.{}() 耗时: {} ms",
//                joinPoint.getSignature().getDeclaringTypeName(),
//                joinPoint.getSignature().getName(),
//                cost);
//        return result;
//    }
//
//    // Mapper 层耗时
//    @Around("mapperMethods()")
//    public Object logMapperTime(ProceedingJoinPoint joinPoint) throws Throwable {
//        long start = System.currentTimeMillis();
//        Object result = joinPoint.proceed();
//        long cost = System.currentTimeMillis() - start;
//
//        log.info("[Mapper] {}.{}() 耗时: {} ms",
//                joinPoint.getSignature().getDeclaringTypeName(),
//                joinPoint.getSignature().getName(),
//                cost);
//        return result;
//    }
}
