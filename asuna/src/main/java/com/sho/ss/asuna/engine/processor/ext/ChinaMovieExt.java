package com.sho.ss.asuna.engine.processor.ext;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.processor.common.CommonSecondaryPageProcessor;
import com.sho.ss.asuna.engine.utils.DecryptUtils;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/7/29 19:55:25
 * @description 中国电影-https://www.zgdy.cn
 **/
public class ChinaMovieExt extends CommonSecondaryPageProcessor
{
    public ChinaMovieExt(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener)
    {
        super(entity, videoSource, episode, listener);
//        addParseTarget(1,getPlayer2Instance());
    }

    /**
     * @deprecated 不要调用该方法，使用{@link #notifyOnCompleted(String)}替代，因为该源的链接需要解密
     */
    @Override
    @Deprecated
    protected void notifyOnCompleted()
    {
        super.notifyOnCompleted();
    }

    @Override
    protected void notifyOnCompleted(@NonNull String videoUrl)
    {
        String url = DecryptUtils.rc4(videoUrl, "202205051426239465", 1);
        System.out.println("解密链接：" + url);
        //解密视频链接后再回调给播放器
        super.notifyOnCompleted(url);
    }

    public static void main(String[] args)
    {
        String url = "pmDOfzWbg/o3JtiWq6T0q4awYwNJb7adL3I5zTkaoaYIVvIOCcnMhLGJzpR/Th5WLG3lSI873x9CsuZc6mm8p/MV7na7CWhpJh+RTVPFquJqIiX/lkMU9Acexjni2UwQX3dhyIjPpuUY3KWa2JTJ9JE0Hr+DvD3DkEWTxrFJvFeMtgS3BiSEBEgVHJeyO7mwrOC0vCT4TEp3AAwmU10cGg==";
        System.out.println("解密结果：" + DecryptUtils.rc4(url, "nzR2", 497));
    }

//    public static String rc4(String data, String key, int t)
//    {
//        String pwd = (key == null) ? "ffsirllq" : key;
//        StringBuilder cipher = new StringBuilder();
//        int[] box = new int[2056];
//        int[] keys = new int[2056];
//        int pwd_length = pwd.length();
//        if (t == 1)
//            data = DecryptUtils.JsBase64Helper.atob(data);
//        else
//        {
//            try
//            {
//                data = URLEncoder.encode(data, StandardCharsets.UTF_8.name());
//            } catch (UnsupportedEncodingException e)
//            {
//                System.out.println(e.getMessage());
//            }
//        }
//        char[] dataChars = data.toCharArray();
//        int data_length = data.length();
//
//        for (int i = 0; i < 256; i++)
//        {
//            keys[i] = pwd.charAt(i % pwd_length);
//            box[i] = i;
//        }
//        for (int j = 0, i = 0; i < 256; i++)
//        {
//            j = (j + box[i] + keys[i]) % 256;
//            int tmp = box[i];
//            box[i] = box[j];
//            box[j] = tmp;
//        }
//        for (int a = 0, j = 0, i = 0; i < data_length; i++)
//        {
//            a = (a + 1) % 256;
//            j = (j + box[a]) % 256;
//            int tmp = box[a];
//            box[a] = box[j];
//            box[j] = tmp;
//            int k = box[((box[a] + box[j]) % 256)];
//            cipher.append((char) (dataChars[i] ^ k));
//        }
//        if (t == 1)
//        {
//            try
//            {
//                return URLDecoder.decode(cipher.toString(), StandardCharsets.UTF_8.name());
//            } catch (UnsupportedEncodingException e)
//            {
//                e.printStackTrace();
//                return cipher.toString();
//            }
//        } else
//            return DecryptUtils.JsBase64Helper.btoa(cipher.toString());
//    }

}
