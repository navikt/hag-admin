package no.nav.hag.domain

import kotlinx.serialization.Serializable

@Serializable
data class MemStats(
    val count: Int,
    val max__Memory: Long,
    val totalMemory: Long,
    val _usedMemory: Long,
    val _freeMemory: Long,
)
