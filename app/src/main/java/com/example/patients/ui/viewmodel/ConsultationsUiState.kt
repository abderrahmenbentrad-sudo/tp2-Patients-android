package com.example.patients.ui.viewmodel

import com.example.patients.data.model.Consultation

sealed interface ConsultationsUiState {
    data object Loading : ConsultationsUiState
    data class Success(val consultations: List<Consultation>) : ConsultationsUiState
    data class Error(val message: String) : ConsultationsUiState
}