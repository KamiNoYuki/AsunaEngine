package com.sho.ss.asuna.engine.processor;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.selector.Html;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Node;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.DetailInfoParseListener;
import com.sho.ss.asuna.engine.processor.base.BaseProcessor;
import com.sho.ss.asuna.engine.utils.ListUtils;
import com.sho.ss.asuna.engine.utils.MapUtils;
import com.sho.ss.asuna.engine.utils.SpiderUtils;
import com.sho.ss.asuna.engine.utils.Xpath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author: Sho Tan
 * @description: 解析Video的相关详情页面
 * @created: 2022/4/8
 */
public class DetailProcessor extends BaseProcessor<Video, DetailInfoParseListener> {
    @NonNull
    private final VideoSource videoSource;

    public DetailProcessor(@NonNull Video entity, @Nullable DetailInfoParseListener listener) {
        super(entity, listener);
        this.videoSource = Objects.requireNonNull(entity.getVideoSource());
    }

    /**
     * process the page, extract urls to fetch, extract the data and store
     *
     * @param page page
     */
    @Override
    public void process(Page page) {
        parseOtherInfoWithXpath(page, page.getHtml());
        parseEpisodeListWithXpath(page, Html.create(page.getRawText()));
        parseRelatedVideoListWithXpath(page, page.getHtml());
    }

    /**
     * 解析相关推荐视频数据
     *
     * @param page page
     * @param html html
     */
    private void parseRelatedVideoListWithXpath(Page page, Html html) {
        List<String> relatedList = $All(videoSource.getRelatedList(), html);
        List<Video> relatedVideoList = null;
        if (null != relatedList && !relatedList.isEmpty()) {
            relatedVideoList = new ArrayList<>();
            for (String item : relatedList) {
                String relatedName = $(videoSource.getRelatedVideoName(), item);
                String relatedSubtitle = $(videoSource.getRelatedSubtitle(), item);
                String coverTitle = $(videoSource.getRelatedCoverTitle(), item);
                String coverUrl = $(videoSource.getRelatedVideoCover(), item);
                String dtUrl = $(videoSource.getRelatedDtUrl(), item);
                //The video name cannot be empty, and it cannot be the same as the name of the video being watched.
                if (!TextUtils.isEmpty(relatedName) && !TextUtils.equals(relatedName, entity.getVideoName())) {
                    coverUrl = !TextUtils.isEmpty(coverUrl) ? SpiderUtils.fixHostIfMissing(coverUrl, videoSource.getHost()) : coverUrl;
                    //如果链接不是绝对链接，则修正为绝对链接
                    dtUrl = SpiderUtils.fixHostIfMissing(dtUrl, videoSource.getHost());
                    relatedVideoList.add(new Video(null, coverUrl, dtUrl,
                            null, coverTitle, relatedName,
                            relatedSubtitle, null, null,
                            null, null, videoSource));
                }
            }
        }
        System.out.println("相关视频数据：" + (null == relatedVideoList ? "no data(null)!" : relatedVideoList));
        if (listener != null) {
            switchToUIThread(relatedVideoList, list ->
                    listener.onCompleted(entity, list));
        }
    }

    /**
     * 使用Xpath解析其他信息
     *
     * @param page page
     * @param html html
     */
    public void parseOtherInfoWithXpath(Page page, Html html) {
        String rating = $(videoSource.getDtRating(), html);
        String plot = $(videoSource.getDtPlot(), html);
        if (TextUtils.isEmpty(entity.getCoverUrl())) {
            entity.setCoverUrl($(videoSource.getDtCover(), html));
            System.out.println("封面链接为空，使用详情封面链接规则解析：" + videoSource.getDtCover());
        }
        System.out.println("封面链接：" + entity.getCoverUrl());
        entity.setRating(rating);
        if (!TextUtils.isEmpty(plot)) {
            entity.setPlot(plot.startsWith("\t\t") ? applyFilter(plot, videoSource.getPlotFilter(), true) : "\t\t" + applyFilter(plot, videoSource.getPlotFilter(), true));
        }
        //其他信息
        Map<String, String> otherInfoMap = new LinkedHashMap<>();
        MapUtils.proxy(videoSource.getDtOther(), (k, v) ->
        {
            String key = $(k, html);
            String value = $(v, html);
            if (null != value) {
                if (value.startsWith("/") || value.startsWith("\\"))
                    value = value.substring(1);
                if (value.endsWith("/") || value.endsWith("\\"))
                    value = value.substring(0, value.length() - 1);
            }
            if (!TextUtils.isEmpty(key)) {
                if (key.endsWith("/") || key.endsWith("\\"))
                    key = key.substring(0, key.length() - 1);
                //应用过滤器
                key = applyFilter(key, videoSource.getDtOtherFilter());
                value = applyFilter(value, videoSource.getDtOtherFilter());
                otherInfoMap.put(key, value);
            }
        });
        entity.setOtherInfo(otherInfoMap);
    }

