package com.sho.ss.asuna.engine.core;

import androidx.annotation.NonNull;

import com.sho.ss.asuna.engine.core.utils.HttpConstant;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;


/**
 * Object contains setting for crawler.<br>
 *
 * @author code4crafter@gmail.com <br>
 * @see com.sho.ss.asuna.engine.core.processor.PageProcessor
 * @since 0.1.0
 */
public class Site {

    private String domain;

    private String userAgent;

    private final Map<String, String> defaultCookies = new LinkedHashMap<>();

    private final Map<String, Map<String, String>> cookies = new HashMap<>();

    private String charset;

    private String defaultCharset;

    private int sleepTime = 5000;

    private int retryTimes = 0;

    private int cycleRetryTimes = 0;

    private int retrySleepTime = 1000;

    private int timeOut = 5000;

    private static final Set<Integer> DEFAULT_STATUS_CODE_SET = new HashSet<>();

    private Set<Integer> acceptStatCode = DEFAULT_STATUS_CODE_SET;

    private final Map<String, String> headers = new HashMap<>();

    private boolean useGzip = true;

    private boolean disableCookieManagement = false;

    static {
        DEFAULT_STATUS_CODE_SET.add(HttpConstant.StatusCode.CODE_200);
    }

    /**
     * new a Site
     *
     * @return new site
     */
    public static Site me() {
        return new Site();
    }

    /**
     * Add a cookie with domain {@link #getDomain()}
     *
     * @param name name
     * @param value value
     * @return this
     */
    public Site addCookie(String name, String value) {
        defaultCookies.put(name, value);
        return this;
    }

    /**
     * Add a cookie with specific domain.
     *
     * @param domain domain
     * @param name name
     * @param value value
     * @return this
     */
    public Site addCookie(String domain, String name, String value) {
        if (!cookies.containsKey(domain)){
            cookies.put(domain, new HashMap<>());
        }
        Map<String, String> targetCookies = cookies.get(domain);
        if(null != targetCookies) {
            targetCookies.put(name, value);
        }
        return this;
    }

    /**
     * set user agent
     *
     * @param userAgent userAgent
     * @return this
     */
    public Site setUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    /**
     * get cookies
     *
     * @return get cookies
     */
    public Map<String, String> getCookies() {
        return defaultCookies;
    }

    /**
     * get cookies of all domains
     *
     * @return get cookies
     */
    public Map<String,Map<String, String>> getAllCookies() {
        return cookies;
    }

    /**
     * get user agent
     *
     * @return user agent
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * get domain
     *
     * @return get domain
     */
    public String getDomain() {
        return domain;
    }

    /**
     * set the domain of site.
     *
     * @param domain domain
     * @return this
     */
    public Site setDomain(String domain) {
        this.domain = domain;
        return this;
    }

    /**
     * Set charset of page manually.<br>
     * When charset is not set or set to null, it can be auto detected by Http header.
     *
     * @param charset charset
     * @return this
     */
    public Site setCharset(String charset) {
        this.charset = charset;
        return this;
    }

    /**
     * get charset set manually
     *
     * @return charset
     */
    public String getCharset() {
        return charset;
    }

    /**
     * Set default charset of page.
     *
     * When charset detect failed, use this default charset.
     *
     * @param defaultCharset the default charset
     * @return this
     * @since 0.9.0
     */
    public Site setDefaultCharset(String defaultCharset) {
        this.defaultCharset = defaultCharset;
        return this;
    }

    /**
     * The default charset if charset detected failed.
     *
     * @return the default charset
     * @since 0.9.0
     */
    public String getDefaultCharset() {
        return defaultCharset;
    }

    public int getTimeOut() {
        return timeOut;
    }

    /**
     * set timeout for downloader in ms
     *
     * @param timeOut timeOut
     * @return this
     */
    public Site setTimeOut(int timeOut) {
        this.timeOut = timeOut;
        return this;
    }

    /**
     * Set acceptStatCode.<br>
     * When status code of http response is in acceptStatCodes, it will be processed.<br>
     * {200} by default.<br>
     * It is not necessarily to be set.<br>
     *
     * @param acceptStatCode acceptStatCode
     * @return this
     */
    public Site setAcceptStatCode(Set<Integer> acceptStatCode) {
        this.acceptStatCode = acceptStatCode;
        return this;
    }

    /**
     * get acceptStatCode
     *
     * @return acceptStatCode
     */
    public Set<Integer> getAcceptStatCode() {
        return acceptStatCode;
    }

    /**
     * Set the interval between the processing of two pages.<br>
     * Time unit is milliseconds.<br>
     *
     * @param sleepTime sleepTime
     * @return this
     */
    public Site setSleepTime(int sleepTime) {
        this.sleepTime = sleepTime;
        return this;
    }

