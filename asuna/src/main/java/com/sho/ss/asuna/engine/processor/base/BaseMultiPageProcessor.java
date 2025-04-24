package com.sho.ss.asuna.engine.processor.base;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.selector.Html;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.entity.Episode;
import com.sho.ss.asuna.engine.entity.Video;
import com.sho.ss.asuna.engine.entity.VideoSource;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.processor.common.CommonVideoExtProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/7/29 13:54:48
 * @description 多页面视频扩展处理器
 **/
public class BaseMultiPageProcessor extends CommonVideoExtProcessor {

    private final List<IParser> targets = new ArrayList<>();
    private final AtomicInteger pageIndex = new AtomicInteger(0);//存在线程安全问题
    /**
     * {@link #pageIndex} 是否解析后自动增量
     */
    private boolean isAutoIncrement = true;

    public BaseMultiPageProcessor(@NonNull Video entity, VideoSource videoSource, @NonNull Episode episode, @Nullable ParseListener<Episode> listener) {
        super(entity, videoSource, episode, listener);
    }

    protected void registryParseTargetQueue(IParser... targets) {
        if (null != targets) {
            for (IParser target : targets) {
                addParseTarget(target);
            }
        }
    }

    protected List<IParser> getAllTargets() {
        return targets;
    }

    protected void addParseTarget(@NonNull IParser parseTarget) {
        targets.add(parseTarget);
    }

    protected void addParseTarget(@IntRange(from = 0) int index, @NonNull IParser parseTarget) {
        targets.add(index, parseTarget);
    }

    protected void removeParseTarget(@NonNull IParser target) {
        targets.remove(target);
    }

    protected void removeParseTarget(@IntRange(from = 0) int index) {
        targets.remove(index);
    }

    protected void clearAllTarget() {
        targets.clear();
        pageIndex.set(0);
    }

    protected void changePageIndex(@IntRange(from = 0) int pageIndex) {
        this.pageIndex.set(pageIndex);
    }

    public int getParseQueueIndex() {
        return pageIndex.get();
    }

    protected void autoIncrementIsEnable(boolean autoIncrement) {
        this.isAutoIncrement = autoIncrement;
    }

    @Override
    protected void extensionParse(Page page, Html html) {
        super.extensionParse(page, html);
        if (pageIndex.get() < targets.size()) {
            System.out.println("pageIndex: " + pageIndex);
            IParser iParser = targets.get(pageIndex.get());
            if (null != iParser) {
                iParser.onPageReadied(page, page.getHtml(), page.getUrl().get());
            }
            if (isAutoIncrement) {
                pageIndex.incrementAndGet();
            }
            System.out.println("自增pageIndex: " + pageIndex);
        } else {
            notifyOnFailed(ErrorFlag.TARGET_PAGE_INDEX_OUT_OF_BOUNDS, "IParser下标越界(" + pageIndex + "/" + targets.size() + ")");
            System.err.println("警告：IParser 下标越界!!! targetList.size = " + targets.size() + ",index = " + pageIndex);
        }
    }

    protected interface IParser {
        /**
         * 在页面解析完毕时回调
         *
         * @param page 当前Page
         * @param html 当前Page的html文档对象
         * @param url  当前Page的Url
         */
        void onPageReadied(Page page, Html html, String url);
    }
}
