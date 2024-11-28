package org.gotson.komga.infrastructure.metadata.dmzj.view

data class DmzjComicView(
	val cover: String? = null,
	val types: List<TypesItem>? = null,
	val num: Int? = null,
	val description: String? = null,
	val id: Int? = null,
	val title: String? = null,
	val status: String? = null,
	val authors: List<AuthorsItem>? = null
)

data class TypesItem(
	val name: String? = null,
	val id: Int? = null
)

data class AuthorsItem(
	val name: String? = null,
	val id: Int? = null
)

