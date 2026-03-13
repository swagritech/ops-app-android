package au.com.swagritech.opsapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DashboardScreen(
    pilotName: String,
    onStartJob: () -> Unit,
    onFlightLog: () -> Unit,
    onReports: () -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Ops Dashboard", style = MaterialTheme.typography.headlineMedium)
            if (pilotName.isNotBlank()) {
                Text("Pilot: $pilotName")
            }
            Button(onClick = onStartJob) { Text("Start Job") }
            Button(onClick = onFlightLog) { Text("Flight Log") }
            Button(onClick = onReports) { Text("Reports") }
        }
    }
}
