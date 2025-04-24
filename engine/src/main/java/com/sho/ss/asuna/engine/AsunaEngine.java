package com.sho.ss.asuna.engine;



import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.sho.ss.asuna.engine.core.Request;
import com.sho.ss.asuna.engine.core.Spider;
import com.sho.ss.asuna.engine.core.SpiderListener;
import com.sho.ss.asuna.engine.core.model.HttpRequestBody;
import com.sho.ss.asuna.engine.core.scheduler.PriorityScheduler;
import com.sho.ss.asuna.engine.core.scheduler.QueueScheduler;
import com.sho.ss.asuna.engine.core.utils.HttpConstant;
import com.sho.ss.asuna.engine.base.BaseSearchSpiderListener;
import com.sho.ss.asuna.engine.constant.EngineConstant;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.constant.SourceType;
import com.sho.ss.asuna.engine.entity.Banner;
import com.sho.ss.asuna.engine.entity.BannerSource;
import com.sho.ss.asuna.engine.entity.CategorySource;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Node;
import com.sho.ss.asuna.engine.entity.Page;
import com.sho.ss.asuna.engine.entity.PageSource;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.CategoryParseListener;
import com.sho.ss.asuna.engine.interfaces.DetailInfoParseListener;
import com.sho.ss.asuna.engine.interfaces.IEngine;
import com.sho.ss.asuna.engine.interfaces.NewSearchListener;
import com.sho.ss.asuna.engine.interfaces.PageParseListener;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.interfaces.StopParseBatchListener;
import com.sho.ss.asuna.engine.interfaces.UISwitcherCallback;
import com.sho.ss.asuna.engine.interfaces.EpisodeParseBatchListener;
import com.sho.ss.asuna.engine.processor.BannerProcessor;
import com.sho.ss.asuna.engine.processor.CategoryProcessor;
import com.sho.ss.asuna.engine.processor.DetailProcessor;
import com.sho.ss.asuna.engine.processor.ExtVideoBatchProcessor;
import com.sho.ss.asuna.engine.processor.SearchProcessor;
import com.sho.ss.asuna.engine.processor.SectionPageProcessor;
import com.sho.ss.asuna.engine.processor.VideoProcessor;
import com.sho.ss.asuna.engine.processor.base.BaseProcessor;
import com.sho.ss.asuna.engine.processor.base.BaseVideoExtensionProcessor;
import com.sho.ss.asuna.engine.processor.common.CommonSecondaryPageProcessor;
import com.sho.ss.asuna.engine.utils.ListUtils;
import com.sho.ss.asuna.engine.utils.MapUtils;
import com.sho.ss.asuna.engine.utils.SpiderUtils;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author Sho tan
 */
public class AsunaEngine implements IEngine<Video> {
    @Deprecated
    @Nullable
    private List<VideoSource> sources;
    //负责搜索请求的爬虫
    private Spider searchSpider;
    //负责解析搜索结果中每个视频相关信息页面爬虫
    private Spider detailSpider;
    //解析视频播放链接的爬虫
    private Spider extVideoSpider;
    //批量解析剧集的爬虫
    private Spider extVideoBatchSpider;
    private Spider pageSpider;
    //分类爬虫
    private Spider categorySpider;
    //搜索处理器
    private SearchProcessor searchProcessor;
    //视频相关页面解析处理器
    private DetailProcessor detailProcessor;
    //视频解析批处理器，批量解析剧集视频链接
    private ExtVideoBatchProcessor extVideoBatchProcessor;
    //视频解析处理器
    private BaseVideoExtensionProcessor<? extends VideoSource, ? extends Episode> extVideoProcessor;
    //Page解析处理器
    private SectionPageProcessor pageProcessor;


    public boolean stopSearchProcessor() {
        return stopTargetProcessor(searchProcessor, searchSpider);
    }

    public boolean stopDetailProcessor() {
        return stopTargetProcessor(detailProcessor, detailSpider);
    }

    public boolean stopExtVideoProcessor() {
        return stopTargetProcessor(extVideoProcessor, extVideoSpider);
    }

    public boolean stopVideoBatchProcessor() {
        return stopTargetProcessor(extVideoBatchProcessor, extVideoBatchSpider);
    }

    public boolean stopPageProcessor() {
        return stopTargetProcessor(pageProcessor, pageSpider);
    }

    public boolean stopCategoryProcessor() {
        return stopTargetProcessor(null, categorySpider);
    }

    private boolean stopTargetProcessor(BaseProcessor<?, ?> processor, Spider spider) {
        if(null != spider) {
            if(spider.getStatus() != Spider.Status.Stopped) {
                spider.stop();
            }
            if(null != processor) {
                processor.setIsRunning(false);
            }
            return true;
        }
        return false;
    }

    protected List<SpiderListener> toSearchSpiderListener(@NonNull List<BaseSearchSpiderListener> searchSpiderListeners) {
        return new ArrayList<>(searchSpiderListeners);
    }

