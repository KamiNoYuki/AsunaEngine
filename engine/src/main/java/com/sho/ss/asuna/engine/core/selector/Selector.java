package com.sho.ss.asuna.engine.core.selector;

import java.util.List;

/**
 * Selector(extractor) for text.<br>
 *
 * @author code4crafter@gmail.com <br>
 * @since 0.1.0
 */
public interface Selector {

    /**
     * Extract single result in text.<br>
     * If there are more than one result, only the first will be chosen.
     *
     * @param text text
     * @return result
     */
    String select(String text);

    /**
     * Extract all results in text.<br>
     *
     * @param text text
     * @return results
     */
    List<String> selectList(String text);

}
