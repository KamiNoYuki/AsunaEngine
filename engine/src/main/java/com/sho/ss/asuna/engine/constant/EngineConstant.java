package com.sho.ss.asuna.engine.constant;

/**
 * @project: 启源视频
 * @author: Sho Tan.
 * @E-mail: 2943343823@qq.com
 * @created: 2022/4/17 4:16:46
 * @description: Engine常量类
 **/
public class EngineConstant
{
    /**
     * 该变量引导Classloader到该路径下动态加载爬虫
     */
    public static final String SPIDER_PATH = "com.sho.ss.asuna.engine.processor.ext.";
    /**
     * 抽取链接方式：正则
     */
    public static final String EXTRACTOR_REGEX = "regex";
    /**
     * 抽取链接方式：截取
     */
    public static final String EXTRACTOR_SUB = "sub";

    public static final String REQUEST_SOURCE_EXTRAS_KEY = "id";

    public static class UrlDecodeType {
        public static final int JT = 1;//解码形如：JTY4JTc0JTc0JTcwJTczJTNBJT 的链接
        public static final int UNICODE = 2;//解码被Unicode编码过的url
        public static final int BASE64 = 3;//对url进行Base64解码
//        public static final int AES = 4;//对url进行AES解密，指定此类型时，需要同时在extras内配置解密所需的key和iv
    }
}
