package org.gotson.komga.infrastructure.metadata.bangumi.view

data class SearchResult(
  val list: List<ListItem> = emptyList(),
  val results: Int? = null
)

data class Count(
	val jsonMember1: Int? = null,
	val jsonMember2: Int? = null,
	val jsonMember3: Int? = null,
	val jsonMember4: Int? = null,
	val jsonMember5: Int? = null,
	val jsonMember6: Int? = null,
	val jsonMember7: Int? = null,
	val jsonMember8: Int? = null,
	val jsonMember9: Int? = null,
	val jsonMember10: Int? = null
)

data class Images(
	val small: String? = null,
	val large: String? = null,
	val common: String? = null,
	val grid: String? = null,
	val medium: String? = null
)

data class ListItem(
	val summary: String? = null,
	val images: Images? = null,
	val airWeekday: Int? = null,
	val rating: Rating? = null,
	val eps: Int? = null,
	val epsCount: Int? = null,
	val collection: Collection? = null,
	val type: Int? = null,
	val url: String? = null,
	val airDate: String? = null,
	val name: String? = null,
	val rank: Int? = null,
	val id: Int,
  val name_cn: String? = null
)

data class Collection(
	val wish: Int? = null,
	val doing: Int? = null,
	val dropped: Int? = null,
	val onHold: Int? = null,
	val collect: Int? = null
)

