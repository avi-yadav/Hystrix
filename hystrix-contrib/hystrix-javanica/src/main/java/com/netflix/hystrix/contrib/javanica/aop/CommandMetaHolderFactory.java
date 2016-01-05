package com.netflix.hystrix.contrib.javanica.aop;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.command.ExecutionType;
import com.netflix.hystrix.contrib.javanica.command.MetaHolder;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.ProceedingJoinPoint;

import java.lang.reflect.Method;

/**
 * Created by gautham.srinivas on 18/11/15.
 */
public class CommandMetaHolderFactory extends MetaHolderFactory {
    @Override
    public MetaHolder.Builder create(Object proxy, Method method, Object obj, Object[] args) {
        HystrixCommand hystrixCommand = method.getAnnotation(HystrixCommand.class);
        MetaHolder.Builder builder = metaHolderBuilder(proxy, method, obj, args);
        builder.defaultCommandKey(method.getName());
        builder.hystrixCommand(hystrixCommand);
        builder.executionType(ExecutionType.getExecutionType(method.getReturnType()));
        return builder;
    }
}
