package org.gotson.komga.infrastructure.metadata.bangumi.view

data class SubjectResult(
	val date: Any? = null,
	val summary: String? = null,
	val images: Images? = null,
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
	val count: Int? = null
)

data class Rating(
	val score: Double? = null,
	val total: Int? = null,
	val count: Count? = null,
	val rank: Int? = null
)
