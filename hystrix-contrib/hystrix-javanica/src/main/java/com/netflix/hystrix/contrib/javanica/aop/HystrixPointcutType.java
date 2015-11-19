package com.netflix.hystrix.contrib.javanica.aop;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

import java.lang.reflect.Method;

/**
 * Created by gautham.srinivas on 18/11/15.
 */
public enum HystrixPointcutType {
    COMMAND,
    COLLAPSER;

    public static HystrixPointcutType of(Method method) {
        return method.isAnnotationPresent(HystrixCommand.class) ? COMMAND : COLLAPSER;
    }
}
