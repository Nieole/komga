package org.gotson.komga.domain.model

data class SeriesMetadataPatch(
  val title: String? = null,
  val titleSort: String? = null,
  val status: SeriesMetadata.Status? = null,
  val summary: String? = null,
  val readingDirection: SeriesMetadata.ReadingDirection? = null,
  val publisher: String? = null,
  val ageRating: Int? = null,
  val language: String? = null,
  val genres: Set<String>? = null,
  val totalBookCount: Int? = null,
  val collections: Set<String> = emptySet(),
  val tags: Set<String>? = emptySet(),
  val links: List<WebLink>? = emptyList(),
  val alternateTitles: List<AlternateTitle>? = emptyList(),
)
