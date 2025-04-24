package com.sho.ss.asuna.engine.processor;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.selector.Html;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.PageSource;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.PageParseListener;
import com.sho.ss.asuna.engine.processor.base.BaseProcessor;
import com.sho.ss.asuna.engine.utils.SpiderUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @project: 启源视频
 * @author: Sho Tan.
 * @E-mail: 2943343823@qq.com
 * @created: 2022/5/3 16:29:38
 * @description: 解析Page的processor
 **/
public class SectionPageProcessor extends BaseProcessor<List<PageSource>, PageParseListener>
{

    private final boolean trendingFirst;
    private final boolean trendingFirstBySource;

    public SectionPageProcessor(@NonNull List<PageSource> entity, boolean trendingFirstBySource, boolean trendingFirst, @Nullable PageParseListener listener)
    {
        super(entity, listener);
        this.trendingFirst = trendingFirst;
        this.trendingFirstBySource = trendingFirstBySource;
    }

    @Override
    public void process(Page page)
    {
        if(isRunning()) {
            parsePage(page);
        }
    }

    @Nullable
    protected String getFirst(boolean trendingFirstFromSource,String first, String str,String str2)
    {
        return getFirst(true,trendingFirstFromSource,first,str,str2);
    }

    /**
     * 在非空的情况下按优先级返回first、str,str2
     * @param first 优先返回该字符
     * @param str 优先级第二
     * @param str2 最后返回该值
     * @return str
     */
    protected String getFirst(String first, String str,String str2)
    {
        return getFirst(false,false,first,str,str2);
    }

    @Nullable
    protected String getFirst(boolean openTrendingFirst,boolean trendingFirstFromSource,String first, String str,String str2)
    {
        if((!openTrendingFirst || (trendingFirstBySource ? trendingFirstFromSource : trendingFirst) ) && !isNullStr(first))
            return first;
        else if(!isNullStr(str))
            return str;
        else
            return str2;
    }

    private void parsePage(Page page)
    {
        com.sho.ss.asuna.engine.entity.Page pageInfo = page.getRequest().getExtra("page");
        int index = page.getRequest().getExtra("index");
        boolean trendingFirstFromSource = page.getRequest().getExtra("trendingFirst");
        if (null != pageInfo)
        {
            VideoSource source = entity.get(index).getVideoSource();
            //解析下一页链接
            String nextPageUrl = parseNextPageUrl(page, page.getHtml(), source);
            String list = getFirst(trendingFirstFromSource,entity.get(index).getTrendingList(),entity.get(index).getList(),null != source ? source.getSearchList() : null);
            if (!TextUtils.isEmpty(list))
            {
                List<String> videoListHtml = $All(list, page.getHtml());
                if (null != videoListHtml)
                {
                    final List<Video> videos = new ArrayList<>();
                    String name,subtitle,cover,coverTitle,dtUrlXpath,nextUrl;
                    Video video;
                    for (String item : videoListHtml)
                    {
                        name = $(getFirst(trendingFirstFromSource,entity.get(index).getTrendingName(),entity.get(index).getName(),null != source ? source.getVideoName() : null), item);
                        subtitle = $(getFirst(trendingFirstFromSource,entity.get(index).getTrendingSubtitle(),entity.get(index).getSubtitle(),null != source ? source.getSubtitle() : null), item);
                        cover = $(getFirst(trendingFirstFromSource,entity.get(index).getTrendingCover(),entity.get(index).getCover(),null != source ? source.getVideoCover() : null), item);
                        if(!TextUtils.isEmpty(cover)) {
                            cover = toAbsoluteUrl(page.getUrl().get(), cover);
                        }
                        coverTitle = $(getFirst(trendingFirstFromSource,entity.get(index).getTrendingCoverTitle(),entity.get(index).getCoverTitle(),null != source ? source.getCoverTitle() : null), item);
                        dtUrlXpath = getFirst(trendingFirstFromSource,entity.get(index).getTrendingDtUrl(),entity.get(index).getDtUrl(),null != source ? source.getDtUrl() : null);
                        if (whenNullNotifyFail(dtUrlXpath, ErrorFlag.RULE_MISSING, "详情页解析规则缺失!!"))
                        {
                            nextUrl = $(dtUrlXpath, item);
                            if (null != nextUrl && !isNullStr(nextUrl) && !TextUtils.isEmpty(name))
                            {
                                video = new Video(null, cover, nextUrl,
                                        null, coverTitle, name,
                                        subtitle, null, null,
                                        null,null, source);
                                videos.add(video);
                            }
                            else
                            {
                                notifyOnFailed(ErrorFlag.NO_VIDEO_DETAIL_URL, "解析详情页面链接失败!");
                                return;
                            }
                        }
                    }
                    if (null != listener)
                    {
                        if(!videos.isEmpty())
                            notifyOnCompleted(entity.get(index),pageInfo,nextPageUrl, videos);
                        else
                            notifyOnFailed(ErrorFlag.NO_PARSED_DATA, "未解析到视频数据");
                    }
                } else
                    notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, "解析视频列表失败!");
            } else
                notifyOnFailed(ErrorFlag.RULE_MISSING, "列表规则缺失");
        } else
            notifyOnFailed(ErrorFlag.PAGE_INVALIDATE, "PAGE无效!!");
    }

    protected void notifyOnCompleted(@NonNull PageSource pageSource,@NonNull com.sho.ss.asuna.engine.entity.Page pageInfo,@Nullable String nextPageUrl,@NonNull List<Video> videos)
    {
        if(isRunning()) {
            switchToUIThread(listener, l -> l.onCompleted(pageSource,pageInfo,nextPageUrl,videos));
        }
    }

    /**
     * 解析出下一页链接
     * @param page page
     * @param html html
     * @param source source
     */
    @Nullable
    private String parseNextPageUrl(Page page, Html html,@Nullable VideoSource source)
    {
        com.sho.ss.asuna.engine.entity.Page pageInfo = page.getRequest().getExtra("page");
        int index = page.getRequest().getExtra("index");
        //优先使用Page中的下一页链接规则
        String nextUrlXpath = getFirst(pageInfo.getNextUrl(),entity.get(index).getNextPage(),null != source ? source.getSearchNext() : null);
        //如果有下一页解析规则
        if(!isNullStr(nextUrlXpath))
        {
            //下一页链接
            String nextUrl = $(nextUrlXpath, html);
            if(!TextUtils.isEmpty(nextUrl))
            {
                //相对链接转绝对链接
                nextUrl = SpiderUtils.fixHostIfMissing(nextUrl,getHostByUrl(page.getUrl().get()));
                //下一页链接与当前页链接相同则丢弃。
                nextUrl = TextUtils.equals(page.getUrl().get(), nextUrl) ? null : nextUrl;
                return nextUrl;
            }
        }
        //未解析到链接
        return null;
    }
}
