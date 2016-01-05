package com.netflix.hystrix.contrib.javanica.aop.guice;

import com.google.inject.AbstractModule;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCollapser;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.cache.annotation.CacheRemove;

import static com.google.inject.matcher.Matchers.annotatedWith;
import static com.google.inject.matcher.Matchers.any;

/**
 * Created by gautham.srinivas on 18/11/15.
 */
public class InterceptorModule extends AbstractModule{
    @Override
    protected void configure() {

        bindInterceptor(
                any(), annotatedWith(HystrixCommand.class), new GuiceCommandInterceptor()
        );
        bindInterceptor(
                any(), annotatedWith(HystrixCollapser.class), new GuiceCommandInterceptor()
        );
        bindInterceptor(
                any(), annotatedWith(CacheRemove.class), new GuiceCacheInterceptor()
        );
    }
}
