package org.gotson.komga.infrastructure.metadata.bangumi

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
import org.gotson.komga.domain.persistence.SeriesRepository
import org.gotson.komga.infrastructure.metadata.BookMetadataProvider
import org.gotson.komga.infrastructure.metadata.SeriesMetadataProvider
import org.gotson.komga.infrastructure.metadata.bangumi.view.SearchResult
import org.gotson.komga.infrastructure.metadata.bangumi.view.SubjectResult
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI

private val logger = KotlinLogging.logger {}

@Service
class BangumiProvider(
  private val seriesMetadataRepository: SeriesMetadataRepository,
) : BookMetadataProvider, SeriesMetadataProvider {
  val restClient: RestClient = RestClient.create()

  val searchUrl: (String) -> String = { subject -> "https://api.bgm.tv/search/subject/$subject?type=1&responseGroup=small" }
  val subjectUrl: (Int) -> String = { subject -> "https://api.bgm.tv/v0/subjects/$subject" }

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
    val searchResult = restClient.get()
      .uri(searchUrl(book.book.name))
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .body<SearchResult>()
    logger.debug { "searchResult: $searchResult" }
    if (searchResult != null) {
      return searchResult.list.firstOrNull {
        it.name == book.book.name
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
          links = listOf(WebLink(
            label = "bangumi",
            url = URI("http://bgm.tv/subject/${it.id}"),
          )),
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
    val searchResult = restClient.get()
      .uri(searchUrl(seriesMetadata.title))
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .body<SearchResult>()
    logger.debug { "searchResult: $searchResult" }
    if (searchResult != null) {
      return searchResult.list.firstOrNull {
        it.name == seriesMetadata.title || it.name_cn == seriesMetadata.title
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


  override fun shouldLibraryHandlePatch(library: Library, target: MetadataPatchTarget): Boolean =
    when (target) {
      MetadataPatchTarget.BOOK -> library.importComicInfoBook
      MetadataPatchTarget.SERIES -> library.importComicInfoSeries
      MetadataPatchTarget.READLIST -> library.importComicInfoReadList
      MetadataPatchTarget.COLLECTION -> library.importComicInfoCollection
    }
}
