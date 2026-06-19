package dev.themselg.jellymusic.data.repository

import dev.themselg.jellymusic.data.session.SessionManagerImpl
import dev.themselg.jellymusic.domain.model.Lyrics
import dev.themselg.jellymusic.domain.repository.LyricsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.api.client.extensions.lyricsApi
import dev.themselg.jellymusic.domain.model.LyricLine as DomainLyricLine
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsRepositoryImpl @Inject constructor(
    private val sessionManager: SessionManagerImpl,
) : LyricsRepository {

    override suspend fun getLyrics(songId: String): Lyrics? = withContext(Dispatchers.IO) {
        runCatching {
            val api = sessionManager.requireApi()
            // SDK: ApiClient.lyricsApi.getLyrics(itemId: UUID) -> Response<LyricDto>.
            // Response<T> supports `by` delegation to unwrap `.content`.
            // LyricDto.lyrics: List<LyricLine>; each LyricLine has text: String and start: Long? (ticks, 100ns).
            val dto by api.lyricsApi.getLyrics(itemId = UUID.fromString(songId))
            val lines = dto.lyrics.map { line ->
                DomainLyricLine(
                    text = line.text,
                    // Ticks (100ns) -> ms, matching the runTimeTicksToMs convention in ItemMappers.
                    startMs = line.start?.let { it / 10_000 },
                )
            }
            if (lines.isEmpty()) return@runCatching null
            Lyrics(
                lines = lines,
                synced = lines.any { it.startMs != null },
            )
        }.getOrNull()
    }
}
