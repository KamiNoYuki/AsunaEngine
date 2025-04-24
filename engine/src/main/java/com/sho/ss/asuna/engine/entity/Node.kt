package com.sho.ss.asuna.engine.entity

import java.io.Serializable

/**
 * @author Sho.tan
 * @date 2022/4/6
 */
class Node : Serializable {
    var nodeName: String? = null
    var episodes: MutableList<Episode>? = null

    constructor()
    constructor(nodeName: String?, episode: Episode) : this(nodeName, toList(episode))
    constructor(nodeName: String?, episodes: MutableList<Episode>) {
        this.nodeName = nodeName
        this.episodes = episodes
    }

    companion object {
        private const val serialVersionUID = 9048084623228293766L
        private fun toList(episode: Episode) = mutableListOf(episode)
    }

    fun epOf(epWhich: Int) = episodes.runCatching { this?.get(epWhich) }.getOrNull()

    override fun toString(): String {
        return "{\n" +
                "name:$nodeName\n" +
                "episodes: ${episodes.toString()}" +"\n" +
                "}"
    }
}
