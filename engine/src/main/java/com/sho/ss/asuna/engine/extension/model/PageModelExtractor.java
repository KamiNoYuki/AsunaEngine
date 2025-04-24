package com.sho.ss.asuna.engine.extension.model;


import static com.sho.ss.asuna.engine.extension.model.annotation.ExtractBy.Source.RawText;

import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.selector.AndSelector;
import com.sho.ss.asuna.engine.core.selector.OrSelector;
import com.sho.ss.asuna.engine.core.selector.RegexSelector;
import com.sho.ss.asuna.engine.core.selector.Selector;
import com.sho.ss.asuna.engine.core.selector.XpathSelector;
import com.sho.ss.asuna.engine.extension.model.annotation.ComboExtract;
import com.sho.ss.asuna.engine.extension.model.annotation.ExtractBy;
import com.sho.ss.asuna.engine.extension.model.annotation.ExtractByUrl;
import com.sho.ss.asuna.engine.extension.model.annotation.HelpUrl;
import com.sho.ss.asuna.engine.extension.model.annotation.TargetUrl;
import com.sho.ss.asuna.engine.extension.model.fields.PageField;
import com.sho.ss.asuna.engine.extension.model.formatter.ObjectFormatterBuilder;
import com.sho.ss.asuna.engine.extension.model.sources.Source;
import com.sho.ss.asuna.engine.extension.model.sources.SourceTextExtractor;
import com.sho.ss.asuna.engine.extension.utils.ClassUtils;
import com.sho.ss.asuna.engine.extension.utils.ExtractorUtils;

import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The main internal logic of page model extractor.
 *
 * @author code4crafter@gmail.com <br>
 * @since 0.2.0
 */
class PageModelExtractor {

    private final List<Pattern> targetUrlPatterns = new ArrayList<>();

    private Selector targetUrlRegionSelector;

    private final List<Pattern> helpUrlPatterns = new ArrayList<>();

    private Selector helpUrlRegionSelector;

    private Class clazz;

    private List<FieldExtractor> fieldExtractors;

    public List<Pattern> getTargetUrlPatterns() {
        return targetUrlPatterns;
    }

    public Selector getTargetUrlRegionSelector() {
        return targetUrlRegionSelector;
    }

    public List<Pattern> getHelpUrlPatterns() {
        return helpUrlPatterns;
    }

    public Selector getHelpUrlRegionSelector() {
        return helpUrlRegionSelector;
    }

    public Class getClazz() {
        return clazz;
    }

    private Extractor objectExtractor;

//    private Logger logger = LoggerFactory.getLogger(getClass());

    public static PageModelExtractor create(Class clazz) {
        PageModelExtractor pageModelExtractor = new PageModelExtractor();
        pageModelExtractor.init(clazz);
        return pageModelExtractor;
    }

    private void init(Class clazz) {
        this.clazz = clazz;
        initClassExtractors();
        fieldExtractors = new ArrayList<FieldExtractor>();
        for (Field field : ClassUtils.getFieldsIncludeSuperClass(clazz)) {
            field.setAccessible(true);
            FieldExtractor fieldExtractor = getAnnotationExtractBy(clazz, field);
            FieldExtractor fieldExtractorTmp = getAnnotationExtractCombo(clazz, field);
            if (fieldExtractor != null && fieldExtractorTmp != null) {
                throw new IllegalStateException("Only one of 'ExtractBy ComboExtract ExtractByUrl' can be added to a field!");
            } else if (fieldExtractor == null && fieldExtractorTmp != null) {
                fieldExtractor = fieldExtractorTmp;
            }
            fieldExtractorTmp = getAnnotationExtractByUrl(clazz, field);
            if (fieldExtractor != null && fieldExtractorTmp != null) {
                throw new IllegalStateException("Only one of 'ExtractBy ComboExtract ExtractByUrl' can be added to a field!");
            } else if (fieldExtractor == null && fieldExtractorTmp != null) {
                fieldExtractor = fieldExtractorTmp;
            }
            if (fieldExtractor != null) {
                fieldExtractor.setObjectFormatter(new ObjectFormatterBuilder().setField(field).build());
                fieldExtractors.add(fieldExtractor);
            }
        }
    }

    private FieldExtractor getAnnotationExtractByUrl(Class clazz, Field field) {
        FieldExtractor fieldExtractor = null;
        ExtractByUrl extractByUrl = field.getAnnotation(ExtractByUrl.class);
        if (extractByUrl != null) {
            String regexPattern = extractByUrl.value();
            if (regexPattern.trim().isEmpty()) {
                regexPattern = ".*";
            }
            fieldExtractor = new FieldExtractor(field,
                    new RegexSelector(regexPattern), new Source.Url(), extractByUrl.notNull(),
                    extractByUrl.multi() || List.class.isAssignableFrom(field.getType()));
            Method setterMethod = getSetterMethod(clazz, field);
            if (setterMethod != null) {
                fieldExtractor.setSetterMethod(setterMethod);
            }
        }
        return fieldExtractor;
    }