    /**
     * Get the interval between the processing of two pages.<br>
     * Time unit is milliseconds.<br>
     *
     * @return the interval between the processing of two pages,
     */
    public int getSleepTime() {
        return sleepTime;
    }

    /**
     * Get retry times immediately when download fail, 0 by default.<br>
     *
     * @return retry times when download fail
     */
    public int getRetryTimes() {
        return retryTimes;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Put an Http header for downloader. <br>
     * Use {@link #addCookie(String, String)} for cookie and {@link #setUserAgent(String)} for user-agent. <br>
     *
     * @param key   key of http header, there are some keys constant in {@link HttpConstant.Header}
     * @param value value of header
     * @return this
     */
    public Site addHeader(String key, String value) {
        headers.put(key, value);
        return this;
    }

    /**
     * Set retry times when download fail, 0 by default.<br>
     *
     * @param retryTimes retryTimes
     * @return this
     */
    public Site setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
        return this;
    }

    /**
     * When cycleRetryTimes is more than 0, it will add back to scheduler and try download again. <br>
     *
     * @return retry times when download fail
     */
    public int getCycleRetryTimes() {
        return cycleRetryTimes;
    }

    /**
     * Set cycleRetryTimes times when download fail, 0 by default. <br>
     *
     * @param cycleRetryTimes cycleRetryTimes
     * @return this
     */
    public Site setCycleRetryTimes(int cycleRetryTimes) {
        this.cycleRetryTimes = cycleRetryTimes;
        return this;
    }

    public boolean isUseGzip() {
        return useGzip;
    }

    public int getRetrySleepTime() {
        return retrySleepTime;
    }

    /**
     * Set retry sleep times when download fail, 1000 by default. <br>
     *
     * @param retrySleepTime retrySleepTime
     * @return this
     */
    public Site setRetrySleepTime(int retrySleepTime) {
        this.retrySleepTime = retrySleepTime;
        return this;
    }

    /**
     * Whether use gzip. <br>
     * Default is true, you can set it to false to disable gzip.
     *
     * @param useGzip useGzip
     * @return this
     */
    public Site setUseGzip(boolean useGzip) {
        this.useGzip = useGzip;
        return this;
    }

    public boolean isDisableCookieManagement() {
        return disableCookieManagement;
    }

    /**
     * Downloader is supposed to store response cookie.
     * Disable it to ignore all cookie fields and stay clean.
     * Warning: Set cookie will still NOT work if disableCookieManagement is true.
     * @param disableCookieManagement disableCookieManagement
     * @return this
     */
    public Site setDisableCookieManagement(boolean disableCookieManagement) {
        this.disableCookieManagement = disableCookieManagement;
        return this;
    }

    public Task toTask() {
        return new Task() {
            @Override
            public String getUUID() {
                String uuid = Site.this.getDomain();
                if (uuid == null) {
                    uuid = UUID.randomUUID().toString();
                }
                return uuid;
            }

            @Override
            public Site getSite() {
                return Site.this;
            }
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Site site = (Site) o;

        if (cycleRetryTimes != site.cycleRetryTimes) return false;
        if (retryTimes != site.retryTimes) return false;
        if (sleepTime != site.sleepTime) return false;
        if (timeOut != site.timeOut) return false;
        if (!Objects.equals(acceptStatCode, site.acceptStatCode))
            return false;
        if (!Objects.equals(charset, site.charset)) return false;
        if (!defaultCookies.equals(site.defaultCookies))
            return false;
        if (!Objects.equals(domain, site.domain)) return false;
        if (!Objects.equals(headers, site.headers)) return false;
        return Objects.equals(userAgent, site.userAgent);
    }

    @Override
    public int hashCode() {
        int result = domain != null ? domain.hashCode() : 0;
        result = 31 * result + (userAgent != null ? userAgent.hashCode() : 0);
        result = 31 * result + defaultCookies.hashCode();
        result = 31 * result + (charset != null ? charset.hashCode() : 0);
        result = 31 * result + sleepTime;
        result = 31 * result + retryTimes;
        result = 31 * result + cycleRetryTimes;
        result = 31 * result + timeOut;
        result = 31 * result + (acceptStatCode != null ? acceptStatCode.hashCode() : 0);
        result = 31 * result + headers.hashCode();
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return "Site{" +
                "domain='" + domain + '\'' +
                ", userAgent='" + userAgent + '\'' +
                ", cookies=" + defaultCookies +
                ", charset='" + charset + '\'' +
                ", sleepTime=" + sleepTime +
                ", retryTimes=" + retryTimes +
                ", cycleRetryTimes=" + cycleRetryTimes +
                ", timeOut=" + timeOut +
                ", acceptStatCode=" + acceptStatCode +
                ", headers=" + headers +
                '}';
    }

}