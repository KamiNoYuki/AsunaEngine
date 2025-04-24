package com.sho.ss.asuna.engine.core.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.msebera.android.httpclient.Header;

/**
 * @author code4crafter@gmail.com
 *         Date: 17/3/27
 */
public abstract class HttpClientUtils {

    public static Map<String,List<String>> convertHeaders(Header[] headers){
        Map<String,List<String>> results = new HashMap<>();
        for (Header header : headers) {
            List<String> list;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
                list = results.computeIfAbsent(header.getName(), k -> new ArrayList<>());
            else
            {
                list = results.get(header.getName());
                if(null == list)
                {
                    list = new ArrayList<>();
                    results.put(header.getName(), list);
                }
            }
            list.add(header.getValue());
        }
        return results;
    }
}
