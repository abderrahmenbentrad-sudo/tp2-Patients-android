package com.example.patients.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Consultation(
    @SerialName("_id") val _id: String? = null,
    val id: String? = null,
    val date: String,
    val motif: String,
    val diagnostic: String? = null,
    val notes: String? = null
) {
    val consultationId: String get() = id ?: _id ?: ""
}