package com.example.patients.ui.viewmodel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.patients.data.model.Consultation
import com.example.patients.data.repository.PatientRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsultationsScreen(
    patientId: String,
    repository: PatientRepository,
    onBack: () -> Unit
) {
    val viewModel: ConsultationsViewModel = viewModel(
        factory = ConsultationsViewModel.provideFactory(patientId, repository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Consultations du patient") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadConsultations() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualiser")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is ConsultationsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ConsultationsUiState.Success -> {
                    if (state.consultations.isEmpty()) {
                        Text(
                            text = "Aucune consultation trouvée pour ce patient",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.consultations, key = { it.consultationId }) { consultation ->
                                ConsultationCard(consultation = consultation)
                            }
                        }
                    }
                }
                is ConsultationsUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Erreur: ${state.message}", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadConsultations() }) {
                            Text("Réessayer")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConsultationCard(consultation: Consultation) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Date : ${consultation.date}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Motif : ${consultation.motif}",
                style = MaterialTheme.typography.bodyMedium
            )
            consultation.diagnostic?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Diagnostic : $it",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            consultation.notes?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Notes : $it",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}