package com.sho.ss.asuna.engine.extension.model.fields;

import com.sho.ss.asuna.engine.extension.model.FieldExtractor;

import java.lang.reflect.InvocationTargetException;

/**
 * @author ShoTan.
 * @project 启源视频
 * @email 2943343823@qq.com
 * @created 2024/6/11 21:35
 * @description
 **/
public class SingleField extends PageField {
    private final String fieldName;

    public SingleField(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public boolean operation(Object o, FieldExtractor fieldExtractor) throws IllegalAccessException, InvocationTargetException {
        if (fieldExtractor.getObjectFormatter() != null) {
            Object converted = this.convert(this.fieldName, fieldExtractor.getObjectFormatter());
            if (converted == null && fieldExtractor.isNotNull())
                return false;
            setField(o, fieldExtractor, converted);
        } else
            setField(o, fieldExtractor, this.fieldName);
        return true;
    }

    public String getFieldName() {
        return fieldName;
    }
}
