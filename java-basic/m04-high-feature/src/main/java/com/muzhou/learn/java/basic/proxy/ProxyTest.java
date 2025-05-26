package com.muzhou.learn.java.basic.proxy;

import java.lang.reflect.Proxy;

public class ProxyTest {
    public static void main(String[] args) {
        Animal pig = new Pig();

        Animal proxy = (Animal) Proxy.newProxyInstance(
                Animal.class.getClassLoader(),
                new Class[]{Animal.class},
                new AnimalInvocationHandler(pig)
        );

        proxy.eat();
    }
}
