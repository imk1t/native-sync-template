package com.example.corenetwork

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
data class RepositorySummary(
    val id: Long,
    val name: String,
    val fullName: String,
    val owner: String,
    val avatarURL: String? = null,
    val description: String? = null,
    val language: String? = null,
    @Required val stars: Int = 0,
    @Required val forks: Int = 0,
    @Required val topics: List<String> = emptyList(),
    @Required val htmlURL: String = ""
)
