package com.sho.ss.asuna.engine.extension.model.fields;

import com.sho.ss.asuna.engine.extension.model.FieldExtractor;
import com.sho.ss.asuna.engine.extension.model.formatter.ObjectFormatter;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

/**
 * @author ShoTan.
 * @project 启源视频
 * @email 2943343823@qq.com
 * @created 2024/6/11 21:35
 * @description
 **/
public abstract class PageField {
    public abstract boolean operation(Object o, FieldExtractor fieldExtractor) throws IllegalAccessException, InvocationTargetException;

    protected Object convert(String value, ObjectFormatter<?> objectFormatter) {
        try {
            Object format = objectFormatter.format(value);
            System.out.println("String " + value + " is converted to " + format);
//            logger.debug("String {} is converted to {}", value, format);
            return format;
        } catch (Exception e) {
//            logger.error("convert " + value + " to " + objectFormatter.clazz() + " error!", e);
            System.out.println("convert " + value + " to " + objectFormatter.clazz() + " error!" + e.getMessage());
        }
        return null;
    }

    protected void setField(Object o, FieldExtractor fieldExtractor, Object value) throws IllegalAccessException, InvocationTargetException {
        if (value != null) {
            if (fieldExtractor.getSetterMethod() != null)
                fieldExtractor.getSetterMethod().invoke(o, value);
            fieldExtractor.getField().set(o, value);
        }
    }
}
