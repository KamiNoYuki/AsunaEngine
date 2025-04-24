package com.sho.ss.asuna.engine.processor.ext;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.processor.base.BaseLeLeVideoExtProcessor;
import com.sho.ss.asuna.engine.processor.common.CommonSecondaryPageProcessor;
import com.sho.ss.asuna.engine.processor.ext.NewVisionExt;
import com.sho.ss.asuna.engine.utils.DecryptUtils;
import com.sho.ss.asuna.engine.utils.Xpath;


/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2023/3/15 17:17:51
 * @description 宫下动漫-https://arlnigdm.com
 **/
public class GongXiaExt extends CommonSecondaryPageProcessor
{
    public GongXiaExt(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener)
    {
        super(entity, videoSource, episode, listener);
    }

    @Override
    protected void handleVideoUrl(@NonNull Page page, @NonNull String videoUrl)
    {
        //该源的链接需要解密
        //获取html页面中解密所需的两个字符串
        String ids = Xpath.select("//meta[@charset='UTF-8']/@id", page.getRawText());
        String texts = Xpath.select("//meta[@name='viewport']/@id", page.getRawText());
        if (whenNullNotifyFail(ids, ErrorFlag.NO_PARSED_DATA, "解密所需id获取失败!") &&
                whenNullNotifyFail(texts, ErrorFlag.NO_PARSED_DATA, "解密所需的text获取失败!") && null != ids && texts != null)
        {
            ids = ids.replace("now_", "");
            texts = texts.replace("now_", "");
            String newText = NewVisionExt.sortById(ids, texts);
            if (whenNullNotifyFail(newText, ErrorFlag.DATA_INVALIDATE, "新的text序列无效!"))
            {
                //需要拼接上该字符串
                newText += "favnow";
                //转md5加密字符串
                String md5 = DecryptUtils.toMd5(newText);
                //解密所需的key
                String key = md5.substring(16);
                //解密所需的iv偏移量
                String iv = md5.substring(0, 16);
                System.out.println("key = " + key + " iv = " + iv);
                String decryptedUrl = BaseLeLeVideoExtProcessor.decryptLeLeVideoUrl(videoUrl, key, iv);
                System.out.println("解密后的视频链接：" + decryptedUrl);
                super.handleVideoUrl(page,decryptedUrl);
            }
        }
    }

}
