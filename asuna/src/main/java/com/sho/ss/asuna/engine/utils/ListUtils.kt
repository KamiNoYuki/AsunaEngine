package com.sho.ss.asuna.engine.utils

import java.util.regex.Pattern

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/5/27 2:22:17
 * @description
 */
object ListUtils {
    @JvmStatic
    @SafeVarargs
    fun <T> toList(vararg t: T): List<T> {
        return ArrayList(listOf(*t))
    }

    @JvmStatic
    fun <E> filter(list: Collection<E>, predicate: ((E) -> Boolean)) = list.filter(predicate)

    @JvmStatic
    fun <T> find(list: Iterable<T>, predicate: ((T) -> Boolean)) = list.find(predicate)

    @JvmStatic
    fun <T> isReversed(list: List<T>,compare: ((t: T) -> String)): Boolean {
        if(list.isNotEmpty() && list.size > 1) {
            val regex = Pattern.compile("\\d+")
            regex.runCatching {
                var pre: Int? = null
                for (item in list) {
                    var mName = compare(item)
                        .replace("第", "")
                        .replace("集", "")
                        .replace("期", "")
                        .replace("话", "")
                        .replace("章","")
                    //中文数字转阿拉伯
                    if (NumberUtils.isCnNumAll(mName)) {
                        mName = NumberUtils.chineseNumToArabicNum(mName).toString()
                    }
                    val matched = this.matcher(mName)
                    if (matched.find()) {
                        val next = matched.group().toInt()
                        //只要发现有后一个大于前一个，则不是逆序
                        if (pre != null && next > pre) {
                            return false
                        }
                        pre = next
                    }
                }
            }.onFailure {
                println("检测列表排序时出错 -> ${it.message}")
                return false
            }
        } else {
            return false
        }
        return true
    }
}