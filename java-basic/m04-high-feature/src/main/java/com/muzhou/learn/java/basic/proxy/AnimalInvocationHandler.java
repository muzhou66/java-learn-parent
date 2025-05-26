package com.muzhou.learn.java.basic.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class AnimalInvocationHandler implements InvocationHandler {

    private Object target;

    public AnimalInvocationHandler(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("method execute before");
        Object result = method.invoke(target, args);
        System.out.println("method execute after");
        return result;
    }
}
