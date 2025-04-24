package com.sho.ss.asuna.engine.entity

import java.io.Serializable

/**
 * @author Sho Tan.
 * @project 启源视频
 * @e-mail 2943343823@qq.com
 * @created 2022/9/15 16:11:05
 * @description
 */
data class Category(
    val categoryName: String? = null,
    val categoryTabs: List<CategoryTab>? = null
) : Serializable
