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
import au.com.swagritech.opsapp.model.ActiveJobResponse

@Composable
fun FlightLogScreen(
    loading: Boolean,
    message: String,
    activeJob: ActiveJobResponse?,
    onLoadActiveJob: () -> Unit,
    onBack: () -> Unit
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
            Text("Flight Log", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = onLoadActiveJob, enabled = !loading) { Text("Load Active Job") }

            if (!activeJob?.DisplayJobId.isNullOrBlank()) {
                Text("Job: ${activeJob?.DisplayJobId}")
                Text("Property: ${activeJob?.LocationProperty ?: "-"}")
                Text("Aircraft: ${activeJob?.AircraftType ?: "-"}")
            }

            if (message.isNotBlank()) {
                Text(message)
            }

            Button(onClick = onBack) { Text("Back") }
        }
    }
}
