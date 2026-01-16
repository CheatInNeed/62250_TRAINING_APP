package com.example.gymlocker.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymlocker.viewmodel.AuthViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun RegisterScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val uiState by authViewModel.uiState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Auto-login after successful register (session becomes logged in)
    LaunchedEffect(Unit) {
        authViewModel.isLoggedIn.collectLatest { loggedIn ->
            if (loggedIn) {
                navController.navigate("workout") {
                    popUpTo("register") { inclusive = true }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Create account",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                authViewModel.clearError()
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") },
            singleLine = true,
            // Inputs: surfaceVariant container + onSurface text + outline/primary borders
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                authViewModel.clearError()
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password (min 6)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        Spacer(Modifier.height(12.dp))

        uiState.error?.let { err ->
            Text(
                text = err,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
        }

        val emailOk = email.contains("@") && email.contains(".")
        val passOk = password.length >= 6

        Button(
            onClick = { authViewModel.register(email, password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading && emailOk && passOk,
            // CTA: primary/onPrimary
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Creating...",
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = "Create account",
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth(),
            // Supporting action: primary text on the screen layer
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Back to login")
        }
    }
}
