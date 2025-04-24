package com.sho.ss.asuna.engine.core.selector;

import com.sho.ss.asuna.engine.core.utils.BaseSelectorUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * @author code4crafter@gmail.com
 * @since 0.3.0
 */
public abstract class BaseElementSelector implements Selector, ElementSelector {
    private Document parse(String text) {
        // Jsoup could not parse <tr></tr> or <td></td> tag directly
        // https://stackoverflow.com/questions/63607740/jsoup-couldnt-parse-tr-tag
        text = BaseSelectorUtils.preParse(text);
        return Jsoup.parse(text);
    }

    @Override
    public String select(String text) {
        if (text != null) {
            return select(parse(text));
        }
        return null;
    }

    @Override
    public List<String> selectList(String text) {
        if (text != null) {
            return selectList(parse(text));
        } else {
            return new ArrayList<>();
        }
    }

    public Element selectElement(String text) {
        if (text != null) {
            return selectElement(parse(text));
        }
        return null;
    }

    public List<Element> selectElements(String text) {
        if (text != null) {
            return selectElements(parse(text));
        } else {
            return new ArrayList<>();
        }
    }

    public abstract Element selectElement(Element element);

    public abstract List<Element> selectElements(Element element);

    public abstract boolean hasAttribute();

}