    /**
     * 使用Xpath解析剧集列表
     *
     * @param page page
     * @param html html
     */
    private void parseEpisodeListWithXpath(Page page, Html html) {
        List<String> nodeList = $All(videoSource.getNodeList(), html);
        List<String> nodeNameList = $All(videoSource.getNodeNameList(), html);
        if (nodeList != null) {
            int index = 1;
            List<Node> nodes = new ArrayList<>();
            //节点
            for (String list : nodeList) {
                List<Episode> episodes = new ArrayList<>();
                List<String> episodesHtml = Xpath.selectList(videoSource.getNodeItem(), list);
                if (null != episodesHtml && !episodesHtml.isEmpty()) {
                    int i = 1;
                    //列表
                    for (String item : episodesHtml) {
                        String playUrl = $(videoSource.getItemUrl(), item);
                        String episodeName = $(videoSource.getItemName(), item);
                        if (TextUtils.isEmpty(episodeName))
                            episodeName = String.format(Locale.CHINA, "第%d集", i);
                        if (whenNullNotifyFail(playUrl, ErrorFlag.EPISODE_URL_INVALIDATE, "剧集链接无效!")) {
                            playUrl = applyFilter(playUrl, videoSource.getItemUrlFilter(), true);
                            episodeName = applyFilter(episodeName, videoSource.getItemNameFilter(), true);
                            if (videoSource.isToAbsoluteUrlInPlay())
                                playUrl = SpiderUtils.fixHostIfMissing(playUrl, videoSource.getHost());
                            if (videoSource.isItemUrlCanPlay())
                                System.out.println("播放链接：" + playUrl);
                            //isItemUrlCanPlay为true，则表示剧集链接可直接播放
                            episodes.add(new Episode(episodeName, playUrl, videoSource.isItemUrlCanPlay() ? playUrl : null, videoSource.getReferer(), videoSource.isEpCacheable()));
                        } else
                            return;
                        i++;
                    }
                    Node node = new Node("节点" + (index++), episodes);
                    nodes.add(node);
                }
            }
            if (!nodes.isEmpty()) {
                //解析节点名称
                String nodeName;
                if (null != nodeNameList && !nodeNameList.isEmpty()) {
                    for (int i = 0; i < nodeNameList.size(); i++) {
                        nodeName = $(videoSource.getNodeNameItem(), nodeNameList.get(i));
                        if (!TextUtils.isEmpty(nodeName) && i < nodes.size()) {
                            nodes.get(i).setNodeName(applyFilter(nodeName, videoSource.getNodeNameFilter()));
                        }
                    }
                }
                checkReverse(nodes);
                entity.setNodes(filtrationNodesWithBlackList(nodes));
            }
        }
        //节点列表为空，根据规则未获取到对应节点
        else
            notifyOnFailed(ErrorFlag.NO_NODE_LIST, "节点解析失败");
    }

    /**
     * 屏蔽在黑名单上的节点
     *
     * @param nodes 节点列表
     * @return 若黑名单为空，则返回原始节点列表
     */
    @NonNull
    private List<Node> filtrationNodesWithBlackList(@NonNull List<Node> nodes) {
        final Set<String> blackList = videoSource.getNodeBlackList();
        if (null != blackList && !blackList.isEmpty()) {
            return ListUtils.filter(nodes, node -> !blackList.contains(node.getNodeName()));
        } else {
            return nodes;
        }
    }

    /**
     * 剧集列表是否需要逆序排行
     *
     * @param nodes 需要逆序的节点列表
     */
    private void checkReverse(List<Node> nodes) {
        VideoSource videoSource = entity.getVideoSource();
        if (null != nodes && videoSource.isListReverse()) {
            for (Node node : nodes) {
                final var epList = node.getEpisodes();
                //判断剧集列表是否为逆序的
                if (null != epList && ListUtils.isReversed(epList, Episode::getName)) {
                    //如果为逆序的则将其转为正序
                    Collections.reverse(epList);
                }
            }
        }
    }
}
