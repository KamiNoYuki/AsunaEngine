package com.sho.ss.asuna.engine.processor.base;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sho.ss.asuna.engine.core.selector.Html;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.processor.common.CommonMultiApiExtVideoProcessor;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.interfaces.ParseListener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import cz.msebera.android.httpclient.extras.Base64;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/5/19 22:43:12
 * @description 基于lele播放器解析的源可继承自该处理器进行解析 lele.start()
 **/
public abstract class BaseLeLeVideoExtProcessor extends CommonMultiApiExtVideoProcessor
{
    public BaseLeLeVideoExtProcessor(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener)
    {
        super(entity, videoSource, episode, listener);
    }

    /**
     * LeLe视频链接解密
     *
     * @param cipherStr 密文
     * @param key       16位key，通常在播放器界面主域名/js/player.js文件内的token_key，但也有个别在其他js文件内
     * @param ivStr     iv，通常在播放器界面head标签的script内的一个变量
     * @return 明文
     */
    @Nullable
    public static String decryptLeLeVideoUrl(String cipherStr,boolean base64Dec, String key, String ivStr)
    {
        IvParameterSpec iv = new IvParameterSpec(ivStr.getBytes(StandardCharsets.UTF_8));
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "AES");
        try
        {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
            return new String(cipher.doFinal(base64Dec ? Base64.decode(cipherStr, Base64.DEFAULT) : cipherStr.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        } catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public static String decryptLeLeVideoUrl(String cipherStr, String key, String ivStr)
    {
        return decryptLeLeVideoUrl(cipherStr,true,key,ivStr);
    }

    /**
     * 解析js文件内的解密所需的key
     *
     * @param link          js链接
     * @param parseListener 解析监听器
     */
    public void parseKey(@NonNull String link, @NonNull InnerParseListener<String> parseListener)
    {
        try
        {
            String js = parseHtml(link);
            if (null != js && !isNullStr(js))
            {
                String key = extractKey(js);
                if (null != key && !isNullStr(key))
                    parseListener.done(key);
                else
                    parseListener.fail("The key is null.");
            } else
                parseListener.fail("Failed to parse the JS file!");
        } catch (IOException e)
        {
            e.printStackTrace();
            parseListener.fail(e.getMessage());
        }
    }

    /**
     * 抽取js中key的抽取逻辑
     * @param js key所在的js
     * @return key
     */
    @Nullable
    protected abstract String extractKey(@NonNull String js);

    /**
     * 解析解密所需的iv，如非默认规则，可重写方法修改解析规则
     *
     * @param html iv所在的html
     * @return iv
     */
    @Nullable
    protected abstract String extractIv(@NonNull Html html);

    /**
     * 抽取url播放链接
     * @param js url所在的javascript
     * @return url
     */
    @Nullable
    protected abstract String extractVideoUrl(String js);
}
