package com.sho.ss.asuna.engine.utils

import com.sho.ss.asuna.engine.interfaces.Consumer

/**
 * @author Sho Tan
 */
object MapUtils {

    @JvmStatic
    fun <K, V> proxy(map: Map<K, V>?, consumer: Consumer<K, V>?) {
        map.takeIf { !it.isNullOrEmpty() }?.let {
            proxy(it, it.size - 1, consumer)
        }
    }

    @JvmStatic
    fun <K, V> proxy(map: Map<K, V>?, count: Int, consumer: Consumer<K, V>?) {
        map.takeUnless {
            map.isNullOrEmpty() || count >= map.size || null === consumer
        }?.let {
            var i = 0
            for ((key, value) in it) {
                if (i > count) return
                consumer?.accept(key, value)
                i++
            }
        }
    }

    /**
     * 将Map<String, String>转换为Map<String, Object> 用于发起请求传递给Request
     */
    @JvmStatic
    fun convertMapToRequestParams(inputMap: Map<String, String>): Map<String, Any> {
        return inputMap.mapValues { (_, value) -> value }
    }
}