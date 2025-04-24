package com.sho.ss.asuna.engine.extension.model;


import com.sho.ss.asuna.engine.core.selector.Selector;
import com.sho.ss.asuna.engine.extension.model.formatter.ObjectFormatter;
import com.sho.ss.asuna.engine.extension.model.sources.Source;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Wrapper of field and extractor.
 * @author code4crafter@gmail.com <br>
 * @since 0.2.0
 */
public class FieldExtractor extends Extractor {

    private final Field field;

    private Method setterMethod;

    private ObjectFormatter<?> objectFormatter;

    public FieldExtractor(Field field, Selector selector, Source source, boolean notNull, boolean multi) {
        super(selector, source, notNull, multi);
        this.field = field;
    }

    public void setSetterMethod(Method setterMethod) {
        this.setterMethod = setterMethod;
    }

    public void setObjectFormatter(ObjectFormatter<?> objectFormatter) {
        this.objectFormatter = objectFormatter;
    }

    public Field getField() {
        return field;
    }

    public Method getSetterMethod() {
        return setterMethod;
    }

    public ObjectFormatter<?> getObjectFormatter() {
        return objectFormatter;
    }
}
