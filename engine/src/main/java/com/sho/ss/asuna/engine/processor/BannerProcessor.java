package com.sho.ss.asuna.engine.processor;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.Site;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Banner;
import com.sho.ss.asuna.engine.entity.BannerSource;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.processor.base.BaseProcessor;
import com.sho.ss.asuna.engine.utils.SpiderUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/9/29 16:09:23
 * @description
 **/
public class BannerProcessor extends BaseProcessor<BannerSource, ParseListener<List<Banner>>> {
    public BannerProcessor(@NonNull BannerSource entity, @Nullable ParseListener<List<Banner>> listener) {
        super(entity, listener);
    }

    @Override
    public void process(Page page) {
        if (whenNullNotifyFail(entity.getList(), ErrorFlag.RULE_MISSING, "Banner列表规则缺失!")) {
            List<String> list = $All(entity.getList(), page.getHtml());
            if (null != list && !list.isEmpty()) {
                List<Banner> banners = new ArrayList<>();
                for (String item : list) {
                    if (whenNullNotifyFail(entity.getBannerUrl(), ErrorFlag.RULE_MISSING, "Banner图片链接规则缺失!")) {
                        String bannerUrl = $(entity.getBannerUrl(), item);
                        //如果banner图片链接未解析到，则不再继续解析其他数据
                        if (!isNullStr(bannerUrl)) {
                            bannerUrl = toAbsoluteUrl(bannerUrl, page);
                            String title = $(entity.getTitle(), item);
                            String dtUrl = $(entity.getDtUrl(), item);
                            dtUrl = toAbsoluteUrl(dtUrl, page);
                            String subtitle = $(entity.getSubtitle(), item);
                            subtitle = applyFilter(subtitle, entity.getSubtitleFilter(), false);
                            if (!TextUtils.isEmpty(subtitle))
                                subtitle = subtitle.trim();
                            banners.add(new Banner(entity.getSource(), bannerUrl, title, subtitle, dtUrl, entity.isSystem() ? Banner.TYPE_SYSTEM : Banner.TYPE_OTHER));
                        }
                    }
                }
                if (null != listener) {
                    if (!banners.isEmpty()) {
                        switchToUIThread(listener, l ->
                                l.onCompleted(banners));
                    } else
                        notifyOnFailed(ErrorFlag.NO_PARSED_DATA, "未解析到Banner数据");
                }
            } else
                notifyOnFailed(ErrorFlag.NO_PARSED_DATA, "未解析到banner列表");
        }
    }

    /**
     * 将相对链接转换为绝对链接
     *
     * @param url  要转换的链接
     * @param page 当前页面
     * @return 绝对链接
     */
    protected String toAbsoluteUrl(String url, Page page) {
        return SpiderUtils.fixHostIfMissing(url, getHostByUrl(page.getUrl().get()));
    }

    @Override
    public Site getSite() {
        return super.getSite().setTimeOut(30_000);
    }
}
