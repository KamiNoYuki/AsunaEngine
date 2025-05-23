package com.sho.ss.asuna.engine.core;

import android.util.Log;

import com.sho.ss.asuna.engine.core.downloader.Downloader;
import com.sho.ss.asuna.engine.core.downloader.HttpClientDownloader;
import com.sho.ss.asuna.engine.core.pipeline.CollectorPipeline;
import com.sho.ss.asuna.engine.core.pipeline.ConsolePipeline;
import com.sho.ss.asuna.engine.core.pipeline.Pipeline;
import com.sho.ss.asuna.engine.core.pipeline.ResultItemsCollectorPipeline;
import com.sho.ss.asuna.engine.core.processor.PageProcessor;
import com.sho.ss.asuna.engine.core.scheduler.MonitorableScheduler;
import com.sho.ss.asuna.engine.core.scheduler.QueueScheduler;
import com.sho.ss.asuna.engine.core.scheduler.Scheduler;
import com.sho.ss.asuna.engine.core.thread.CountableThreadPool;
import com.sho.ss.asuna.engine.core.utils.UrlUtils;
import com.sho.ss.asuna.engine.core.utils.WMCollections;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.jsoup.HttpStatusException;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Entrance of a crawler.<br>
 * A spider contains four modules: Downloader, Scheduler, PageProcessor and
 * Pipeline.<br>
 * Every module is a field of Spider. <br>
 * The modules are defined in interface. <br>
 * You can customize a spider with various implementations of them. <br>
 * Examples: <br>
 * <br>
 * A simple crawler: <br>
 * Spider.create(new SimplePageProcessor("{@code http://my.oschina.net/}",
 * "{@code http://my.oschina.net/*blog/*}")).run();<br>
 * <br>
 * Store results to files by FilePipeline: <br>
 * Spider.create(new SimplePageProcessor("{@code http://my.oschina.net/}",
 * "{@code http://my.oschina.net/*blog/*}")) <br>
 * .pipeline(new FilePipeline("{@code /data/temp/webmagic/}")).run(); <br>
 * <br>
 * Use FileCacheQueueScheduler to store urls and cursor in files, so that a
 * Spider can resume the status when shutdown. <br>
 * Spider.create(new SimplePageProcessor("{@code http://my.oschina.net/}",
 * "{@code http://my.oschina.net/*blog/*}")) <br>
 * .scheduler(new FileCacheQueueScheduler("{@code /data/temp/webmagic/cache/}")).run(); <br>
 *
 * @author code4crafter@gmail.com <br>
 * @see Downloader
 * @see Scheduler
 * @see PageProcessor
 * @see Pipeline
 * @since 0.1.0
 */
public class Spider implements Runnable, Task {

    protected Downloader downloader;

    protected List<Pipeline> pipelines = new ArrayList<>();

    protected PageProcessor pageProcessor;

    protected List<Request> startRequests;

    protected Site site;

    protected String uuid;

    protected SpiderScheduler scheduler;

//    protected Logger logger = LoggerFactory.getLogger(getClass());

    protected CountableThreadPool threadPool;

    protected ExecutorService executorService;

    protected int threadNum = 1;

    protected AtomicInteger stat = new AtomicInteger(STAT_INIT);

    protected boolean exitWhenComplete = true;

    protected final static int STAT_INIT = 0;

    protected final static int STAT_RUNNING = 1;

    protected final static int STAT_STOPPED = 2;

    protected boolean spawnUrl = true;

    protected boolean destroyWhenExit = true;

    private List<SpiderListener> spiderListeners;

    private final AtomicLong pageCount = new AtomicLong(0);

    private Date startTime;

    private long emptySleepTime = 30000;

    /**
     * create a spider with pageProcessor.
     *
     * @param pageProcessor pageProcessor
     * @return new spider
     * @see PageProcessor
     */
    public static Spider create(PageProcessor pageProcessor) {
        return new Spider(pageProcessor);
    }

    /**
     * create a spider with pageProcessor.
     *
     * @param pageProcessor pageProcessor
     */
    public Spider(PageProcessor pageProcessor) {
        this.pageProcessor = pageProcessor;
        this.site = pageProcessor.getSite();
        this.scheduler = new SpiderScheduler(new QueueScheduler());
    }