    /**
     * 解析搜索下一页
     *
     * @param nextPage       下一页链接
     * @param source         该搜索对应得到搜索源
     * @param searchListener 监听器
     */
    @Override
    public void searchNextPage(@NonNull String nextPage, @NonNull VideoSource source, @NonNull NewSearchListener searchListener) {
        //检查nextPage链接是否绝对链接，如果不是则补全
        SpiderUtils.fixHostIfMissing(nextPage, source.getHost());
        //转换为Request
        Request request = new Request();
        if (source.isTranscoding()) {
            request.setUrl(SpiderUtils.encodingUrlPath(nextPage));
        } else {
            request.setUrl(nextPage);
        }
        //应用请求方式
        SpiderUtils.applyMethod(request, source.getSearchNextMD());
        String ua = TextUtils.isEmpty(source.getSearchNextUA()) ? source.getSearchUA() : source.getSearchNextUA();
        if (TextUtils.isEmpty(ua)) ua = source.getUserAgent();
        SpiderUtils.addUserAgent(request, ua, true);
        SpiderUtils.initRequest(request, null, null, source.getSearchNextCK(), source.getSearchNextHD());
        SpiderUtils.addReferer(source, request, source.getSearchNextReferer(), true);
        //id
        request.putExtra(EngineConstant.REQUEST_SOURCE_EXTRAS_KEY, 0);
        //创建爬虫
        searchProcessor = new SearchProcessor(source, searchListener);
        searchSpider = SpiderUtils.buildSpider(searchProcessor, request, 3);
        if (null != searchSpider) {
            //监听器
            SpiderUtils.addListenerForSpider(searchSpider, new SpiderListener() {
                @Override
                public void onError(Request request, Exception e) {
                    notifyOnRequestFailed(true, source, request, e, searchListener);
                }
            });
            //异步执行
            searchSpider.runAsync();
            System.out.println("执行下一页搜索[" + request + "]");
        } else {
            runOnUiThread(searchListener,
                    l -> l.onNextPageInitFailed(new Pair<>(ErrorFlag.INIT_ENGINE_EXCEPTION, "解析下一页时引擎初始化失败!")));
            System.out.println("搜索下一页时初始化Spider出错");
        }
    }

    protected void notifyOnRequestFailed(@NonNull VideoSource source, Request request, Exception e, @NonNull NewSearchListener listener) {
        notifyOnRequestFailed(false, source, request, e, listener);
    }

    protected void notifyOnRequestFailed(boolean isNextPage, @NonNull VideoSource source, Request request, Exception e, @NonNull NewSearchListener listener) {
        String errMsg = "源[" + source.getName() + "]" + (isNextPage ? "请求下一页" : "发送搜索") + "请求时出错。";
        System.out.println(errMsg);
        if (null != e) errMsg += "\n" + e.getMessage();
        if (!isNextPage) {
            if (null != searchProcessor) {
                searchProcessor.notifySingleSourceFailed(request, e, source, ErrorFlag.ERROR_ON_REQUEST, errMsg);
                searchProcessor.doCheckState();
            }
        } else {
            final String msg = errMsg;
            runOnUiThread(listener, l ->
            {
                listener.onNextPageFailed(new Pair<>(request, e), source, new Pair<>(ErrorFlag.EXCEPTION_WHEN_PARSING, msg));
            });
        }
    }

    /**
     * 执行搜索
     *
     * @param keyword        搜索关键词
     * @param searchListener 搜索监听
     */
    @Override
    public void search(List<VideoSource> sources, @NonNull String keyword, @NonNull NewSearchListener searchListener) {
        searchListener.onStarted(keyword);
        if (null != sources && !sources.isEmpty()) {
            //初始化Request队列
            List<Request> requests = new ArrayList<>();
            //监听器队列
            List<SpiderListener> spiderListeners = new ArrayList<>();
            //启用的源列表
            List<VideoSource> enabledSources = new ArrayList<>();
            AtomicInteger id = new AtomicInteger();
            //初始化搜索监听拦截器
//            SearchInterceptor searchInterceptor = new SearchInterceptor(searchListener);
            @Nullable
            Request searchRequest;
            @Nullable
            SpiderListener spiderListener;
            //为每一个源生成对应请求和爬虫监听器
            for (VideoSource source : sources) {
                //跳过未启用的源
                if (!source.isEnable()) continue;
                String url = source.getHost() + source.getSearchApi();
                //GET请求方式与其他请求不同，需单独设置参数。
                //如果SearchMD为空，默认为GET请求
                String method = source.getSearchMD();
                if (TextUtils.isEmpty(method) || method.equalsIgnoreCase(HttpConstant.Method.GET))
                    url = SpiderUtils.buildSearchGetUrl(keyword, source);
                else if (url.contains("{kw}"))
                    url = SpiderUtils.applyKeywordByPlaceholder(url, keyword, source.isTranscoding());
                //TODO: 此处应当添加else分支，在搜索api为空时回调失败
                if (!TextUtils.isEmpty(url)) {
                    //初始化搜索请求
                    searchRequest = new Request(url);
                    //请求优先级权重分配,高质量并且无广告的源优先执行请求
                    //有广告的源搜索优先级权重-1
                    long priority = source.getQuality() + (source.isHasAd() ? -1 : 0);
                    searchRequest.setPriority(priority);
                    //应用请求方式
                    SpiderUtils.applyMethod(searchRequest, source.getSearchMD());
                    SpiderUtils.initRequest(searchRequest, source.getUserAgent(), null, source.getSearchCK(), source.getSearchHD());
                    SpiderUtils.addReferer(source, searchRequest, source.getReferer(), true);
                    SpiderUtils.addRequestParamsForKeyword(keyword, source.isTranscoding(), searchRequest, source.getSearchPM());
                    requests.add(searchRequest);
                    enabledSources.add(source);
                    spiderListener = new SpiderListener() {
                        @Override
                        public void onError(Request request, Exception e) {
                            notifyOnRequestFailed(source, request, e, searchListener);
                        }
                    };
                    //记录该Request对应的监听器下标
                    searchRequest.putExtra(Request.SPIDER_LISTENER_INDEX, id.get());
                    spiderListeners.add(spiderListener);
                    //在Request中为Source添加一个ID(下标)，以标识对应源
                    searchRequest.putExtra(EngineConstant.REQUEST_SOURCE_EXTRAS_KEY, id.getAndIncrement());
                    System.out.println("SEARCH REQUEST [" + searchRequest + "]");
                }
            }
            if (!requests.isEmpty()) {
                //创建爬虫
                searchProcessor = new SearchProcessor(enabledSources, keyword, searchListener);
                searchSpider = SpiderUtils.buildSpider(searchProcessor, requests, 5);
                if (null != searchSpider) {
                    //设置优先级调度器
                    searchSpider.setScheduler(new PriorityScheduler());
                    //监听器
                    searchSpider.setSpiderListeners(spiderListeners);
                    //异步执行
                    searchSpider.runAsync();
                    System.out.println("搜索请求数量[" + requests.size() + "]");
                } else {
                    searchListener.onSearchingProgress(100, 1, 1);
                    notifyOnInitFailed(searchListener, ErrorFlag.CONFIG_SOURCE_NO_ENABLE, "初始化引擎失败");
                }
            } else {
                searchListener.onSearchingProgress(100, 1, 1);
                notifyOnInitFailed(searchListener, ErrorFlag.CONFIG_SOURCE_NO_ENABLE, "没有已启用搜索的源");
            }
        } else {
            searchListener.onSearchingProgress(100, 1, 1);
            notifyOnInitFailed(searchListener, ErrorFlag.CONFIG_SOURCE_INVALIDATE, "没有可用的搜索源");
        }
    }

