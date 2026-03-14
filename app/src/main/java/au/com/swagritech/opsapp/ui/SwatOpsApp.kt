package au.com.swagritech.opsapp.ui

import android.app.Activity
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import au.com.swagritech.opsapp.auth.AuthManager
import au.com.swagritech.opsapp.auth.AuthSession
import au.com.swagritech.opsapp.ui.screens.DashboardScreen
import au.com.swagritech.opsapp.ui.screens.FlightLogScreen
import au.com.swagritech.opsapp.ui.screens.ReportsScreen
import au.com.swagritech.opsapp.ui.screens.StartJobScreen
import au.com.swagritech.opsapp.vm.OpsViewModel
import au.com.swagritech.opsapp.vm.OpsViewModelFactory

private object Routes {
    const val Login = "login"
    const val Dashboard = "dashboard"
    const val StartJob = "start_job"
    const val FlightLog = "flight_log"
    const val Reports = "reports"
}

@Composable
fun SwatOpsApp(activity: Activity, navController: NavHostController = rememberNavController()) {
    val vm: OpsViewModel = viewModel(factory = OpsViewModelFactory())
    val authManager = remember { AuthManager(activity.applicationContext) }

    LaunchedEffect(Unit) {
        authManager.restoreFromStore()
        authManager.refreshIfNeeded()
        vm.refreshQueueCount(activity.applicationContext)
        if (!AuthSession.accessToken.isNullOrBlank()) {
            val sessionReady = authManager.ensureApiSession().getOrDefault(false)
            if (sessionReady) {
                vm.setMicrosoftSignedIn(AuthSession.username ?: "Microsoft User")
            } else {
                vm.setMicrosoftSignInError(
                    AuthSession.lastError
                        ?: "Microsoft sign-in found, but API session is missing. Sign in with Microsoft again."
                )
            }
        } else if (!AuthSession.lastError.isNullOrBlank()) {
            vm.setMicrosoftSignInError(AuthSession.lastError ?: "Sign-in failed")
        }
    }

    NavHost(navController = navController, startDestination = Routes.Login) {
        composable(Routes.Login) {
            LoginScreen(
                loading = vm.uiState.loading,
                message = vm.uiState.message,
                signedInUsername = vm.uiState.signedInUsername,
                microsoftSignedIn = vm.uiState.microsoftSignedIn,
                currentPilot = vm.uiState.currentPilot,
                onMicrosoftSignIn = {
                    vm.setMessage("")
                    authManager.startSignIn(activity)
                },
                onVerifyIdentity = { vm.verifyIdentity() },
                onPilotChange = { vm.setPilotName(it) },
                onContinue = {
                    if (vm.uiState.microsoftSignedIn || vm.uiState.identityVerified) {
                        navController.navigate(Routes.Dashboard)
                    } else {
                        vm.setMessage("Sign in with Microsoft first")
                    }
                }
            )
        }
        composable(Routes.Dashboard) {
            DashboardScreen(
                pilotName = vm.uiState.currentPilot,
                onStartJob = { navController.navigate(Routes.StartJob) },
                onFlightLog = { navController.navigate(Routes.FlightLog) },
                onReports = { navController.navigate(Routes.Reports) },
                onSignOut = {
                    authManager.signOut()
                    vm.resetAuthState(activity.applicationContext)
                    navController.navigate(Routes.Login) {
                        popUpTo(0)
                    }
                }
            )
        }
        composable(Routes.StartJob) {
            StartJobScreen(
                loading = vm.uiState.loading,
                message = vm.uiState.message,
                onSubmit = { c, p, b, a, o -> vm.createJob(c, p, b, a, o) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.FlightLog) {
            FlightLogScreen(
                loading = vm.uiState.loading,
                message = vm.uiState.message,
                activeJob = vm.uiState.activeJob,
                queueCount = vm.uiState.offlineQueueCount,
                onLoadActiveJob = { vm.loadActiveJob() },
                onSubmitFlight = { id, type, battery, op, tko, lnd, notes, lat, lon ->
                    vm.submitFlight(
                        context = activity.applicationContext,
                        aircraftIdentifier = id,
                        aircraftType = type,
                        batteryId = battery,
                        operationType = op,
                        takeoffTimeUtc = tko,
                        landingTimeUtc = lnd,
                        notes = notes,
                        latitudeText = lat,
                        longitudeText = lon
                    )
                },
                onSyncQueue = { vm.syncQueuedFlights(activity.applicationContext) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.Reports) {
            ReportsScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun LoginScreen(
    loading: Boolean,
    message: String,
    signedInUsername: String,
    microsoftSignedIn: Boolean,
    currentPilot: String,
    onPilotChange: (String) -> Unit,
    onMicrosoftSignIn: () -> Unit,
    onVerifyIdentity: () -> Unit,
    onContinue: () -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Southwest Agri-Tech", style = MaterialTheme.typography.headlineMedium)
            Text("ReOC Operations Platform", style = MaterialTheme.typography.bodyLarge)

            Button(onClick = onMicrosoftSignIn, modifier = Modifier.padding(top = 16.dp), enabled = !loading) {
                Text(if (microsoftSignedIn) "Microsoft Signed In" else "Sign in with Microsoft")
            }

            if (signedInUsername.isNotBlank()) {
                Text("Signed in: $signedInUsername", modifier = Modifier.padding(top = 8.dp))
            }

            OutlinedTextField(
                value = currentPilot,
                onValueChange = {
                    onPilotChange(it)
                },
                label = { Text("Pilot Name") },
                modifier = Modifier.padding(top = 16.dp)
            )

            Button(onClick = onVerifyIdentity, modifier = Modifier.padding(top = 16.dp), enabled = !loading) {
                Text("Verify API Identity")
            }

            Button(onClick = onContinue, modifier = Modifier.padding(top = 8.dp), enabled = !loading) {
                Text("Continue")
            }

            if (message.isNotBlank()) {
                Text(message, modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}
