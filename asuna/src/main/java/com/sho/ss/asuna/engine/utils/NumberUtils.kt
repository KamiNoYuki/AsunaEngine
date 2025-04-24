package com.sho.ss.asuna.engine.utils

import java.math.BigDecimal
import java.util.Objects

/**
 * @project  启源视频
 * @author   Sho Tan.
 * @e-mail   2943343823@qq.com
 * @created 2023/5/26 14:42:43
 * @description  数字处理工具类
 **/
class NumberUtils {
    companion object {
        /**
         * 中文数字
         */
        private val cnArr_a = charArrayOf('零', '一', '二', '三', '四', '五', '六', '七', '八', '九')
        private val cnArr_A = charArrayOf('零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖')
        private const val allChineseNum = "零一二三四五六七八九壹贰叁肆伍陆柒捌玖十拾百佰千仟万萬亿"

        /**
         * 中文单位
         */
        private val unit_a = charArrayOf('亿', '万', '千', '百', '十')
        private val unit_A = charArrayOf('亿', '萬', '仟', '佰', '拾')
        private const val allChineseUnit = "十拾百佰千仟万萬亿"

        /**
         * 将汉字中的数字转换为阿拉伯数字
         * (例如：三万叁仟零肆拾伍亿零贰佰萬柒仟陆佰零伍)
         *
         * @param chineseNum;
         * @return long
         */
        fun chineseNumToArabicNum(chineseNum: String?): BigDecimal? {
            var mChineseNum = chineseNum
            return try {
                // 最终返回的结果
                var result = BigDecimal(0)
                if (mChineseNum == null || mChineseNum.trim { it <= ' ' }.isEmpty()) {
                    return result
                }
                val lastUnit = mChineseNum[mChineseNum.length - 1]
                var appendUnit = true
                var lastUnitNum: Long = 1
                if (isCnUnit(lastUnit)) {
                    mChineseNum = mChineseNum.substring(0, mChineseNum.length - 1)
                    lastUnitNum = chnNameValue[chnUnitToValue(lastUnit.toString())].value
                    appendUnit = chnNameValue[chnUnitToValue(lastUnit.toString())].secUnit
                } else if (mChineseNum.length == 1) {
                    // 如果长度为1时
                    val num = strToNum(mChineseNum)
                    return if (num != -1) {
                        BigDecimal.valueOf(num.toLong())
                    } else {
                        null
                    }
                }

                // 将小写中文数字转为大写中文数字
                for (i in cnArr_a.indices) {
                    mChineseNum = mChineseNum!!.replace(cnArr_a[i].toString().toRegex(), cnArr_A[i].toString())
                }
                // 将小写单位转为大写单位
                for (i in unit_a.indices) {
                    mChineseNum = mChineseNum!!.replace(unit_a[i].toString().toRegex(), unit_A[i].toString())
                }
                for (i in unit_A.indices) {
                    if (mChineseNum!!.trim { it <= ' ' }.isEmpty()) {
                        break
                    }
                    val unitUpperCase = unit_A[i].toString()
                    var str: String? = null
                    if (mChineseNum.contains(unitUpperCase)) {
                        str = mChineseNum.substring(0, mChineseNum.lastIndexOf(unitUpperCase) + 1)
                    }
                    if (str != null && str.trim { it <= ' ' }.isNotEmpty()) {
                        // 下次循环截取的基础字符串
                        mChineseNum = mChineseNum.replace(str.toRegex(), "")
                        // 单位基础值
                        val unitNum = chnNameValue[chnUnitToValue(unitUpperCase)].value
                        val temp = str.substring(0, str.length - 1)
                        val number = chnStringToNumber(temp)
                        result = result.add(BigDecimal.valueOf(number).multiply(BigDecimal.valueOf(unitNum)))
                    }
                    // 最后一次循环，被传入的数字没有处理完并且没有单位的个位数处理
                    if (i + 1 == unit_a.size && "" != mChineseNum) {
                        var number = chnStringToNumber(mChineseNum)
                        if (!appendUnit) {
                            number = BigDecimal.valueOf(number).multiply(BigDecimal.valueOf(lastUnitNum)).toLong()
                        }
                        result = result.add(BigDecimal.valueOf(number))
                    }
                }
                // 加上单位
                if (appendUnit && lastUnitNum > 1) {
                    result = result.multiply(BigDecimal.valueOf(lastUnitNum))
                } else if (lastUnitNum > 0) {
                    if (result.compareTo(BigDecimal.ZERO) == BigDecimal.ZERO.toInt()) {
                        result = BigDecimal.ONE
                        result = result.multiply(BigDecimal.valueOf(lastUnitNum))
                    }
                }
                result
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        /**
         * 返回中文数字汉字所对应的阿拉伯数字，若str不为中文数字，则返回-1
         *
         * @param string;
         * @return int
         */
        private fun strToNum(string: String): Int {
            for (i in cnArr_a.indices) {
                if (Objects.equals(string, cnArr_a[i].toString()) || Objects.equals(string, cnArr_A[i].toString())) {
                    return i
                }
            }
            return -1
        }

        /**
         * 判断传入的字符串是否全是汉字数字和单位
         *
         * @param chineseStr;
         * @return boolean
         */
        fun isCnNumAll(chineseStr: String): Boolean {
            if (chineseStr.isBlank()) {
                return false
            }
            val charArray = chineseStr.toCharArray()
            for (c in charArray) {
                if (!allChineseNum.contains(c.toString())) {
                    return false
                }
            }
            return true
        }

        /**
         * 判断传入的字符是否是汉字数字和单位
         *
         * @param chineseChar;
         * @return boolean
         */
        fun isCnNum(chineseChar: Char) = allChineseNum.contains(chineseChar.toString())

        /**
         * 判断是否是中文单位
         *
         * @param unitStr;
         * @return boolean
         */
        fun isCnUnit(unitStr: Char) = allChineseUnit.contains(unitStr.toString())

        /**
         * 中文转换成阿拉伯数字，中文字符串除了包括0-9的中文汉字，还包括十，百，千，万等权位。
         * 此处是完成对这些权位的类型定义。
         * name是指这些权位的汉字字符串。
         * value是指权位多对应的数值的大小。诸如：十对应的值的大小为10，百对应为100等
         * secUnit若为true，代表该权位为节权位，即万，亿，万亿等
         */
        class ChnNameValue internal constructor(var name: String, var value: Long, var secUnit: Boolean)


        private var chnNameValue = arrayOf(
            ChnNameValue("十", 10, false),
            ChnNameValue("拾", 10, false),
            ChnNameValue("百", 100, false),
            ChnNameValue("佰", 100, false),
            ChnNameValue("千", 1000, false),
            ChnNameValue("仟", 1000, false),
            ChnNameValue("万", 10000, true),
            ChnNameValue("萬", 10000, true),
            ChnNameValue("亿", 100000000, true)
        )

        /**
         * 返回中文汉字权位在chnNameValue数组中所对应的索引号，若不为中文汉字权位，则返回-1
         *
         * @param str;
         * @return int
         */
        private fun chnUnitToValue(str: String): Int {
            for (i in chnNameValue.indices) {
                if (str == chnNameValue[i].name) {
                    return i
                }
            }
            return -1
        }

        /**
         * 返回中文数字字符串所对应的int类型的阿拉伯数字
         * (千亿/12位数)
         *
         * @param str;
         * @return long
         */
        private fun chnStringToNumber(str: String?): Long {
            var returnNumber: Long = 0
            var section: Long = 0
            var index = 0
            var number: Long = 0
            while (index < str!!.length) {
                // 从左向右依次获取对应中文数字，取不到返回-1
                val num = strToNum(str.substring(index, index + 1))
                //若num>=0，代表该位置（pos），所对应的是数字不是权位。若小于0，则表示为权位
                if (num >= 0) {
                    number = num.toLong()
                    index++
                    //pos是最后一位，直接将number加入到section中。
                    if (index >= str.length) {
                        section += number
                        returnNumber += section
                        break
                    }
                } else {
                    val chnNameValueIndex = chnUnitToValue(str.substring(index, index + 1))
                    if (chnNameValueIndex == -1) {
                        // 字符串存在除 数字和单位 以外的中文
                        throw NumberFormatException("字符串存在除 <数字和单位> 以外的中文")
                    }

                    //chnNameValue[chnNameValueIndex].secUnit==true，表示该位置所对应的权位是节权位，
                    if (chnNameValue[chnNameValueIndex].secUnit) {
                        section = (section + number) * chnNameValue[chnNameValueIndex].value
                        returnNumber += section
                        section = 0
                    } else {
                        section += number * chnNameValue[chnNameValueIndex].value
                    }
                    index++
                    number = 0
                    if (index >= str.length) {
                        returnNumber += section
                        break
                    }
                }
            }
            return returnNumber
        }
    }
}