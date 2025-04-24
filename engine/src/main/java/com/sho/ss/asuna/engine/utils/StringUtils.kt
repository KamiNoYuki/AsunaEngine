package com.sho.ss.asuna.engine.utils

import java.net.URLEncoder
import java.util.regex.Pattern


/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2023/3/15 17:08:27
 * @description 字符串处理工具类
 */
object StringUtils {
    /**
     * 检查给定的字符串中是否包含中文字符
     * @param str 待检查的字符串
     * @return true 包含中文字符 false 未包含中文字符
     */
    @JvmStatic
    fun isContainChinese(str: String) = Pattern.compile("[\u4e00-\u9fa5]").matcher(str).find()

    /**
     * 将给定的字符串编码为unicode
     * @param string str
     * @return unicode str
     */
    @JvmStatic
    fun unicodeEncode(string: String): String {
        val utfBytes = string.toCharArray()
        val unicodeBytes = StringBuilder()
        for (utfByte in utfBytes) {
            var hexB = Integer.toHexString(utfByte.code)
            if (hexB.length <= 2) {
                hexB = "00$hexB"
            }
            unicodeBytes.append("\\u").append(hexB)
        }
        return unicodeBytes.toString()
    }

    /**
     * 将unicode字符编码解码为utf-8
     * @param string 需要转换的unicode字符串
     * @return 转换之后的内容
     */
    @JvmStatic
    fun unicodeDecode(string: String): String? {
        var newString = string
        val pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))")
        val matcher = pattern.matcher(newString)
        return try {
            var ch: Char
            while (matcher.find()) {
                val g2 = matcher.group(2)
                val g1 = matcher.group(1)
                if (!g2.isNullOrBlank() && !g1.isNullOrBlank()) {
                    ch = g2.toInt(16).toChar()
                    newString = newString.replace(g1, ch.toString() + "")
                }
            }
            newString
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 检查给定的字符串中是否包含形如%20%7D这种被[URLEncoder.encode]编码之后的字符。
     */
    @JvmStatic
    fun isContainUrlEncodedChar(url: String?) = url?.let { Regex("%[A-Za-z0-9]{2}").containsMatchIn(it) } ?: false

}