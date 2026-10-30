package eu.kanade.tachiyomi.extension.zh.comicbaby

import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import keiyoushi.utils.tryParse
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
class WorkList(val items: List<Work>, private val total: Int) {
    fun toMangasPage(page: Int, size: Int) = MangasPage(items.map { it.toManga() }, items.isNotEmpty() && page.toLong() * size < total)
}

@Serializable
class Updates(val items: List<WorkEntry>, val nextCursor: Cursor? = null)

@Serializable
class Cursor(private val cursor: String, private val cursorId: String)

@Serializable
class WorkEntry(val work: Work)

@Serializable
class Label(val name: String)

@Serializable
class Work(
    private val id: String,
    private val title: String,
    private val authorName: String? = null,
    private val displayAuthorName: String? = null,
    private val summary: String? = null,
    private val alternativeNames: String? = null,
    private val coverImageUrl: String? = null,
    private val serialStatus: String? = null,
    private val categories: List<Label> = emptyList(),
    private val tags: List<Label> = emptyList(),
    val chapters: List<Chapter> = emptyList(),
) {
    fun toManga() = SManga.create().apply {
        url = id
        title = this@Work.title
        author = displayAuthorName?.takeIf { it.isNotBlank() } ?: authorName
        thumbnail_url = coverImageUrl
        description = listOfNotNull(summary?.takeIf { it.isNotBlank() }, alternativeNames?.takeIf { it.isNotBlank() }?.let { "别名：$it" }).joinToString("\n\n")
        genre = (categories + tags).map { it.name }.distinct().joinToString()
        status = when (serialStatus?.lowercase()) {
            "ongoing" -> SManga.ONGOING
            "completed" -> SManga.COMPLETED
            "hiatus" -> SManga.ON_HIATUS
            else -> SManga.UNKNOWN
        }
    }
}

@Serializable
class Chapter(
    private val id: String,
    private val title: String,
    val chapterNumber: Float,
    private val publishedAt: String? = null,
) {
    fun toChapter() = SChapter.create().apply {
        url = id
        name = title.ifBlank { "第 ${chapterNumber.toString().removeSuffix(".0")} 话" }
        chapter_number = chapterNumber
        date_upload = publishedAt?.let { Instant.tryParse(it) } ?: 0L
    }
}

@Serializable
class Reader(val pages: List<ReaderPage> = emptyList(), val access: Access? = null)

@Serializable
class Access(val allowed: Boolean)

@Serializable
class ReaderPage(val index: Int, val imageUrl: String, val descrambleSegments: Int = 0)
