package com.example.patients.ui.patients


import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.patients.PatientsApplication
import com.example.patients.data.model.Patient
import com.example.patients.data.model.PatientRequest
import com.example.patients.ui.viewmodel.ConsultationsScreen
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientsScreen(
    viewModel: PatientViewModel = viewModel(factory = PatientViewModel.Factory),
    openDialogOnStart: Boolean = false,
    onDialogOpened: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val statutFiltre by viewModel.statutFiltre.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(openDialogOnStart) }
    var selectedPatient by remember { mutableStateOf<Patient?>(null) }
    var patientForConsultations by remember { mutableStateOf<Patient?>(null) }
    var patientToDelete by remember { mutableStateOf<Patient?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as PatientsApplication

    LaunchedEffect(openDialogOnStart) {
        if (openDialogOnStart) {
            showAddDialog = true
            onDialogOpened()
        }
    }

    // ... (le reste de PatientsScreen reste inchangé)

    if (patientForConsultations != null) {
        ConsultationsScreen(
            patientId = patientForConsultations!!.id,
            repository = app.container.patientRepository,
            onBack = { patientForConsultations = null }
        )
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Gestion des Patients") },
                    actions = {
                        IconButton(onClick = { viewModel.loadPatients(isRefresh = true) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Actualiser")
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        selectedPatient = null
                        showAddDialog = true
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Nouveau patient") }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (val state = uiState) {
                    is PatientsUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is PatientsUiState.Error -> {
                        ErrorContent(
                            message = state.message,
                            onRetry = { viewModel.loadPatients() },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is PatientsUiState.Success -> {
                        val filteredPatients = state.patients.filter {
                            it.nom.contains(searchQuery, ignoreCase = true) ||
                                    it.prenom.contains(searchQuery, ignoreCase = true)
                        }

                        PatientList(
                            patients = filteredPatients,
                            currentStatut = statutFiltre,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            onStatutChange = { viewModel.setStatutFiltre(it) },
                            isRefreshing = isRefreshing,
                            onRefresh = { viewModel.loadPatients(isRefresh = true) },
                            onItemClick = { patient ->
                                selectedPatient = patient
                                showAddDialog = true
                            },
                            onConsultationsClick = { patient ->
                                patientForConsultations = patient
                            },
                            onDelete = { patient ->
                                patientToDelete = patient
                            }
                        )
                    }
                }

                if (showAddDialog) {
                    AddPatientDialog(
                        patient = selectedPatient,
                        onDismiss = { showAddDialog = false },
                        onConfirm = { request ->
                            if (selectedPatient == null) {
                                viewModel.addPatient(request)
                            } else {
                                viewModel.updatePatient(selectedPatient!!.id, request)
                            }
                            showAddDialog = false
                        }
                    )
                }

                if (patientToDelete != null) {
                    AlertDialog(
                        onDismissRequest = { patientToDelete = null },
                        title = { Text("Confirmer la suppression") },
                        text = { Text("Voulez-vous vraiment supprimer ${patientToDelete?.prenom} ${patientToDelete?.nom} ?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.deletePatient(patientToDelete!!)
                                    patientToDelete = null
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Supprimer")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { patientToDelete = null }) {
                                Text("Annuler")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorContent(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry) {
            Text("Réessayer")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientList(
    patients: List<Patient>,
    currentStatut: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onStatutChange: (String?) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onItemClick: (Patient) -> Unit,
    onConsultationsClick: (Patient) -> Unit,
    onDelete: (Patient) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Barre de recherche
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Rechercher un patient...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Effacer")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.medium
            )

            // Filtres par statut
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = currentStatut == null,
                    onClick = { onStatutChange(null) },
                    label = { Text("Tous") }
                )
                FilterChip(
                    selected = currentStatut == "Actif",
                    onClick = { onStatutChange("Actif") },
                    label = { Text("Actif") }
                )
                FilterChip(
                    selected = currentStatut == "Inactif",
                    onClick = { onStatutChange("Inactif") },
                    label = { Text("Inactif") }
                )
            }

            if (patients.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Aucun patient trouvé",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(patients, key = { it.id }) { patient ->
                        PatientCard(
                            patient = patient,
                            onClick = { onItemClick(patient) },
                            onConsultationsClick = { onConsultationsClick(patient) },
                            onDelete = { onDelete(patient) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PatientCard(
    patient: Patient,
    onClick: () -> Unit,
    onConsultationsClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${patient.prenom} ${patient.nom}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = listOfNotNull(
                        patient.age?.let { "$it ans" },
                        patient.telephone?.let { "Tél: $it" },
                        patient.medecin,
                        patient.statut
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = onConsultationsClick) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Consultations",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Supprimer",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPatientDialog(
    patient: Patient? = null,
    onDismiss: () -> Unit,
    onConfirm: (PatientRequest) -> Unit
) {
    var nom by rememberSaveable(patient) { mutableStateOf(patient?.nom ?: "") }
    var prenom by rememberSaveable(patient) { mutableStateOf(patient?.prenom ?: "") }
    var age by rememberSaveable(patient) { mutableStateOf(patient?.age?.toString() ?: "") }
    var telephone by rememberSaveable(patient) { mutableStateOf(patient?.telephone ?: "") }
    var medecin by rememberSaveable(patient) { mutableStateOf(patient?.medecin ?: "") }
    var statut by rememberSaveable(patient) { mutableStateOf(patient?.statut ?: "Actif") }

    val formulaireValide = nom.isNotBlank() && prenom.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (patient == null) "Nouveau patient" else "Modifier le patient") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())

            ) {
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it },
                    label = { Text("Nom *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = prenom,
                    onValueChange = { prenom = it },
                    label = { Text("Prénom *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it.filter { c -> c.isDigit() } },
                    label = { Text("Âge") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = telephone,
                    onValueChange = { telephone = it },
                    label = { Text("Téléphone") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = medecin,
                    onValueChange = { medecin = it },
                    label = { Text("Médecin") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text("Statut du patient", style = MaterialTheme.typography.bodyMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = statut == "Actif",
                        onClick = { statut = "Actif" },
                        label = { Text("Actif") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = statut == "Inactif",
                        onClick = { statut = "Inactif" },
                        label = { Text("Inactif") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = formulaireValide,
                onClick = {
                    onConfirm(
                        PatientRequest(
                            nom = nom.trim(),
                            prenom = prenom.trim(),
                            age = age.toIntOrNull(),
                            telephone = telephone.trim().ifBlank { null },
                            medecin = medecin.trim().ifBlank { null },
                            statut = statut
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
