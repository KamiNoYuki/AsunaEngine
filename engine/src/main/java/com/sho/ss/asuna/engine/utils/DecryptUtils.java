package com.sho.ss.asuna.engine.utils;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/9/11 13:41:32
 * @description 解密工具，包含各种解密方式
 **/
public class DecryptUtils {

    public static void main(String[] args) throws UnsupportedEncodingException {
//        System.out.println(JsBase64Helper.atob(URLDecoder.decode(BaseVideoExtensionProcessor.decodeUrlType1("JTRCJTQ1JTQ2JTQ1JTUxJTU1JTU2JTRDJTU0JTMwJTRBJTQyJTUxJTU1JTQ2JTQyJTUzJTMwJTc0JTM4JTUxJTMzJTM1JTREJTU3JTQ1JTc0JTQ3JTUyJTY5JTQ2JTQ1JTRCJTMwJTRFJTREJTU1JTMxJTQyJTJCJTU1JTQ1JTM4JTcxJTU0JTZCJTcwJTQ4JTUxJTQ1JTVBJTQ2JTRCJTMwJTYzJTcxJTU0JTZCJTcwJTQ4JTUxJTQ1JTVBJTQ2JTRCJTMwJTYzJTM5JTUwJTUxJTNEJTNE)"))));

//        String url = "Fdt2BwQQRmR0MkJ3UVFodHRwczovL20zdTguY2FjaGUuc2h0cGluLmNvbS9EZGNhY2hlLzIwMjIwOTI1L2E0NDJmM2RhMTA4ZjZhZjkyY2Q3ZWEwY2VkZGFlOWQxLm0zdTg/c3Q9QXI2V25oYzNTUU9kcFhlLWEzSU5IUSZlPTE2NjQwNTIyNjhGZHQyQndRUQ==";
//        //解密后的链接
        String decUrl = JsBase64Helper.atob("7WvFMdQyj8J2S7hQ1I+E+bI+dqQFhMV0l00tksiW4/MdRSZs7SpPcoPTeUUnNJUpERMZUwJu5qrNgwqEKFsK7A==");
        decUrl = decUrl.substring(decUrl.indexOf("http"), decUrl.length() - 8);
        System.out.println(decUrl);
    }

