package com.example.patients.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Accueil", Icons.Default.Home)
    object Patients : Screen("patients", "Patients", Icons.Default.Person)
    object Consultations : Screen("consultations", "Consultations", Icons.Default.DateRange)
    object Settings : Screen("settings", "Paramètres", Icons.Default.Settings)
}