    private void notifyOnInitFailed(@NotNull NewSearchListener listener, int errFlag, String msg) {
        runOnUiThread(listener, l -> l.onInitFailed(new Pair<>(errFlag, msg)));
    }

    /**
     * 解析视频的相关详情页面信息
     *
     * @param video                   待解析的video
     * @param detailInfoParseListener 解析结果监听
     */
    @Override
    public void parseDetailInfo(@NonNull Video video, @Nullable DetailInfoParseListener detailInfoParseListener) {
        notifyOnStarted(detailInfoParseListener);
        String detailUrl = video.getDetailUrl();
        if (!TextUtils.isEmpty(detailUrl)) {
            VideoSource source = video.getVideoSource();
            if(null != source) {
                if (!detailUrl.startsWith("http"))
                    detailUrl = source.getHost() + detailUrl;
                Request parseRequest = new Request(detailUrl);
                parseRequest.putExtra(Request.SPIDER_LISTENER_INDEX, 0);
                String userAgent = SpiderUtils.checkUserAgent(source.getDtUrlUA(), source);
                SpiderUtils.initRequest(
                        parseRequest,
                        userAgent,
                        null,
                        source.getDtUrlCK(),
                        source.getDtUrlHD());
                SpiderUtils.addReferer(source, parseRequest, source.getDtUrlReferer(), true);
                SpiderUtils.applyMethod(parseRequest, source.getDtUrlMD());
                if (!parseRequest.getMethod().equalsIgnoreCase(HttpConstant.Method.GET))
                    SpiderUtils.buildRequestParams(parseRequest, source.getDtUrlPM());
                detailProcessor = new DetailProcessor(video, detailInfoParseListener);
                detailSpider = SpiderUtils.buildSpider(detailProcessor, parseRequest, 1);
                if (null != detailSpider) {
                    SpiderUtils.addListenerForSpider(detailSpider, new SpiderListener() {
                        @Override
                        public void onError(Request request, Exception e) {
                            e.printStackTrace();
                            notifyOnError(detailInfoParseListener, ErrorFlag.ERROR_ON_REQUEST, "视频信息加载失败!\n" + e.getMessage());
                        }
                    });
                    detailSpider.runAsync();
                } else
                    notifyOnError(detailInfoParseListener, ErrorFlag.EXCEPTION_WHEN_PARSING, "初始化失败!");
            } else notifyOnError(detailInfoParseListener, ErrorFlag.CONFIG_SOURCE_INVALIDATE, "视频源无效");
        } else
            notifyOnError(detailInfoParseListener, ErrorFlag.NO_VIDEO_DETAIL_URL, "详情页链接无效");
    }

