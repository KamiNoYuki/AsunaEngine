package com.sho.ss.asuna.engine.processor;

import static com.sho.ss.asuna.engine.utils.SpiderUtils.getNotNullConfig;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Category;
import com.sho.ss.asuna.engine.entity.CategorySource;
import com.sho.ss.asuna.engine.entity.CategoryTab;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.CategoryParseListener;
import com.sho.ss.asuna.engine.processor.base.BaseProcessor;
import com.sho.ss.asuna.engine.utils.SpiderUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/9/15 14:05:35
 * @description 分类页面处理器
 **/
public class CategoryProcessor extends BaseProcessor<VideoSource, CategoryParseListener> {
    private final CategorySource categorySource;
    /**
     * 通用分类配置源
     */
    private final CategorySource commonCategory;

    public CategoryProcessor(@NonNull VideoSource entity, @NonNull CategorySource categorySource, @Nullable CategoryParseListener listener) {
        super(entity, listener);
        this.categorySource = categorySource;
        this.commonCategory = entity.getCommonCategory();
    }

    @Override
    public void process(Page page) {
        if (!isRunning()) return;
        if (whenNullNotifyFail(categorySource, ErrorFlag.CATEGORY_SOURCE_IS_EMPTY, "未配置分类信息") && null != categorySource) {
            System.out.println("分类源：" + categorySource);
            String tabListXpath = getNotNullConfig(categorySource.getCategoryTabList(), commonCategory != null ? commonCategory.getCategoryTabList() : null);
            String tabItemXpath = getNotNullConfig(categorySource.getCategoryTabItem(), commonCategory != null ? commonCategory.getCategoryTabItem() : null);
            String categoryListName = getNotNullConfig(categorySource.getCategoryListName(), commonCategory != null ? commonCategory.getCategoryListName() : null);
            String categoryTabName = getNotNullConfig(categorySource.getCategoryTabName(), commonCategory != null ? commonCategory.getCategoryTabName() : null);
            String categoryTabUrl = getNotNullConfig(categorySource.getCategoryTabUrl(), commonCategory != null ? commonCategory.getCategoryTabUrl() : null);
            String dtUrlRule = getNotNullConfig(categorySource.getDtUrl(), commonCategory != null ? commonCategory.getDtUrl() : null);
            if (whenNullNotifyFail(tabListXpath, ErrorFlag.RULE_MISSING, "分类列表规则缺失") &&
                    whenNullNotifyFail(tabItemXpath, ErrorFlag.RULE_MISSING, "分类子Item规则缺失") &&
                    /* whenNullNotifyFail(categoryListName, ErrorFlag.RULE_MISSING, "分类列表名称规则缺失") && */
                    whenNullNotifyFail(categoryTabName, ErrorFlag.RULE_MISSING, "分类列表子Item名称规则缺失") &&
                    whenNullNotifyFail(categoryTabUrl, ErrorFlag.RULE_MISSING, "分类列表子Item链接规则缺失") &&
                    whenNullNotifyFail(dtUrlRule, ErrorFlag.RULE_MISSING, "分类视频详情链接规则缺失")
            ) {
//                System.out.println("page -> " + page.getRawText());
                List<String> tabList = $All(tabListXpath, page.getHtml());
                boolean tabNameIsEmpty = false;
                if (null != tabList && !tabList.isEmpty()) {
                    List<Category> categories = new ArrayList<>();
                    for (String singleLineTab : tabList) {
                        String listName = $(categoryListName, singleLineTab);
                        List<String> tabItems = $All(tabItemXpath, singleLineTab);
                        //只有一个tab的类别直接去除
                        if (null != tabItems && !tabItems.isEmpty()) {
                            List<CategoryTab> categoryTabs = new ArrayList<>();
                            for (String tabItem : tabItems) {
                                String tabName = $(categoryTabName, tabItem);
                                String tabUrl = $(categoryTabUrl, tabItem);
                                whenNullPrintln(tabUrl, ErrorFlag.EXCEPTION_WHEN_PARSING, "未解析到分类列表子Item链接，补充当前页为链接。url = " + page.getUrl().get());
                                if (!TextUtils.isEmpty(tabName)) {
                                    if (TextUtils.isEmpty(tabUrl))
                                        tabUrl = page.getUrl().get();
                                    categoryTabs.add(new CategoryTab(tabName, tabUrl));
                                    tabNameIsEmpty = false;
                                } else tabNameIsEmpty = true;
                            }
                            if (categoryTabs.isEmpty()) {
                                String errMsg = tabNameIsEmpty ? "未解析到子标签名称" : "未解析到子标签数据";
                                notifyOnFailed(ErrorFlag.NO_PARSED_DATA, errMsg);
                                return;
                            } else if (categoryTabs.size() > 1 || !categorySource.isDropSingleTabItem()) { //直接去除分类选项Item数量只有1个的
                                categories.add(new Category(listName, categoryTabs));
                            }
                        } else {
                            notifyOnEmpty("未解析到分类子列表数据");
                            return;
                        }
                    }
                    //解析视频数据
                    if (!categories.isEmpty()) {
                        //当前page的域名
                        final String host = getHostByUrl(page.getUrl().get());
                        List<String> videoList = $All(getNotNullConfig(categorySource.getVideoList(), commonCategory != null ? commonCategory.getVideoList() : null), page.getHtml());
//                        System.out.println("videoList -> " + videoList);
                        if (null != videoList && !videoList.isEmpty()) {
                            List<Video> videos = new ArrayList<>();
                            //下一页链接
                            String nextPageUrl = $(getNotNullConfig(categorySource.getNextUrl(), commonCategory != null ? commonCategory.getNextUrl() : null), page.getHtml());
                            nextPageUrl = SpiderUtils.fixHostIfMissing(nextPageUrl, host);
                            for (String videoItem : videoList) {
                                String videoName = $(getNotNullConfig(categorySource.getVideoName(), commonCategory != null ? commonCategory.getVideoName() : null), videoItem);
                                if (!TextUtils.isEmpty(videoName)) {
                                    String cover = $(getNotNullConfig(categorySource.getCover(), commonCategory != null ? commonCategory.getCover() : null), videoItem);
                                    cover = SpiderUtils.fixHostIfMissing(cover, host);
                                    String coverTitle = $(getNotNullConfig(categorySource.getCoverTitle(), commonCategory != null ? commonCategory.getCoverTitle() : null), videoItem);
                                    String subtitle = $(getNotNullConfig(categorySource.getSubtitle(), commonCategory != null ? commonCategory.getSubtitle() : null), videoItem);
                                    String dtUrl = $(dtUrlRule, videoItem);
                                    dtUrl = SpiderUtils.fixHostIfMissing(dtUrl, host);
                                    videos.add(new Video(null, cover, dtUrl,
                                            null, coverTitle, videoName,
                                            subtitle, null, null,
                                            null, null, entity));
                                } else {
                                    System.err.println("源[" + entity.getName() + "]解析分类数据时异常 -> 未解析到有效的视频名称\nvideoItem: " + videoItem);
                                }
                            }
                            if (!videos.isEmpty())
                                notifyOnCompleted(categories, videos, TextUtils.equals(page.getUrl().get(), nextPageUrl) ? null : nextPageUrl);
                            else
                                notifyOnEmpty("未解析到分类视频数据");
                        } else
                            notifyOnEmpty("未解析到视频列表数据");
                    } else
                        notifyOnEmpty("未解析到分类列表数据");
                } else
                    notifyOnEmpty("未解析到分类列表数据");
            }
        }
    }

    protected void notifyOnEmpty(@NonNull String message) {
        if (isRunning()) {
            if (null != listener)
                switchToUIThread(listener, categoryParseListener
                        -> categoryParseListener.onEmpty(message));
        }
    }

    protected void notifyOnCompleted(@NonNull List<Category> categories, @NonNull List<Video> videos, @Nullable String nextUrl) {
        if (isRunning()) {
            if (null != listener)
                switchToUIThread(listener, categoryParseListener
                        -> categoryParseListener.onCompleted(categories, videos, nextUrl));
        }
    }
}
