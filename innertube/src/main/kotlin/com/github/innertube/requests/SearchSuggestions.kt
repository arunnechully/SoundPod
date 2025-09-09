package com.github.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import com.github.innertube.Innertube
import com.github.innertube.models.SearchSuggestionsResponse
import com.github.innertube.models.bodies.SearchSuggestionsBody
import com.github.innertube.utils.runCatchingNonCancellable

suspend fun Innertube.searchSuggestions(input: String) = runCatchingNonCancellable {
    val response = client.post(SEARCH_SUGGESTIONS) {
        setBody(SearchSuggestionsBody(input = input))
        mask("contents.searchSuggestionsSectionRenderer.contents.searchSuggestionRenderer.navigationEndpoint.searchEndpoint.query")
    }.body<SearchSuggestionsResponse>()

    response
        .contents
        ?.firstOrNull()
        ?.searchSuggestionsSectionRenderer
        ?.contents
        ?.mapNotNull { content ->
            content
                .searchSuggestionRenderer
                ?.navigationEndpoint
                ?.searchEndpoint
                ?.query
        }
}