    @Override
    public void parseCategory(VideoSource videoSource, CategorySource categorySource, String categoryUrl, @NonNull CategoryParseListener parseListener) {
        parseListener.onStarted();
        if (null != videoSource) {
            if (null != categorySource) {
                if (!TextUtils.isEmpty(categoryUrl)) {
                    final CategorySource common = videoSource.getCommonCategory();
                    final boolean commonAvailable = common != null;
                    //相对转绝对链接
                    categoryUrl = SpiderUtils.fixHostIfMissing(categoryUrl, videoSource.getHost());
                    Request request = new Request(categoryUrl);
                    Map<String, String> ck = commonAvailable ? SpiderUtils.getNotNullConfig(categorySource.getCategoryUrlCk(), common.getCategoryUrlCk()) : categorySource.getCategoryUrlCk();
                    Map<String, String> hd = commonAvailable ? SpiderUtils.getNotNullConfig(categorySource.getCategoryUrlHd(), common.getCategoryUrlHd()) : categorySource.getCategoryUrlHd();
                    SpiderUtils.initRequest(request, null, null, ck, hd);
                    String referer = commonAvailable ? SpiderUtils.getNotNullConfig(categorySource.getCategoryUrlReferer(), common.getCategoryUrlReferer()) : categorySource.getCategoryUrlReferer();
                    SpiderUtils.addReferer(videoSource, request, referer, true);
                    String ua = commonAvailable ? SpiderUtils.getNotNullConfig(categorySource.getCategoryUrlUa(), common.getCategoryUrlUa()) : categorySource.getCategoryUrlUa();
                    SpiderUtils.addUserAgent(request, ua, true);
                    String md = commonAvailable ? SpiderUtils.getNotNullConfig(categorySource.getCategoryUrlMd(), common.getCategoryUrlMd()) : categorySource.getCategoryUrlMd();
                    SpiderUtils.applyMethod(request, md);
                    Map<String, String> pm = commonAvailable ? SpiderUtils.getNotNullConfig(categorySource.getCategoryUrlPm(), common.getCategoryUrlPm()) : categorySource.getCategoryUrlPm();
                    if (null != pm)
                        request.setRequestBody(HttpRequestBody.form(MapUtils.convertMapToRequestParams(pm), StandardCharsets.UTF_8.name()));
                    CategoryProcessor categoryProcessor = new CategoryProcessor(videoSource, categorySource, parseListener);
                    SpiderListener listener = new SpiderListener() {
                        @Override
                        public void onError(Request request, Exception e) {
                            notifyOnError(parseListener, ErrorFlag.ERROR_ON_REQUEST, "分类解析请求错误!");
                            System.out.println("分类解析请求错误!\nrequest -> " + request + (null != e ? "\nexception -> " + e.getMessage() : ""));
                            if (null != e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    categorySpider = SpiderUtils.buildSpider(categoryProcessor, request, 2);
                    if (null != categorySpider) {
                        SpiderUtils.addListenerForSpider(categorySpider, listener);
                        categorySpider.runAsync();
                    } else
                        notifyOnError(parseListener, ErrorFlag.EXCEPTION_WHEN_PARSING, "初始化失败!");
                } else
                    notifyOnError(parseListener, ErrorFlag.API_MISSING, "分类页链接为空!");
            } else
                notifyOnError(parseListener, ErrorFlag.CATEGORY_SOURCE_IS_EMPTY, "未找到可用的分类源!");
        } else
            notifyOnError(parseListener, ErrorFlag.CONFIG_SOURCE_INVALIDATE, "视频源为空!");
    }

    @Override
    public void parseCategory(@NotNull VideoSource videoSource, CategorySource categorySource, @NotNull CategoryParseListener parseListener) {
        try {
            parseCategory(videoSource, categorySource, categorySource.getCategoryUrl(), parseListener);
        } catch (Exception e) {
            e.printStackTrace();
            notifyOnError(parseListener, ErrorFlag.EXCEPTION_WHEN_PARSING, e.getMessage());
        }
    }

    @Override
    public void parseCategory(@NotNull VideoSource videoSource, int categoryIndex, String categoryUrl, @NotNull CategoryParseListener parseListener) {
        try {
            CategorySource categorySource = videoSource.getCategoryConfig() != null ? videoSource.getCategoryConfig().get(categoryIndex) : null;
            parseCategory(videoSource, categorySource, categorySource != null ? categorySource.getCategoryUrl() : null, parseListener);
        } catch (Exception e) {
            e.printStackTrace();
            notifyOnError(parseListener, ErrorFlag.EXCEPTION_WHEN_PARSING, e.getMessage());
        }
    }

    @Override
    public void parseCategory(@NonNull VideoSource videoSource, @IntRange(from = 0) int categoryIndex, @NonNull CategoryParseListener parseListener) {
        List<CategorySource> categoryConfig = videoSource.getCategoryConfig();
        if (null != categoryConfig && !categoryConfig.isEmpty()) {
            if (categoryIndex >= 0) {
                if (categoryIndex < categoryConfig.size()) {
                    final CategorySource categorySource = categoryConfig.get(categoryIndex);
                    parseCategory(videoSource, categorySource, parseListener);
                } else
                    notifyOnError(parseListener, ErrorFlag.EXCEPTION_WHEN_PARSING, "分类源索引越界");
            } else
                notifyOnError(parseListener, ErrorFlag.EXCEPTION_WHEN_PARSING, "非法的分类源索引");
        } else
            notifyOnError(parseListener, ErrorFlag.CATEGORY_SOURCE_IS_EMPTY, "未找到可用的分类源");
    }

    public void stopVideoParseBatch(@NotNull Video video, @IntRange(from = 0) int nodePos, @NotNull StopParseBatchListener stopListener, @NotNull Episode... episodes) {
        try {
            if(null != extVideoBatchSpider && null != extVideoBatchProcessor) {
                var scheduler = (QueueScheduler) extVideoBatchSpider.getScheduler();
                var requests = scheduler.getAllRequests();
                if(null != requests && !requests.isEmpty()) {
                    for (Episode episode : episodes) {
                        if(extVideoBatchProcessor.isExists(video, nodePos, episode)) {
                            var result = ListUtils.find(requests, request ->
                                    TextUtils.equals(episode.getUrl(), request.getUrl()));
                            if(null != result) {
                                scheduler.remove(result);
                                extVideoBatchProcessor.removeTargetEpisode(video, nodePos, episode);
                                stopListener.onStopped(video, nodePos, episode);
                                continue;
                            }
                        }
                        stopListener.onStopFailed(video, nodePos, episode);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("尝试停止批处理任务失败：" + e.getMessage());
        }
    }

    /**
     * 批量解析视频播放链接
     *
     * @param video    待解析的视频
     * @param episodes 待解析的剧集列表
     * @param listener listener
     */
    public void parseVideoUrlBatch(
            @NotNull Video video,
            @IntRange(from = 0) int nodeIndex,
            @NotNull List<Episode> episodes,
            @NotNull EpisodeParseBatchListener listener
    ) {
        listener.onStartBatch(video, nodeIndex, episodes);
        if(null == extVideoBatchProcessor) {
            extVideoBatchProcessor = new ExtVideoBatchProcessor(listener);
        }
        extVideoBatchProcessor.setListener(listener);
        final VideoSource source = video.getVideoSource();
        if(null != source) {
            final String host = source.getHost();
            if(!TextUtils.isEmpty(host)) {
                if(!episodes.isEmpty()) {
                    final List<Request> requests = new ArrayList<>();
                    for(int i = 0; i < episodes.size(); i++) {
                        final Episode ep = episodes.get(i);
                        //该待解析的剧集不存在于待解析序列中才加入解析请求，避免重复解析
                        if(!extVideoBatchProcessor.isExists(video, nodeIndex, ep)) {
                            String epUrl = ep.getUrl();
                            if(!TextUtils.isEmpty(epUrl)) {
                                epUrl = SpiderUtils.fixHostIfMissing(epUrl, host);
                                final Request request = buildRequestForVideoParse(epUrl, source, i);
                                request.putExtra(ExtVideoBatchProcessor.VIDEO_NAME_EXTRAS_KEY, video.getVideoName());
                                request.putExtra(ExtVideoBatchProcessor.SOURCE_NAME_EXTRAS_KEY, source.getName());
                                request.putExtra(ExtVideoBatchProcessor.NODE_INDEX_EXTRAS_KEY, nodeIndex);
                                final var epList = extVideoBatchProcessor.getTargetEpList(video, nodeIndex);
                                var epIndex = i;
                                if(null != epList && !epList.isEmpty()) {
                                    epIndex += epList.size();
                                }
                                request.putExtra(ExtVideoBatchProcessor.EP_INDEX_EXTRAS_KEY, epIndex);
                                requests.add(request);
                            } else listener.onTargetError(video, nodeIndex, ep, ErrorFlag.EPISODE_URL_INVALIDATE, "剧集链接无效");
                        } else {
                            listener.onTargetError(video, nodeIndex, ep, ErrorFlag.DATA_INVALIDATE, "目标剧集已解析");
                        }
                    }
                    extVideoBatchProcessor.addParseBatch(video, nodeIndex, episodes);
                    if(null == extVideoBatchSpider) {
                        extVideoBatchSpider = SpiderUtils.buildSpider(extVideoBatchProcessor, requests, 5);
                        extVideoBatchSpider.setEmptySleepTime(1000);
                        extVideoBatchSpider.setExitWhenComplete(false);
                        extVideoBatchSpider.setUUID("VideoBatchSpider");
                        if (null != extVideoBatchSpider) {
                            SpiderUtils.addListenerForSpider(extVideoBatchSpider, new SpiderListener() {
                                @Override
                                public void onError(Request request, Exception e) {
                                    System.out.println("Failed to request batch: request[" +request.getUrl() + "], err: " + e.getMessage());
                                    int index = request.getExtra(ExtVideoBatchProcessor.EP_INDEX_EXTRAS_KEY);
                                    final var epList = extVideoBatchProcessor.getTargetEpList(video, nodeIndex);
                                    if(null != epList && !epList.isEmpty()) {
                                        System.out.println("epIndex: " + index + ", epList.size: " + epList.size());
                                        if(index >= 0 && index < epList.size()) {
                                            Episode ep = epList.get(index);
                                            runOnUiThread(e, err ->
                                                    listener.onTargetError(video, nodeIndex, ep, ErrorFlag.ERROR_ON_REQUEST, null ==  err.getMessage() ? "解析请求失败" : err.getMessage()));
                                        }
                                    } else {
                                        System.out.println("待解析剧集列表为空");
                                    }
                                }
                            });
                            extVideoBatchSpider.runAsync();
                        } else
                            notifyOnBatchParseError(video, nodeIndex, episodes, listener, ErrorFlag.INIT_ENGINE_EXCEPTION, "引擎初始化失败");
                    } else {
                        if(!requests.isEmpty()) {
                            try {
                                final var remover = ((QueueScheduler) extVideoBatchSpider.getScheduler()).getDuplicateRemover();
                                //重置url记录列表，防止因为url重复导致Request无法被加入到解析序列
                                remover.resetDuplicateCheck(extVideoBatchSpider);
                            } catch (Exception ignored) {

                            }
                            extVideoBatchSpider.addRequest(requests.toArray(new Request[0]));
                            if(extVideoBatchSpider.getStatus() != Spider.Status.Running) {
                                extVideoBatchSpider.runAsync();
                            }
                        }
                    }
                } else {
                    notifyOnBatchParseError(video, nodeIndex, episodes, listener, ErrorFlag.TARGET_EPISODE_LIST_IS_EMPTY, "无效的剧集列表");
                }
            } else notifyOnBatchParseError(video, nodeIndex, episodes, listener, ErrorFlag.HOST_INVALIDATE, "源host为空");
        } else notifyOnBatchParseError(video, nodeIndex, episodes, listener, ErrorFlag.NO_SOURCE_WHEN_VIDEO_PARSE, "空的视频源");
    }

    /**
     * 解析视频url/播放直链
     *
     * @param video         video
     * @param episode       要解析的对应剧集
     * @param parseListener 解析结果监听
     */
    @Override
    public void parseVideoUrl(@NonNull Video video, @NonNull Episode episode, @NonNull ParseListener<Episode> parseListener) {
        notifyOnStarted(parseListener);
        String url = episode.getUrl();
        if (null != url && !TextUtils.isEmpty(url)) {
            VideoSource source = video.getVideoSource();
            if (null != source) {
                url = SpiderUtils.fixHostIfMissing(url, source.getHost());
                Request parseRequest = buildRequestForVideoParse(url, source, 0);
                extVideoProcessor = findProcessorBySourceType(video, episode, parseListener);
                if(null != extVideoProcessor) {
                    extVideoSpider = SpiderUtils.buildSpider(extVideoProcessor, parseRequest, 1);
                    if (null != extVideoSpider) {
                        SpiderUtils.addListenerForSpider(extVideoSpider, getCommonVideoSpiderListener(parseListener));
                        extVideoSpider.runAsync();
                    } else
                        notifyOnError(parseListener, ErrorFlag.INIT_ENGINE_EXCEPTION, "引擎初始化失败");
                }
            } else {
                notifyOnError(parseListener, ErrorFlag.NO_SOURCE_WHEN_VIDEO_PARSE, "该剧集的源无效");
            }
        } else {
            notifyOnError(parseListener, ErrorFlag.NO_VIDEO_DETAIL_URL, "剧集链接无效 : " + url);
        }
    }

    private SpiderListener getCommonVideoSpiderListener(@NotNull ParseListener<?> listener) {
        return new SpiderListener() {
            @Override
            public void onError(Request request, Exception e) {
                String msg = e.getMessage();
                if (null != msg && msg.contains("Read timed out"))
                    msg = "剧集解析超时啦";
                else
                    msg = "剧集解析请求失败啦";
                notifyOnError(listener, ErrorFlag.ERROR_ON_REQUEST, msg);
            }
        };
    }

    @Nullable
    public BaseVideoExtensionProcessor<?, ?> findProcessorBySourceType(
            @NotNull Video video,
            @IntRange(from = 0) int  nodeIndex,
            @IntRange(from = 0) int epIndex,
            @NotNull ParseListener<Episode> listener
    ) {
        final Node node = video.nodeOf(nodeIndex);
        if(null != node) {
            final Episode ep = node.epOf(epIndex);
            if(null != ep) {
                return findProcessorBySourceType(video, ep, listener);
            } else notifyOnError(listener,
                    (null == node.getEpisodes()||node.getEpisodes().isEmpty() ?
                            ErrorFlag.TARGET_EPISODE_LIST_IS_EMPTY : ErrorFlag.EPISODE_INDEX_OUT_OF_BOUNDS),
                    "未找到目标剧集");
        } else notifyOnError(listener, ErrorFlag.NO_TARGET_NODE, "未找到目标节点");
        return null;
    }

    @Nullable
    public BaseVideoExtensionProcessor<?, ?> findProcessorBySourceType(
            @NotNull Video video,
            @NotNull Episode episode,
            @NotNull ParseListener<Episode> listener
    ) {
        if(null != video.getVideoSource()) {
            //获取扩展处理器
            String extProcessorName = EngineConstant.SPIDER_PATH + video.getVideoSource().getExt();
//            BaseVideoExtensionProcessor<?, ?> processor;
            //如果源是扩展类型，则根据源中的处理器路径动态创建爬虫处理器
            switch (video.getVideoSource().getType()) {
                case SourceType.TYPE_EXTENSION://扩展源类型，需要使用指定的扩展处理器进行解析
                    if (!TextUtils.isEmpty(extProcessorName)) {
                        try {
                            //动态创建扩展爬虫处理器
                            Class<?> extProcessor = Class.forName(extProcessorName);
                            //获取到指定的构造器
                            Constructor<?> constructor = extProcessor.getConstructor(Video.class, VideoSource.class, Episode.class, ParseListener.class);
                            //实例化对象
                            return (BaseVideoExtensionProcessor<? extends VideoSource, ? extends Episode>) constructor.newInstance(video, video.getVideoSource(), episode, listener);
                        } catch (ClassNotFoundException | IllegalAccessException |
                                 InstantiationException | NoSuchMethodException |
                                 InvocationTargetException e) {
                            String msg = getExtProcessorErrMsgByException(e);
                            System.out.println("加载扩展处理器失败：" + msg);
                            notifyOnError(listener, ErrorFlag.EXT_PROCESSOR_NOT_FOUND, msg);
                            return null;
                        }
                    } else {
                        notifyOnError(listener, ErrorFlag.EXT_PROCESSOR_NOT_FOUND, "扩展处理器路径异常!");
                        return null;
                    }
                case SourceType.TYPE_SECONDARY_PAGE://二级扩展类型，在播放器页面可解析到播放链接
                    return new CommonSecondaryPageProcessor(video, video.getVideoSource(), episode, listener);
                case SourceType.TYPE_NORMAL://默认的源类型，观看界面可解析到播放链接
                    return new VideoProcessor(video, video.getVideoSource(), episode, listener);
                default:
                    notifyOnError(listener, ErrorFlag.SOURCE_TYPE_UN_EXPECTED, "未知的视频源类型");
            }
        } else notifyOnError(listener, ErrorFlag.NO_SOURCE_WHEN_VIDEO_PARSE, "空的视频源");
        return null;
    }

    private static @NonNull Request buildRequestForVideoParse(@NotNull String url,@NotNull VideoSource source, @IntRange(from = 0) int listenerIndex) {
        Request parseRequest = new Request(url);
        parseRequest.putExtra(Request.SPIDER_LISTENER_INDEX, listenerIndex);
        //链接请求方式
        String method = source.getItemUrlMD();
        //默认GET请求
        SpiderUtils.applyMethod(parseRequest, method);
        //如果PlayUrl为空，则采用全局Url
        String userAgent = SpiderUtils.checkUserAgent(source.getPlayUrlUA(), source);
        SpiderUtils.initRequest(parseRequest, userAgent, null, source.getPlayUrlCK(), source.getPlayUrlHD());
        SpiderUtils.addReferer(source, parseRequest, source.getReferer(), true);
        return parseRequest;
    }

    @NonNull
    public static String getExtProcessorErrMsgByException(Exception e) {
        String msg;
        if (e instanceof ClassNotFoundException) {
            msg = "未找到该源的扩展处理器";
        } else if (e instanceof IllegalAccessException) {
            msg = "无法访问扩展处理器";
        } else if (e instanceof InvocationTargetException) {
            msg = "无法调用扩展处理器";
        } else if (e instanceof NoSuchMethodException) {
            msg = "扩展处理器构造器异常";
        } else {
            msg = "扩展处理器初始化失败";
        }
        return msg;
    }

    @Override
    public void parsePage(PageSource pageSource, @Nullable PageParseListener listener) {
        parsePage(pageSource, null, pageSource.isTrendingFirst(), listener);
    }

    @Override
    public void parsePage(PageSource pageSource, boolean trendFirst, @Nullable PageParseListener listener) {
        parsePage(pageSource, null, trendFirst, listener);
    }

    /**
     * 批量解析PageSource中的Page
     *
     * @param pageSources        PageSourceList
     * @param trendFirst         当trendFirstBySource为false时，则采用该参数决定是否优先采用热搜解析规则
     * @param trendFirstBySource 是否根据source中的trendingFirst来决定是否优先采用热搜解析规则
     * @param listener           解析监听器
     */
    @Override
    public void parsePage(List<PageSource> pageSources, boolean trendFirstBySource, boolean trendFirst, @Nullable PageParseListener listener) {
        notifyOnStarted(listener);
        if (null != pageSources && !pageSources.isEmpty()) {
            List<Request> requests = new ArrayList<>();
            //pageSource下标
            int index = 0;
            for (PageSource pageSource : pageSources) {
                List<Page> pages = pageSource.getPages();
                if (null != pages && !pages.isEmpty()) {
                    for (Page page : pages) {
                        if (page.isEnabled()) {
                            Request request = buildPageRequest(pageSource, page, listener);
                            if (null != request) {
                                request.putExtra("trendingFirst", pageSource.isTrendingFirst());
                                request.putExtra("index", index);
                                requests.add(request);
                            }
                        }
                    }
                } else {
                    notifyOnError(listener, ErrorFlag.NONE_ENABLED_PAGE, "分区列表为空");
                    return;
                }
                index++;
            }
            startPageRequest(requests, pageSources, trendFirstBySource, trendFirst, listener);
        } else
            notifyOnError(listener, ErrorFlag.PAGE_SOURCE_INVALIDATE, "分区源列表为空!");
    }

    /**
     * 解析单个PageSource的页面数据
     *
     * @param pageSource    pageSource
     * @param page          要解析的Page，如果为空，则默认解析pageSource中的全部Page
     * @param trendingFirst 是否优先使用热门数据解析规则
     * @param listener      listener
     */
    @Override
    public void parsePage(PageSource pageSource, @Nullable Page page, boolean trendingFirst, @Nullable PageParseListener listener) {
        notifyOnStarted(listener);
        if (null != pageSource && null != pageSource.getPages() && !pageSource.getPages().isEmpty()) {
            List<Request> requests = new ArrayList<>();
            if (null == page) {
                for (Page tempPage : pageSource.getPages()) {
                    if (tempPage.isEnabled()) {
                        Request request = buildPageRequest(pageSource, tempPage, listener);
                        if (null != request) {
                            //因为只有一个pageSource，传参给Processor时会转为size为1的List，因此index为0
                            request.putExtra("index", 0);
                            request.putExtra("trendingFirst", trendingFirst);
                            requests.add(request);
                        }
                    }
                }
            } else if (page.isEnabled()) {
                Request request = buildPageRequest(pageSource, page, listener);
                if (null != request) {
                    request.putExtra("index", 0);
                    request.putExtra("trendingFirst", trendingFirst);
                    requests.add(request);
                }
            }
            startPageRequest(requests, pageSource, false, trendingFirst, listener);
        } else
            notifyOnError(listener, ErrorFlag.PAGE_SOURCE_INVALIDATE, "PageSource无效!");
    }

    protected void startPageRequest(List<Request> requests, PageSource pageSource, boolean trendingFirstBySource, boolean trendingFirst, @Nullable PageParseListener listener) {
        List<PageSource> pageSources = new ArrayList<>();
        pageSources.add(pageSource);
        startPageRequest(requests, pageSources, trendingFirstBySource, trendingFirst, listener);
    }

    protected void startPageRequest(List<Request> requests, List<PageSource> pageSources, boolean trendingFirstBySource, boolean trendingFirst, @Nullable PageParseListener listener) {
        if (null != requests && !requests.isEmpty()) {
            pageProcessor = new SectionPageProcessor(pageSources, trendingFirstBySource, trendingFirst, listener);
            final int threadCount = Math.min(requests.size(), 5);
            pageSpider = SpiderUtils.buildSpider(pageProcessor, requests, threadCount);
            if (null != pageSpider) {
                SpiderUtils.addListenerForSpider(pageSpider, new SpiderListener() {
                    //记录请求错误量
//                    final AtomicInteger errCount = new AtomicInteger(0);
//                    //是否已经回调过全部解析失败，防止重复回调
//                    final AtomicBoolean calledFail = new AtomicBoolean(false);
                    @Override
                    public void onError(Request request, Exception e) {
////                        errCount.getAndIncrement();
//                        System.out.println("请求解析Page失败 -> msg: " + e.getMessage() + ", page[" + request + "]");
//                        System.out.println("errCount: " + errCount + ", totalCount: " + requests.size());
                        //是否全部出错
//                        if(errCount.get() >= requests.size() && !calledFail.get()) {
//                            System.out.println("所有Page任务请求失败！");
                        notifyOnError(listener, ErrorFlag.ERROR_ON_REQUEST, "加载失败啦\n请检查网络状态或稍后再试");
//                            calledFail.set(true);
//                        }
                    }
                });
                pageSpider.start();
            }
        } else
            notifyOnError(listener, ErrorFlag.NONE_ENABLED_PAGE, "没有可用的页源");
    }

    @Nullable
    private Request buildPageRequest(@NonNull PageSource pageSource, @NonNull Page page, @Nullable PageParseListener listener) {
        @Nullable
        VideoSource videoSource = pageSource.getVideoSource();
        String pageUrl = page.getPageUrl();
        if (!TextUtils.isEmpty(pageUrl)) {
            if (null != videoSource) {
                String host = videoSource.getHost();
                pageUrl = pageUrl.replace("{host}", host);
                pageUrl = SpiderUtils.fixHostIfMissing(pageUrl, host);
            }
            if (SpiderUtils.isNotMalformedUrl(pageUrl)) {
                Request request = new Request(pageUrl);
                request.putExtra("page", page);
                SpiderUtils.initRequest(request, pageSource.getUserAgent(), null, pageSource.getCookie(), pageSource.getHeader());
                SpiderUtils.applyMethod(request, pageSource.getMethod());
                if (pageSource.getParam() != null && !request.getMethod().equalsIgnoreCase(HttpConstant.Method.GET))
                    SpiderUtils.buildRequestParams(request, pageSource.getParam());
                SpiderUtils.addUserAgent(request, pageSource.getUserAgent(), true);
                return request;
            } else
                notifyOnError(listener, ErrorFlag.URL_ILLEGAL, "畸形的分区链接!");
        } else
            notifyOnError(listener, ErrorFlag.PAGE_URL_INVALIDATE, "分区链接无效!");
        return null;
    }

    public void parseBanner(BannerSource bannerSource, @NonNull ParseListener<List<Banner>> listener) {
        listener.onStarted();
        if (null != bannerSource) {
            VideoSource videoSource = bannerSource.getSource();
            String url = bannerSource.getUrl();
            if(null != videoSource) {
                String host = videoSource.getHost();
                if(!TextUtils.isEmpty(host)) {
                    if(TextUtils.equals(url, "host")) {
                        url = host;
                    }
                    url = SpiderUtils.fixHostIfMissing(url, host);
                }
            }
            if (!TextUtils.isEmpty(url)) {
                Request request = new Request(url);
                SpiderUtils.initRequest(request, bannerSource.getUserAgent(), bannerSource.getReferer(), bannerSource.getCookie(), bannerSource.getHeader());
                SpiderUtils.applyMethod(request, bannerSource.getMethod());
                if (request.getMethod().equalsIgnoreCase(HttpConstant.Method.POST)) {
                    SpiderUtils.buildRequestParams(request, bannerSource.getParams());
                }
                Spider spider = SpiderUtils.buildSpider(new BannerProcessor(bannerSource, listener), request, 2);
                if (null != spider) {
                    SpiderUtils.addListenerForSpider(spider, new SpiderListener() {
                        @Override
                        public void onError(Request request, Exception e) {
                            listener.onFail(ErrorFlag.EXCEPTION_WHEN_PARSING, "解析Banner数据失败：request = " + request);
                            if (null != e)
                                e.printStackTrace();
                        }
                    });
                    spider.runAsync();
                } else
                    notifyOnError(listener, ErrorFlag.INIT_ENGINE_EXCEPTION, "引擎初始化失败");
            } else
                notifyOnError(listener, ErrorFlag.BANNER_URL_INVALIDATE, "Banner源链接无效");
        } else
            notifyOnError(listener, ErrorFlag.NO_BANNER_SOURCE, "BannerSource无效");
    }

    protected <V> void notifyOnBatchParseError(@NotNull Video video, int nodeIndex, @NotNull List<Episode> episodes, @NotNull EpisodeParseBatchListener listener, int errFlag, @NotNull String msg) {
        runOnUiThread(listener, callback -> listener.onFailedBatch(video, nodeIndex, episodes, errFlag, msg));
    }

    /**
     * 回调监听器,引擎异常
     *
     * @param parseListener 解析监听器
     * @param errFlag       异常标志
     * @param errMsg        故障信息
     * @param <V>           entity
     */
    private <V> void notifyOnError(ParseListener<V> parseListener, int errFlag, String errMsg) {
        if (null != parseListener) {
            runOnUiThread(parseListener, listener -> parseListener.onFail(errFlag, errMsg));
        }
    }

    /**
     * 回调监听器,处理器已开始解析
     *
     * @param parseListener 解析监听器
     * @param <V>           entity
     */
    private <V> void notifyOnStarted(ParseListener<V> parseListener) {
        if (parseListener != null) {
            runOnUiThread(parseListener, ParseListener::onStarted);
        }
    }

    /**
     * 切换到UI线程
     *
     * @param v                param
     * @param switcherCallback callback
     * @param <V>              entity
     */
    private <V> void runOnUiThread(V v, UISwitcherCallback<V> switcherCallback) {
        if (null != switcherCallback) {
            new Handler(Looper.getMainLooper()).post(() -> switcherCallback.onSwitch(v));
        }
    }
}
