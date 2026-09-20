package com.jcompute.mermaid.data

import kotlinx.serialization.Serializable

@Serializable
data class Diagram(
    val id: String,
    val title: String,
    val source: String,
    val updatedAtMillis: Long,
    val originName: String? = null,
)
