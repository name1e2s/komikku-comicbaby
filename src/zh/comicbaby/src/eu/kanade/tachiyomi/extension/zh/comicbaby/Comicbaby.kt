package eu.kanade.tachiyomi.extension.zh.comicbaby

import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import keiyoushi.annotation.Source
import keiyoushi.network.get
import keiyoushi.network.rateLimit
import keiyoushi.source.KeiSource
import keiyoushi.utils.firstInstanceOrNull
import keiyoushi.utils.parseAs
import kotlinx.serialization.json.JsonElement
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

@Source
abstract class Comicbaby : KeiSource() {
    override fun Headers.Builder.configureHeaders() = apply {
        set("X-Platform", "h5")
        set("X-Locale", "zh-CN")
        set("X-Client-Capabilities", "img-aes-cbc-v1")
    }

    override fun OkHttpClient.Builder.configureClient() = rateLimit(3).addInterceptor(ImageInterceptor())

    override suspend fun getPopularManga(page: Int): MangasPage = browse(page, "popular")

    override suspend fun getLatestUpdates(page: Int): MangasPage {
        val url = "$baseUrl/api/catalog/updates".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.toString())
            .addQueryParameter("pageSize", PAGE_SIZE.toString())
            .build()
        val result = client.get(url).parseAs<Updates>()
        return MangasPage(result.items.map { it.work.toManga() }, result.nextCursor != null)
    }

    override suspend fun getSearchMangaList(page: Int, query: String, filters: FilterList): MangasPage {
        val sort = filters.firstInstanceOrNull<SortFilter>()?.value ?: "popular"
        val tag = filters.firstInstanceOrNull<TagFilter>()?.state?.trim().orEmpty()
        if (query.isBlank()) return browse(page, sort, tag)
        val url = "$baseUrl/api/catalog/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", query.trim())
            .addQueryParameter("page", page.toString())
            .addQueryParameter("pageSize", PAGE_SIZE.toString())
            .addQueryParameter("sort", sort)
            .apply { if (tag.isNotEmpty()) addQueryParameter("tag", tag) }
            .build()
        return client.get(url).parseAs<WorkList>().toMangasPage(page, PAGE_SIZE)
    }

    private suspend fun browse(page: Int, sort: String, tag: String = ""): MangasPage {
        val url = "$baseUrl/api/catalog/works".toHttpUrl().newBuilder()
            .addQueryParameter("type", "COMIC")
            .addQueryParameter("hasChapters", "true")
            .addQueryParameter("limit", PAGE_SIZE.toString())
            .addQueryParameter("offset", ((page - 1) * PAGE_SIZE).toString())
            .addQueryParameter("sort", sort)
            .apply { if (tag.isNotEmpty()) addQueryParameter("tag", tag) }
            .build()
        return client.get(url).parseAs<WorkList>().toMangasPage(page, PAGE_SIZE)
    }

    private suspend fun work(id: String) = client.get("$baseUrl/api/catalog/works/$id").parseAs<Work>()

    override suspend fun getMangaByUrl(url: HttpUrl): SManga? {
        if (url.host != baseUrl.toHttpUrl().host || url.pathSegments.firstOrNull() != "work") return null
        val id = url.pathSegments.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return null
        return work(id).toManga()
    }

    override fun getMangaUrl(manga: SManga) = "$baseUrl/work/${manga.url}"

    override fun getChapterUrl(chapter: SChapter) = "$baseUrl/chapter/${chapter.url}"

    override suspend fun fetchMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate {
        val result = work(manga.url)
        return SMangaUpdate(result.toManga(), result.chapters.sortedByDescending { it.chapterNumber }.map { it.toChapter() })
    }

    override val supportsRelatedMangas = true

    override suspend fun fetchRelatedMangaList(manga: SManga): List<SManga> = client.get("$baseUrl/api/recommend/related/${manga.url}?limit=18")
        .parseAs<List<WorkEntry>>()
        .map { it.work.toManga() }
        .filter { it.url != manga.url }
        .distinctBy { it.title.trim() }
        .take(12)

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        val result = client.get("$baseUrl/api/assets/chapters/${chapter.url}/read").parseAs<Reader>()
        // Do not cache a limited preview as a complete downloaded chapter.
        check(result.access?.allowed != false) { "此章节需要登录或会员权限，请在网站查看。" }
        return result.pages.sortedBy { it.index }.mapIndexed { index, page ->
            val url = page.imageUrl.toHttpUrl().newBuilder()
                .fragment("segments=${page.descrambleSegments}")
                .build().toString()
            Page(index, imageUrl = url)
        }
    }

    override fun getFilterList(data: JsonElement?) = FilterList(SortFilter(), TagFilter())

    private class SortFilter : Filter.Select<String>("排序", arrayOf("最多点阅", "最新更新", "最多爱心", "最多评论")) {
        val value get() = arrayOf("popular", "updatedAt", "favorites", "comments")[state]
    }

    private class TagFilter : Filter.Text("标签")

    companion object {
        private const val PAGE_SIZE = 24
    }
}
