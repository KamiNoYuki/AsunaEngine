package com.sho.ss.asuna.engine.extension.model.fields;

import com.sho.ss.asuna.engine.extension.model.FieldExtractor;
import com.sho.ss.asuna.engine.extension.model.formatter.ObjectFormatter;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ShoTan.
 * @project 启源视频
 * @email 2943343823@qq.com
 * @created 2024/6/11 21:33
 * @description
 **/
public class MultipleField extends PageField {

    private final List<String> fieldNames;

    public MultipleField(List<String> fieldNames) {
        this.fieldNames = fieldNames;
    }

    @Override
    public boolean operation(Object o, FieldExtractor fieldExtractor) throws IllegalAccessException, InvocationTargetException {
        if ((this.fieldNames == null || this.fieldNames.isEmpty()) && fieldExtractor.isNotNull())
            return false;
        if (fieldExtractor.getObjectFormatter() != null && null != fieldNames) {
            List<Object> converted = this.convert(this.fieldNames, fieldExtractor.getObjectFormatter());
            setField(o, fieldExtractor, converted);
        }
        else
            setField(o, fieldExtractor, this.fieldNames);
        return true;
    }

    private List<Object> convert(List<String> values, ObjectFormatter<?> objectFormatter) {
        List<Object> objects = new ArrayList<>();
        for (String value : values) {
            Object converted = this.convert(value, objectFormatter);
            if (converted != null)
                objects.add(converted);
        }
        return objects;
    }

    public List<String> getFieldNames() {
        return fieldNames;
    }
}