    /**
     * Set startUrls of Spider.<br>
     * Prior to startUrls of Site.
     *
     * @param startUrls startUrls
     * @return this
     */
    public Spider startUrls(List<String> startUrls) {
        checkIfRunning();
        this.startRequests = UrlUtils.convertToRequests(startUrls);
        return this;
    }

    /**
     * Set startUrls of Spider.<br>
     * Prior to startUrls of Site.
     *
     * @param startRequests startRequests
     * @return this
     */
    public Spider startRequest(List<Request> startRequests) {
        checkIfRunning();
        this.startRequests = startRequests;
        return this;
    }

    /**
     * Set an uuid for spider.<br>
     * Default uuid is domain of site.<br>
     *
     * @param uuid uuid
     * @return this
     */
    public Spider setUUID(String uuid) {
        this.uuid = uuid;
        return this;
    }

    /**
     * set scheduler for Spider
     *
     * @param scheduler scheduler
     * @return this
     * @see #setScheduler(Scheduler)
     */
    @Deprecated
    public Spider scheduler(Scheduler scheduler) {
        return setScheduler(scheduler);
    }

    /**
     * set scheduler for Spider
     *
     * @param updateScheduler scheduler
     * @return this
     * @see Scheduler
     * @since 0.2.1
     */
    public Spider setScheduler(Scheduler updateScheduler) {
        checkIfRunning();
        Scheduler oldScheduler = scheduler.getScheduler();
        scheduler.setScheduler(updateScheduler);
        Request request;
        while ((request = oldScheduler.poll(this)) != null) {
            this.scheduler.push(request, this);
        }
        return this;
    }

    /**
     * add a pipeline for Spider
     *
     * @param pipeline pipeline
     * @return this
     * @see #addPipeline(Pipeline)
     * @deprecated
     */
    @Deprecated
    public Spider pipeline(Pipeline pipeline) {
        return addPipeline(pipeline);
    }

    /**
     * add a pipeline for Spider
     *
     * @param pipeline pipeline
     * @return this
     * @see Pipeline
     * @since 0.2.1
     */
    public Spider addPipeline(Pipeline pipeline) {
        checkIfRunning();
        this.pipelines.add(pipeline);
        return this;
    }

    /**
     * set pipelines for Spider
     *
     * @param pipelines pipelines
     * @return this
     * @see Pipeline
     * @since 0.4.1
     */
    public Spider setPipelines(List<Pipeline> pipelines) {
        checkIfRunning();
        this.pipelines = pipelines;
        return this;
    }

    /**
     * clear the pipelines set
     *
     * @return this
     */
    public Spider clearPipeline() {
        pipelines = new ArrayList<>();
        return this;
    }

    /**
     * set the downloader of spider
     *
     * @param downloader downloader
     * @return this
     * @see #setDownloader(Downloader)
     * @deprecated
     */
    @Deprecated
    public Spider downloader(Downloader downloader) {
        return setDownloader(downloader);
    }

    /**
     * set the downloader of spider
     *
     * @param downloader downloader
     * @return this
     * @see Downloader
     */
    public Spider setDownloader(Downloader downloader) {
        checkIfRunning();
        this.downloader = downloader;
        return this;
    }

    protected void initComponent() {
        if (downloader == null) {
            this.downloader = new HttpClientDownloader();
        }
        if (pipelines.isEmpty()) {
            pipelines.add(new ConsolePipeline());
        }
        downloader.setThread(threadNum);
        if (threadPool == null || threadPool.isShutdown()) {
            if (executorService != null && !executorService.isShutdown()) {
                threadPool = new CountableThreadPool(threadNum, executorService);
            } else {
                threadPool = new CountableThreadPool(threadNum);
            }
        }
        if (startRequests != null) {
            for (Request request : startRequests) {
                addRequest(request);
            }
            startRequests.clear();
        }
        startTime = new Date();
    }

