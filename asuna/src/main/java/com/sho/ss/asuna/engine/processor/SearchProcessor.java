package com.sho.ss.asuna.engine.processor;


import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.Request;
import com.sho.ss.asuna.engine.core.Site;
import com.sho.ss.asuna.engine.core.processor.PageProcessor;
import com.sho.ss.asuna.engine.core.selector.Html;
import com.sho.ss.asuna.engine.constant.EngineConstant;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.NewSearchListener;
import com.sho.ss.asuna.engine.processor.base.BaseProcessor;
import com.sho.ss.asuna.engine.utils.ListUtils;
import com.sho.ss.asuna.engine.utils.MapUtils;
import com.sho.ss.asuna.engine.utils.SpiderUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author Sho Tan.
 */
public class SearchProcessor extends BaseProcessor<List<? extends VideoSource>, NewSearchListener> implements PageProcessor {
    private String keyword;
    /**
     * 全局搜索结果，存放所有源的搜索结果数据
     */
    private final List<Pair<VideoSource, Pair<List<Video>, String>>> allResults = new ArrayList<>();
    /**
     * 存放搜索结果为空的源
     */
    private final List<VideoSource> emptySources = new ArrayList<>();
    /**
     * 存放搜索失败的源以及错误信息
     */
    private final List<Pair<VideoSource, Pair<Integer, String>>> failedSources = new ArrayList<>();
    /**
     * 每个源的搜索结果解析状态管理器
     */
    private final HashMap<VideoSource, ParseState> parseStateManager = new LinkedHashMap<>();

    public SearchProcessor(@NonNull List<VideoSource> sources, String keyword, @NonNull NewSearchListener searchListener) {
        super(sources, searchListener);
        this.keyword = keyword;
    }

    public SearchProcessor(@NonNull VideoSource source, @NonNull NewSearchListener searchListener) {
        super(ListUtils.toList(source), searchListener);
    }

    public String getSearchKeyword() {
        return keyword;
    }

    /**
     * process the page, extract urls to fetch, extract the data and store
     *
     * @param page page
     */
    @Override
    public void process(Page page) {
        if (isRunning()) {
            int id = page.getRequest().getExtra(EngineConstant.REQUEST_SOURCE_EXTRAS_KEY);
            parseSearchListWithXpath(id, page, page.getHtml());
        }
    }

    protected void releaseData() {
        keyword = null;
        allResults.clear();
        emptySources.clear();
        failedSources.clear();
        parseStateManager.clear();
    }

    /**
     * 采用Xpath解析搜索列表
     *
     * @param page page
     * @param html html
     */
    private void parseSearchListWithXpath(int id, Page page, Html html) {
        VideoSource videoSource = entity.get(id);
        if (null != videoSource) {
            try {
                List<String> list = $All(videoSource.getSearchList(), html);
                if (null != list) {
                    if (!list.isEmpty()) {
                        //下一页
                        final String nextPage = SpiderUtils.fixHostIfMissing($(videoSource.getSearchNext(), html), videoSource.getHost());
                        List<Video> videos = new ArrayList<>();
                        //变量声明在for之外，避免引起频繁GC
                        Video video;
                        String cover, videoName, subtitle, coverTitle, category, detailPageUrl;
                        for (String item : list) {
//                            System.out.println("item = " + item);
                            cover = $(videoSource.getVideoCover(), item);
                            //处理相对链接
                            if (null != cover && !isNullStr(cover) && !cover.startsWith("http"))
                                cover = videoSource.getHost() + cover;
                            videoName = $(videoSource.getVideoName(), item);
                            if (null != videoName && !TextUtils.isEmpty(videoName))
                                videoName = videoName.replaceAll("&.*;", "");
//                            System.out.println("videoName = " + videoName);
                            category = $(videoSource.getCategory(), item);
                            subtitle = $(videoSource.getSubtitle(), item);
                            if (!isNullStr(subtitle)) {
                                subtitle = applyFilter(subtitle, videoSource.getSubtitleFilter());
                            }
                            coverTitle = $(videoSource.getCoverTitle(), item);
                            detailPageUrl = $(videoSource.getDtUrl(), item);
                            //源系统基于视频名称工作，视频名称必须不为空。
                            if (null != detailPageUrl && !isNullStr(detailPageUrl) && !TextUtils.isEmpty(videoName)) {
                                video = new Video(null, cover, detailPageUrl,
                                        category, coverTitle, videoName,
                                        subtitle, null, null,
                                        null, null, videoSource);
                                videos.add(video);
                            }
                        }
                        if (!videos.isEmpty()) {
                            //某些网页的末页也有下一页，但是下一页是指向当前页的，因此判断一下，避免重复循环下一页
                            String mNextUrl = TextUtils.equals(page.getUrl().get(), nextPage) ? null : nextPage;
                            final Pair<List<Video>, String> result = new Pair<>(videos, mNextUrl);
                            switchToUIThread(listener, l ->
                                    l.onSingleSourceCompleted(videoSource, result));
                            allResults.add(new Pair<>(videoSource, result));
                            addParseState(videoSource, ParseState.COMPLETED);
                        } else
                            notifyOnSingleSourceEmpty(videoSource);
                    } else
                        notifyOnSingleSourceEmpty(videoSource);
                } else {
                    String msg = "";
                    if (isNullStr(html.get()))
                        msg += "搜索响应数据为空!";
                    notifySingleSourceFailed(page.getRequest(), null, videoSource, ErrorFlag.RULE_MISSING, msg);
                }
            } catch (Exception e) {
                e.printStackTrace();
                String msg = e.getMessage() == null ? "解析搜索结果时出错!" : e.getMessage();
                notifySingleSourceFailed(page.getRequest(), e, videoSource, ErrorFlag.EXCEPTION_WHEN_PARSING, msg);
            }
            doCheckState();
        } else {
            switchToUIThread(listener, l -> l.onInitFailed(new Pair<>(ErrorFlag.CONFIG_SOURCE_INVALIDATE, "解析时源为空!")));
        }
    }