    public static String toMd5(String sourceStr) {
        //通过result返回加密值
        String result = null;
        try {
            //1.初始化MessageDigest信息摘要对象,并指定为MD5不分大小写都可以
            MessageDigest md = MessageDigest.getInstance("MD5");
            //2.传入需要计算的字符串更新摘要信息，传入的为字节数组byte[],将字符串转换为字节数组使用getBytes()方法完成
            md.update(sourceStr.getBytes());
            //3.计算信息摘要digest()方法,返回值为字节数组
            byte[] b = md.digest();
            //定义整型
            int i;
            //声明StringBuffer对象
            StringBuilder builder = new StringBuilder();
            for (byte value : b) {
                //将首个元素赋值给i
                i = value;
                if (i < 0) i += 256;
                //前面补0
                if (i < 16) builder.append("0");
                //转换成16进制编码
                builder.append(Integer.toHexString(i));
            }
            result = builder.toString();//转换成字符串
            System.out.println("toMd5(" + sourceStr + ",32) = " + result);//输出32位16进制字符串
//            System.out.println("toMd5(" + sourceStr + ",16) = " + buf.toString().substring(8, 24));//输出16位16进制字符串
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**************** getVIdeoInfo ****************/

    /**
     * 必须传入绝对链接，区别于{@link #getVideoInfo(String, int)}或{@link  #getVideoInfo(String, int)} 该方法判断链接开头是否包含干扰字符，如果有则从链接协议头开始去掉干扰字符，并检查链接尾部，有和首部相同干扰字符则一并去掉。
     *
     * @return 去干扰后的链接
     */
    public static String removePretendChars(@NonNull String url) {
        String string = url.substring(8);
        String subStr = base64_decode(string);
        System.out.println("解base64后链接：" + subStr);
        if (!subStr.startsWith("http") && subStr.contains("http")) {
            //截取掉开头的干扰字符
            String pretendChars = subStr.substring(0, subStr.indexOf("http"));
            if (!TextUtils.isEmpty(pretendChars) && !pretendChars.isEmpty()) {
                //干扰字符长度
                int pretendCharLen = pretendChars.length();
                System.out.println("识别到干扰字符：" + pretendChars + "|len = " + pretendCharLen);
                int pretendCharsIndex = subStr.indexOf(pretendChars);
                //在http协议头前面才进行截取
                if (pretendCharsIndex < subStr.indexOf("http")) {
                    //移除链接头部的干扰字符
                    String newUrl = subStr.substring(pretendCharLen);
                    //尾部干扰字符位置
                    int lastPretendIndex = newUrl.lastIndexOf(pretendChars);
                    //尾部发现干扰字符
                    if (lastPretendIndex > subStr.indexOf("http")) {
                        //去除尾部干扰字符
                        newUrl = newUrl.substring(0, lastPretendIndex);
                    }
                    System.out.println("已过滤干扰字符：" + newUrl);
                    return newUrl;
                }
            }
        }
        return url;
    }

    /**
     * 去除链接首尾长度为len位的干扰字符
     *
     * @param url 首尾包含干扰字符的链接
     * @param len 干扰字符长度
     * @return 去除干扰字符后的链接
     * @throws UnsupportedEncodingException 不支持的编码格式异常
     */
    public static String getVideoInfo(@NonNull String url, int len) throws UnsupportedEncodingException {
        String string = url.substring(8);
        String subStr = base64_decode(string);
        if (len < subStr.length()) {
            subStr = subStr.substring(subStr.indexOf("http"), subStr.length() - len);
        }
        return UrlDecode(subStr);
    }

    /**
     * 采用正则表达式提取链接格式为{http/s开头并以e=10位数字}结尾的链接
     *
     * @param url 要提取的链接
     * @return 如果匹配到则返回提取后的链接，否则返回原链接
     * @throws UnsupportedEncodingException 不支持的编码格式异常。
     */
    public static String getVideoInfo(@NonNull String url) throws UnsupportedEncodingException {
        String string = url.substring(8);
        String subStr = base64_decode(string);
        //去除链接开头的8个干扰字符
//        subStr = subStr.substring(8);
        //去除链接末尾的干扰字符，长度不确定
        Pattern pattern = Pattern.compile("http(|s).*e=\\d{10}");
        Matcher matcher = pattern.matcher(subStr);
        if (matcher.find()) {
            subStr = matcher.group();
            System.out.println("匹配成功");
        } else
            System.out.println("匹配失败");
        return UrlDecode(subStr);
    }

    public static String UrlDecode(String params) throws UnsupportedEncodingException {
        StringBuilder cipher = new StringBuilder();
        for (int i = 0; i < params.length(); i++) {
            char mChar = params.charAt(i);
            if (mChar == '+') {
                cipher.append(" ");
            } else {
                if (mChar == '%') {
                    String subStr = params.substring(i + 1, i + 3);
                    if (Integer.parseInt("0x" + subStr) > 0x7f) {
                        cipher.append(URLDecoder.decode("%" + subStr + params.substring(i + 3, i + 9), StandardCharsets.UTF_8.name()));
                        i += 8;
                    } else {
                        cipher.append(AsciiToString(Integer.parseInt("0x" + subStr)));
                        i += 2;
                    }
                } else {
                    cipher.append(mChar);
                }
            }
        }
        return cipher.toString();
    }

    public static String AsciiToString(int mChar) {
        return String.valueOf(mChar);
    }

    public static String base64_decode(@NonNull String url) {
        String range = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
        int num1, num2, num3, num4, num5, num6, num7, num8;
        int num9 = 0, num10 = 0;
        StringBuilder cipher = new StringBuilder();
        List<String> chars = new ArrayList<>();
        url += "";
        do {
            //__Oxdef43[0xb] = indexOf      __Oxdef43[0x3] = charAt
            num4 = range.indexOf(url.charAt(num9++));
//            num4 = range[__Oxdef43[0xb]] (url[__Oxdef43[0x3]] (num9++));
            num5 = range.indexOf(url.charAt(num9++));
//            num5 = range[__Oxdef43[0xb]] (url[__Oxdef43[0x3]] (num9++));
            num6 = range.indexOf(url.charAt(num9++));
//            num6 = range[__Oxdef43[0xb]] (url[__Oxdef43[0x3]] (num9++));
            num7 = range.indexOf(url.charAt(num9++));
//            num7 = range[__Oxdef43[0xb]] (url[__Oxdef43[0x3]] (num9++));
            num8 = (num4 << 18 | num5 << 12 | num6 << 6 | num7);
            num1 = (num8 >> 16) & 0xff;
            num2 = (num8 >> 0x8) & 0xff;
            num3 = (num8 & 0xff);
            if (num6 == 64) {
                //__Oxdef43[0x9] = fromCharCode
                chars.add(num10++, String.valueOf((char) (num1)));
            } else {
                if (num7 == 64) {
                    //__Oxdef43[0x9] = fromCharCode
                    chars.add(num10++, num1 + String.valueOf((char) num2));
                } else {
                    //__Oxdef43[0x9] = fromCharCode
                    chars.add(num10++, String.valueOf((char) num1) + (char) num2 + (char) num3);
                }
            }
            //__Oxdef43[0x0] = length
        } while (num9 < url.length());
        //__Oxdef43[0xc] = join     __Oxdef43[0x2] = 空格
        for (String s : chars) {
            cipher.append(s);
        }
        return cipher.toString();
    }
    /******************* getVideoInfo End *********************/

    /**
     * rc4解密
     *
     * @param data        被加密的密文
     * @param key         解密密钥
     * @param isBase64Dec 若该值为1，则先将密文进行base-64解码，然后再进行解密。否则进行base64编码之后再解密
     * @return 解密后的明文
     */
    @Nullable
    public static String rc4(String data, String key, int isBase64Dec) {
        return rc4(data, key, isBase64Dec == 1);
    }

    /**
     * rc4解密
     *
     * @param data        被加密的密文
     * @param key         解密密钥
     * @param isBase64Dec 若该值为1，则先将密文进行base-64解码，然后再进行解密。否则进行base64编码之后再解密
     * @return 解密后的明文
     */
    @Nullable
    public static String rc4(String data, String key, boolean isBase64Dec) {
        String pwd = (key == null) ? "ffsirllq" : key;
        StringBuilder cipher = new StringBuilder();
        int[] box = new int[2056];
        int[] keys = new int[2056];
        int pwd_length = pwd.length();
        if (isBase64Dec)
            data = JsBase64Helper.atob(data);
        else {
            try {
                data = URLEncoder.encode(data, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return null;
            }
        }
        char[] dataChars = data.toCharArray();
        int data_length = data.length();

        for (int i = 0; i < 256; i++) {
            keys[i] = pwd.charAt(i % pwd_length);
            box[i] = i;
        }
        for (int j = 0, i = 0; i < 256; i++) {
            j = (j + box[i] + keys[i]) % 256;
            int tmp = box[i];
            box[i] = box[j];
            box[j] = tmp;
        }
        for (int a = 0, j = 0, i = 0; i < data_length; i++) {
            a = (a + 1) % 256;
            j = (j + box[a]) % 256;
            int tmp = box[a];
            box[a] = box[j];
            box[j] = tmp;
            int k = box[((box[a] + box[j]) % 256)];
            cipher.append((char) (dataChars[i] ^ k));
        }
        if (isBase64Dec) {
            try {
                return URLDecoder.decode(cipher.toString(), StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return data;
            }
        } else {
            try {
                return MyBase64.encode(cipher.toString().getBytes(StandardCharsets.UTF_8.name()));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return data;
            }
        }
    }

    public static class JsBase64Helper {

        private static final Logger log = Logger.getLogger("JsBase64Helper.class");

        private static final String base64hash = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

        public static boolean isMatcher(String inStr, String reg) {
            Pattern pattern = Pattern.compile(reg);
            Matcher matcher = pattern.matcher(inStr);
            return matcher.matches();
        }

        /**
         * base64 encode
         * btoa method
         *
         * @param inStr str
         * @return str
         */
        public static String btoa(String inStr) {

            if (inStr == null || isMatcher(inStr, "([^\\u0000-\\u00ff])")) {
                return null;
            }

            StringBuilder result = new StringBuilder();
            int i = 0;
            int mod = 0;
            int ascii;
            int prev = 0;
            while (i < inStr.length()) {
                ascii = inStr.charAt(i);
                mod = i % 3;
                switch (mod) {
                    case 0:
                        result.append(base64hash.charAt(ascii >> 2));
                        break;
                    case 1:
                        result.append(base64hash.charAt((prev & 3) << 4 | (ascii >> 4)));
                        break;
                    case 2:
                        result.append(base64hash.charAt((prev & 0x0f) << 2 | (ascii >> 6)));
                        result.append(base64hash.charAt(ascii & 0x3f));
                        break;
                }
                prev = ascii;
                i++;
            }
            if (mod == 0) {
                result.append(base64hash.charAt((prev & 3) << 4));
                result.append("==");
            } else if (mod == 1) {
                result.append(base64hash.charAt((prev & 0x0f) << 2));
                result.append("=");
            }
            return result.toString();
        }

        /**
         * base64 decode
         * atob method  逆转encode的思路即可
         *
         * @param inStr str
         * @return str
         */
        public static String atob(@NonNull String inStr) {
            inStr = inStr.replaceAll("\\s|=", "");
            StringBuilder result = new StringBuilder();
            int cur;
            int prev = -1;
            int mod;
            int i = 0;
            while (i < inStr.length()) {
                cur = base64hash.indexOf(inStr.charAt(i));
                mod = i % 4;
                switch (mod) {
                    case 0:
                        break;
                    case 1:
                        result.append((char) (prev << 2 | cur >> 4));
                        break;
                    case 2:

                        result.append((char) ((prev & 0x0f) << 4 | cur >> 2));
                        break;
                    case 3:

                        result.append((char) ((prev & 3) << 6 | cur));
                        break;
                }
                prev = cur;
                i++;
            }
            return result.toString();
        }

        /**
         * 加密字符串
         *
         * @return str
         */
        public static String encryption(String str) {
            String encode;
            try {
                encode = URLEncoder.encode(str, "utf-8");
                encode = encode.replaceAll("\\+", "%20");//URLEncoder.encode 会将空格解释为+号
                return JsBase64Helper.btoa(encode);
            } catch (UnsupportedEncodingException e) {
                log.info("btoa加密函数出现错误。");
            }
            return str;
        }

        /**
         * 解密字符串
         *
         * @return str
         */
        public static String decrypt(String str) {
            String atob = JsBase64Helper.atob(str);
            try {
                return URLDecoder.decode(atob, "utf-8");
            } catch (UnsupportedEncodingException e) {
                log.info("atob加密函数出现错误。");
            }
            return str;
        }

    }
}
