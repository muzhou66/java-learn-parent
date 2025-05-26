package com.muzhou.learn.java.basic.reflect;

import java.lang.reflect.*;

public class ReflectApiUtils {

    /**
     * 获取Class对象的几种方式
     */
    public static Class<?> getClassByClassName(String className) throws ClassNotFoundException {
        return Class.forName(className);
    }

    public static Class<?> getClassByObject(Object obj) {
        return obj.getClass();
    }

    public static <T> Class<T> getClassByType(Class<T> clazz) {
        return clazz;
    }

    /**
     * 创建对象实例
     */
    public static Object newInstance(Class<?> clazz) throws InstantiationException, IllegalAccessException {
        return clazz.newInstance();
    }

    public static Object newInstance(Constructor<?> constructor, Object... args)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {
        return constructor.newInstance(args);
    }

    /**
     * 获取构造方法
     */
    public static Constructor<?> getConstructor(Class<?> clazz, Class<?>... paramTypes)
            throws NoSuchMethodException {
        return clazz.getConstructor(paramTypes);
    }

    public static Constructor<?> getDeclaredConstructor(Class<?> clazz, Class<?>... paramTypes)
            throws NoSuchMethodException {
        return clazz.getDeclaredConstructor(paramTypes);
    }

    /**
     * 获取字段
     */
    public static Field getField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        return clazz.getField(fieldName);
    }

    public static Field getDeclaredField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        return clazz.getDeclaredField(fieldName);
    }

    public static Field[] getFields(Class<?> clazz) {
        return clazz.getFields();
    }

    public static Field[] getDeclaredFields(Class<?> clazz) {
        return clazz.getDeclaredFields();
    }

    /**
     * 获取方法
     */
    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... paramTypes)
            throws NoSuchMethodException {
        return clazz.getMethod(methodName, paramTypes);
    }

    public static Method getDeclaredMethod(Class<?> clazz, String methodName, Class<?>... paramTypes)
            throws NoSuchMethodException {
        return clazz.getDeclaredMethod(methodName, paramTypes);
    }

    public static Method[] getMethods(Class<?> clazz) {
        return clazz.getMethods();
    }

    public static Method[] getDeclaredMethods(Class<?> clazz) {
        return clazz.getDeclaredMethods();
    }

    /**
     * 操作字段值
     */
    public static void setFieldValue(Object obj, Field field, Object value)
            throws IllegalAccessException {
        field.setAccessible(true);
        field.set(obj, value);
    }

    public static Object getFieldValue(Object obj, Field field)
            throws IllegalAccessException {
        field.setAccessible(true);
        return field.get(obj);
    }

    /**
     * 调用方法
     */
    public static Object invokeMethod(Object obj, Method method, Object... args)
            throws IllegalAccessException, InvocationTargetException {
        method.setAccessible(true);
        return method.invoke(obj, args);
    }

    /**
     * 操作私有成员
     */
    public static void makeAccessible(AccessibleObject accessibleObject) {
        accessibleObject.setAccessible(true);
    }


}