    /**
     * 仅在单个源解析完毕或失败时回调，检测已执行源的状态和进度
     */
    public void doCheckState() {
        calcProgress();
        checkParseState();
    }

    /**
     * 计算搜索进度百分比并回调
     */
    protected void calcProgress() {
        switchToUIThread(listener, l ->
        {
            //当前进度
            int soFarProgress = parseStateManager.size();
            int total = entity.size();
            int percentage = soFarProgress / total * 100;
            l.onSearchingProgress(percentage, soFarProgress, total);
        });
    }

    /**
     * 校验搜索任务是否执行完毕
     */
    protected void checkParseState() {
        //如果全部任务执行完毕
        if (parseStateManager.size() >= entity.size()) {
            //加载失败的源数量
            AtomicInteger failedCount = new AtomicInteger(0);
            //加载完毕且有数据的源数量
            AtomicInteger completedCount = new AtomicInteger(0);
            //加载完毕但没有数据的源数量
            AtomicInteger emptyCount = new AtomicInteger(0);
            //校验各个源的加载情况
            MapUtils.proxy(parseStateManager, (parsedSource, parseState) ->
            {
                switch (parseState) {
                    case COMPLETED:
                        completedCount.incrementAndGet();
                        break;
                    case EMPTY:
                        emptyCount.incrementAndGet();
                        break;
                    case FAILED:
                        failedCount.incrementAndGet();
                        break;
                }
            });
            switchToUIThread(listener, l ->
            {
                if (failedCount.get() == entity.size())
                    l.onAllFailed(failedSources);
                else if (emptyCount.get() == entity.size())
                    l.onAllEmpty(emptySources);
                else if (completedCount.get() == entity.size())
                    l.onAllCompleted(allResults);
                l.onSearchTaskCompleted(allResults, emptySources, failedSources);
            });
        }
    }

    public void addParseState(@NonNull VideoSource videoSource, @NonNull ParseState state) {
        System.out.println("源[" + videoSource.getName() + "]解析完毕：解析状态 = " + state);
        parseStateManager.put(videoSource, state);
    }

    protected void notifyOnSingleSourceEmpty(@NonNull VideoSource videoSource) {
        switchToUIThread(listener, l -> l.onSingleSourceEmpty(videoSource));
        emptySources.add(videoSource);
        addParseState(videoSource, ParseState.EMPTY);
    }

    public void notifySingleSourceFailed(@Nullable Request request, @Nullable Exception e, @NonNull VideoSource videoSource, int errFlag, @NonNull String errMsg) {
        final Pair<Integer, String> errInfo = new Pair<>(errFlag, errMsg);
        switchToUIThread(listener, l -> l.onSingleSourceFailed(new Pair<>(request, e), videoSource, errInfo));
        failedSources.add(new Pair<>(videoSource, errInfo));
        addParseState(videoSource, ParseState.FAILED);
    }

    @Override
    public Site getSite() {
        return Site.me()
                .setTimeOut(15_000)//15秒超时
                .setRetryTimes(2);
    }

    /**
     * 源的搜索解析状态
     * 分别有：解析完成且有数据、解析完成但无数据，解析失败
     */
    public enum ParseState {
        COMPLETED, EMPTY, FAILED
    }
}