    @Override
    public void run() {
        checkRunningStat();
        initComponent();
        System.out.println("Spider {" + getUUID() + "} started!");
//        logger.info("Spider {} started!", getUUID());
        // interrupt won't be necessarily detected
        while (!Thread.currentThread().isInterrupted() && stat.get() == STAT_RUNNING) {
            Request poll = scheduler.poll(this);
            if (poll == null) {
                if (threadPool.getThreadAlive() == 0) {
                    //no alive thread anymore , try again
                    poll = scheduler.poll(this);
                    if (poll == null) {
                        if (exitWhenComplete) {
                            break;
                        } else {
                            // wait
                            try {
                                Thread.sleep(emptySleepTime);
                                continue;
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                } else {
                    // wait until new url added，
                    if (scheduler.waitNewUrl(threadPool, emptySleepTime)) {
                        // if interrupted
                        break;
                    }
                    continue;
                }
            }
            final Request request = poll;
            //this may swallow the interruption
            threadPool.execute(() -> {
                try {
                    processRequest(request);
                    onSuccess(request);
                } catch (Exception e) {
                    onError(request, e);
//                        logger.error("process request " + request + " error", e);
                } finally {
                    pageCount.incrementAndGet();
                    scheduler.signalNewUrl();
                }
            });
        }
        stat.set(STAT_STOPPED);
        // release some resources
        if (destroyWhenExit) {
            close();
        }
        System.out.println("Spider {" + getUUID() + "} closed! {" + pageCount.get() + "} pages downloaded.");
//        logger.info("Spider {} closed! {} pages downloaded.", getUUID(), pageCount.get());
    }

    /**
     * @deprecated Use {@link #onError(Request, Exception)} instead.
     */
    @Deprecated
    protected void onError(Request request) {
    }

    protected void onError(Request request, Exception e) {
        this.onError(request);
        //不采用逐个调用，会导致已解析完成的请求回调错误
        Integer index = request.getExtra(Request.SPIDER_LISTENER_INDEX);
        if(null != spiderListeners && null != index && index >= 0 && index < spiderListeners.size())
        {
            SpiderListener listener = spiderListeners.get(index);
            if(null != listener)
            {
                listener.onError(request, e);
                return;
            }
        }
        //如果Request未携带监听器index，才逐个回调
        //TODO：以下为原项目代码
        if (CollectionUtils.isNotEmpty(spiderListeners)) {
            for (SpiderListener spiderListener : spiderListeners) {
                spiderListener.onError(request, e);
            }
        }
    }

    protected void onSuccess(Request request) {
        if (CollectionUtils.isNotEmpty(spiderListeners)) {
            for (SpiderListener spiderListener : spiderListeners) {
                spiderListener.onSuccess(request);
            }
        }
    }

    private void checkRunningStat() {
        while (true) {
            int statNow = stat.get();
            if (statNow == STAT_RUNNING) {
                throw new IllegalStateException("Spider is already running!");
            }
            if (stat.compareAndSet(statNow, STAT_RUNNING)) {
                break;
            }
        }
    }

    public void close() {
        destroyEach(downloader);
        destroyEach(pageProcessor);
        destroyEach(scheduler);
        for (Pipeline pipeline : pipelines) {
            destroyEach(pipeline);
        }
        threadPool.shutdown();
    }

    private void destroyEach(Object object) {
        if (object instanceof Closeable) {
            try {
                ((Closeable) object).close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    /**
     * Process specific urls without url discovering.
     *
     * @param urls urls to process
     */
    public void test(String... urls) {
        initComponent();
        for (String url : urls) {
            processRequest(new Request(url));
        }
    }

    private void processRequest(Request request) {
        Page page;
        if (null != request.getDownloader()){
            page = request.getDownloader().download(request,this);
        } else {
            page = downloader.download(request, this);
        }
        if (page.isDownloadSuccess()){
            onDownloadSuccess(request, page);
        } else {
            onDownloaderFail(request);
        }
    }

    private void onDownloadSuccess(Request request, Page page) {
        //Site的默认状态码为200，仅返回Site接受的状态码才会回调processor的process
        if (site.getAcceptStatCode().contains(page.getStatusCode())) {
            pageProcessor.process(page);
            extractAndAddRequests(page, spawnUrl);
            if (!page.getResultItems().isSkip()) {
                for (Pipeline pipeline : pipelines) {
                    pipeline.process(page.getResultItems(), this);
                }
            }
        } else {
            //Set-Cookie
//            System.out.println("page.Headers: " + page.getHeaders());
            Log.e(Spider.class.getSimpleName(),String.format("page status code error, page %s , code: %d",request.getUrl(), page.getStatusCode()));
            //请求接受到的响应码不为200，则回调失败
            onError(request, new HttpStatusException("HTTP error fetching URL",page.getStatusCode(),request.getUrl()));
            //因状态码问题导致的请求失败则回调onDownloaderFail
//            onDownloaderFail(request);
            //TODO 此处被改动，上面均为新添加的代码，下面这句为原始代码
//            logger.info("page status code error, page {} , code: {}", request.getUrl(), page.getStatusCode());
        }
        sleep(site.getSleepTime());
    }

    /**
     * 下载器下载失败时回调
     * @param request 下载失败的请求
     */
    private void onDownloaderFail(Request request) {
        System.out.println("onDownloaderFail -> " + request);
        //以上代码为新增，非自带
        if (site.getCycleRetryTimes() == 0) {
            sleep(site.getSleepTime());
            //新增代码，重试次数达到上限，依然失败则回调失败
            Exception e = request.getExtra(Request.REQUEST_FAILED_EXCEPTION);
            if(null != e) {
                onError(request, e);
            }
        } else {
            // for cycle retry
            doCycleRetry(request);
        }
    }

    private void doCycleRetry(Request request) {
        Object cycleTriedTimesObject = request.getExtra(Request.CYCLE_TRIED_TIMES);
        if (cycleTriedTimesObject == null) {
            addRequest(SerializationUtils.clone(request).setPriority(0).putExtra(Request.CYCLE_TRIED_TIMES, 1));
        } else {
            int cycleTriedTimes = (Integer) cycleTriedTimesObject;
            cycleTriedTimes++;
            //Request已重试次数未达到上限，则继续尝试重试
            if (cycleTriedTimes < site.getCycleRetryTimes()) {
                addRequest(SerializationUtils.clone(request).setPriority(0).putExtra(Request.CYCLE_TRIED_TIMES, cycleTriedTimes));
            }
            else
                //新增代码，重试次数达到上限，依然失败则回调失败
                onError(request,request.getExtra(Request.REQUEST_FAILED_EXCEPTION));
        }
        sleep(site.getRetrySleepTime());
    }

    protected void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
//            logger.error("Thread interrupted when sleep",e);
            System.err.println("Thread interrupted when sleep：" + e.getMessage());
        }
    }

    protected void extractAndAddRequests(Page page, boolean spawnUrl) {
        if (spawnUrl && CollectionUtils.isNotEmpty(page.getTargetRequests())) {
            for (Request request : page.getTargetRequests()) {
                addRequest(request);
            }
        }
    }

    private void addRequest(Request request) {
        if (site.getDomain() == null && request != null && request.getUrl() != null) {
            site.setDomain(UrlUtils.getDomain(request.getUrl()));
        }
        scheduler.push(request, this);
    }

    protected void checkIfRunning() {
        if (stat.get() == STAT_RUNNING) {
            throw new IllegalStateException("Spider is already running!");
        }
    }

    public void runAsync() {
        Thread thread = new Thread(this);
        thread.setDaemon(false);
        thread.start();
    }

    /**
     * Add urls to crawl. <br>
     *
     * @param urls urls
     * @return this
     */
    public Spider addUrl(String... urls) {
        for (String url : urls) {
            addRequest(new Request(url));
        }
        scheduler.signalNewUrl();
        return this;
    }

    /**
     * Download urls synchronizing.
     *
     * @param urls urls
     * @param <T> type of process result
     * @return list downloaded
     */
    public <T> List<T> getAll(Collection<String> urls) {
        destroyWhenExit = false;
        spawnUrl = false;
        if (startRequests!=null){
            startRequests.clear();
        }
        for (Request request : UrlUtils.convertToRequests(urls)) {
            addRequest(request);
        }
        CollectorPipeline<?> collectorPipeline = getCollectorPipeline();
        pipelines.add(collectorPipeline);
        run();
        spawnUrl = true;
        destroyWhenExit = true;
        return (List<T>) collectorPipeline.getCollected();
    }

    protected CollectorPipeline<?> getCollectorPipeline() {
        return new ResultItemsCollectorPipeline();
    }

    public <T> T get(String url) {
        List<String> urls = WMCollections.newArrayList(url);
        List<T> resultItems = getAll(urls);
        if (resultItems != null && !resultItems.isEmpty()) {
            return resultItems.get(0);
        } else {
            return null;
        }
    }

    /**
     * Add urls with information to crawl.<br>
     *
     * @param requests requests
     * @return this
     */
    public Spider addRequest(Request... requests) {
        for (Request request : requests) {
            addRequest(request);
        }
        scheduler.signalNewUrl();
        return this;
    }

    public void start() {
        runAsync();
    }

    public void stop() {
        if (stat.compareAndSet(STAT_RUNNING, STAT_STOPPED)) {
            System.out.println("Spider [" + getUUID() + "] stop success!");
//            logger.info("Spider " + getUUID() + " stop success!");
        } else {
//            logger.info("Spider " + getUUID() + " stop fail!");
            System.out.println("Spider [" + getUUID() + "] stop fail!");
        }
    }

    /**
     * start with more than one threads
     *
     * @param threadNum threadNum
     * @return this
     */
    public Spider thread(int threadNum) {
        checkIfRunning();
        this.threadNum = threadNum;
        if (threadNum <= 0) {
            throw new IllegalArgumentException("threadNum should be more than one!");
        }
        return this;
    }

    /**
     * start with more than one threads
     *
     * @param executorService executorService to run the spider
     * @param threadNum threadNum
     * @return this
     */
    public Spider thread(ExecutorService executorService, int threadNum) {
        checkIfRunning();
        this.threadNum = threadNum;
        if (threadNum <= 0) {
            throw new IllegalArgumentException("threadNum should be more than one!");
        }
        this.executorService = executorService;
        return this;
    }

    public boolean isExitWhenComplete() {
        return exitWhenComplete;
    }

    /**
     * Exit when complete. <br>
     * True: exit when all url of the site is downloaded. <br>
     * False: not exit until call stop() manually.<br>
     *
     * @param exitWhenComplete exitWhenComplete
     * @return this
     */
    public Spider setExitWhenComplete(boolean exitWhenComplete) {
        this.exitWhenComplete = exitWhenComplete;
        return this;
    }

    public boolean isSpawnUrl() {
        return spawnUrl;
    }

    /**
     * Get page count downloaded by spider.
     *
     * @return total downloaded page count
     * @since 0.4.1
     */
    public long getPageCount() {
        return pageCount.get();
    }

    /**
     * Get running status by spider.
     *
     * @return running status
     * @see Status
     * @since 0.4.1
     */
    public Status getStatus() {
        return Status.fromValue(stat.get());
    }


    public enum Status {
        Init(0), Running(1), Stopped(2);

        Status(int value) {
            this.value = value;
        }

        private final int value;

        int getValue() {
            return value;
        }

        public static Status fromValue(int value) {
            for (Status status : Status.values()) {
                if (status.getValue() == value) {
                    return status;
                }
            }
            //default value
            return Init;
        }
    }

    /**
     * Get thread count which is running
     *
     * @return thread count which is running
     * @since 0.4.1
     */
    public int getThreadAlive() {
        if (threadPool == null) {
            return 0;
        }
        return threadPool.getThreadAlive();
    }

    /**
     * Whether add urls extracted to download.<br>
     * Add urls to download when it is true, and just download seed urls when it is false. <br>
     * DO NOT set it unless you know what it means!
     *
     * @param spawnUrl spawnUrl
     * @return this
     * @since 0.4.0
     */
    public Spider setSpawnUrl(boolean spawnUrl) {
        this.spawnUrl = spawnUrl;
        return this;
    }

    @Override
    public String getUUID() {
        if (uuid != null) {
            return uuid;
        }
        if (site != null) {
            return site.getDomain();
        }
        uuid = UUID.randomUUID().toString();
        return uuid;
    }

    public Spider setExecutorService(ExecutorService executorService) {
        checkIfRunning();
        this.executorService = executorService;
        return this;
    }

    @Override
    public Site getSite() {
        return site;
    }

    public List<SpiderListener> getSpiderListeners() {
        return spiderListeners;
    }

    public Spider setSpiderListeners(List<SpiderListener> spiderListeners) {
        this.spiderListeners = spiderListeners;
        return this;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Scheduler getScheduler() {
        return scheduler.getScheduler();
    }

    /**
     * Set wait time when no url is polled.<br><br>
     *
     * @param emptySleepTime In MILLISECONDS.
     * @return this
     */
    public Spider setEmptySleepTime(long emptySleepTime) {
        if(emptySleepTime<=0){
            throw new IllegalArgumentException("emptySleepTime should be more than zero!");
        }
        this.emptySleepTime = emptySleepTime;
        return this;
    }
}