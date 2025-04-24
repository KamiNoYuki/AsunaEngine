package com.sho.ss.asuna.engine.processor.base;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.Site;
import com.sho.ss.asuna.engine.core.processor.PageProcessor;
import com.sho.ss.asuna.engine.core.selector.Html;
import com.sho.ss.asuna.engine.constant.ErrorFlag;
import com.sho.ss.asuna.engine.interfaces.ParseListener;
import com.sho.ss.asuna.engine.interfaces.UISwitcherCallback;
import com.sho.ss.asuna.engine.utils.SpiderUtils;
import com.sho.ss.asuna.engine.utils.UserAgentLibrary;
import com.sho.ss.asuna.engine.utils.Xpath;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * @project: SourcesEngine
 * @author: Sho Tan.
 * @E-mail: 2943343823@qq.com
 * @created: 2022/4/8 15:24:18
 * @description: 该类为PageProcessor的基类
 * <p>
 * 泛型E：需要的解析源实体类 entity
 * 泛型S：回调实体类型
 * 泛型L：解析监听器 extends PageListener
 **/
@WorkerThread
public abstract class BaseProcessor</* 解析源实体类 */T,/* 解析监听器 */L extends ParseListener<?>> implements PageProcessor {
    protected boolean isRunning = false;
    @NonNull
    protected T entity;
    @Nullable
    protected L listener;
    /**
     * 是否首次运行process
     */
    protected boolean isFirstProcess;

    public BaseProcessor(@NonNull T entity) {
        this(entity, null);
    }

    public BaseProcessor(@NonNull T entity, @Nullable L listener) {
        this.entity = entity;
        this.listener = listener;
        setIsRunning(true);
    }

    @Override
    public void process(Page page) {

    }

    /**
     * 是否处于运行状态
     */
    public synchronized boolean isRunning() {
        return isRunning;
    }

    public synchronized void setIsRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

    protected UISwitcherCallback<Pair<Integer, String>> failCallback = new UISwitcherCallback<>() {
        @Override
        public void onSwitch(Pair<Integer, String> pair) {
            if (null != listener && isRunning()) {
                listener.onFail(pair.first, pair.second);
                setIsRunning(false);
            }
        }
    };

    /**
     * 通知UI解析时异常
     *
     * @param errCode 错误代码
     * @param errMsg  错误信息
     */
    protected void notifyOnFailed(int errCode, String errMsg) {
        if (isRunning()) {
            if (null != listener) {
                switchToUIThread(new Pair<>(errCode, errMsg), failCallback);
            }
        }
    }

