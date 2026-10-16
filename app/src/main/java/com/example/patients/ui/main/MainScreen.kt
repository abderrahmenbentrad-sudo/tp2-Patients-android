package com.example.patients.ui.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.patients.ui.home.HomeScreen
import com.example.patients.ui.navigation.Screen
import com.example.patients.ui.patients.PatientsScreen
import com.example.patients.ui.patients.PatientViewModel
import com.example.patients.ui.settings.SettingsScreen

@Composable
fun MainScreen() {
    val viewModel: PatientViewModel = viewModel(factory = PatientViewModel.Factory)
    var currentRoute by remember { mutableStateOf(Screen.Home.route) }
    var openAddDialogOnPatients by remember { mutableStateOf(false) }

    val items = listOf(
        Screen.Home,
        Screen.Patients,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = { currentRoute = screen.route }
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentRoute) {
                Screen.Home.route -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToPatients = { currentRoute = Screen.Patients.route },
                    onAddPatient = {
                        openAddDialogOnPatients = true
                        currentRoute = Screen.Patients.route
                    }
                )
                Screen.Patients.route -> PatientsScreen(
                    viewModel = viewModel,
                    openDialogOnStart = openAddDialogOnPatients,
                    onDialogOpened = { openAddDialogOnPatients = false }
                )
                Screen.Settings.route -> SettingsScreen()
                else -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToPatients = { currentRoute = Screen.Patients.route },
                    onAddPatient = {
                        openAddDialogOnPatients = true
                        currentRoute = Screen.Patients.route
                    }
                )
            }
        }
    }
}