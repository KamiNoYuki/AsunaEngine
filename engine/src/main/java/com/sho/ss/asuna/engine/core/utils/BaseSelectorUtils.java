package com.sho.ss.asuna.engine.core.utils;

/**
 * @author hooy
 * @project 启源视频
 * @email 2943343823@qq.com
 * @created 2024/6/11 17:51
 * @description
 **/
public class BaseSelectorUtils {

    /**
     * Jsoup/HtmlCleaner could not parse "tr" or "td" tag directly
     * <a href="https://stackoverflow.com/questions/63607740/jsoup-couldnt-parse-tr-tag">related questions</a>
     *
     * @param text - the html string
     * @return text
     */
    public static String preParse(String text) {
        if (((text.startsWith("<tr>") || text.startsWith("<tr ")) && text.endsWith("</tr>"))
                || ((text.startsWith("<td>") || text.startsWith("<td ")) && text.endsWith("</td>"))) {
            text = "<table>" + text + "</table>";
        }
        return text;
    }
}