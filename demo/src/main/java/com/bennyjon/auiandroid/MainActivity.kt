package com.bennyjon.auiandroid

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bennyjon.aui.compose.theme.AuiTheme
import com.bennyjon.aui.core.AuiCatalogPrompt
import com.bennyjon.auiandroid.plugins.DemoPluginRegistry
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

            if (themeKey == "plugins") {
                val context = LocalContext.current
                val pluginRegistry = DemoPluginRegistry.create(context)

                LaunchedEffect(Unit) {
                    val prompt = AuiCatalogPrompt.generate(pluginRegistry)
                    Log.d("AuiDemo", "AuiCatalogPrompt with plugins:\n$prompt")
                }

                val vm: DemoViewModel = viewModel(
                    key = "plugins",
                    factory = DemoViewModelFactory(DemoViewModel.PLUGIN_SEQUENCE),
                )

                ChatScreen(
                    viewModel = vm,
                    title = "Plugin Showcase",
                    auiTheme = AuiTheme.fromMaterialTheme(),
                    pluginRegistry = pluginRegistry,
                    onBack = { navController.popBackStack() },
                )
            } else {
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
}

@Composable
private fun resolveTheme(key: String): Pair<String, AuiTheme> = when (key) {
    "dark_neon" -> "Dark Neon" to DemoThemes.darkNeon()
    "warm_organic" -> "Warm Organic" to DemoThemes.warmOrganic()
    else -> "Default" to AuiTheme.fromMaterialTheme()
}
