package com.sho.ss.asuna.engine.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @project: 启源视频
 * @author: Sho Tan.
 * @E-mail: 2943343823@qq.com
 * @created: 2022/4/16 16:19:16
 * @description:
 **/
public class UserAgentLibrary
{
    //Edge
    public final String USER_AGENT_EDGE = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Edg/124.0.0.0";
    //Opera
    public final String USER_AGENT_OPERA1 = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.95 Safari/537.36 OPR/26.0.1656.60";
    public final String USER_AGENT_OPERA2 = "Opera/8.0 (Windows NT 5.1; U; en)";
    public final String USER_AGENT_OPERA3 = "Mozilla/5.0 (Windows NT 5.1; U; en; rv:1.8.1) Gecko/20061208 Firefox/2.0.0 Opera 9.50";
    public final String USER_AGENT_OPERA4 = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; en) Opera 9.50";
    //Firefox
    public final String USER_AGENT_FIREFOX1 = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:34.0) Gecko/20100101 Firefox/34.0";
    public final String USER_AGENT_FIREFOX2 = "Mozilla/5.0 (X11; U; Linux x86_64; zh-CN; rv:1.9.2.10) Gecko/20100922 Ubuntu/10.10 (maverick) Firefox/3.6.10";
    //Safari
    public final String USER_AGENT_SAFARI = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/534.57.2 (KHTML, like Gecko) Version/5.1.7 Safari/534.57.2";
    //chrome
    public final String USER_AGENT_CHROME1 = "ozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36";
    public final String USER_AGENT_CHROME2 = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.64 Safari/537.11";
    public final String USER_AGENT_CHROME3 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36";
    //360
    public final String USER_AGENT_360_1 = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/30.0.1599.101 Safari/537.36";
    public final String USER_AGENT_360_2 = "Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; rv:11.0) like Gecko";
    //淘宝浏览器
    public final String USER_AGENT_TAOBAO = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.11 TaoBrowser/2.0 Safari/536.11";
    //猎豹浏览器
    public final String USER_AGENT_LIEBAO1 = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1180.71 Safari/537.1 LBBROWSER";
    public final String USER_AGENT_LIEBAO2 = "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; .NET4.0C; .NET4.0E; LBBROWSER)";
    public final String USER_AGENT_LIEBAO3 = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; QQDownload 732; .NET4.0C; .NET4.0E; LBBROWSER)";
    //QQ浏览器
    public final String USER_AGENT_QQ1 = "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; .NET4.0C; .NET4.0E; QQBrowser/7.0.3698.400)";
    public final String USER_AGENT_QQ2 = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; QQDownload 732; .NET4.0C; .NET4.0E)";
    //sogou浏览器
    public final String USER_AGENT_SOGOU1 = "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/535.11 (KHTML, like Gecko) Chrome/17.0.963.84 Safari/535.11 SE 2.X MetaSr 1.0";
    public final String USER_AGENT_SOGOU2= "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; Trident/4.0; SV1; QQDownload 732; .NET4.0C; .NET4.0E; SE 2.X MetaSr 1.0)";
    //maxthon浏览器
    public final String USER_AGENT_MAXTHON = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Maxthon/4.4.3.4000 Chrome/30.0.1599.101 Safari/537.36";
    //UC浏览器
    public final String USER_AGENT_UC = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.122 UBrowser/4.0.3214.0 Safari/537.36";

    public List<String> getAllUserAgent()
    {
        //仅返回主流的浏览器UserAgent
        List<String> userAgents = new ArrayList<>();
        userAgents.add(USER_AGENT_OPERA1);
        userAgents.add(USER_AGENT_SAFARI);
        userAgents.add(USER_AGENT_EDGE);
        userAgents.add(USER_AGENT_CHROME3);
        userAgents.add(USER_AGENT_MAXTHON);
        userAgents.add(USER_AGENT_UC);
        return userAgents;
    }

    public String getProxyUserAgent()
    {
        List<String> userAgents = getAllUserAgent();
        Random rand = new Random();
        return userAgents.get(rand.nextInt(userAgents.size()));
    }
}
