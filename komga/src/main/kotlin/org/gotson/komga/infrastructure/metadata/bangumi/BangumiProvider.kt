package org.gotson.komga.infrastructure.metadata.bangumi

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.gotson.komga.domain.model.Library
import org.gotson.komga.domain.model.MetadataPatchTarget
import org.gotson.komga.domain.model.Series
import org.gotson.komga.domain.model.SeriesMetadata
import org.gotson.komga.domain.model.SeriesMetadataPatch
import org.gotson.komga.domain.model.WebLink
import org.gotson.komga.domain.persistence.SeriesMetadataRepository
import org.gotson.komga.infrastructure.metadata.SeriesMetadataProvider
import org.gotson.komga.infrastructure.metadata.bangumi.view.SubjectSearchRequest
import org.gotson.komga.infrastructure.metadata.bangumi.view.SubjectSearchResult
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI

private val logger = KotlinLogging.logger {}

@Service
class BangumiProvider(
  private val seriesMetadataRepository: SeriesMetadataRepository,
  private val restClient: RestClient,
  private val objectMapper: ObjectMapper,
) : SeriesMetadataProvider {
  override fun getSeriesMetadata(series: Series): SeriesMetadataPatch? {
    logger.info { "getSeriesMetadata by bangumi $series" }
    val seriesMetadata = seriesMetadataRepository.findById(series.id)
    val seriesTitle = getSeriesTitle(seriesMetadata.title)

    val subjectSearchResult =
      restClient
        .post()
        .uri("https://api.bgm.tv/v0/search/subjects")
        .contentType(MediaType.APPLICATION_JSON)
        .body(objectMapper.writeValueAsString(SubjectSearchRequest(seriesTitle)))
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .body<SubjectSearchResult>()
    logger.info { "searchResult: $subjectSearchResult " }
    if (subjectSearchResult == null) {
      return null
    }
    val result =
      if (subjectSearchResult.total == 1) {
        subjectSearchResult.data?.firstOrNull()
      } else {
        subjectSearchResult.data?.firstOrNull {
          it.platform == "漫画" && (same(it.name, seriesTitle) || same(it.name_cn, seriesTitle))
        }
      }

    return result?.let {
      logger.debug { "Found subject $it in search result" }
      return SeriesMetadataPatch(
//        title = notBlankName(it.name_cn, it.name, seriesMetadata.title),
        status = null,
        summary = it.summary,
        readingDirection = SeriesMetadata.ReadingDirection.RIGHT_TO_LEFT,
        publisher = null,
        ageRating = null,
        language = "zh-CN",
        score = it.rating?.score,
        genres =
          it.platform?.let {
            setOf(it)
          },
        totalBookCount = it.volumes ?: it.total_episodes,
        collections = emptySet(),
        tags =
          it.tags
            ?.filter { it.name != null }
            ?.map { it.name!! }
            ?.toSet(),
        links =
          listOf(
            WebLink(
              label = "bangumi",
              url = URI("http://bgm.tv/subject/${it.id}"),
            ),
          ),
        alternateTitles = null,
      )
    }
  }

  private fun same(
    s1: String?,
    s2: String,
  ): Boolean = true

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
