package com.github.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class Tabs(
    val tabs: List<Tab>?
) {
    @Serializable
    data class Tab(
        val tabRenderer: TabRenderer?
    ) {
        @Serializable
        data class TabRenderer(
            val content: Content? = null,
            val title: String?,
            val tabIdentifier: String? = null,
            val navigationEndpoint: NavigationEndpoint? = null
        ) {
            @Serializable
            data class Content(
                val sectionListRenderer: SectionListRenderer?
            )
        }
    }
}

@Serializable
data class TwoColumnBrowseResultsRenderer(
    val tabs: List<Tabs.Tab>?,
    val secondaryContents: Tabs.Tab.TabRenderer.Content?
)