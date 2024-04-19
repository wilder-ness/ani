package me.him188.ani.datasources.api.source

import me.him188.ani.datasources.api.DefaultMedia
import me.him188.ani.datasources.api.EpisodeSort
import me.him188.ani.datasources.api.MediaProperties
import me.him188.ani.datasources.api.paging.SizedSource
import me.him188.ani.datasources.api.paging.map
import me.him188.ani.datasources.api.paging.merge
import me.him188.ani.datasources.api.topic.Alliance
import me.him188.ani.datasources.api.topic.Resolution
import me.him188.ani.datasources.api.topic.Topic
import me.him188.ani.datasources.api.topic.TopicCategory

/**
 * A [remote][MediaSourceLocation.ONLINE] [MediaSource] that uses [Topic] internally.
 */
abstract class TopicMediaSource : MediaSource {
    override val location: MediaSourceLocation get() = MediaSourceLocation.ONLINE

    private fun Topic.toOnlineMedia(): DefaultMedia {
        val details = details
        return DefaultMedia(
            mediaId = "$mediaSourceId.${topicId}",
            mediaSourceId = mediaSourceId,
            originalUrl = originalLink,
            download = downloadLink,
            originalTitle = rawTitle,
            publishedTime = publishedTimeMillis ?: 0,
            episodes = details?.episodeRange?.sorts?.toList() ?: emptyList(),
            properties = MediaProperties(
                subtitleLanguageIds = details?.subtitleLanguages?.map { it.id } ?: emptyList(),
                resolution = details?.resolution?.toString() ?: Resolution.R1080P.toString(),
                alliance = alliance,
                size = size,
            ),
        )
    }

    // For backward compatibility
    protected abstract suspend fun startSearch(query: DownloadSearchQuery): SizedSource<Topic>

    final override suspend fun fetch(query: MediaFetchRequest): SizedSource<MediaMatch> {
        return query.subjectNames
            .map { name ->
                startSearch(
                    DownloadSearchQuery(
                        keywords = name,
                        category = TopicCategory.ANIME,
                        episodeSort = query.episodeSort,
                        episodeName = query.episodeName,
                    )
                ).map {
                    MediaMatch(it.toOnlineMedia(), MatchKind.FUZZY)
                }
            }.merge()
    }
}

// Only used by data source modules
data class DownloadSearchQuery(
    val keywords: String? = null,
    val category: TopicCategory? = null,
    val alliance: Alliance? = null,
    val ordering: SearchOrdering? = null,
    val episodeSort: EpisodeSort? = null,
    val episodeName: String? = null,
)
