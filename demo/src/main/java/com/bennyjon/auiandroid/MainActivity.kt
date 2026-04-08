package com.bennyjon.auiandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bennyjon.aui.compose.theme.AuiTheme
import com.bennyjon.auiandroid.ui.theme.AUIAndroidTheme
import com.bennyjon.auiandroid.ui.theme.DemoThemes

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AUIAndroidTheme {
                DemoNavHost()
            }
        }
    }
}

@Composable
private fun DemoNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            DemoHomeScreen(
                onThemeSelected = { themeKey -> navController.navigate("chat/$themeKey") },
            )
        }
        composable("chat/{theme}") { backStackEntry ->
            val themeKey = backStackEntry.arguments?.getString("theme") ?: "default"
            val (title, auiTheme) = resolveTheme(themeKey)
            val vm: DemoViewModel = viewModel()

            ChatScreen(
                viewModel = vm,
                title = title,
                auiTheme = auiTheme,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

private fun resolveTheme(key: String): Pair<String, AuiTheme> = when (key) {
    "dark_neon" -> "Dark Neon" to DemoThemes.DarkNeon
    "warm_organic" -> "Warm Organic" to DemoThemes.WarmOrganic
    else -> "Default" to AuiTheme.Default
}
