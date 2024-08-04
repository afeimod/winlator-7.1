package com.ewt45.winlator;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class UtilsReflect {

    public static int getPid(Process process) {
        int pid = -1;
        try {
            Field pidField = process.getClass().getDeclaredField("pid");
            pidField.setAccessible(true);
            pid = pidField.getInt(process);
            pidField.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return pid;
    }

    public static <T,C> T getFieldObject(Class<C> clz, C inst,  String fieldName) {
        try {
            Field field = clz.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object obj =  field.get(inst);
            field.setAccessible(false);
            return (T) obj;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T invokeMethod(Method method, Object obj, Object... args) {
        try {
            method.setAccessible(true);
            Object ret = method.invoke(obj, args);
            method.setAccessible(false);
            return (T) ret;
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    public static Method getMethod(Class<?> clz, String name, Class<?>... parameterTypes) {
        try {
            return clz.getDeclaredMethod(name, parameterTypes);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }


}