    @Nullable
    protected JSONObject toJSONObject(@NonNull String jsonStr, @NonNull String msgWhenError) {
        try {
            JSONObject jsonObject = JSON.parseObject(jsonStr);
            if (whenNullNotifyFail(jsonObject, ErrorFlag.EXCEPTION_WHEN_PARSING, msgWhenError)) {
                return jsonObject;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            notifyOnFailed(ErrorFlag.EXCEPTION_WHEN_PARSING, e.getMessage());
        }
        return null;
    }

    /**
     * 通知监听器任务开始
     *
     * @deprecated 已废弃，在引擎初始化时会调用onStart，在处理器解析开始时调用会造成UI状态停滞
     */
    @Deprecated
    protected void notifyOnStart() {
        if (!isRunning()) {
            if (isFirstProcess()) {
                switchToUIThread(listener, l ->
                {
                    if (null != l) l.onStarted();
                    setIsFirstProcess(false);
                });
            }
        }
    }

    public boolean isFirstProcess() {
        return isFirstProcess;
    }

    public void setIsFirstProcess(boolean isFirstProcess) {
        this.isFirstProcess = isFirstProcess;
    }

    /**
     * 切换到UI线程
     *
     * @param callback 切换到线程后的回调
     */
    protected <B> void switchToUIThread(B bean, UISwitcherCallback<B> callback) {
        if (null != callback) {
            new Handler(Looper.getMainLooper()).post(() -> callback.onSwitch(bean));
        }
    }

    /**
     * get the site settings
     *
     * @return site
     * @see Site
     */
    @Override
    public Site getSite() {
        return Site.me()
                .setTimeOut(10_000);
    }

    @Nullable
    protected String $(String xpath, String html) {
        if (!TextUtils.isEmpty(xpath) && !TextUtils.isEmpty(html)) {
            return Xpath.select(xpath, html);
        }
        return "";
    }

    @Nullable
    protected String $(String xpath, Html html) {
        return $(xpath, html.get());
    }

    @Nullable
    protected List<String> $All(String xpath, String html) {
        if (!TextUtils.isEmpty(xpath) && !TextUtils.isEmpty(html)) {
            return Xpath.selectList(xpath, html);
        }
        return null;
    }

    @Nullable
    protected List<String> $All(String xpath, Html html) {
        return $All(xpath, html.get());
    }

    protected boolean isNullStr(String str) {
        return isNullObj(str) || TextUtils.isEmpty(str);
    }

    protected boolean isNullObj(Object o) {
        return null == o;
    }

    /**
     * 过滤器
     *
     * @param target target
     * @param filter filter
     * @return str
     */
    protected String applyFilter(String target, Map<String, String> filter) {
        return applyFilter(target, filter, false);
    }

    protected String applyFilter(String target, String replaceTarget, String to) {
        Map<String, String> filter = new HashMap<>();
        filter.put(replaceTarget, to);
        return applyFilter(target, filter);
    }

    protected boolean whenNullNotifyFail(String str, int errFlag, String msg) {
        if (isNullStr(str)) {
            notifyOnFailed(errFlag, msg);
        }
        return !isNullStr(str);
    }

    protected boolean whenNullNotifyFail(Object o, int errFlag, String errMsg) {
        if (isNullObj(o)) {
            notifyOnFailed(errFlag, errMsg);
        }
        return !isNullObj(o);
    }

    protected boolean whenNullPrintln(Object o, int errFlag, String errMsg) {
        if (isNullObj(o))
            System.out.println("*{flag}=" + errFlag + ",*{errMsg}=" + errMsg);
        return !isNullObj(o);
    }

    protected boolean whenNullPrintln(String str, int errFlag, String errMsg) {
        if (isNullStr(str))
            System.out.println("*{flag}=" + errFlag + ",*{errMsg}=" + errMsg);
        return !isNullStr(str);
    }

    /**
     * 获取给定url的全主机域名
     * 例如https://www.baidu.com/add/bbb/ccc 返回https://www.baidu.com
     *
     * @param target 目标url
     * @return 全host链接
     */
    @Nullable
    public String getHostByUrl(String target) {
        if (isNullStr(target)) return null;
        URL url;
        try {
            url = new URL(target);
            return url.getProtocol() + "://" + url.getHost();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 应用过滤器
     *
     * @param target   带过滤字符
     * @param filter   过滤器
     * @param isNormal 是否使用内置的默认过滤器
     * @return 过滤后的字符
     */
    protected String applyFilter(String target, Map<String, String> filter, boolean isNormal) {
        if (!TextUtils.isEmpty(target)) {
            if (isNormal) {
                if (null == filter)
                    filter = new LinkedHashMap<>();
                filter.putAll(getNormalFilter());
            }
            if (null != filter && !filter.isEmpty()) {
                for (Map.Entry<String, String> map : filter.entrySet()) {
                    if (!TextUtils.isEmpty(map.getKey())) {
                        target = target.replaceAll(map.getKey(), map.getValue());
                    }
                }
            }
        }
        return target;
    }

    /**
     * 如果是相对链接，则转为绝对链接，否则返回原链接
     *
     * @param currentPageUrl 当前html页面的url链接
     * @param targetUrl      要转换的相对链接
     * @return 将当前html页面的url的host补全到相对链接
     */
    protected String toAbsoluteUrl(@NonNull String currentPageUrl, @NonNull String targetUrl) {
        return SpiderUtils.fixHostIfMissing(targetUrl, getHostByUrl(currentPageUrl));
    }

    @SafeVarargs
    protected final boolean parameterValidator(int errFlag, @NonNull kotlin.Pair<String, String>... params) {
        for (kotlin.Pair<String, String> param : params) {
            if (!whenNullNotifyFail(param.getFirst(), errFlag, param.getSecond())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 校验给定的extras中的参数是否为空
     *
     * @param params 需要检验的重要且不能为空的数据，first为数据，second则是为空时的回调错误信息
     * @return 传递的所有不能为空的参数均校验通过则返回true，否则false
     */
    @SafeVarargs
    protected final boolean extrasValidator(@NonNull kotlin.Pair<String, String>... params) {
        return parameterValidator(ErrorFlag.EXTRAS_MISSING, params);
    }

    /**
     * 获取随机的用户代理UserAgent
     *
     * @return 随机UserAgent
     */
    @NonNull
    protected String getProxyUserAgent() {
        return new UserAgentLibrary().getProxyUserAgent();
    }

    protected Map<String, String> getNormalFilter() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("&.+;", "");
        map.put("\\\\/", "/");
        map.put("▶", "");
        map.put("。", "。\n\t\t");
        map.put("'", "");
        map.put(";", "");
        map.put("<(|/)\\w*[a-zA-Z](|/)>", "");
        return map;
    }
}
