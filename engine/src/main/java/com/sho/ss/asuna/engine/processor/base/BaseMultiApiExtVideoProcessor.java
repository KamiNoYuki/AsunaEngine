package com.sho.ss.asuna.engine.processor.base;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sho.ss.asuna.engine.core.selector.Html;
import com.sho.ss.asuna.engine.core.utils.HttpConstant;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.processor.common.CommonVideoExtProcessor;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.utils.UserAgentLibrary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/5/19 23:22:16
 * @description 需要根据js文件中的api信息请求播放器链接可继承自该处理器解析
 **/
@WorkerThread
public abstract class BaseMultiApiExtVideoProcessor extends CommonVideoExtProcessor
{

    public BaseMultiApiExtVideoProcessor(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener)
    {
        super(entity, videoSource, episode, listener);
    }

    /**
     * Jsoup解析的内容有时会解析不全，因此用流解析
     * @param link 解析链接
     * @return 文件内容
     * @throws IOException e
     */
    @Nullable
    protected String parseHtml(@NonNull String link) throws IOException
    {
        URL url = new URL(link);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.addRequestProperty(HttpConstant.Header.USER_AGENT, new UserAgentLibrary().getProxyUserAgent());
        InputStream is = connection.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(is);
        BufferedReader reader = new BufferedReader(inputStreamReader);
        String buffer;
        StringBuilder builder = new StringBuilder();
        while ((buffer = reader.readLine()) != null)
        {
            builder.append(buffer);
        }
        return builder.toString();
    }

    /**
     * 解析存放有api信息的js文件
     * @param url js链接
     */
    protected void getApiJson(@NonNull String url,@NonNull InnerParseListener<JSONObject> parseListener)
    {
        try
        {
            String js = parseHtml(url);
            if(null != js && !isNullStr(js))
            {
                String apis = extractApiJson(js);
                if(null != apis && !isNullStr(apis))
                    parseListener.done(JSON.parseObject(apis));
                else
                    parseListener.fail("js接口信息抽取失败");
            }
            else
                parseListener.fail("js解析失败");
        } catch (IOException e)
        {
            e.printStackTrace();
            parseListener.fail(e.getMessage());
        }
    }

    /**
     * 抽取js文件中存放接口信息的部分
     * @param js js
     * @return json string.
     */
    @Nullable
    protected abstract String extractApiJson(@NonNull String js);

    /**
     * 抽取播放界面的js标签中存放播放器接口信息的部分
     * @param js js
     * @return Json object.
     */
    @Nullable
    protected abstract JSONObject extractPlayerInfoJs(@NonNull String js);

    @Nullable
    protected JSONObject extractPlayerInfoJs(@NonNull Html html)
    {
        return extractPlayerInfoJs(html.get());
    }

    /**
     * 解析key监听器
     */
    protected interface InnerParseListener<T>
    {
        /**
         * 解析完毕
         */
        void done(@NonNull T t);

        /**
         * 解析失败
         * @param msg 失败信息
         */
        void fail(String msg);
    }
}
