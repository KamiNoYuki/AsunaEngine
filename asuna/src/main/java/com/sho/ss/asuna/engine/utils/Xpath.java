package com.sho.ss.asuna.engine.utils;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.sho.ss.asuna.engine.core.selector.Html;

import org.seimicrawler.xpath.JXDocument;
import org.seimicrawler.xpath.JXNode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: Sho tan
 * @created: 2022/4/7
 * @description: 采用XPath进行解析
 */
public class Xpath
{
    public static String select(@NonNull String xpath, @NonNull Html html)
    {
        return select(xpath,html.get());
    }

    public static String select(String xpath,String text)
    {
        if(!TextUtils.isEmpty(xpath) && !TextUtils.isEmpty(text))
        {
            JXNode jxNode = JXDocument.create(text).selNOne(xpath);
            return null != jxNode ? jxNode.asString() : null;
        }
        return null;
    }

    public static List<String> selectList(@NonNull String xpath,@NonNull Html html)
    {
        return selectList(xpath, html.get());
    }

    public static List<String> selectList(String xpath,String text)
    {
        if(!TextUtils.isEmpty(xpath) && !TextUtils.isEmpty(text))
        {
            List<String> list = new ArrayList<>();
            for (JXNode jxNode : JXDocument.create(text).selN(xpath))
            {
                list.add(jxNode.asString());
            }
            return list;
        }
        return null;
    }
}
