package com.ordermanagement.aspect;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class MetricsAspect {

    private final MeterRegistry meterRegistry;

    public MetricsAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Around("within(com.ordermanagement.service.OrderService)")
    public Object trackOrderMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            Object result = joinPoint.proceed();

            Counter.builder("orders.operations")
                    .tag("method", methodName)
                    .tag("status", "success")
                    .register(meterRegistry)
                    .increment();

            return result;
        } catch (Throwable throwable) {
            Counter.builder("orders.operations")
                    .tag("method", methodName)
                    .tag("status", "error")
                    .register(meterRegistry)
                    .increment();
            throw throwable;
        } finally {
            sample.stop(Timer.builder("orders.operation.duration")
                    .tag("method", methodName)
                    .register(meterRegistry));
        }
    }
}
