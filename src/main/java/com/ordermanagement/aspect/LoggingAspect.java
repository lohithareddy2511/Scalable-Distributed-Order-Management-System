package com.ordermanagement.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Pointcut("within(com.ordermanagement.controller..*)")
    public void controllerMethods() {}

    @Pointcut("within(com.ordermanagement.service..*)")
    public void serviceMethods() {}

    @Around("controllerMethods()")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        log.info("==> {}.{}()", className, methodName);

        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - startTime;

        log.info("<== {}.{}() completed in {}ms",
                className, methodName, duration);

        return result;
    }

    @AfterThrowing(pointcut = "serviceMethods()", throwing = "exception")
    public void logServiceException(JoinPoint joinPoint, Throwable exception) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        log.error("Exception in {}.{}(): {} - {}",
                className, methodName, exception.getClass().getSimpleName(), exception.getMessage());
    }
}
