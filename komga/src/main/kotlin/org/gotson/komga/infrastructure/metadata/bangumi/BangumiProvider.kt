package org.gotson.komga.infrastructure.metadata.bangumi

import com.github.houbb.opencc4j.util.ZhConverterUtil
import io.github.oshai.kotlinlogging.KotlinLogging
import org.gotson.komga.domain.model.BookMetadataPatch
import org.gotson.komga.domain.model.BookMetadataPatchCapability
import org.gotson.komga.domain.model.BookWithMedia
import org.gotson.komga.domain.model.Library
import org.gotson.komga.domain.model.MetadataPatchTarget
import org.gotson.komga.domain.model.Series
import org.gotson.komga.domain.model.SeriesMetadata
import org.gotson.komga.domain.model.SeriesMetadataPatch
import org.gotson.komga.domain.model.WebLink
import org.gotson.komga.domain.persistence.SeriesMetadataRepository
import org.gotson.komga.infrastructure.metadata.BookMetadataProvider
import org.gotson.komga.infrastructure.metadata.SeriesMetadataProvider
import org.gotson.komga.infrastructure.metadata.bangumi.view.SameResult
import org.gotson.komga.infrastructure.metadata.bangumi.view.SearchResult
import org.gotson.komga.infrastructure.metadata.bangumi.view.SubjectResult
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI
import java.util.regex.Pattern

private val logger = KotlinLogging.logger {}

@Service
class BangumiProvider(
  private val seriesMetadataRepository: SeriesMetadataRepository,
) : BookMetadataProvider, SeriesMetadataProvider {
  val restClient: RestClient = RestClient.create()

  val searchUrl: (String) -> String = { subject -> "https://api.bgm.tv/search/subject/$subject?type=1&responseGroup=small" }
  val subjectUrl: (Int) -> String = { subject -> "https://api.bgm.tv/v0/subjects/$subject" }
  val sameUsl: (String, String) -> String = { s1: String, s2: String -> "http://127.0.0.1:8000/sts?s1=$s1&s2=$s2" }

  // 正则表达式匹配完整的方括号对 [内容]
  val pattern = Pattern.compile("\\[[^]]*]")

  override val capabilities: Set<BookMetadataPatchCapability> = setOf(
    BookMetadataPatchCapability.TITLE,
    BookMetadataPatchCapability.SUMMARY,
    BookMetadataPatchCapability.NUMBER,
    BookMetadataPatchCapability.NUMBER_SORT,
    BookMetadataPatchCapability.RELEASE_DATE,
    BookMetadataPatchCapability.AUTHORS,
    BookMetadataPatchCapability.TAGS,
    BookMetadataPatchCapability.ISBN,
    BookMetadataPatchCapability.READ_LISTS,
    BookMetadataPatchCapability.THUMBNAILS,
    BookMetadataPatchCapability.LINKS,
  )

  override fun getBookMetadataFromBook(book: BookWithMedia): BookMetadataPatch? {
    logger.debug { "getBookMetadataFromBook $book" }
    val searchUrlVal = searchUrl(book.book.name)
    logger.info { "searchUrl : $searchUrlVal" }
    val searchResult = restClient.get()
      .uri(searchUrlVal)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .body<SearchResult>()
    logger.debug { "searchResult: $searchResult" }
    if (searchResult != null) {
      return searchResult.list.firstOrNull {
        same(it.name,book.book.name)
      }?.let {
        logger.debug { "Found series $it in search result" }
        restClient.get()
          .uri(subjectUrl(it.id))
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body<SubjectResult>()
      }?.let {
        return BookMetadataPatch(
          title = it.name ?: it.name_cn,
          summary = it.summary,
          releaseDate = null,
          authors = null,
          links = listOf(
            WebLink(
              label = "bangumi",
              url = URI("http://bgm.tv/subject/${it.id}"),
            ),
          ),
          tags = it.tags?.filter { it.name != null }
            ?.map { it.name!! }
            ?.toSet(),
        )
      }
    }
    return null
  }

  override fun getSeriesMetadata(series: Series): SeriesMetadataPatch? {
    logger.debug { "getSeriesMetadata $series" }
    val seriesMetadata = seriesMetadataRepository.findById(series.id)
    val seriesTitle = getSeriesTitle(seriesMetadata.title)
    val searchUrlVal = searchUrl(seriesTitle)
    logger.info { "searchUrl : $searchUrlVal" }
    val searchResult = restClient.get()
      .uri(searchUrlVal)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .body<SearchResult>()
    logger.debug { "searchResult: $searchResult" }
    if (searchResult != null) {
      return searchResult.list.firstOrNull {
        same(it.name,seriesTitle) || same(it.name_cn, seriesTitle)
      }?.let {
        logger.debug { "Found series $it in search result" }
        restClient.get()
          .uri(subjectUrl(it.id))
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body<SubjectResult>()
      }
        ?.let {
          logger.debug { "Found subject $it in search result" }
          return SeriesMetadataPatch(
            title = it.name_cn,
            titleSort = it.name_cn,
            status = null,
            summary = it.summary,
            readingDirection = SeriesMetadata.ReadingDirection.RIGHT_TO_LEFT,
            publisher = null,
            ageRating = null,
            language = null,
            genres = it.platform?.let {
              setOf(it)
            },
            totalBookCount = it.volumes ?: it.total_episodes,
            collections = emptySet(),
            tags = it.tags?.filter { it.name != null }
              ?.map { it.name!! }
              ?.toSet(),
            links = listOf(
              WebLink(
                label = "bangumi",
                url = URI("http://bgm.tv/subject/${it.id}"),
              ),
            ),
            alternateTitles = null,
          )
        }
    } else {
      return null
    }
  }

  private fun same(s1: String?, s2: String): Boolean {
    if (s1 == null) {
      return false
    }
    val sameResult = restClient.get()
      .uri(sameUsl(s1,s2))
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .body<SameResult>()
    return sameResult?.result?.first()?.let {
      it > 0.9
    } ?: false
  }

  /**
   * 提取标题
   * 1. 不包含[]直接转换成简体
   * 2. 只包含一个[]取出来转换成简体
   * 3. 包含两个以上[]取第二个转换成简体
   */
  private fun getSeriesTitle(title: String): String {
    val countBracket = countBracket(title)
    val name = when (countBracket.size) {
      0 -> title
      1 -> countBracket[0]
      else -> countBracket[1]
    }
    return ZhConverterUtil.toSimple(name)
  }

  private fun countBracket(name: String): List<String> {
    val matcher = pattern.matcher(name)

    val result = mutableListOf<String>()
    while (matcher.find()) {
      result.add(matcher.group(1))
    }
    return result
  }

  override fun shouldLibraryHandlePatch(library: Library, target: MetadataPatchTarget): Boolean =
    when (target) {
      MetadataPatchTarget.BOOK -> library.importComicInfoBook
      MetadataPatchTarget.SERIES -> library.importComicInfoSeries
      MetadataPatchTarget.READLIST -> library.importComicInfoReadList
      MetadataPatchTarget.COLLECTION -> library.importComicInfoCollection
    }
}
