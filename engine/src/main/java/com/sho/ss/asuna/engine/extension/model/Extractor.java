package com.sho.ss.asuna.engine.extension.model;


import com.sho.ss.asuna.engine.core.selector.Selector;
import com.sho.ss.asuna.engine.extension.model.sources.Source;

/**
 * The object contains 'ExtractBy' information.
 * @author code4crafter@gmail.com <br>
 * @since 0.2.0
 */
public class Extractor {

    protected Selector selector;

    protected final Source source;

    protected final boolean notNull;

    protected final boolean multi;

    public Extractor(Selector selector, Source source, boolean notNull, boolean multi) {
        this.selector = selector;
        this.source = source;
        this.notNull = notNull;
        this.multi = multi;
    }

    public boolean isNotNull() {
        return notNull;
    }

    public boolean isMulti() {
        return multi;
    }

    public Selector getSelector() {
        return selector;
    }

    public void setSelector(Selector selector) {
        this.selector = selector;
    }

    public Source getSource() {
        return source;
    }
}
