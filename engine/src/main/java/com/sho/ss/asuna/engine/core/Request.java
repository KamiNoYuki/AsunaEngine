package com.sho.ss.asuna.engine.core;

import androidx.annotation.NonNull;

import com.sho.ss.asuna.engine.core.downloader.Downloader;
import com.sho.ss.asuna.engine.core.model.HttpRequestBody;
import com.sho.ss.asuna.engine.core.utils.Experimental;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Object contains url to crawl.<br>
 * It contains some additional information.<br>
 *
 * @author code4crafter@gmail.com <br>
 * @since 0.1.0
 */
public class Request implements Serializable {

    private static final long serialVersionUID = 2062192774891352043L;

    public static final String CYCLE_TRIED_TIMES = "_cycle_tried_times";
    //在该Request请求失败时，put异常所用的key，如果下载失败，可通过该key获取到异常信息
    public static final String REQUEST_FAILED_EXCEPTION = "_request_failed_exception";
    //爬虫监听器下标key，由于原项目请求出错时会遍历全部监听器并挨个调用，导致一个请求出错，其他未出错的也会回调出错
    //因此通过一个request对应一个监听器，保证回调绝对正确
    public static final String SPIDER_LISTENER_INDEX = "_spider_listener_index";

    private String url;

    private String method;

    private HttpRequestBody requestBody;

    /**
     * this req use this downloader
     */
    private Downloader downloader;

    /**
     * Store additional information in extras.
     */
    private final Map<String, Object> extras = new HashMap<>();

    /**
     * cookies for current url, if not set use Site's cookies
     */
    private final Map<String, String> cookies = new HashMap<>();

    private final Map<String, String> headers = new HashMap<>();

    /**
     * Priority of the request.<br>
     * The bigger will be processed earlier. <br>
     * @see com.sho.ss.asuna.engine.core.scheduler.PriorityScheduler
     */
    private long priority;

    /**
     * When it is set to TRUE, the downloader will not try to parse response body to text.
     *
     */
    private boolean binaryContent = false;

    private String charset;

    public Request() {
    }

    public Request(String url) {
        this.url = url;
    }

    public long getPriority() {
        return priority;
    }

    /**
     * Set the priority of request for sorting.<br>
     * Need a scheduler supporting priority.<br>
     * @see com.sho.ss.asuna.engine.core.scheduler.PriorityScheduler
     *
     * @param priority priority
     * @return this
     */
    @Experimental
    public Request setPriority(long priority) {
        this.priority = priority;
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T getExtra(String key) {
        return (T) extras.get(key);
    }

    public <T> Request putExtra(String key, T value) {
        extras.put(key, value);
        return this;
    }

    public String getUrl() {
        return url;
    }

    public Map<String, Object> getExtras() {
        return Collections.unmodifiableMap(extras);
    }

    public Request setExtras(Map<String, Object> extras) {
        this.extras.putAll(extras);
        return this;
    }

    public Request setUrl(String url) {
        this.url = url;
        return this;
    }

    /**
     * The http method of the request. Get for default.
     * @return httpMethod
     * @see com.sho.ss.asuna.engine.core.utils.HttpConstant.Method
     * @since 0.5.0
     */
    public String getMethod() {
        return method;
    }

    public Request setMethod(String method) {
        this.method = method;
        return this;
    }

    @Override
    public int hashCode() {
        int result = url != null ? url.hashCode() : 0;
        result = 31 * result + (method != null ? method.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Request request = (Request) o;

        if (!Objects.equals(url, request.url)) return false;
        return Objects.equals(method, request.method);
    }

    public Request addCookie(String name, String value) {
        cookies.put(name, value);
        return this;
    }

    public Request addHeader(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public HttpRequestBody getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(HttpRequestBody requestBody) {
        this.requestBody = requestBody;
    }

    public boolean isBinaryContent() {
        return binaryContent;
    }

    public Downloader getDownloader() {
        return downloader;
    }

    public void setDownloader(Downloader downloader) {
        this.downloader = downloader;
    }

    public Request setBinaryContent(boolean binaryContent) {
        this.binaryContent = binaryContent;
        return this;
    }

    public String getCharset() {
        return charset;
    }

    public Request setCharset(String charset) {
        this.charset = charset;
        return this;
    }

    @NonNull
    @Override
    public String toString() {
        return "Request{" +
                "url='" + url + '\'' +
                ", method='" + method + '\'' +
                ", extras=" + extras +
                ", priority=" + priority +
                ", headers=" + headers +
                ", cookies="+ cookies+
                '}';
    }

}