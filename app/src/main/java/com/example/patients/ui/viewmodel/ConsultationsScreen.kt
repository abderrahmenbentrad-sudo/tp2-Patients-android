package com.example.patients.ui.viewmodel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.patients.data.model.Consultation
import com.example.patients.data.model.ConsultationRequest
import com.example.patients.data.model.Patient
import com.example.patients.data.repository.PatientRepository
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsultationsScreen(
    patientId: String? = null,
    repository: PatientRepository,
    onBack: (() -> Unit)? = null
) {
    val viewModel: ConsultationsViewModel = viewModel(
        factory = ConsultationsViewModel.provideFactory(patientId, repository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Charger la liste des patients pour le menu déroulant du formulaire d'ajout
    var patientsList by remember { mutableStateOf<List<Patient>>(emptyList())  }
    LaunchedEffect(Unit) {
        repository.getPatients().onSuccess { patientsList = it }
        viewModel.messages.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (patientId == null) "Toutes les Consultations" else "Consultations du patient") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadConsultations() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualiser")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nouvelle consultation") }
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
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Aucune consultation trouvée",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
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

            if (showAddDialog) {
                AddConsultationDialog(
                    patients = patientsList,
                    defaultPatientId = patientId,
                    onDismiss = { showAddDialog = false },
                    onConfirm = { request ->
                        viewModel.createConsultation(request)
                        showAddDialog = false
                    }
                )
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
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            consultation.notes?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Notes : $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddConsultationDialog(
    patients: List<Patient>,
    defaultPatientId: String?,
    onDismiss: () -> Unit,
    onConfirm: (ConsultationRequest) -> Unit
) {
    var selectedPatientId by rememberSaveable { mutableStateOf(defaultPatientId ?: patients.firstOrNull()?.id ?: "") }
    var date by rememberSaveable { mutableStateOf("2026-10-15") }
    var motif by rememberSaveable { mutableStateOf("") }
    var diagnostic by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val selectedPatientName = patients.find { it.id == selectedPatientId }?.let { "${it.prenom} ${it.nom}" } ?: "Sélectionner un patient"
    val formulaireValide = selectedPatientId.isNotBlank() && motif.isNotBlank() && date.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle consultation") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Dropdown pour choisir le patient
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedPatientName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Patient *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        patients.forEach { patient ->
                            DropdownMenuItem(
                                text = { Text("${patient.prenom} ${patient.nom}") },
                                onClick = {
                                    selectedPatientId = patient.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (ex: 2026-10-15) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = motif,
                    onValueChange = { motif = it },
                    label = { Text("Motif *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = diagnostic,
                    onValueChange = { diagnostic = it },
                    label = { Text("Diagnostic") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = formulaireValide,
                onClick = {
                    onConfirm(
                        ConsultationRequest(
                            patient = selectedPatientId,
                            date = date.trim(),
                            motif = motif.trim(),
                            diagnostic = diagnostic.trim().ifBlank { null },
                            notes = notes.trim().ifBlank { null }
                        )
                    )
                }
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}