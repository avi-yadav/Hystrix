package com.netflix.hystrix.contrib.javanica.aop;

import com.netflix.hystrix.contrib.javanica.command.ExecutionType;
import com.netflix.hystrix.contrib.javanica.command.MetaHolder;
import com.netflix.hystrix.contrib.javanica.utils.FallbackMethod;
import com.netflix.hystrix.contrib.javanica.utils.MethodProvider;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

import static com.netflix.hystrix.contrib.javanica.utils.AopUtils.getMethodFromTarget;
import static com.netflix.hystrix.contrib.javanica.utils.EnvUtils.isCompileWeaving;
import static com.netflix.hystrix.contrib.javanica.utils.ajc.AjcUtils.getAjcMethodAroundAdvice;

/**
 * Created by gautham.srinivas on 18/11/15.
 */
public abstract class MetaHolderFactory {
    public MetaHolder create(final ProceedingJoinPoint joinPoint) {
        Method method = getMethodFromTarget(joinPoint);
        Object obj = joinPoint.getTarget();
        Object[] args = joinPoint.getArgs();
        Object proxy = joinPoint.getThis();
        return create(proxy, method, obj, args, joinPoint);
    }

    public MetaHolder create(final MethodInvocation invocation) {
        Method method = invocation.getMethod();
        Object obj = invocation.getThis();
        Object[] args = invocation.getArguments();
        Object proxy = invocation.getThis();
        return create(proxy, method, obj, args, null);
    }

    public abstract MetaHolder create(Object proxy, Method method, Object obj, Object[] args, final ProceedingJoinPoint joinPoint);


    MetaHolder.Builder metaHolderBuilder(Object proxy, Method method, Object obj, Object[] args, final ProceedingJoinPoint joinPoint) {
        MetaHolder.Builder builder = MetaHolder.builder()
                .args(args).method(method).obj(obj).proxyObj(proxy)
                .defaultGroupKey(obj.getClass().getSimpleName())
                .joinPoint(joinPoint);
        if (isCompileWeaving()) {
            builder.ajcMethod(getAjcMethodFromTarget(joinPoint));
        }

        FallbackMethod fallbackMethod = MethodProvider.getInstance().getFallbackMethod(obj.getClass(), method);
        if (fallbackMethod.isPresent()) {
            fallbackMethod.validateReturnType(method);
            builder
                    .fallbackMethod(fallbackMethod.getMethod())
                    .fallbackExecutionType(ExecutionType.getExecutionType(fallbackMethod.getMethod().getReturnType()));
        }
        return builder;
    }

    private static Method getAjcMethodFromTarget(JoinPoint joinPoint) {
        return (joinPoint == null)?null:getAjcMethodAroundAdvice(joinPoint.getTarget().getClass(), (MethodSignature) joinPoint.getSignature());
    }
}
