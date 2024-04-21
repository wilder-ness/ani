package me.him188.ani.danmaku.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.transformLatest
import me.him188.ani.danmaku.protocol.DanmakuInfo
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

interface DanmakuSession {
    val totalCount: Flow<Int?> get() = emptyFlow()

    /**
     * 创建一个随视频进度 [progress] 匹配到的弹幕数据流.
     *
     * 每当有一个新的 [Duration] 从 [progress] emit, 本函数返回的 flow 都会 emit 一些新的 [Danmaku] 对象, 表示在该视频进度 [Duration] 匹配到的弹幕列表.
     *
     * 当有新的 [Duration] 从 [progress] emit, 并且上一个时间点匹配到的弹幕列表还没被完全 collect 时, 将会抛弃上一个时间点匹配到的弹幕列表, 并且从新的时间点开始匹配.
     *
     * ### Flow 终止
     *
     * 当 [progress] [完结][Flow.onCompletion] 时, 本函数返回的 flow 也会 [完结][Flow.onCompletion].
     */
    fun at(progress: Flow<Duration>): Flow<Danmaku>
}

fun emptyDanmakuSession(): DanmakuSession {
    return object : DanmakuSession {
        override fun at(progress: Flow<Duration>): Flow<Danmaku> = emptyFlow()
    }
}

class TimeBasedDanmakuSession private constructor(
    /**
     * List of danmaku. Must be sorted by [DanmakuInfo.playTime], and must not change after construction.
     */
    private val list: List<Danmaku>,
    private val shiftMillis: Long = 0,
) : DanmakuSession {
    override val totalCount: Flow<Int?> = flowOf(list.size)

    companion object {
        fun create(
            sequence: Sequence<Danmaku>,
            shiftMillis: Long = 0,
        ): DanmakuSession {
            val list = sequence.toCollection(ArrayList())
            list.sortBy { it.playTimeMillis }
            return TimeBasedDanmakuSession(list, shiftMillis)
        }
    }

    override fun at(progress: Flow<Duration>): Flow<Danmaku> {
        if (list.isEmpty()) {
            return emptyFlow() // fast path
        }

        var lastTime: Duration = Duration.ZERO
        var lastIndex = -1// last index at which we accessed [list]
        return progress.map { it - shiftMillis.milliseconds }.transformLatest { curTime ->
            if (curTime < lastTime) {
                // Went back, reset position so we restart from the beginning
                lastIndex = -1
            }

            lastTime = curTime

            val curTimeSecs = curTime.inWholeMilliseconds

            for (i in (lastIndex + 1)..list.lastIndex) {
                val item = list[i]
                if (curTimeSecs >= item.playTimeMillis) {
                    // 达到了弹幕发送的时间
                    lastIndex = i
                    emit(item) // Note: 可能会因为有新的 [curTime] 而 cancel
                } else {
                    // not yet, 因为 list 是排序的, 这也说明后面的弹幕都还没到时间
                    break
                }
            }
        }
    }
}