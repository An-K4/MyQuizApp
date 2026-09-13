package android.kma.myquizzapp.feature.home.domain.discover

enum class DiscoverSort(val apiValue: String) {
    NEWEST("newest"),
    OLDEST("oldest"),
    MOST_PLAYED("most_played"),
    TRENDING("trending"),
    NAME_ASC("name_asc"),
    NAME_DESC("name_desc")
}

sealed interface DiscoverFilter {
    data object All : DiscoverFilter
    data class Category(val topic: String, val label: String) : DiscoverFilter
}

data class DiscoverQuery(
    val filter: DiscoverFilter = DiscoverFilter.All,
    val sort: DiscoverSort = DiscoverSort.NEWEST
)

data class DiscoverRequest(
    val initialQuery: DiscoverQuery,
    val filters: List<DiscoverFilter>
)

private val DEFAULT_CATEGORY_FILTERS = listOf(
    DiscoverFilter.Category(topic = "General", label = "Tổng hợp"),
    DiscoverFilter.Category(topic = "Science", label = "Khoa học"),
    DiscoverFilter.Category(topic = "Geography", label = "Địa lý"),
    DiscoverFilter.Category(topic = "Movies", label = "Phim ảnh"),
    DiscoverFilter.Category(topic = "Sports", label = "Thể thao"),
    DiscoverFilter.Category(topic = "Music", label = "Âm nhạc")
)

fun resolveDiscoverRequest(sectionType: String?, title: String?, topic: String?): DiscoverRequest {
    val routedTopic = topic?.trim()?.takeIf { it.isNotEmpty() }
    val category = routedTopic?.let { value ->
        DEFAULT_CATEGORY_FILTERS.firstOrNull { it.topic.equals(value, ignoreCase = true) }
            ?: DiscoverFilter.Category(
                topic = value,
                label = title?.takeIf(String::isNotBlank) ?: value
            )
    }
    val initialQuery = when (sectionType) {
        "trending" -> DiscoverQuery(sort = DiscoverSort.TRENDING)
        "category" -> category?.let { DiscoverQuery(it, DiscoverSort.TRENDING) } ?: DiscoverQuery()
        "newest" -> DiscoverQuery(sort = DiscoverSort.NEWEST)
        else -> DiscoverQuery()
    }
    val customCategory = category?.takeUnless { selected ->
        DEFAULT_CATEGORY_FILTERS.any { it.topic.equals(selected.topic, ignoreCase = true) }
    }
    return DiscoverRequest(
        initialQuery = initialQuery,
        filters = listOf(DiscoverFilter.All) + DEFAULT_CATEGORY_FILTERS + listOfNotNull(customCategory)
    )
}
