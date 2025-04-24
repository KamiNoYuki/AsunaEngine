package com.sho.ss.asuna.engine.extension.model.sources;

import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.extension.model.FieldExtractor;
import com.sho.ss.asuna.engine.extension.model.fields.MultipleField;
import com.sho.ss.asuna.engine.extension.model.fields.PageField;
import com.sho.ss.asuna.engine.extension.model.fields.SingleField;

/**
 * @author ShoTan.
 * @project 启源视频
 * @email 2943343823@qq.com
 * @created 2024/6/11 21:40
 * @description
 **/
public class SourceTextExtractor {
    public static PageField getText(Page page, String html, boolean isRaw, FieldExtractor fieldExtractor) {
        Source source = fieldExtractor.getSource();
        if (fieldExtractor.isMulti())
            return new MultipleField(source.getTextList(page, html, isRaw, fieldExtractor));
        else
            return new SingleField(source.getText(page, html, isRaw, fieldExtractor));
    }
}