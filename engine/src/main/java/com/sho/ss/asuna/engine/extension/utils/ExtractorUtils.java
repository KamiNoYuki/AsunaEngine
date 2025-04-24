package com.sho.ss.asuna.engine.extension.utils;

import com.sho.ss.asuna.engine.core.selector.CssSelector;
import com.sho.ss.asuna.engine.core.selector.JsonPathSelector;
import com.sho.ss.asuna.engine.core.selector.RegexSelector;
import com.sho.ss.asuna.engine.core.selector.Selector;
import com.sho.ss.asuna.engine.core.selector.XpathSelector;
import com.sho.ss.asuna.engine.extension.model.annotation.ExtractBy;

import java.util.ArrayList;
import java.util.List;

/**
 * Tools for annotation converting. <br>
 *
 * @author code4crafter@gmail.com <br>
 * @since 0.2.1
 */
public class ExtractorUtils {

    public static Selector getSelector(ExtractBy extractBy) {
        String value = extractBy.value();
        Selector selector;
        switch (extractBy.type()) {
            case Css:
                selector = new CssSelector(value);
                break;
            case Regex:
                selector = new RegexSelector(value);
                break;
            case XPath:
                selector = new XpathSelector(value);
                break;
            case JsonPath:
                selector = new JsonPathSelector(value);
                break;
            default:
                selector = new XpathSelector(value);
        }
        return selector;
    }

    public static List<Selector> getSelectors(ExtractBy[] extractBies) {
        List<Selector> selectors = new ArrayList<Selector>();
        if (extractBies == null) {
            return selectors;
        }
        for (ExtractBy extractBy : extractBies) {
            selectors.add(getSelector(extractBy));
        }
        return selectors;
    }
}
