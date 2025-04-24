package com.sho.ss.asuna.engine.processor

import android.text.TextUtils
import com.sho.ss.asuna.engine.core.Page
import com.sho.ss.asuna.engine.AsunaEngine
import com.sho.ss.asuna.engine.constant.EngineConstant
import com.sho.ss.asuna.engine.constant.ErrorFlag
import com.sho.ss.asuna.engine.constant.SourceType
import com.sho.ss.asuna.engine.entity.BatchParseTask
import com.sho.ss.asuna.engine.entity.Episode
import com.sho.ss.asuna.engine.entity.Video
import com.sho.ss.asuna.engine.entity.VideoSource
import com.sho.ss.asuna.engine.interfaces.EpisodeParseBatchListener
import com.sho.ss.asuna.engine.interfaces.ParseListener
import com.sho.ss.asuna.engine.processor.base.BaseProcessor
import com.sho.ss.asuna.engine.processor.base.BaseVideoExtensionProcessor
import com.sho.ss.asuna.engine.processor.common.CommonSecondaryPageProcessor

/**
 * @project 启源视频
 * @author ShoTan.
 * @email 2943343823@qq.com
 * @created 2024/6/23 11:50
 * @description 扩展剧集批量解析处理器
 **/
class ExtVideoBatchProcessor(
    listener: EpisodeParseBatchListener,
) : BaseProcessor<MutableMap</*视频名称*/String, MutableMap</*源名称*/String, BatchParseTask>>, EpisodeParseBatchListener>(
    mutableMapOf(),
    listener
) {

    companion object {
        const val VIDEO_NAME_EXTRAS_KEY = "video_name"
        const val SOURCE_NAME_EXTRAS_KEY = "source_name"
        const val NODE_INDEX_EXTRAS_KEY = "node_index"
        const val EP_INDEX_EXTRAS_KEY = "episode_index"
    }

    fun setListener(listener: EpisodeParseBatchListener) {
        super.listener = listener
    }

    /**
     * 获取目标视频下的所有源视频解析任务
     */
    fun getTasksForTargetVideo(videoName: String) = entity[videoName]

    /**
     * 获取目标视频下指定源下的所有解析任务
     */
    fun getTasksForTargetSource(videoName: String, sourceName: String?) =
        if (!sourceName.isNullOrBlank()) {
            entity[videoName]?.get(sourceName)
        } else null

    fun isExists(video: Video, nodeIndex: Int, ep: Episode): Boolean {
        val sourceName = video.videoSource?.name
        if (video.videoName.isNotBlank() && !sourceName.isNullOrBlank()) {
            getTasksForTargetSource(video.videoName, sourceName)
                ?.tasks
                ?.get(nodeIndex)?.let { episodes ->
                    val isFound = null != episodes.find {
                        TextUtils.equals(
                            it.name,
                            ep.name
                        ) && TextUtils.equals(it.url, ep.url)
                    }
                    return isFound
                }
        }
        return false
    }

    fun getTargetEpList(video: Video, nodeIndex: Int): MutableList<Episode>? =
        getTasksForTargetSource(video.videoName, video.videoSource?.name)
            ?.tasks?.get(nodeIndex)?.toMutableList()

    /**
     * 根据给定的视频与节点下标，从与之对应的剧集列表内移除目标剧集
     * @param video 目标Video
     * @param nodeIndex 节点索引
     * @param ep 待移除的目标剧集
     *
     */
    fun removeTargetEpisode(video: Video, nodeIndex: Int, ep: Episode): Boolean {
        return getTasksForTargetSource(video.videoName, video.videoSource?.name)
            ?.tasks
            ?.get(nodeIndex)
            ?.let { episodes ->
                episodes.find {
                    //根据剧集名称与解析页面url查找序列中的剧集
                    TextUtils.equals(it.name, ep.name) && TextUtils.equals(
                        it.url,
                        ep.url
                    )
                }?.let {
                    episodes.remove(it)//将其移除
                }
            } ?: false
    }

    fun addParseBatch(video: Video, nodeIndex: Int, episodes: MutableList<Episode>) {
        val source = video.videoSource
        if (null != source) {
            entity[video.videoName] =
                (entity[video.videoName] ?: mutableMapOf()).apply {
                    this[source.name].let {
                        if (null != it) {
                            it.tasks[nodeIndex] = (it.tasks[nodeIndex] ?: mutableSetOf()).apply {
                                addAll(episodes.toMutableSet())
                            }
                        } else {
                            this[source.name] = BatchParseTask(
                                video,
                                mutableMapOf(nodeIndex to episodes.toMutableSet())
                            )
                        }
                    }
                }
        }
    }

    override fun process(page: Page) {
        takeIf { isRunning() }?.let {
            val videoName: String = page.request.getExtra(VIDEO_NAME_EXTRAS_KEY)
            val sourceName: String = page.request.getExtra(SOURCE_NAME_EXTRAS_KEY)
            val nodeIndex: Int = page.request.getExtra(NODE_INDEX_EXTRAS_KEY)
            val epIndex: Int = page.request.getExtra(EP_INDEX_EXTRAS_KEY)
            if (isNullNotifyFail(
                    str = videoName,
                    errFlag = ErrorFlag.EXTRAS_MISSING,
                    errMsg = "Request#extras@videoName缺失"
                ) &&
                isNullNotifyFail(
                    str = sourceName,
                    errFlag = ErrorFlag.EXTRAS_MISSING,
                    errMsg = "Request#extras@sourceName缺失"
                ) &&
                isNullNotifyFail(
                    o = nodeIndex,
                    errFlag = ErrorFlag.EXTRAS_MISSING,
                    errMsg = "Request#extras@nodeIndex缺失"
                ) &&
                isNullNotifyFail(
                    o = epIndex,
                    errFlag = ErrorFlag.EXTRAS_MISSING,
                    errMsg = "Request#extras@epIndex缺失"
                )
            ) {
                getTasksForTargetSource(videoName, sourceName).takeIf {
                    isNullNotifyFail(
                        o = it,
                        errFlag = ErrorFlag.TARGET_PARSE_BATCH_NOT_EXISTS,
                        errMsg = "${videoName}>${sourceName}>${nodeIndex}：批处理任务不存在"
                    )
                }?.let { (video, nodes) ->
                    val source = video.videoSource
                    if (isNullNotifyFail(
                            o = source,
                            errFlag = ErrorFlag.NO_SOURCE_WHEN_VIDEO_PARSE,
                            errMsg = "空的视频源"
                        )
                    ) {
                        nodes[nodeIndex].takeIf {
                            isNullNotifyFail(
                                o = it,
                                errFlag = ErrorFlag.TARGET_PARSE_BATCH_NOT_EXISTS,
                                errMsg = "待解析的剧集序列不存在"
                            )
                        }?.let { episodes ->
                            episodes.takeIf {
                                isNullNotifyFail(
                                    o = episodes,
                                    errFlag = ErrorFlag.TARGET_PARSE_BATCH_NOT_EXISTS,
                                    errMsg = "待解析的剧集序列不存在"
                                )
                            }?.toMutableList()?.let {
                                if (epIndex in it.indices) {
                                    scheduleProcess(video, nodeIndex, it[epIndex], page)
                                } else {
                                    notifyOnTargetFail(
                                        video,
                                        nodeIndex,
                                        null,
                                        ErrorFlag.EPISODE_INDEX_OUT_OF_BOUNDS,
                                        "待解析的目标剧集不存在"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun scheduleProcess(video: Video, nodeIndex: Int, ep: Episode, page: Page) {
        val videoSource = video.videoSource
        videoSource.takeIf {
            isNullNotifyFail(
                o = videoSource,
                errFlag = ErrorFlag.NO_SOURCE_WHEN_VIDEO_PARSE,
                errMsg = "空的视频源"
            ).apply {
                if (!this) {
                    setIsRunning(false) //若视频源为空，则没必要进行后续流程，直接停止运行此处理器
                }
            }
        }?.let {
            switchToUIThread(listener) {
                listener?.onTargetStart(video, nodeIndex, ep)//因此此处手动回调一下
            }
            //获取扩展处理器
            val extProcessorName = EngineConstant.SPIDER_PATH + videoSource!!.ext
            getExtProcessor(video, extProcessorName, ep, object : ParseListener<Episode> {
                override fun onStarted() {
                    //该方法不会被回调，因为该方法仅在引擎初始化解析工作时回调的
                }

                override fun onFail(flag: Int, errMsg: String?) {
                    switchToUIThread(listener) {
                        listener?.onTargetError(video, nodeIndex, ep, flag, errMsg ?: "解析失败")
                    }
                    removeTargetEpisode(video, nodeIndex, ep)
                }

                override fun onCompleted(v: Episode) {
                    switchToUIThread(listener) {
                        listener?.onTargetComplete(video, nodeIndex, ep)
                    }
//                    removeTargetEpisode(video, nodeIndex, ep).apply {
//                        println("onCompleted -> 移除剧集[$nodeIndex-${v.name}]结果：${this}")
//                    }
                }
            })?.process(page)
        }
    }

    private fun getExtProcessor(
        video: Video,
        processorName: String,
        ep: Episode,
        listener: ParseListener<Episode>
    ): BaseVideoExtensionProcessor<out VideoSource, out Episode>? {
        return when (video.videoSource!!.type) {
            SourceType.TYPE_EXTENSION -> if (!TextUtils.isEmpty(processorName)) {
                processorName.runCatching {
                    //动态创建扩展爬虫处理器
                    val extProcessor = Class.forName(processorName)
                    //获取到指定的构造器
                    val constructor = extProcessor.getConstructor(
                        Video::class.java,
                        VideoSource::class.java,
                        Episode::class.java,
                        ParseListener::class.java
                    )
                    //实例化对象
                    constructor.newInstance(
                        video,
                        video.videoSource,
                        ep,
                        listener
                    ) as BaseVideoExtensionProcessor<out VideoSource, out Episode>
                }.onFailure {
                    val msg = AsunaEngine.getExtProcessorErrMsgByException(it as Exception)
                    println("加载扩展处理器失败：$msg")
                    listener.onFail(ErrorFlag.EXT_PROCESSOR_NOT_FOUND, msg)
                }.getOrNull()
            } else {
                listener.onFail(
                    ErrorFlag.EXT_PROCESSOR_NOT_FOUND,
                    "扩展处理器路径无效"
                )
                null
            }

            SourceType.TYPE_SECONDARY_PAGE -> CommonSecondaryPageProcessor(
                video,
                video.videoSource,
                ep,
                listener
            )

            SourceType.TYPE_NORMAL -> VideoProcessor(
                video,
                video.videoSource!!,
                ep,
                listener
            )

            else -> {
                listener.onFail(
                    ErrorFlag.SOURCE_TYPE_UN_EXPECTED,
                    "未知的视频源类型"
                )
                null
            }
        }
    }

    fun isNullNotifyFail(
        video: Video? = null,
        nodeIndex: Int? = null,
        episode: Episode? = null,
        o: Any?,
        errFlag: Int,
        errMsg: String
    ) = if (isNullObj(o)) {
            notifyOnTargetFail(video, nodeIndex, episode, errFlag, errMsg)
            setIsRunning(false)
            false
        } else true

    private fun isNullNotifyFail(
        video: Video? = null,
        nodeIndex: Int? = null,
        episode: Episode? = null,
        str: String?,
        errFlag: Int,
        errMsg: String
    ) = if (str.isNullOrEmpty()) {
            notifyOnTargetFail(video, nodeIndex, episode, errFlag, errMsg)
            setIsRunning(false)
            false
        } else true

    private fun notifyOnTargetFail(
        video: Video? = null,
        nodeIndex: Int? = null,
        episode: Episode? = null,
        flag: Int,
        msg: String
    ) {
        switchToUIThread(listener) {
            it?.onTargetError(video, nodeIndex, episode, flag, msg)
        }
    }

    override fun whenNullNotifyFail(o: Any?, errFlag: Int, errMsg: String?): Boolean {
        throw UnsupportedOperationException("Unsupported")
    }

    override fun whenNullNotifyFail(str: String?, errFlag: Int, msg: String?): Boolean {
        throw UnsupportedOperationException("Unsupported")
    }

    @Deprecated(
        "Don't call this method",
        replaceWith = ReplaceWith("use the {@link notifyOnTargetFail(Video, Int, Episode, Int, String} to instead.")
    )
    override fun notifyOnFailed(errCode: Int, errMsg: String?) {

    }
}