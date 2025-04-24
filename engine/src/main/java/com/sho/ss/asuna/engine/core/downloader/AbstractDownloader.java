package com.sho.ss.asuna.engine.core.downloader;


import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.Request;
import com.sho.ss.asuna.engine.core.Site;
import com.sho.ss.asuna.engine.core.Task;
import com.sho.ss.asuna.engine.core.selector.Html;

/**
 * Base class of downloader with some common methods.
 *
 * @author code4crafter@gmail.com
 * @since 0.5.0
 */
public abstract class AbstractDownloader implements Downloader {

    /**
     * A simple method to download a url.
     *
     * @param url url
     * @return html
     */
    public Html download(String url) {
        return download(url, null);
    }

    /**
     * A simple method to download a url.
     *
     * @param url     url
     * @param charset charset
     * @return html
     */
    public Html download(String url, String charset) {
        Page page = download(new Request(url), Site.me().setCharset(charset).toTask());
        return page.getHtml();
    }

    /**
     * @param request the {@link Request}.
     * @deprecated Use {@link #onSuccess(Page, Task)} instead.
     */
    @Deprecated
    protected void onSuccess(Request request) {
    }

    /**
     * @param request the {@link Request}.
     * @param task the {@link Task}.
     * @since 0.7.6
     * @deprecated Use {@link #onSuccess(Page, Task)} instead.
     */
    @Deprecated
    protected void onSuccess(Request request, Task task) {
        this.onSuccess(request);
    }

    /**
     * @param page the {@link Page}.
     * @param task the {@link Task}.
     * @since 0.10.0
     */
    protected void onSuccess(Page page, Task task) {
        this.onSuccess(page.getRequest(), task);
    }

    /**
     * @param request the {@link Request}.
     * @deprecated Use {@link #onError(Page, Task, Throwable)} instead.
     */
    @Deprecated
    protected void onError(Request request) {
    }

    /**
     * @param request the {@link Request}.
     * @param task the {@link Task}.
     * @param e the exception.
     * @since 0.7.6
     * @deprecated Use {@link #onError(Page, Task, Throwable)} instead.
     */
    @Deprecated
    protected void onError(Request request, Task task, Throwable e) {
        this.onError(request);
    }

    /**
     * @param page the {@link Page}.
     * @param task the {@link Task}.
     * @param e the exception.
     * @since 0.10.0
     */
    protected void onError(Page page, Task task, Throwable e) {
        //以下代码为新增，非原项目自带
        //下载器下载失败，将异常存放到Request，以便后续回调失败传递
        if(null != page.getRequest()) {
            page.getRequest().putExtra(Request.REQUEST_FAILED_EXCEPTION, e);
        }
        this.onError(page.getRequest(), task, e);
    }
}
