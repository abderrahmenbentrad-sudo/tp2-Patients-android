package com.example.patients.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject

object PatientOrStringSerializer : KSerializer<Patient?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("PatientOrString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Patient?) {
        if (value != null) {
            encoder.encodeSerializableValue(Patient.serializer(), value)
        } else {
            encoder.encodeNull()
        }
    }

    override fun deserialize(decoder: Decoder): Patient? {
        val input = decoder as? JsonDecoder ?: return null
        val element = input.decodeJsonElement()
        return if (element is JsonObject) {
            input.json.decodeFromJsonElement(Patient.serializer(), element)
        } else {
            null
        }
    }
}

@Serializable
data class Consultation(
    @SerialName("_id") val _id: String? = null,
    val id: String? = null,
    val date: String,
    val motif: String,
    val diagnostic: String? = null,
    val notes: String? = null,
    @Serializable(with = PatientOrStringSerializer::class)
    val patient: Patient? = null
) {
    val consultationId: String get() = id ?: _id ?: ""
}

@Serializable
data class ConsultationRequest(
    val patient: String,
    val date: String,
    val motif: String,
    val diagnostic: String? = null,
    val notes: String? = null
)