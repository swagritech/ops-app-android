package au.com.swagritech.opsapp.vm

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import au.com.swagritech.opsapp.data.FlightQueueStore
import au.com.swagritech.opsapp.model.ActiveJobResponse
import au.com.swagritech.opsapp.model.CreateFlightRequest
import au.com.swagritech.opsapp.model.QueuedFlightItem
import au.com.swagritech.opsapp.model.StartJobRequest
import au.com.swagritech.opsapp.repo.ApiHttpException
import au.com.swagritech.opsapp.repo.OpsRepository
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.launch

data class UiState(
    val loading: Boolean = false,
    val message: String = "",
    val identityVerified: Boolean = false,
    val microsoftSignedIn: Boolean = false,
    val signedInUsername: String = "",
    val currentPilot: String = "",
    val activeJob: ActiveJobResponse? = null,
    val offlineQueueCount: Int = 0
)

class OpsViewModel(
    private val repository: OpsRepository = OpsRepository()
) : ViewModel() {
    var uiState by mutableStateOf(UiState())
        private set

    fun setPilotName(name: String) {
        uiState = uiState.copy(currentPilot = name)
    }

    fun setMicrosoftSignedIn(username: String?) {
        val resolvedPilot = resolvePilotDisplayName(username)
        uiState = uiState.copy(
            microsoftSignedIn = true,
            signedInUsername = username.orEmpty(),
            currentPilot = if (uiState.currentPilot.isBlank()) resolvedPilot else uiState.currentPilot,
            message = "Microsoft sign-in successful"
        )
    }

    fun setMicrosoftSignInError(message: String) {
        uiState = uiState.copy(microsoftSignedIn = false, message = message)
    }

    fun setMessage(message: String) {
        uiState = uiState.copy(message = message)
    }

    fun refreshQueueCount(context: Context) {
        uiState = uiState.copy(offlineQueueCount = FlightQueueStore.count(context))
    }

    fun resetAuthState(context: Context) {
        FlightQueueStore.clear(context)
        uiState = uiState.copy(
            identityVerified = false,
            microsoftSignedIn = false,
            signedInUsername = "",
            activeJob = null,
            offlineQueueCount = 0,
            message = "Signed out"
        )
    }

    fun verifyIdentity() {
        viewModelScope.launch {
            uiState = uiState.copy(loading = true, message = "Checking identity...")
            repository.verifyIdentity()
                .onSuccess {
                    val ok = it.authenticated == true
                    uiState = uiState.copy(
                        loading = false,
                        identityVerified = ok,
                        message = if (ok) "Identity verified" else "Not verified yet"
                    )
                }
                .onFailure {
                    uiState = uiState.copy(loading = false, message = "Identity check failed: ${it.message}")
                }
        }
    }

    fun createJob(client: String, property: String, block: String, aircraftType: String, operationType: String) {
        val pilot = uiState.currentPilot.trim()
        if (pilot.isBlank()) {
            uiState = uiState.copy(message = "Enter pilot name first")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(loading = true, message = "Creating job...")
            repository.startJob(
                StartJobRequest(
                    ClientName = client,
                    LocationProperty = property,
                    SiteBlockId = block,
                    AircraftType = aircraftType,
                    OperationType = operationType,
                    Pilot = pilot,
                    ExpectedDate = null,
                    Notes = null
                )
            ).onSuccess { res ->
                val msg = if (res.status == "ok") {
                    "Job created: ${res.DisplayJobId ?: res.JobId ?: "OK"}"
                } else {
                    "Start job failed: ${res.error ?: "Unknown"}"
                }
                uiState = uiState.copy(loading = false, message = msg)
            }.onFailure {
                uiState = uiState.copy(loading = false, message = "Start job error: ${it.message}")
            }
        }
    }

    fun loadActiveJob() {
        val pilot = uiState.currentPilot.trim()
        if (pilot.isBlank()) {
            uiState = uiState.copy(message = "Enter pilot name first")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(loading = true, message = "Loading active job...")
            repository.getActiveJob(pilot)
                .onSuccess { res ->
                    val msg = if (res.status == "ok" && !res.DisplayJobId.isNullOrBlank()) {
                        "Active job loaded"
                    } else {
                        res.error ?: "No active job"
                    }
                    uiState = uiState.copy(loading = false, activeJob = res, message = msg)
                }
                .onFailure {
                    uiState = uiState.copy(loading = false, message = "Load job error: ${it.message}")
                }
        }
    }

    fun submitFlight(
        context: Context,
        aircraftIdentifier: String,
        aircraftType: String,
        batteryId: String,
        operationType: String,
        takeoffTimeUtc: String,
        landingTimeUtc: String,
        notes: String,
        latitudeText: String,
        longitudeText: String
    ) {
        val pilot = uiState.currentPilot.trim()
        if (pilot.isBlank()) {
            uiState = uiState.copy(message = "Pilot name is required")
            return
        }

        val job = uiState.activeJob
        if (job?.JobId.isNullOrBlank()) {
            uiState = uiState.copy(message = "Load an active job before logging flight")
            return
        }

        val minutes = calculateMinutes(takeoffTimeUtc, landingTimeUtc)
        if (minutes <= 0) {
            uiState = uiState.copy(message = "Invalid takeoff/landing UTC time")
            return
        }

        val latitude = latitudeText.trim().takeIf { it.isNotBlank() }?.toDoubleOrNull()
        val longitude = longitudeText.trim().takeIf { it.isNotBlank() }?.toDoubleOrNull()
        if (latitudeText.isNotBlank() && latitude == null) {
            uiState = uiState.copy(message = "Latitude must be numeric")
            return
        }
        if (longitudeText.isNotBlank() && longitude == null) {
            uiState = uiState.copy(message = "Longitude must be numeric")
            return
        }
        if (latitude != null && (latitude < -90.0 || latitude > 90.0)) {
            uiState = uiState.copy(message = "Latitude out of range (-90 to 90)")
            return
        }
        if (longitude != null && (longitude < -180.0 || longitude > 180.0)) {
            uiState = uiState.copy(message = "Longitude out of range (-180 to 180)")
            return
        }

        val payload = CreateFlightRequest(
            JobId = job?.JobId,
            Pilot = pilot,
            AircraftIdentifier = aircraftIdentifier.trim(),
            AircraftType = aircraftType.trim(),
            LocationProperty = job?.LocationProperty ?: "",
            SiteBlockId = job?.SiteBlockId,
            OperationType = operationType.trim(),
            BatteryId = batteryId.trim(),
            TakeoffTimeUtc = takeoffTimeUtc.trim(),
            LandingTimeUtc = landingTimeUtc.trim(),
            FlightMinutes = minutes,
            FlightDateTimeUtc = takeoffTimeUtc.trim(),
            Notes = notes.trim().ifBlank { null },
            OfflineClientId = UUID.randomUUID().toString(),
            Latitude = latitude,
            Longitude = longitude
        )

        viewModelScope.launch {
            uiState = uiState.copy(loading = true, message = "Submitting flight...")
            val result = repository.createFlight(payload)
            result.onSuccess { response ->
                if (response.status == "flight logged" || response.status == "duplicate") {
                    uiState = uiState.copy(loading = false, message = "Flight logged")
                } else {
                    uiState = uiState.copy(loading = false, message = response.error ?: response.message ?: "Unexpected response")
                }
            }.onFailure { error ->
                if (shouldQueue(error)) {
                    queueFlight(context, payload)
                    refreshQueueCount(context)
                    uiState = uiState.copy(
                        loading = false,
                        message = "Offline/network issue. Flight queued for sync (${uiState.offlineQueueCount} queued)."
                    )
                } else {
                    uiState = uiState.copy(loading = false, message = "Flight submit failed: ${error.message}")
                }
            }
        }
    }

    fun syncQueuedFlights(context: Context) {
        val queued = FlightQueueStore.getAll(context)
        if (queued.isEmpty()) {
            uiState = uiState.copy(message = "No queued flights to sync", offlineQueueCount = 0)
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(loading = true, message = "Syncing ${queued.size} queued flights...")
            var sent = 0
            queued.forEach { item ->
                val result = repository.createFlight(item.payload)
                val remove = result.isSuccess || isDuplicate(result.exceptionOrNull())
                if (remove) {
                    FlightQueueStore.removeById(context, item.localQueueId)
                    sent += 1
                }
            }
            refreshQueueCount(context)
            uiState = uiState.copy(
                loading = false,
                message = "Synced $sent flight(s). ${uiState.offlineQueueCount} still queued."
            )
        }
    }

    private fun queueFlight(context: Context, payload: CreateFlightRequest) {
        val item = QueuedFlightItem(
            localQueueId = UUID.randomUUID().toString(),
            payload = payload,
            queuedAtUtc = Instant.now().toString()
        )
        FlightQueueStore.enqueue(context, item)
    }

    private fun shouldQueue(error: Throwable): Boolean {
        return when (error) {
            is IOException -> true
            is ApiHttpException -> error.code >= 500
            else -> false
        }
    }

    private fun isDuplicate(error: Throwable?): Boolean {
        return (error as? ApiHttpException)?.code == 409
    }

    private fun calculateMinutes(takeoffUtc: String, landingUtc: String): Int {
        return runCatching {
            val start = Instant.parse(takeoffUtc)
            val end = Instant.parse(landingUtc)
            val minutes = Duration.between(start, end).toMinutes()
            minutes.toInt()
        }.getOrDefault(0)
    }

    private fun resolvePilotDisplayName(identity: String?): String {
        val raw = identity?.trim().orEmpty()
        if (raw.isBlank()) return ""

        val explicitMap = mapOf(
            "sean@swagritech.com.au" to "Sean Maynard",
            "james@jhviticulture.com.au" to "James Harris"
        )
        val lower = raw.lowercase()
        explicitMap[lower]?.let { return it }

        // If no mapping exists, keep non-email names as-is, otherwise use local-part title case.
        if (!raw.contains("@")) return raw
        val local = raw.substringBefore("@")
        return local
            .split('.', '_', '-')
            .filter { it.isNotBlank() }
            .joinToString(" ") { token ->
                token.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }
}

class OpsViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OpsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OpsViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
