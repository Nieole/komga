package org.gotson.komga.infrastructure.metadata.bangumi.view

data class SubjectSearchRequest(
  val keyword: String,
  val filter: Filter = Filter(),
)

data class Filter(
  val type: List<Int> = listOf(1),
  val nsfw: Boolean = true,
)

data class SubjectSearchResult(
  val data: List<SubjectResult>? = null,
  val total: Int? = null,
  val limit: Int? = null,
  val offset: Int? = null,
)

data class SubjectResult(
  val date: String? = null,
  val summary: String? = null,
  val images: Images? = null,
  val image: String? = null,
  val nsfw: Boolean? = null,
  val rating: Rating? = null,
  val volumes: Int? = null,
  val infobox: List<Any>,
  val eps: Int? = null,
  val collection: Collection? = null,
  val type: Int? = null,
  val platform: String? = null,
  val tags: List<TagsItem>? = null,
  val total_episodes: Int? = null,
  val series: Boolean? = null,
  val name: String? = null,
  val id: Int? = null,
  val locked: Boolean? = null,
  val name_cn: String? = null,
)

data class TagsItem(
  val name: String? = null,
  val count: Int? = null,
  val total_cont: Int? = null,
)

data class Rating(
  val score: Double? = null,
  val total: Int? = null,
  val count: Count? = null,
  val rank: Int? = null,
)
