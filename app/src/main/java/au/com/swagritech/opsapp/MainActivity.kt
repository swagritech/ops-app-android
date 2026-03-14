package au.com.swagritech.opsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import au.com.swagritech.opsapp.ui.SwatOpsApp
import au.com.swagritech.opsapp.ui.theme.SwatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SwatTheme {
                SwatOpsApp(activity = this)
            }
        }
    }
}
