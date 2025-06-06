package org.gotson.komga.infrastructure.metadata.dmzj

import io.github.oshai.kotlinlogging.KotlinLogging
import org.gotson.komga.domain.model.Author
import org.gotson.komga.domain.model.Library
import org.gotson.komga.domain.model.MetadataPatchTarget
import org.gotson.komga.domain.model.Series
import org.gotson.komga.domain.model.SeriesMetadata
import org.gotson.komga.domain.model.SeriesMetadataPatch
import org.gotson.komga.domain.persistence.BookRepository
import org.gotson.komga.domain.persistence.SeriesMetadataRepository
import org.gotson.komga.infrastructure.jooq.main.BookMetadataDao
import org.gotson.komga.infrastructure.metadata.SeriesMetadataProvider
import org.gotson.komga.infrastructure.metadata.dmzj.view.DmzjComicView
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

//@Service
class DmzjProvider(
  private val seriesMetadataRepository: SeriesMetadataRepository,
  private val bookRepository: BookRepository,
  private val restClient: RestClient,
  private val bookMetadataDao: BookMetadataDao,
) : SeriesMetadataProvider {

  private val logger = KotlinLogging.logger {}

  @Value("\${dmzj.url:http://localhost:8080}")
  lateinit var dmzjUrl: String

  override fun getSeriesMetadata(series: Series): SeriesMetadataPatch? {
    logger.debug { "getSeriesMetadata $series" }
    val seriesMetadata = seriesMetadataRepository.findById(series.id)
    val seriesTitle = getSeriesTitle(seriesMetadata.titleSort)
    return restClient.post()
      .uri("${dmzjUrl}/comic")
      .body(DmzjRequest(seriesTitle))
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .body<DmzjComicView>()
      ?.let { comicView ->

        var authors = comicView.authors
          ?.filter { author -> author.name?.isNotBlank() == true }
          ?.map { author ->
            Author(author.name!!, "writer")
          }

        if (authors?.isNotEmpty() == true) {
          bookRepository.findAllBySeriesId(series.id)
            .forEach { book ->
              var originAuthors = bookMetadataDao.findById(book.id).authors
              bookMetadataDao.insertAuthors(book.id, authors.filter {
                !originAuthors.contains(it)
              })
            }
        }

        return SeriesMetadataPatch(
          title = notBlankName(comicView.title, seriesMetadata.titleSort),
          status = when (comicView.status) {
            null -> null
            "已完结" -> SeriesMetadata.Status.ENDED
            "连载中" -> SeriesMetadata.Status.ONGOING
            else -> null
          },
          summary = comicView.description,
          readingDirection = SeriesMetadata.ReadingDirection.RIGHT_TO_LEFT,
          publisher = null,
          ageRating = null,
          language = "zh-CN",
          score = seriesMetadata.score,
          genres = seriesMetadata.genres,
//          totalBookCount = it.volumes ?: it.total_episodes,
//          collections = emptySet(),
          tags = seriesMetadata.tags +
            (comicView.types
              ?.filter { it.name != null }
              ?.map { it.name!! }
              ?.toSet() ?: emptySet()),
          links = seriesMetadata.links,
          alternateTitles = null,
        )
      }
  }

  override fun shouldLibraryHandlePatch(
    library: Library,
    target: MetadataPatchTarget,
  ): Boolean =
    when (target) {
      MetadataPatchTarget.BOOK -> library.importComicInfoBook
      MetadataPatchTarget.SERIES -> library.importComicInfoSeries
      MetadataPatchTarget.READLIST -> library.importComicInfoReadList
      MetadataPatchTarget.COLLECTION -> library.importComicInfoCollection
    }
}

data class DmzjRequest(val title: String)
