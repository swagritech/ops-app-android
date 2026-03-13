package au.com.swagritech.opsapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StartJobScreen(
    loading: Boolean,
    message: String,
    onSubmit: (String, String, String, String, String) -> Unit,
    onBack: () -> Unit
) {
    var client by remember { mutableStateOf("") }
    var property by remember { mutableStateOf("") }
    var block by remember { mutableStateOf("") }
    var aircraft by remember { mutableStateOf("T50") }
    var operation by remember { mutableStateOf("Spraying") }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Start Job", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(value = client, onValueChange = { client = it }, label = { Text("Client") })
            OutlinedTextField(value = property, onValueChange = { property = it }, label = { Text("Property") })
            OutlinedTextField(value = block, onValueChange = { block = it }, label = { Text("Block") })
            OutlinedTextField(value = aircraft, onValueChange = { aircraft = it }, label = { Text("Aircraft Type") })
            OutlinedTextField(value = operation, onValueChange = { operation = it }, label = { Text("Operation") })

            Button(
                onClick = { onSubmit(client, property, block, aircraft, operation) },
                enabled = !loading
            ) { Text("Create Job") }
            Button(onClick = onBack) { Text("Back") }

            if (message.isNotBlank()) {
                Text(message)
            }
        }
    }
}
