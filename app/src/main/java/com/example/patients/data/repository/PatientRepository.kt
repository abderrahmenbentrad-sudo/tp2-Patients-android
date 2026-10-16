package com.example.patients.data.repository

import com.example.patients.data.model.ApiError
import com.example.patients.data.model.Consultation
import com.example.patients.data.model.ConsultationRequest
import com.example.patients.data.model.Patient
import com.example.patients.data.model.PatientRequest
import com.example.patients.data.remote.PatientApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

class PatientRepository(
    private val api: PatientApi,
    private val json: Json
) {

    private suspend fun <T> safeCall(call: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(call())
        } catch (e: HttpException) {
            Result.failure(Exception(messageFrom(e)))
        } catch (e: IOException) {
            android.util.Log.e(
                "PatientRepository",
                "Erreur réseau : ${e.message}",
                e
            )
            Result.failure(
                Exception("Erreur réseau : ${e.message ?: "cause inconnue"}")
            )
        } catch (e: SerializationException) {
            Result.failure(Exception("Réponse inattendue du serveur."))
        }
    }

    private fun messageFrom(e: HttpException): String {
        val body = e.response()?.errorBody()?.string()
        val apiError = body?.let { runCatching { json.decodeFromString(ApiError.serializer(), it) }.getOrNull() }
        return when {
            apiError == null -> "Erreur ${e.code()}"
            apiError.erreurs.isNotEmpty() -> apiError.erreurs.joinToString("\n")
            else -> apiError.message
        }
    }

    suspend fun getPatients(statut: String? = null): Result<List<Patient>> = safeCall {
        api.getPatients(statut)
    }

    suspend fun getPatient(id: String): Result<Patient> = safeCall {
        api.getPatient(id)
    }

    suspend fun createPatient(patient: PatientRequest): Result<Patient> = safeCall {
        api.createPatient(patient)
    }

    suspend fun updatePatient(id: String, patient: PatientRequest): Result<Patient> = safeCall {
        api.updatePatient(id, patient)
    }

    suspend fun deletePatient(id: String): Result<Unit> = safeCall {
        val response = api.deletePatient(id)
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
        Unit
    }

    suspend fun getConsultations(patientId: String? = null): Result<List<Consultation>> = safeCall {
        api.getConsultations(patientId)
    }

    suspend fun createConsultation(request: ConsultationRequest): Result<Consultation> = safeCall {
        api.createConsultation(request)
    }
}