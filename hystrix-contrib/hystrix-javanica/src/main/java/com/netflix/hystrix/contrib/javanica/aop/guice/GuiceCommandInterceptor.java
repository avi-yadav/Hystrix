package com.netflix.hystrix.contrib.javanica.aop.guice;

import com.google.common.collect.ImmutableMap;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCollapser;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.aop.CollapserMetaHolderFactory;
import com.netflix.hystrix.contrib.javanica.aop.CommandMetaHolderFactory;
import com.netflix.hystrix.contrib.javanica.aop.HystrixPointcutType;
import com.netflix.hystrix.contrib.javanica.aop.MetaHolderFactory;
import com.netflix.hystrix.contrib.javanica.command.MetaHolder;
import com.netflix.hystrix.contrib.javanica.utils.AopUtils;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.Validate;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Created by gautham.srinivas on 18/11/15.
 */
public class GuiceCommandInterceptor implements MethodInterceptor {

    private static final Map<HystrixPointcutType, MetaHolderFactory> META_HOLDER_FACTORY_MAP;

    static {
        META_HOLDER_FACTORY_MAP = ImmutableMap.<HystrixPointcutType, MetaHolderFactory>builder()
                .put(HystrixPointcutType.COMMAND, new CommandMetaHolderFactory())
                .put(HystrixPointcutType.COLLAPSER, new CollapserMetaHolderFactory())
                .build();
    }


    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Validate.notNull(method, "failed to get method from method invocation: %s", invocation);
        if (method.isAnnotationPresent(HystrixCommand.class) && method.isAnnotationPresent(HystrixCollapser.class)) {
            throw new IllegalStateException("method cannot be annotated with HystrixCommand and HystrixCollapser " +
                    "annotations at the same time");
        }

        MetaHolderFactory metaHolderFactory = META_HOLDER_FACTORY_MAP.get(HystrixPointcutType.of(method));
        MetaHolder metaHolder = metaHolderFactory.create(invocation);

        return AopUtils.getHystrixResultFromMetaHolder(metaHolder);
    }}
