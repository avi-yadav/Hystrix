package com.netflix.hystrix.contrib.javanica.aop;

import com.google.common.base.Throwables;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCollapser;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.command.ExecutionType;
import com.netflix.hystrix.contrib.javanica.command.MetaHolder;
import com.netflix.hystrix.contrib.javanica.utils.FallbackMethod;
import com.netflix.hystrix.contrib.javanica.utils.MethodProvider;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.ProceedingJoinPoint;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.Future;

import static com.netflix.hystrix.contrib.javanica.utils.AopUtils.getDeclaredMethod;
import static com.netflix.hystrix.contrib.javanica.utils.EnvUtils.isCompileWeaving;
import static com.netflix.hystrix.contrib.javanica.utils.ajc.AjcUtils.getAjcMethodAroundAdvice;

/**
 * Created by gautham.srinivas on 18/11/15.
 */
public class CollapserMetaHolderFactory extends MetaHolderFactory {
    @Override
    public MetaHolder.Builder create(Object proxy, Method collapserMethod, Object obj, Object[] args) {
        HystrixCollapser hystrixCollapser = collapserMethod.getAnnotation(HystrixCollapser.class);
        if (collapserMethod.getParameterTypes().length > 1 || collapserMethod.getParameterTypes().length == 0) {
            throw new IllegalStateException("Collapser method must have one argument: " + collapserMethod);
        }

        Method batchCommandMethod = getDeclaredMethod(obj.getClass(), hystrixCollapser.batchMethod(), List.class);
        if (batchCommandMethod == null || !batchCommandMethod.getReturnType().equals(List.class)) {
            throw new IllegalStateException("required batch method for collapser is absent: "
                    + "(java.util.List) " + obj.getClass().getCanonicalName() + "." +
                    hystrixCollapser.batchMethod() + "(java.util.List)");
        }

        if (!collapserMethod.getParameterTypes()[0]
                .equals(getGenericParameter(batchCommandMethod.getGenericParameterTypes()[0]))) {
            throw new IllegalStateException("required batch method for collapser is absent, wrong generic type: expected"
                    + obj.getClass().getCanonicalName() + "." +
                    hystrixCollapser.batchMethod() + "(java.util.List<" + collapserMethod.getParameterTypes()[0] + ">), but it's " +
                    getGenericParameter(batchCommandMethod.getGenericParameterTypes()[0]));
        }

        Class<?> collapserMethodReturnType;
        if (Future.class.isAssignableFrom(collapserMethod.getReturnType())) {
            collapserMethodReturnType = getGenericParameter(collapserMethod.getGenericReturnType());
        } else {
            collapserMethodReturnType = collapserMethod.getReturnType();
        }

        if (!collapserMethodReturnType
                .equals(getGenericParameter(batchCommandMethod.getGenericReturnType()))) {
            throw new IllegalStateException("Return type of batch method must be java.util.List parametrized with corresponding type: expected " +
                    "(java.util.List<" + collapserMethodReturnType + ">)" + obj.getClass().getCanonicalName() + "." +
                    hystrixCollapser.batchMethod() + "(java.util.List<" + collapserMethod.getParameterTypes()[0] + ">), but it's " +
                    getGenericParameter(batchCommandMethod.getGenericReturnType()));
        }

        HystrixCommand hystrixCommand = batchCommandMethod.getAnnotation(HystrixCommand.class);
        if (hystrixCommand == null) {
            throw new IllegalStateException("batch method must be annotated with HystrixCommand annotation");
        }
        // method of batch hystrix command must be passed to metaholder because basically collapser doesn't have any actions
        // that should be invoked upon intercepted method, its required only for underlying batch command
        MetaHolder.Builder builder = MetaHolder.builder()
                .args(args).method(batchCommandMethod).obj(obj).proxyObj(proxy)
                .defaultGroupKey(obj.getClass().getSimpleName());

        if (isCompileWeaving()) {
            builder.ajcMethod(getAjcMethodAroundAdvice(obj.getClass(), batchCommandMethod.getName(), List.class));
        }

        builder.hystrixCollapser(hystrixCollapser);
        builder.defaultCollapserKey(collapserMethod.getName());
        builder.collapserExecutionType(ExecutionType.getExecutionType(collapserMethod.getReturnType()));

        builder.defaultCommandKey(batchCommandMethod.getName());
        builder.hystrixCommand(hystrixCommand);
        builder.executionType(ExecutionType.getExecutionType(batchCommandMethod.getReturnType()));
        FallbackMethod fallbackMethod = MethodProvider.getInstance().getFallbackMethod(obj.getClass(), batchCommandMethod);
        if (fallbackMethod.isPresent()) {
            fallbackMethod.validateReturnType(batchCommandMethod);
            builder
                    .fallbackMethod(fallbackMethod.getMethod())
                    .fallbackExecutionType(ExecutionType.getExecutionType(fallbackMethod.getMethod().getReturnType()));
        }
        return builder;
    }

    private Class<?> getGenericParameter(Type type) {
        Type tType = ((ParameterizedType) type).getActualTypeArguments()[0];
        String className = tType.toString().split(" ")[1];
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw Throwables.propagate(e);
        }
    }
}
