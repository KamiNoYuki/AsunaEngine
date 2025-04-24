package com.sho.ss.asuna.engine.utils

import com.sho.ss.asuna.engine.core.selector.Json

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2023/3/13 18:53:55
 * @description
 */
object JsonPathUtils {

    @JvmStatic
    fun selAsInt(json: String, jsonPath: String) =
        json.runCatching {
            Json(this).jsonPath(jsonPath).get().toInt()
        }.onFailure {
            println("selAsInt -> failed by: ${it.message}")
        }.getOrElse { -1 }

    @JvmStatic
    fun selAsString(json: String, jsonPath: String) =
        json.runCatching {
            Json(this).jsonPath(jsonPath).get()
        }.onFailure {
            println("selAsString -> failed by: ${it.message}")
        }.getOrNull()
}