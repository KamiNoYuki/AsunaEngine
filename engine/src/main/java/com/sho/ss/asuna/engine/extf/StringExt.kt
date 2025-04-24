package com.sho.ss.asuna.engine.extension

/**
 * @project 启源视频
 * @author ShoTan.
 * @email 2943343823@qq.com
 * @created 2024/2/1 20:19
 * @description
 **/

/**
 * 将16进制的字符串转换为字节数组
 */
fun String.hexStringToByteArray(): ByteArray {
    val hexChars = "0123456789abcdef"
    val result = ByteArray(length / 2)
    for (i in indices step 2) {
        val firstIndex = hexChars.indexOf(this[i].lowercaseChar())
        val secondIndex = hexChars.indexOf(this[i + 1].lowercaseChar())
        val octet = (firstIndex shl 4) or secondIndex
        result[i / 2] = octet.toByte()
    }
    return result
}