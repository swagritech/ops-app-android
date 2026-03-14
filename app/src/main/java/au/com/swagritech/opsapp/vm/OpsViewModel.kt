package au.com.swagritech.opsapp.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import au.com.swagritech.opsapp.model.ActiveJobResponse
import au.com.swagritech.opsapp.model.StartJobRequest
import au.com.swagritech.opsapp.repo.OpsRepository
import kotlinx.coroutines.launch

data class UiState(
    val loading: Boolean = false,
    val message: String = "",
    val identityVerified: Boolean = false,
    val microsoftSignedIn: Boolean = false,
    val signedInUsername: String = "",
    val currentPilot: String = "",
    val activeJob: ActiveJobResponse? = null
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
        uiState = uiState.copy(
            microsoftSignedIn = true,
            signedInUsername = username.orEmpty(),
            currentPilot = if (uiState.currentPilot.isBlank()) (username ?: "") else uiState.currentPilot,
            message = "Microsoft sign-in successful"
        )
    }

    fun setMicrosoftSignInError(message: String) {
        uiState = uiState.copy(microsoftSignedIn = false, message = message)
    }

    fun setMessage(message: String) {
        uiState = uiState.copy(message = message)
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
