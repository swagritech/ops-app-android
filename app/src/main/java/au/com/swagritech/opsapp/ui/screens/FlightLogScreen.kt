package au.com.swagritech.opsapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import au.com.swagritech.opsapp.model.ActiveJobResponse
import com.google.android.gms.location.LocationServices

@Composable
fun FlightLogScreen(
    loading: Boolean,
    message: String,
    activeJob: ActiveJobResponse?,
    queueCount: Int,
    onLoadActiveJob: () -> Unit,
    onSubmitFlight: (String, String, String, String, String, String, String, String, String) -> Unit,
    onSyncQueue: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var aircraftIdentifier by remember { mutableStateOf("") }
    var aircraftType by remember { mutableStateOf("T50") }
    var batteryId by remember { mutableStateOf("A01") }
    var operationType by remember { mutableStateOf("Spraying") }
    var takeoffUtc by remember { mutableStateOf("2026-03-14T00:00:00Z") }
    var landingUtc by remember { mutableStateOf("2026-03-14T00:05:00Z") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var locationStatus by remember { mutableStateOf("") }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    fun loadCurrentGps() {
        locationStatus = "Reading GPS..."
        fusedLocationClient.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    latitude = "%.6f".format(loc.latitude)
                    longitude = "%.6f".format(loc.longitude)
                    locationStatus = "GPS updated"
                } else {
                    locationStatus = "No recent GPS fix yet"
                }
            }
            .addOnFailureListener {
                locationStatus = "GPS read failed"
            }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            loadCurrentGps()
        } else {
            locationStatus = "Location permission denied"
        }
    }

    LaunchedEffect(activeJob?.AircraftType) {
        if (!activeJob?.AircraftType.isNullOrBlank() && aircraftType.isBlank()) {
            aircraftType = activeJob?.AircraftType ?: aircraftType
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Flight Log", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = onLoadActiveJob, enabled = !loading) { Text("Load Active Job") }

            val job = activeJob
            if (!job?.DisplayJobId.isNullOrBlank()) {
                Text("Job: ${job?.DisplayJobId}")
                Text("Property: ${job?.LocationProperty ?: "-"}")
                Text("Aircraft Type (job): ${job?.AircraftType ?: "-"}")
            }

            Text("Offline queue: $queueCount")

            OutlinedTextField(value = aircraftIdentifier, onValueChange = { aircraftIdentifier = it }, label = { Text("Aircraft Identifier") })
            OutlinedTextField(value = aircraftType, onValueChange = { aircraftType = it }, label = { Text("Aircraft Type") })
            OutlinedTextField(value = batteryId, onValueChange = { batteryId = it }, label = { Text("Battery ID") })
            OutlinedTextField(value = operationType, onValueChange = { operationType = it }, label = { Text("Operation Type") })
            OutlinedTextField(value = takeoffUtc, onValueChange = { takeoffUtc = it }, label = { Text("Takeoff UTC (ISO)") })
            OutlinedTextField(value = landingUtc, onValueChange = { landingUtc = it }, label = { Text("Landing UTC (ISO)") })

            Button(
                onClick = {
                    val hasPerm = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPerm) {
                        loadCurrentGps()
                    } else {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                enabled = !loading
            ) {
                Text("Use Current GPS")
            }

            OutlinedTextField(value = latitude, onValueChange = { latitude = it }, label = { Text("Latitude") })
            OutlinedTextField(value = longitude, onValueChange = { longitude = it }, label = { Text("Longitude") })
            if (locationStatus.isNotBlank()) {
                Text(locationStatus)
            }

            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") })

            Button(
                onClick = {
                    onSubmitFlight(
                        aircraftIdentifier.trim(),
                        aircraftType.trim(),
                        batteryId.trim(),
                        operationType.trim(),
                        takeoffUtc.trim(),
                        landingUtc.trim(),
                        notes,
                        latitude.trim(),
                        longitude.trim()
                    )
                },
                enabled = !loading
            ) { Text("Submit Flight") }

            Button(onClick = onSyncQueue, enabled = !loading) { Text("Sync Queued Flights") }

            if (message.isNotBlank()) {
                Text(message)
            }

            Button(onClick = onBack) { Text("Back") }
        }
    }
}
