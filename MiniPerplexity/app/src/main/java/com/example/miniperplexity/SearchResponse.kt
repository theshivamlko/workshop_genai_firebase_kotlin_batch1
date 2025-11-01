package com.example.miniperplexity

// Holds text response and optional list of sources
data class SearchResponse(
    val textResponse: String = "",
    val links: List<WebLink> = emptyList()
)

