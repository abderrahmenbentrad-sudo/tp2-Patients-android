package com.example.patients.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.patients.data.model.ConsultationRequest
import com.example.patients.data.repository.PatientRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConsultationsViewModel(
    private val patientId: String?,
    private val repository: PatientRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConsultationsUiState>(ConsultationsUiState.Loading)
    val uiState: StateFlow<ConsultationsUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        loadConsultations()
    }

    fun loadConsultations() {
        _uiState.value = ConsultationsUiState.Loading
        viewModelScope.launch {
            repository.getConsultations(patientId)
                .onSuccess { consultations ->
                    _uiState.value = ConsultationsUiState.Success(consultations)
                }
                .onFailure { e ->
                    _uiState.value = ConsultationsUiState.Error(e.message ?: "Erreur inconnue")
                }
        }
    }

    fun createConsultation(request: ConsultationRequest) {
        viewModelScope.launch {
            repository.createConsultation(request)
                .onSuccess {
                    loadConsultations()
                    _messages.emit("Consultation ajoutée avec succès")
                }
                .onFailure { e ->
                    _messages.emit(e.message ?: "Échec de l'ajout de la consultation")
                }
        }
    }

    companion object {
        fun provideFactory(
            patientId: String?,
            repository: PatientRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ConsultationsViewModel(patientId, repository)
            }
        }
    }
}