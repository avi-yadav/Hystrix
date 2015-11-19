package com.netflix.hystrix.contrib.javanica.aop.guice;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.cache.CacheInvocationContext;
import com.netflix.hystrix.contrib.javanica.cache.CacheInvocationContextFactory;
import com.netflix.hystrix.contrib.javanica.cache.HystrixRequestCacheManager;
import com.netflix.hystrix.contrib.javanica.cache.annotation.CacheRemove;
import com.netflix.hystrix.contrib.javanica.command.ExecutionType;
import com.netflix.hystrix.contrib.javanica.command.MetaHolder;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.Validate;

import java.lang.reflect.Method;

/**
 * Created by gautham.srinivas on 18/11/15.
 */
public class GuiceCacheInterceptor implements MethodInterceptor {
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Object obj = invocation.getThis();
        Object[] args = invocation.getArguments();
        Validate.notNull(method, "failed to get method from invocation: %s", invocation);
        MetaHolder metaHolder = MetaHolder.builder()
                .args(args).method(method).obj(obj)
                .executionType(ExecutionType.SYNCHRONOUS)
                .ajcMethod(null)
                .build();
        CacheInvocationContext<CacheRemove> context = CacheInvocationContextFactory
                .createCacheRemoveInvocationContext(metaHolder);
        HystrixRequestCacheManager.getInstance().clearCache(context);

        return invocation.proceed();
    }
}
