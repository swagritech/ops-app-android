package au.com.swagritech.opsapp.ui

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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import au.com.swagritech.opsapp.ui.screens.DashboardScreen
import au.com.swagritech.opsapp.ui.screens.FlightLogScreen
import au.com.swagritech.opsapp.ui.screens.ReportsScreen
import au.com.swagritech.opsapp.ui.screens.StartJobScreen

private object Routes {
    const val Login = "login"
    const val Dashboard = "dashboard"
    const val StartJob = "start_job"
    const val FlightLog = "flight_log"
    const val Reports = "reports"
}

@Composable
fun SwatOpsApp(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.Login) {
        composable(Routes.Login) {
            LoginScreen(onContinue = { navController.navigate(Routes.Dashboard) })
        }
        composable(Routes.Dashboard) {
            DashboardScreen(
                onStartJob = { navController.navigate(Routes.StartJob) },
                onFlightLog = { navController.navigate(Routes.FlightLog) },
                onReports = { navController.navigate(Routes.Reports) }
            )
        }
        composable(Routes.StartJob) {
            StartJobScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.FlightLog) {
            FlightLogScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.Reports) {
            ReportsScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun LoginScreen(onContinue: () -> Unit) {
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
            Button(onClick = onContinue, modifier = Modifier.padding(top = 24.dp)) {
                Text("Continue")
            }
        }
    }
}