    private FieldExtractor getAnnotationExtractCombo(Class clazz, Field field) {
        FieldExtractor fieldExtractor = null;
        ComboExtract comboExtract = field.getAnnotation(ComboExtract.class);
        if (comboExtract != null) {
            ExtractBy[] extractBies = comboExtract.value();
            Selector selector;
            switch (comboExtract.op()) {
                case Or:
                    selector = new OrSelector(ExtractorUtils.getSelectors(extractBies));
                    break;
                case And:
                default:
                    selector = new AndSelector(ExtractorUtils.getSelectors(extractBies));
            }
            fieldExtractor = new FieldExtractor(field, selector, comboExtract.source() == ComboExtract.Source.RawHtml ? new Source.RawHtml() : new Source.SelectedHtml(),
                    comboExtract.notNull(), comboExtract.multi() || List.class.isAssignableFrom(field.getType()));
            Method setterMethod = getSetterMethod(clazz, field);
            if (setterMethod != null) {
                fieldExtractor.setSetterMethod(setterMethod);
            }
        }
        return fieldExtractor;
    }

    private FieldExtractor getAnnotationExtractBy(Class clazz, Field field) {
        FieldExtractor fieldExtractor = null;
        ExtractBy extractBy = field.getAnnotation(ExtractBy.class);
        if (extractBy != null) {
            Selector selector = ExtractorUtils.getSelector(extractBy);
            ExtractBy.Source extractSource = extractBy.source();
            if (extractBy.type()== ExtractBy.Type.JsonPath)
                extractSource = RawText;
            Source source;
            switch (extractSource) {
                case RawText:
                    source = new Source.RawText();
                    break;
                case RawHtml:
                    source = new Source.RawHtml();
                    break;
                case SelectedHtml:
                default:
                    source = new Source.SelectedHtml();
            }
            fieldExtractor = new FieldExtractor(field, selector, source,
                    extractBy.notNull(), List.class.isAssignableFrom(field.getType()));
            fieldExtractor.setSetterMethod(getSetterMethod(clazz, field));
        }
        return fieldExtractor;
    }

    public static Method getSetterMethod(Class clazz, Field field) {
        String name = "set" + StringUtils.capitalize(field.getName());
        try {
            Method declaredMethod = clazz.getDeclaredMethod(name, field.getType());
            declaredMethod.setAccessible(true);
            return declaredMethod;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private void initClassExtractors() {
        Annotation annotation = clazz.getAnnotation(TargetUrl.class);
        if (annotation == null) {
            targetUrlPatterns.add(Pattern.compile(".*"));
        } else {
            TargetUrl targetUrl = (TargetUrl) annotation;
            String[] value = targetUrl.value();
            for (String s : value) {
                targetUrlPatterns.add(Pattern.compile(s.replace(".", "\\.").replace("*", "[^\"'#]*")));
            }
            if (!targetUrl.sourceRegion().isEmpty()) {
                targetUrlRegionSelector = new XpathSelector(targetUrl.sourceRegion());
            }
        }
        annotation = clazz.getAnnotation(HelpUrl.class);
        if (annotation != null) {
            HelpUrl helpUrl = (HelpUrl) annotation;
            String[] value = helpUrl.value();
            for (String s : value) {
                helpUrlPatterns.add(Pattern.compile(s.replace(".", "\\.").replace("*", "[^\"'#]*")));
            }
            if (!helpUrl.sourceRegion().isEmpty()) {
                helpUrlRegionSelector = new XpathSelector(helpUrl.sourceRegion());
            }
        }
        annotation = clazz.getAnnotation(ExtractBy.class);
        if (annotation != null) {
            ExtractBy extractBy = (ExtractBy) annotation;
            objectExtractor = new Extractor(new XpathSelector(extractBy.value()), new Source.SelectedHtml(), extractBy.notNull(), extractBy.multi());
        }
    }

    public Object process(Page page) {
        boolean matched = false;
        for (Pattern targetPattern : targetUrlPatterns) {
            if (targetPattern.matcher(page.getUrl().toString()).matches()) {
                matched = true;
            }
        }
        if (!matched) {
            return null;
        }
        if (objectExtractor == null) {
            return processSingle(page, null, true);
        } else {
            if (objectExtractor.multi) {
                List<Object> os = new ArrayList<>();
                List<String> list = objectExtractor.getSelector().selectList(page.getRawText());
                for (String s : list) {
                    Object o = processSingle(page, s, false);
                    if (o != null) {
                        os.add(o);
                    }
                }
                return os;
            } else {
                String select = objectExtractor.getSelector().select(page.getRawText());
                Object o = processSingle(page, select, false);
                return o;
            }
        }
    }

    private Object processSingle(Page page, String html, boolean isRaw) {
        Object o = null;
        try {
            o = clazz.newInstance();
            for (FieldExtractor fieldExtractor : fieldExtractors) {
                PageField field = SourceTextExtractor.getText(page, html, isRaw, fieldExtractor);
                if (!field.operation(o, fieldExtractor))
                    return null;
            }
            if (AfterExtractor.class.isAssignableFrom(clazz))
                ((AfterExtractor) o).afterProcess(page);
        } catch (Exception e) {
            System.out.println("extract fail: " + e.getMessage());
//            logger.error("extract fail", e);
        }
        return o;
    }
}